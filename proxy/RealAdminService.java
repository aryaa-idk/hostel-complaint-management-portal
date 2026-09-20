package proxy;

import model.ComplaintStatus;
import singleton.ComplaintManager;

// PROXY PATTERN: This is the "Real Subject".
// It performs the actual admin work by talking to the ComplaintManager.
// It has NO idea that a Proxy exists in front of it.
public class RealAdminService implements AdminService {

    private ComplaintManager manager = ComplaintManager.getInstance();

    @Override
    public void updateComplaintStatus(String complaintId, ComplaintStatus status) {
        manager.updateStatus(complaintId, status);
    }

    @Override
    public void viewAllComplaints() {
        manager.viewAllComplaints();
    }
}
