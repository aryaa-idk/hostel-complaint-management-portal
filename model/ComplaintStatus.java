package model;

// Simple enum for the complaint life-cycle:
// Registered -> Assigned -> In Progress -> Resolved
public enum ComplaintStatus {
    REGISTERED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED
}