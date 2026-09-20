import model.*;
import factory.ComplaintFactory;
import singleton.ComplaintManager;
import observer.StudentObserver;
import proxy.AdminProxy;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

// A simple Swing GUI front-end for the Hostel Complaint Management System.
// It does NOT change any of the pattern classes -- it just calls them,
// exactly like the console Main.java did. All 5 design patterns
// (Singleton, Factory Method, Chain of Responsibility, Observer, Proxy)
// still run exactly the same way underneath.
public class MainGUI extends JFrame {

    // SINGLETON: same single manager instance used everywhere.
    private ComplaintManager manager = ComplaintManager.getInstance();

    // ----- Submit tab fields -----
    private JTextField nameField = new JTextField(15);
    private JTextField roomField = new JTextField(8);
    private JComboBox<String> typeCombo = new JComboBox<>(
            new String[]{"Electrical", "Plumbing", "Cleaning", "Internet", "Furniture"});
    private JTextArea descriptionArea = new JTextArea(4, 20);
    private JTextArea submitResultArea = new JTextArea(6, 30);

    // ----- View tab fields -----
    private JTextArea viewArea = new JTextArea(15, 40);

    // ----- Admin tab fields -----
    private JPasswordField passwordField = new JPasswordField(12);
    private JTextField complaintIdField = new JTextField(10);
    private JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{"IN_PROGRESS", "RESOLVED"});
    private JTextArea adminResultArea = new JTextArea(8, 30);

    public MainGUI() {
        super("Hostel Complaint Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Submit Complaint", buildSubmitPanel());
        tabs.addTab("View Complaints", buildViewPanel());
        tabs.addTab("Admin Panel", buildAdminPanel());

        add(tabs);
    }

    // ---------- Tab 1: Submit Complaint ----------
    private JPanel buildSubmitPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.add(new JLabel("Student Name:"));
        form.add(nameField);
        form.add(new JLabel("Room Number:"));
        form.add(roomField);
        form.add(new JLabel("Complaint Type:"));
        form.add(typeCombo);
        form.add(new JLabel("Description:"));
        form.add(new JScrollPane(descriptionArea));

        JButton submitButton = new JButton("Submit Complaint");
        submitButton.addActionListener(e -> handleSubmit());

        submitResultArea.setEditable(false);

        panel.add(form, BorderLayout.NORTH);
        panel.add(submitButton, BorderLayout.CENTER);
        panel.add(new JScrollPane(submitResultArea), BorderLayout.SOUTH);
        return panel;
    }

    private void handleSubmit() {
        String name = nameField.getText().trim();
        String room = roomField.getText().trim();
        String description = descriptionArea.getText().trim();
        int type = typeCombo.getSelectedIndex() + 1; // combo is 0-based, factory expects 1-5

        if (name.isEmpty() || room.isEmpty() || description.isEmpty()) {
            submitResultArea.setText("Please fill in all fields.");
            return;
        }

        Student student = new Student(name, room);

        // FACTORY METHOD: GUI never picks the subclass itself.
        Complaint complaint = ComplaintFactory.createComplaint(type, student, description);

        // OBSERVER: student is registered before the complaint is submitted.
        complaint.addObserver(new StudentObserver(student));

        // Capture the text that submitComplaint() normally prints to the console
        // so we can show it inside the GUI instead.
        String output = captureOutput(() -> manager.submitComplaint(complaint));
        submitResultArea.setText(output);

        nameField.setText("");
        roomField.setText("");
        descriptionArea.setText("");
    }

    // ---------- Tab 2: View Complaints ----------
    private JPanel buildViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        viewArea.setEditable(false);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            String output = captureOutput(() -> manager.viewAllComplaints());
            viewArea.setText(output);
        });

        panel.add(new JScrollPane(viewArea), BorderLayout.CENTER);
        panel.add(refreshButton, BorderLayout.SOUTH);
        return panel;
    }

    // ---------- Tab 3: Admin Panel ----------
    private JPanel buildAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        form.add(new JLabel("Admin Password:"));
        form.add(passwordField);
        form.add(new JLabel("Complaint ID:"));
        form.add(complaintIdField);
        form.add(new JLabel("New Status:"));
        form.add(statusCombo);

        JButton updateButton = new JButton("Update Status");
        updateButton.addActionListener(e -> handleAdminUpdate());

        // CHAIN OF RESPONSIBILITY (escalation): Staff -> Warden -> Rector -> Admin
        JButton escalateButton = new JButton("Escalate Complaint");
        escalateButton.addActionListener(e -> handleEscalate());

        adminResultArea.setEditable(false);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 5));
        buttons.add(updateButton);
        buttons.add(escalateButton);

        panel.add(form, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.CENTER);
        panel.add(new JScrollPane(adminResultArea), BorderLayout.SOUTH);
        return panel;
    }

    private void handleAdminUpdate() {
        String password = new String(passwordField.getPassword());
        String id = complaintIdField.getText().trim();
        ComplaintStatus newStatus = statusCombo.getSelectedIndex() == 0
                ? ComplaintStatus.IN_PROGRESS
                : ComplaintStatus.RESOLVED;

        boolean isAdmin = password.equals("admin123");

        // PROXY: GUI always goes through AdminProxy, never RealAdminService directly.
        AdminProxy adminProxy = new AdminProxy(isAdmin);

        String output = captureOutput(() -> adminProxy.updateComplaintStatus(id, newStatus));
        adminResultArea.setText(output);

        passwordField.setText("");
        complaintIdField.setText("");
    }

    // Asks the manager (not the GUI) to escalate an unresolved complaint
    // one step up the chain: Staff -> Warden -> Rector -> Admin.
    private void handleEscalate() {
        String id = complaintIdField.getText().trim();
        String output = captureOutput(() -> manager.escalateComplaint(id));
        adminResultArea.setText(output);
        complaintIdField.setText("");
    }

    // Small helper: temporarily redirects System.out so we can show
    // console-style pattern output inside a GUI text area.
    private String captureOutput(Runnable action) {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGUI().setVisible(true));
    }
}
