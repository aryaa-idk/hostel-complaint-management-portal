import model.*;
import factory.ComplaintFactory;
import singleton.ComplaintManager;
import observer.StudentObserver;
import proxy.AdminProxy;

import com.sun.net.httpserver.HttpExchange;
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
import java.util.List;
import java.util.Map;

// WEB FRONT-END for the Hostel Complaint Management System.
//
// ONE single webpage with TWO tabs (Student / Admin).
// Tabs are plain HTML + CSS only (radio buttons + labels) --
// no JavaScript, no frameworks.
//
// The tab layout is ONLY a UI organization mechanism:
// all design-pattern logic stays in the Java backend:
//   FACTORY METHOD   -> ComplaintFactory
//   SINGLETON        -> ComplaintManager
//   CHAIN OF RESP.   -> ComplaintHandler / EscalationChain (called by manager)
//   OBSERVER         -> StudentObserver (triggered by setStatus)
//   PROXY            -> AdminProxy (admin status updates)
public class WebMain {

    // SINGLETON: same single manager instance used everywhere.
    private static ComplaintManager manager = ComplaintManager.getInstance();

    public static void main(String[] args) throws IOException {
        // DEPLOYMENT: use the PORT environment variable when present (cloud
        // platforms inject it); fall back to 8080 when running locally.
        // Bind to 0.0.0.0 explicitly so cloud traffic is accepted.
        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server =
                HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/", WebMain::handleHome);       // GET: the one webpage
        server.createContext("/submit", WebMain::handleSubmit);   // POST: student submits
        server.createContext("/admin", WebMain::handleAdmin);     // POST: proxy status update
        server.createContext("/escalate", WebMain::handleEscalate); // POST: escalation chain

        server.setExecutor(null); // simple default executor
        server.start();

        System.out.println("Server started!");
        System.out.println("Open this in your browser: http://localhost:8080/");
    }

    // =================================================================
    // GET / -- THE one and only webpage (two CSS-only tabs)
    // =================================================================
    private static void handleHome(HttpExchange exchange) throws IOException {
        sendPage(exchange, "student", ""); // fresh visit: Student tab, no output
    }

    // ---------- POST /submit : STUDENT TAB ----------
    // Student -> Factory Method -> Singleton -> Chain -> ASSIGNED -> Observer
    private static void handleSubmit(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseFormData(exchange.getRequestBody());

        String name = form.getOrDefault("name", "").trim();
        String room = form.getOrDefault("room", "").trim();
        String description = form.getOrDefault("description", "").trim();
        int type = Integer.parseInt(form.getOrDefault("type", "0"));

        String output;
        if (name.isEmpty() || room.isEmpty() || description.isEmpty()) {
            output = "Please fill in all fields.";
        } else {
            Student student = new Student(name, room);

            // FACTORY METHOD: web layer never picks the subclass itself.
            Complaint complaint = ComplaintFactory.createComplaint(type, student, description);

            // OBSERVER: student is registered before the complaint is submitted.
            complaint.addObserver(new StudentObserver(student));

            // SINGLETON manager receives the complaint; it runs the
            // CHAIN OF RESPONSIBILITY (routes + assigns staff).
            output = captureOutput(() -> manager.submitComplaint(complaint));
        }

        sendPage(exchange, "student", output); // re-render the ONE page, Student tab
    }

    // ---------- POST /admin : ADMIN TAB (Update Status) ----------
    // Admin -> Proxy (role check) -> update status -> Observer notification
    private static void handleAdmin(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseFormData(exchange.getRequestBody());

        String password = form.getOrDefault("password", "");
        String id = form.getOrDefault("id", "").trim();
        ComplaintStatus newStatus =
                ComplaintStatus.valueOf(form.getOrDefault("status", "IN_PROGRESS"));

        boolean isAdmin = password.equals("admin123"); // demo role check

        // PROXY: web layer always goes through AdminProxy, never RealAdminService directly.
        AdminProxy adminProxy = new AdminProxy(isAdmin);

        String output = captureOutput(() -> adminProxy.updateComplaintStatus(id, newStatus));

        sendPage(exchange, "admin", output); // re-render the ONE page, Admin tab
    }

