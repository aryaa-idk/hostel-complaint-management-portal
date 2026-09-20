package proxy;

import model.ComplaintStatus;

// PROXY PATTERN: This is the "Proxy".
// Main never talks to RealAdminService directly. It always goes through
// this proxy, which checks whether the caller is authenticated BEFORE
// allowing the real operation to happen.
public class AdminProxy implements AdminService {

    private RealAdminService realAdminService;
    private boolean isAuthenticated;

    public AdminProxy(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
        if (isAuthenticated) {
            // The real service is only created when access is allowed.
            realAdminService = new RealAdminService();
        }
    }

    @Override
    public void updateComplaintStatus(String complaintId, ComplaintStatus status) {
        if (isAuthenticated) {
            realAdminService.updateComplaintStatus(complaintId, status);
        } else {
            System.out.println("Access Denied: Only an authorized admin can update complaints.");
        }
    }

    @Override
    public void viewAllComplaints() {
        if (isAuthenticated) {
            realAdminService.viewAllComplaints();
        } else {
            System.out.println("Access Denied: Only an authorized admin can view complaints.");
        }
    }
}
