package model;

import observer.ComplaintSubject;
import observer.Observer;
import java.util.ArrayList;
import java.util.List;

// OBSERVER PATTERN:
// Complaint is the "Subject". It keeps a list of observers and notifies
// them automatically whenever its status changes.
//
// It is also the abstract base class used by the FACTORY METHOD pattern:
// the small nested subclasses below (Electrical, Plumbing, ...) simply say
// which department the complaint belongs to.
public abstract class Complaint implements ComplaintSubject {

    private String id;
    private Student student;
    private String description;
    private ComplaintStatus status;
    private String assignedTo = "Not assigned yet";
    private List<Observer> observers = new ArrayList<>();

    public Complaint(Student student, String description) {
        this.student = student;
        this.description = description;
        this.status = ComplaintStatus.REGISTERED;
    }

    // Each concrete complaint type must say which department handles it.
    // This is used by the Chain of Responsibility to route the complaint.
    public abstract String getDepartment();

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public String getDescription() {
        return description;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    // OBSERVER: changing the status automatically notifies all observers.
    public void setStatus(ComplaintStatus status) {
        this.status = status;
        notifyObservers("Your complaint " + id + " is now " + status + ".");
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        for (Observer o : observers) {
            o.update(id, message);
        }
    }

    public void printDetails() {
        System.out.println("Complaint ID   : " + id);
        System.out.println("Student        : " + student);
        System.out.println("Type           : " + getDepartment());
        System.out.println("Description    : " + description);
        System.out.println("Assigned To    : " + assignedTo);
        System.out.println("Status         : " + status);
    }

    // ---------------------------------------------------------------
    // FACTORY METHOD support:
    // These tiny subclasses are what the ComplaintFactory creates.
    // They are grouped here as nested classes to keep the file count low.
    // ---------------------------------------------------------------
    public static class Electrical extends Complaint {
        public Electrical(Student student, String description) {
            super(student, description);
        }
        @Override
        public String getDepartment() {
            return "Electrical";
        }
    }

    public static class Plumbing extends Complaint {
        public Plumbing(Student student, String description) {
            super(student, description);
        }
        @Override
        public String getDepartment() {
            return "Plumbing";
        }
    }

    public static class Cleaning extends Complaint {
        public Cleaning(Student student, String description) {
            super(student, description);
        }
        @Override
        public String getDepartment() {
            return "Cleaning";
        }
    }

    public static class Internet extends Complaint {
        public Internet(Student student, String description) {
            super(student, description);
        }
        @Override
        public String getDepartment() {
            return "Internet";
        }
    }

    public static class Furniture extends Complaint {
        public Furniture(Student student, String description) {
            super(student, description);
        }
        @Override
        public String getDepartment() {
            return "Furniture";
        }
    }
}