    // ---------- POST /escalate : ADMIN TAB (Escalation) ----------
    // Escalate -> Staff -> Warden -> Rector -> Admin (Chain of Responsibility)
    private static void handleEscalate(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseFormData(exchange.getRequestBody());
        String id = form.getOrDefault("id", "").trim();

        String output = captureOutput(() -> manager.escalateComplaint(id));

        sendPage(exchange, "admin", output);
    }

    // =================================================================
    // Renders THE one webpage with the requested tab active.
    // activeTab: "student" or "admin"; output: pattern log to display.
    // =================================================================
    private static void sendPage(HttpExchange exchange, String activeTab, String output)
            throws IOException {

        boolean adminActive = "admin".equals(activeTab);

        // Pre-check the correct radio button so the right tab is shown
        // after a form POST (pure CSS tabs -- the server decides which
        // radio is "checked", no JavaScript needed).
        String studentChecked = adminActive ? "" : " checked";
        String adminChecked = adminActive ? " checked" : "";

        String outputBlock = output.isEmpty()
                ? ""
                : "<div class='output'><h3>Pattern Output</h3><pre>" + escape(output) + "</pre></div>";

        String html =
                "<!DOCTYPE html><html><head><title>Hostel Complaint Management System</title>" +
                "<style>" +
                "body{font-family:Arial,sans-serif;background:#f0f2f5;margin:0;}" +
                ".wrap{max-width:760px;margin:30px auto;background:#fff;border-radius:8px;" +
                "  box-shadow:0 2px 8px rgba(0,0,0,.15);overflow:hidden;}" +
                "h1{text-align:center;background:#2c3e50;color:#fff;margin:0;padding:18px;}" +
                // --- CSS-only tabs ---
                ".tabs{padding:0 20px;}" +
                ".tabs input{display:none;}" +
                ".tabs label{display:inline-block;padding:12px 28px;cursor:pointer;" +
                "  background:#dfe3e8;border:1px solid #ccc;border-bottom:none;" +
                "  border-radius:8px 8px 0 0;font-weight:bold;color:#555;}" +
                "#tab-student:checked ~ label[for=tab-student]," +
                "#tab-admin:checked ~ label[for=tab-admin]{background:#2c3e50;color:#fff;}" +
                ".tabcontent{display:none;padding:20px;border:1px solid #ccc;}" +
                "#tab-student:checked ~ #content-student{display:block;}" +
                "#tab-admin:checked ~ #content-admin{display:block;}" +
                // --- forms & table ---
                "form{margin-bottom:25px;}" +
                "label.field{display:inline-block;width:130px;margin:6px 0;font-weight:bold;}" +
                "input[type=text],input[type=password],select,textarea{margin:6px 0;padding:6px;}" +
                "textarea{width:300px;}" +
                "button{padding:8px 18px;margin-top:8px;background:#2c3e50;color:#fff;" +
                "  border:none;border-radius:4px;cursor:pointer;}" +
                "button.escalate{background:#c0392b;}" +
                "table{border-collapse:collapse;width:100%;margin-top:10px;}" +
                "th,td{border:1px solid #ccc;padding:8px;text-align:left;font-size:14px;}" +
                "th{background:#ecf0f1;}" +
                ".output pre{background:#1e1e1e;color:#d4d4d4;padding:14px;border-radius:6px;" +
                "  overflow-x:auto;font-size:13px;}" +
                "</style></head><body>" +
                "<div class='wrap'>" +
                "<h1>Hostel Complaint Management System</h1>" +

                "<div class='tabs'>" +
                "<input type='radio' name='tabs' id='tab-student'" + studentChecked + ">" +
                "<input type='radio' name='tabs' id='tab-admin'" + adminChecked + ">" +
                "<label for='tab-student'>Student</label>" +
                "<label for='tab-admin'>Admin</label>" +

                // ---------------- TAB 1: STUDENT ----------------
                "<div class='tabcontent' id='content-student'>" +
                "<h2>Submit Complaint</h2>" +
                "<form method='POST' action='/submit'>" +
                "<label class='field'>Student Name:</label><input type='text' name='name' required><br>" +
                "<label class='field'>Room Number:</label><input type='text' name='room' required><br>" +
                "<label class='field'>Complaint Type:</label>" +
                "<select name='type'>" +
                "<option value='1'>Electrical</option>" +
                "<option value='2'>Plumbing</option>" +
                "<option value='3'>Cleaning</option>" +
                "<option value='4'>Internet</option>" +
                "<option value='5'>Furniture</option>" +
                "</select><br>" +
                "<label class='field'>Description:</label><br>" +
                "<textarea name='description' rows='3' required></textarea><br>" +
                "<button type='submit'>Submit Complaint</button>" +
                "</form>" +

                "<h2>View Complaints</h2>" +
                buildComplaintsTable() +
                "</div>" +

                // ---------------- TAB 2: ADMIN ----------------
                "<div class='tabcontent' id='content-admin'>" +
                "<h2>Update Complaint Status</h2>" +
                "<form method='POST' action='/admin'>" +
                "<label class='field'>Admin Password:</label><input type='password' name='password' required><br>" +
                "<label class='field'>Complaint ID:</label><input type='text' name='id' required><br>" +
                "<label class='field'>New Status:</label>" +
                "<select name='status'>" +
                "<option value='IN_PROGRESS'>IN_PROGRESS</option>" +
                "<option value='RESOLVED'>RESOLVED</option>" +
                "</select><br>" +
                "<button type='submit'>Update Status</button>" +
                "</form>" +

                "<h2>Escalate Unresolved Complaint</h2>" +
                "<form method='POST' action='/escalate'>" +
                "<label class='field'>Complaint ID:</label><input type='text' name='id' required><br>" +
                "<button type='submit' class='escalate'>Escalate Complaint</button>" +
                "</form>" +
                "<p><i>Escalation chain: Maintenance Staff &rarr; Warden &rarr; Rector &rarr; Admin</i></p>" +
                "</div>" +

                "</div>" + // /.tabs
                outputBlock +
                "</div></body></html>";

        sendHtml(exchange, html);
    }

