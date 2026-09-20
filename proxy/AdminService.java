package proxy;

import model.ComplaintStatus;

// PROXY PATTERN
// This is the common interface implemented by both the RealAdminService
// and the AdminProxy, so Main can use either one interchangeably.
public interface AdminService {
    void updateComplaintStatus(String complaintId, ComplaintStatus status);
    void viewAllComplaints();
}
