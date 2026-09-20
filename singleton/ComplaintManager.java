package singleton;

import model.Complaint;
import model.ComplaintStatus;
import chain.ComplaintHandler;
import chain.EscalationChain;

import java.util.ArrayList;
import java.util.List;

// SINGLETON PATTERN:
// One shared ComplaintManager is used by the whole application.
// The constructor is private, so the only way to get it is getInstance().
//
// It is the central place that:
//  - stores all complaints
//  - routes each complaint through the Chain of Responsibility
//  - updates complaint status (which triggers Observer notifications)
//  - escalates unresolved complaints
public class ComplaintManager {

    private static ComplaintManager instance;

    private List<Complaint> complaints = new ArrayList<>();
    private int complaintCounter = 1;
    private ComplaintHandler chainHead;

    // Private constructor: no other class can do "new ComplaintManager()".
    private ComplaintManager() {
        buildChain();
    }

    // The only way to get the manager.
    public static ComplaintManager getInstance() {
        if (instance == null) {
            instance = new ComplaintManager();
        }
        return instance;
    }

    // Builds the routing chain once:
    // Electrical -> Plumbing -> Cleaning -> Internet -> Furniture
    private void buildChain() {
        ComplaintHandler electrical = new ComplaintHandler.Electrical();
        ComplaintHandler plumbing = new ComplaintHandler.Plumbing();
        ComplaintHandler cleaning = new ComplaintHandler.Cleaning();
        ComplaintHandler internet = new ComplaintHandler.Internet();
        ComplaintHandler furniture = new ComplaintHandler.Furniture();

        electrical.setNextHandler(plumbing);
        plumbing.setNextHandler(cleaning);
        cleaning.setNextHandler(internet);
        internet.setNextHandler(furniture);
        // furniture is the last handler, its nextHandler stays null

        chainHead = electrical;
    }

    public void submitComplaint(Complaint complaint) {
        String id = "C" + String.format("%03d", complaintCounter++);
        complaint.setId(id);
        complaints.add(complaint);

        System.out.println("\nComplaint submitted successfully!");
        System.out.println("Complaint ID: " + id);

        // CHAIN OF RESPONSIBILITY: send the complaint through the
        // department handlers; the matching one assigns staff.
        chainHead.handleComplaint(complaint);

        System.out.println("Status: " + complaint.getStatus());
        System.out.println();
    }

    public void viewAllComplaints() {
        if (complaints.isEmpty()) {
            System.out.println("No complaints found.");
            return;
        }
        for (Complaint c : complaints) {
            System.out.println("----------------------------");
            c.printDetails();
        }
    }

    public Complaint getComplaintById(String id) {
        for (Complaint c : complaints) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

    // Updates the status. Calling setStatus() on the complaint automatically
    // triggers the OBSERVER notification to the student.
    public void updateStatus(String id, ComplaintStatus status) {
        Complaint c = getComplaintById(id);
        if (c == null) {
            System.out.println("Complaint not found.");
            return;
        }
        System.out.println("\nComplaint " + id + " found.");
        System.out.println("Status changed:");
        System.out.println(c.getStatus() + " -> " + status);
        c.setStatus(status);
    }

    // CHAIN OF RESPONSIBILITY (escalation):
    // Called when a complaint is unresolved: Staff -> Warden -> Rector -> Admin.
    public void escalateComplaint(String id) {
        Complaint c = getComplaintById(id);
        if (c == null) {
            System.out.println("Complaint not found.");
            return;
        }
        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            System.out.println("Complaint " + id + " is already resolved; no escalation needed.");
            return;
        }
        EscalationChain.escalate(id, c.getAssignedTo());
        // The assigned person moves up one level in the escalation chain.
        c.setAssignedTo(nextEscalationTarget(c.getAssignedTo()));
    }

    private String nextEscalationTarget(String current) {
        switch (current) {
            case "Warden":      return "Rector";
            case "Rector":      return "Admin";
            default:            return "Warden";
        }
    }
}