    // Renders the complaints table for the Student tab.
    // Shows: ID, Student, Type, Department/assigned staff, Status.
    private static String buildComplaintsTable() {
        List<Complaint> complaints = manager.getAllComplaints();
        if (complaints.isEmpty()) {
            return "<p><i>No complaints submitted yet.</i></p>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tr><th>Complaint ID</th><th>Student</th><th>Type</th>")
          .append("<th>Assigned To</th><th>Status</th></tr>");
        for (Complaint c : complaints) {
            sb.append("<tr>")
              .append("<td>").append(escape(c.getId())).append("</td>")
              .append("<td>").append(escape(c.getStudent().toString())).append("</td>")
              .append("<td>").append(escape(c.getDepartment())).append("</td>")
              .append("<td>").append(escape(c.getAssignedTo())).append("</td>")
              .append("<td>").append(escape(c.getStatus().toString())).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    // ---------- Helpers ----------

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
    // (Entities are built via concatenation so no editor can mangle them.)
    private static String escape(String text) {
        String amp = "&" + "amp;";
        String lt  = "&" + "lt;";
        String gt  = "&" + "gt;";
        return text.replace("&", amp)
                .replace("<", lt)
                .replace(">", gt);
    }

    // Same System.out capture trick used by the other front-ends, so the
    // pattern classes' console output (routing, notifications) can be
    // displayed inside the webpage.
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