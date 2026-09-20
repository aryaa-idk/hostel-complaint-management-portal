package model;

// Represents a hostel student who submits complaints
public class Student {
    private String name;
    private String roomNumber;

    public Student(String name, String roomNumber) {
        this.name = name;
        this.roomNumber = roomNumber;
    }

    public String getName() {
        return name;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    @Override
    public String toString() {
        return name + " (Room " + roomNumber + ")";
    }
}
