package observer;

// OBSERVER PATTERN: This is the "Observer" interface.
// Any class that wants to be notified of complaint status changes
// must implement this interface.
public interface Observer {
    void update(String complaintId, String message);
}
