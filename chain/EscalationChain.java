package chain;

// CHAIN OF RESPONSIBILITY PATTERN (escalation):
// When a complaint is not resolved, it is escalated along a second chain:
//   Staff -> Warden -> Rector -> Admin
// Each level checks whether it is the final authority; if not, it passes
// the complaint up to the next level.
public class EscalationChain {

    // Simple chain of escalation level names; index = current level.
    private static final String[] LEVELS = {"Maintenance Staff", "Warden", "Rector", "Admin"};

    // Escalates the complaint one step up the chain and reports it.
    public static void escalate(String complaintId, String currentAssignedTo) {
        int level = 0;
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i].equalsIgnoreCase(currentAssignedTo)) {
                level = i;
                break;
            }
        }

        if (level >= LEVELS.length - 1) {
            System.out.println("Complaint " + complaintId
                    + " is already with the Admin. No further escalation possible.");
            return;
        }

        String next = LEVELS[level + 1];
        System.out.println("Complaint " + complaintId + " unresolved. Escalated: "
                + LEVELS[level] + " -> " + next + ".");
    }
}