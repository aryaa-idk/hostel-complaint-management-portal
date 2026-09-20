import model.*;
import factory.ComplaintFactory;
import singleton.ComplaintManager;
import observer.StudentObserver;
import proxy.AdminProxy;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // SINGLETON: always fetched via getInstance(), never created with "new"
        ComplaintManager manager = ComplaintManager.getInstance();

        System.out.println("====================================");
        System.out.println(" HOSTEL COMPLAINT MANAGEMENT SYSTEM");
        System.out.println("====================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Submit Complaint");
            System.out.println("2. View All Complaints");
            System.out.println("3. Update Complaint Status (Admin)");
            System.out.println("4. View Complaint Details");
            System.out.println("5. Escalate Unresolved Complaint");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");
            int choice = readInt(sc);

            switch (choice) {
                case 1:
                    submitComplaint(sc, manager);
                    break;

                case 2:
                    manager.viewAllComplaints();
                    break;

                case 3:
                    updateComplaintStatus(sc, manager);
                    break;

                case 4:
                    System.out.print("Enter Complaint ID: ");
                    String cid = sc.nextLine();
                    Complaint found = manager.getComplaintById(cid);
                    if (found == null) {
                        System.out.println("Complaint not found.");
                    } else {
                        found.printDetails();
                    }
                    break;

                case 5:
                    System.out.print("Enter Complaint ID to escalate: ");
                    manager.escalateComplaint(sc.nextLine());
                    break;

                case 6:
                    running = false;
                    System.out.println("Exiting... Thank you!");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
        sc.close();
    }

    private static void submitComplaint(Scanner sc, ComplaintManager manager) {
        System.out.print("Enter Student Name: ");
        String name = sc.nextLine();
        System.out.print("Enter Room Number: ");
        String room = sc.nextLine();
        Student student = new Student(name, room);

        System.out.println("\nSelect Complaint Type:");
        System.out.println("1. Electrical");
        System.out.println("2. Plumbing");
        System.out.println("3. Cleaning");
        System.out.println("4. Internet");
        System.out.println("5. Furniture");
        System.out.print("Enter choice: ");
        int type = readInt(sc);

        System.out.println("\nEnter Complaint Description:");
        String desc = sc.nextLine();

        // FACTORY METHOD: Main does not decide which class to instantiate.
        // It just asks the factory for a complaint of the given type.
        Complaint complaint = ComplaintFactory.createComplaint(type, student, desc);

        if (complaint == null) {
            System.out.println("Invalid complaint type.");
            return;
        }

        // OBSERVER: the student is registered so they get notified later.
        complaint.addObserver(new StudentObserver(student));

        manager.submitComplaint(complaint);
    }

    private static void updateComplaintStatus(Scanner sc, ComplaintManager manager) {
        System.out.print("\nEnter Admin Password: ");
        String password = sc.nextLine();

        // In a real system this would check a database. Here, a fixed
        // password is enough to demonstrate the Proxy pattern.
        boolean isAdmin = password.equals("admin123");

        // PROXY: Main always goes through AdminProxy, never RealAdminService directly.
        AdminProxy adminProxy = new AdminProxy(isAdmin);

        if (isAdmin) {
            System.out.println("Admin logged in.");
        }

        System.out.print("Enter Complaint ID to update: ");
        String id = sc.nextLine();

        System.out.println("1. IN_PROGRESS   2. RESOLVED");
        System.out.print("Enter new status choice: ");
        int statusChoice = readInt(sc);

        ComplaintStatus newStatus =
                (statusChoice == 1) ? ComplaintStatus.IN_PROGRESS : ComplaintStatus.RESOLVED;

        adminProxy.updateComplaintStatus(id, newStatus);
    }

    // Small helper so the menu doesn't crash on bad input.
    private static int readInt(Scanner sc) {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
