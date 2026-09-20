package chain;

import model.Complaint;
import model.ComplaintStatus;

// CHAIN OF RESPONSIBILITY PATTERN:
// Each handler checks whether it can handle the complaint type.
// If yes -> it assigns the complaint to the right staff and marks it ASSIGNED.
// If no  -> it passes the complaint to the next handler in the chain.
//
// All 5 department handlers are grouped here as nested classes to keep
// the file count small and easy to review.
public abstract class ComplaintHandler {

    protected ComplaintHandler nextHandler;

    public void setNextHandler(ComplaintHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void handleComplaint(Complaint complaint) {
        if (complaint.getDepartment().equals(getDepartmentName())) {
            // This handler is responsible -> assign staff and set status.
            complaint.setAssignedTo(getStaffName());
            complaint.setStatus(ComplaintStatus.ASSIGNED); // triggers Observer notification
            System.out.println("Complaint routed to " + getDepartmentName()
                    + " -> assigned to " + getStaffName() + ".");
        } else if (nextHandler != null) {
            // Not my responsibility -> pass it along the chain.
            nextHandler.handleComplaint(complaint);
        } else {
            System.out.println("No department found for this complaint.");
        }
    }

    protected abstract String getDepartmentName();

    protected abstract String getStaffName();

    // ---------- Concrete handlers (one per complaint type) ----------

    public static class Electrical extends ComplaintHandler {
        protected String getDepartmentName() { return "Electrical"; }
        protected String getStaffName() { return "Electrical Maintenance Staff"; }
    }

    public static class Plumbing extends ComplaintHandler {
        protected String getDepartmentName() { return "Plumbing"; }
        protected String getStaffName() { return "Plumbing Maintenance Staff"; }
    }

    public static class Cleaning extends ComplaintHandler {
        protected String getDepartmentName() { return "Cleaning"; }
        protected String getStaffName() { return "Cleaning Staff"; }
    }

    public static class Internet extends ComplaintHandler {
        protected String getDepartmentName() { return "Internet"; }
        protected String getStaffName() { return "Internet Technician"; }
    }

    public static class Furniture extends ComplaintHandler {
        protected String getDepartmentName() { return "Furniture"; }
        protected String getStaffName() { return "Furniture Repair Staff"; }
    }
}