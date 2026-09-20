package observer;

import model.Student;

// OBSERVER PATTERN: Concrete Observer.
// A StudentObserver "watches" a complaint and prints a notification
// whenever the complaint's status changes.
public class StudentObserver implements Observer {
    private Student student;

    public StudentObserver(Student student) {
        this.student = student;
    }

    @Override
    public void update(String complaintId, String message) {
        System.out.println("\nNotification to " + student.getName() + ":");
        System.out.println(message);
    }
}
