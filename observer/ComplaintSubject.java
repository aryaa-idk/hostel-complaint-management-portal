package observer;

// OBSERVER PATTERN: This is the "Subject" interface.
// Any class that can be observed (like Complaint) implements this.
public interface ComplaintSubject {
    void addObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers(String message);
}
