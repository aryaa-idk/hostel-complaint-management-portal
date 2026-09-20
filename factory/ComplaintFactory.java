package factory;

import model.Complaint;
import model.Student;

// FACTORY METHOD PATTERN:
// Creates the correct complaint object based on the selected type.
// The GUI/Main never writes "new ElectricalComplaint(...)" directly --
// they always ask this factory, so the creation decision lives in one place.
public class ComplaintFactory {

    public static Complaint createComplaint(int typeChoice, Student student, String description) {
        switch (typeChoice) {
            case 1:
                return new Complaint.Electrical(student, description);
            case 2:
                return new Complaint.Plumbing(student, description);
            case 3:
                return new Complaint.Cleaning(student, description);
            case 4:
                return new Complaint.Internet(student, description);
            case 5:
                return new Complaint.Furniture(student, description);
            default:
                return null; // invalid choice
        }
    }
}