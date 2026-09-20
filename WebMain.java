import model.*;
import factory.ComplaintFactory;
import singleton.ComplaintManager;
import observer.StudentObserver;
import proxy.AdminProxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// A tiny local web server for the Hostel Complaint Management System.
// Uses only the built-in JDK HTTP server (com.sun.net.httpserver) --
// no Spring, no extra libraries, no Maven dependencies.
// It calls the SAME pattern classes as Main.java / MainGUI.java, so
// Singleton, Factory Method, Chain of Responsibility, Observer and
// Proxy all still run exactly as before.
public class WebMain {

    // SINGLETON: same single manager instance used everywhere.
    private static ComplaintManager manager = ComplaintManager.getInstance();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", WebMain::handleHome);
        server.createContext("/submit", WebMain::handleSubmit);
        server.createContext("/view", WebMain::handleView);
        server.createContext("/admin", WebMain::handleAdmin);
        server.createContext("/escalate", WebMain::handleEscalate);

        server.setExecutor(null); // simple default executor
        server.start();

        System.out.println("Server started!");
        System.out.println("Open this in your browser: http://localhost:8080/");
    }

    // ---------- Home page: shows Submit form + Admin form + link to View ----------
    private static void handleHome(HttpExchange exchange) throws IOException {
        String html =
                page("Home",
                        "<h2>Submit a Complaint</h2>" +
                        "<form method='POST' action='/submit'>" +
                        "  <label>Student Name:</label><input type='text' name='name' required><br>" +
                        "  <label>Room Number:</label><input type='text' name='room' required><br>" +
                        "  <label>Complaint Type:</label>" +
                        "  <select name='type'>" +
                        "    <option value='1'>Electrical</option>" +
                        "    <option value='2'>Plumbing</option>" +
                        "    <option value='3'>Cleaning</option>" +
                        "    <option value='4'>Internet</option>" +
                        "    <option value='5'>Furniture</option>" +
                        "  </select><br>" +
                        "  <label>Description:</label><br>" +
                        "  <textarea name='description' rows='4' cols='40' required></textarea><br>" +
                        "  <button type='submit'>Submit Complaint</button>" +
                        "</form>" +
                        "<hr>" +
                        "<h2><a href='/view'>View All Complaints</a></h2>" +
                        "<h2><a href='/escalate?id=C001'>Escalate Complaint (Staff -> Warden -> Rector -> Admin)</a></h2>" +
                        "<hr>" +
                        "<h2>Admin: Update Complaint Status</h2>" +
                        "<form method='POST' action='/admin'>" +
                        "  <label>Admin Password:</label><input type='password' name='password' required><br>" +
                        "  <label>Complaint ID:</label><input type='text' name='id' required><br>" +
                        "  <label>New Status:</label>" +
                        "  <select name='status'>" +
                        "    <option value='IN_PROGRESS'>IN_PROGRESS</option>" +
                        "    <option value='RESOLVED'>RESOLVED</option>" +
                        "  </select><br>" +
                        "  <button type='submit'>Update Status</button>" +
                        "</form>"
                );
        sendHtml(exchange, html);
    }

    // ---------- POST /submit ----------
    private static void handleSubmit(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseFormData(exchange.getRequestBody());

        String name = form.getOrDefault("name", "").trim();
        String room = form.getOrDefault("room", "").trim();
        String description = form.getOrDefault("description", "").trim();
        int type = Integer.parseInt(form.getOrDefault("type", "0"));

        Student student = new Student(name, room);

        // FACTORY METHOD
        Complaint complaint = ComplaintFactory.createComplaint(type, student, description);

        // OBSERVER
        complaint.addObserver(new StudentObserver(student));

        // Capture the console-style output that submitComplaint() normally prints.
        String output = captureOutput(() -> manager.submitComplaint(complaint));

        String html = page("Complaint Submitted",
                "<pre>" + escape(output) + "</pre>" +
                "<p><a href='/'>Back to Home</a></p>");
        sendHtml(exchange, html);
    }

    // ---------- GET /view ----------
    private static void handleView(HttpExchange exchange) throws IOException {
        String output = captureOutput(() -> manager.viewAllComplaints());

        String html = page("All Complaints",
                "<pre>" + escape(output) + "</pre>" +
                "<p><a href='/'>Back to Home</a></p>");
        sendHtml(exchange, html);
    }

    // ---------- POST /admin ----------
    private static void handleAdmin(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseFormData(exchange.getRequestBody());

        String password = form.getOrDefault("password", "");
        String id = form.getOrDefault("id", "").trim();
        String statusText = form.getOrDefault("status", "IN_PROGRESS");
        ComplaintStatus newStatus = ComplaintStatus.valueOf(statusText);

        boolean isAdmin = password.equals("admin123");

        // PROXY: web layer always goes through AdminProxy, never RealAdminService directly.
        AdminProxy adminProxy = new AdminProxy(isAdmin);

        String output = captureOutput(() -> adminProxy.updateComplaintStatus(id, newStatus));

        String html = page("Admin Update",
                "<pre>" + escape(output) + "</pre>" +
                "<p><a href='/'>Back to Home</a></p>");
        sendHtml(exchange, html);
    }

    // ---------- GET /escalate ----------
    // CHAIN OF RESPONSIBILITY (escalation): Staff -> Warden -> Rector -> Admin.
    private static void handleEscalate(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String id = params.getOrDefault("id", "").trim();

        String output = captureOutput(() -> manager.escalateComplaint(id));

        String html = page("Escalation",
                "<pre>" + escape(output) + "</pre>" +
                "<p><a href='/'>Back to Home</a></p>");
        sendHtml(exchange, html);
    }

    // ---------- Helpers ----------

    // Parses "a=1&b=2" query strings into a Map.
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }

    // Wraps body content in a simple, consistently styled HTML page.
    private static String page(String title, String bodyContent) {
        return "<html><head><title>" + title + "</title>" +
                "<style>" +
                "body{font-family:Arial, sans-serif; margin:40px; background:#f5f5f5;}" +
                "form{background:#fff; padding:15px; border-radius:8px; max-width:400px; margin-bottom:20px;}" +
                "label{display:inline-block; width:130px; margin:6px 0;}" +
                "input,select,textarea{margin:6px 0;}" +
                "button{padding:6px 14px; margin-top:10px;}" +
                "pre{background:#fff; padding:15px; border-radius:8px;}" +
                "</style></head><body>" +
                "<h1>Hostel Complaint Management System</h1>" +
                bodyContent +
                "</body></html>";
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Reads "name=value&name2=value2" style POST bodies into a Map.
    private static Map<String, String> parseFormData(InputStream body) throws IOException {
        String raw = new String(body.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> result = new HashMap<>();
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    // Escapes HTML special characters so complaint text can't break the page.
    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // Same System.out capture trick used in MainGUI, so the same
    // pattern classes can be reused unchanged in a web context.
    private static String captureOutput(Runnable action) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }
}
