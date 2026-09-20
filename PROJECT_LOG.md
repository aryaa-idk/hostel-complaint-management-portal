# Hostel Complaint Management System — Complete Project Log
### (Everything you need to understand the project, the code, and the 5 design patterns)

A plain-Java prototype that manages hostel complaints while demonstrating
**5 classic design patterns working together in ONE single workflow**.
No frameworks, no databases — every pattern is easy to point at and
explain in a Design Patterns lab viva.

---

## 1. WHAT THE PROJECT IS ABOUT

Students living in a hostel report problems (broken lights, leaking taps,
dirty rooms, internet issues, damaged furniture). The system:

1. Lets a student **submit** a complaint of a chosen type (Electrical,
   Plumbing, Cleaning, Internet, Furniture).
2. Checks **who is allowed** to perform certain operations (role-based
   access — only an admin may update a complaint's status).
3. **Creates** the right complaint object for the chosen type.
4. **Routes** the complaint to the correct department staff member.
5. Lets an admin **update the status** as work progresses
   (ASSIGNED → IN_PROGRESS → RESOLVED).
6. **Notifies** the student automatically at every status change.
7. **Escalates** complaints that stay unresolved:
   Staff → Warden → Rector → Admin.

---

## 2. THE COMPLETE WORKFLOW (one flow, all 5 patterns)

```
User (Student / Admin)
   |
   v
MainGUI / Main / WebMain          <- collect input, display output ONLY
   |
   v
PROXY (AdminProxy)                <- role check before admin status updates
   |
   v
FACTORY METHOD (ComplaintFactory) <- creates Electrical/Plumbing/... object
   |
   v
SINGLETON (ComplaintManager)      <- ONE shared manager receives the complaint
   |
   v
CHAIN OF RESPONSIBILITY           <- routes to the matching department handler
   |                                 (Electrical -> Plumbing -> Cleaning ->
   |                                  Internet -> Furniture); the matching
   |                                  handler assigns staff, sets ASSIGNED
   v
OBSERVER fires                    <- StudentObserver notified automatically
   |
   v
Admin updates status (IN_PROGRESS / RESOLVED)
   |
   v
OBSERVER notifies again
   |
   v
Escalation chain (if unresolved)  <- Staff -> Warden -> Rector -> Admin
```

---

## 3. FILE STRUCTURE AND WHAT EACH FILE DOES

```
Main.java                      Console menu front-end
                               (submit / view / details / admin update / escalate)
MainGUI.java                   Swing GUI front-end
                               (tabs: Submit Complaint / View Complaints / Admin Panel)
WebMain.java                   Optional browser front-end (http://localhost:8080)
                               - built-in JDK HttpServer only, no libraries

factory/
  ComplaintFactory.java        FACTORY METHOD: creates the right complaint object
                               from the selected type number (1-5)

singleton/
  ComplaintManager.java        SINGLETON: the one shared manager. Stores all
                               complaints, builds and calls the routing chain,
                               updates status (triggering Observer), escalates.

chain/
  ComplaintHandler.java        CHAIN OF RESPONSIBILITY: abstract handler +
                               all 5 department handlers as nested classes
                               (Electrical, Plumbing, Cleaning, Internet,
                               Furniture). Each handler checks "is this my
                               department?" — if yes it assigns staff and
                               sets ASSIGNED; if no it passes to nextHandler.
  EscalationChain.java         CHAIN (escalation): Staff -> Warden -> Rector -> Admin

observer/
  Observer.java                OBSERVER interface: update(complaintId, message)
  ComplaintSubject.java        OBSERVER subject interface: add/remove/notify
  StudentObserver.java         OBSERVER concrete observer: prints the
                               notification sent to the student

proxy/
  AdminService.java            PROXY common interface (update status / view)
  AdminProxy.java              PROXY: checks whether the caller is an admin
                               BEFORE forwarding to the real service
  RealAdminService.java        PROXY real subject: talks to ComplaintManager

model/
  Complaint.java               Abstract complaint (the Observer SUBJECT — it
                               keeps the observer list and notifies on every
                               status change) + the 5 tiny subclasses as
                               nested classes: Electrical, Plumbing, Cleaning,
                               Internet, Furniture (each just says its
                               department — used by the chain for routing)
  ComplaintStatus.java         Enum: REGISTERED, ASSIGNED, IN_PROGRESS, RESOLVED
  Student.java                 Student data class (name, room number)
```

Three interchangeable front-ends (console / Swing GUI / web) share the SAME
pattern classes. The GUI only collects input and shows output — it never
decides routing, escalation, notifications or storage.

---

## 4. THE 5 DESIGN PATTERNS — WHERE, WHY, HOW

### 4.1 SINGLETON — `singleton/ComplaintManager.java`
**Why:** The whole application must share ONE central manager that holds all
complaints and owns the routing chain. If every screen created its own
manager, complaints would be scattered across separate objects and the
chain would be rebuilt repeatedly.

**Key code:**
```java
private static ComplaintManager instance;
private ComplaintManager() { buildChain(); }      // private constructor

public static ComplaintManager getInstance() {    // only access point
    if (instance == null) {
        instance = new ComplaintManager();
    }
    return instance;
}
```
**Used by:** Main, MainGUI, WebMain, RealAdminService:
```java
ComplaintManager manager = ComplaintManager.getInstance();
```
**Honest viva explanation:** "One shared ComplaintManager is used by the
whole application. The private constructor means nobody can create a second
one." (Do NOT claim it creates one DB record or prevents database
conflicts — there is no database in this prototype.)

---

### 4.2 FACTORY METHOD — `factory/ComplaintFactory.java`
**Why:** The GUI has a dropdown of complaint types. The GUI must NOT contain
`new Electrical(...)` / `new Plumbing(...)` logic — the creation decision
belongs in ONE place so it can change safely (e.g., renaming a class or
adding a new type touches only the factory).

**Key code:**
```java
public static Complaint createComplaint(int typeChoice, Student student, String description) {
    switch (typeChoice) {
        case 1: return new Complaint.Electrical(student, description);
        case 2: return new Complaint.Plumbing(student, description);
        case 3: return new Complaint.Cleaning(student, description);
        case 4: return new Complaint.Internet(student, description);
        case 5: return new Complaint.Furniture(student, description);
        default: return null;
    }
}
```
**Used by all three front-ends:**
```java
Complaint complaint = ComplaintFactory.createComplaint(type, student, description);
```
The subclasses themselves are tiny nested classes inside `model/Complaint.java`
that only implement `getDepartment()` — e.g. `return "Plumbing";` — which the
chain later uses for routing.

---

### 4.3 CHAIN OF RESPONSIBILITY — `chain/ComplaintHandler.java` + `chain/EscalationChain.java`
**Why (routing):** A submitted complaint must reach the right department.
Instead of a big if/else inside the manager, handlers are linked in a line;
each checks "is this my department?" — if yes it handles it, if no it passes
the complaint to the next handler.

**Key code (base handler):**
```java
public void handleComplaint(Complaint complaint) {
    if (complaint.getDepartment().equals(getDepartmentName())) {
        complaint.setAssignedTo(getStaffName());        // right staff member
        complaint.setStatus(ComplaintStatus.ASSIGNED);  // triggers Observer
        System.out.println("Complaint routed to " + getDepartmentName()
                + " -> assigned to " + getStaffName() + ".");
    } else if (nextHandler != null) {
        nextHandler.handleComplaint(complaint);         // pass along the chain
    } else {
        System.out.println("No department found for this complaint.");
    }
}
```
The chain is built ONCE inside the Singleton (`ComplaintManager.buildChain()`):
```
Electrical -> Plumbing -> Cleaning -> Internet -> Furniture
```
**Why (escalation):** Unresolved complaints move up a second chain:
Staff → Warden → Rector → Admin (`EscalationChain.escalate(...)`).
Called via `manager.escalateComplaint(id)` from the GUI's "Escalate Complaint"
button. The GUI never touches the chain directly — that is ComplaintManager's
job.

---

### 4.4 OBSERVER — `observer/` package + `model/Complaint.java`
**Why:** When a complaint's status changes (ASSIGNED, IN_PROGRESS, RESOLVED)
the student must be told automatically, without the manager knowing anything
about "students". This decouples notification from business logic.

**Key code (Subject — inside `Complaint`):**
```java
// OBSERVER: changing the status automatically notifies all observers.
public void setStatus(ComplaintStatus status) {
    this.status = status;
    notifyObservers("Your complaint " + id + " is now " + status + ".");
}
```
**Key code (registration — in GUI/Main):**
```java
complaint.addObserver(new StudentObserver(student));
```
**Key code (Concrete Observer — `StudentObserver`):**
```java
public void update(String complaintId, String message) {
    System.out.println("\nNotification to " + student.getName() + ":");
    System.out.println(message);
}
```
Every `setStatus(...)` call — from the routing chain (ASSIGNED) or from the
admin update (IN_PROGRESS / RESOLVED) — automatically triggers a
notification. Adding a StaffObserver/WardenObserver later would need ZERO
changes to `Complaint` (just one more `addObserver` call).

---

### 4.5 PROXY — `proxy/` package
**Why:** Status updates are an admin operation. The GUI/Main must not call
the real service directly; they go through a proxy that CHECKS THE ROLE
FIRST and only forwards the request if the caller is an admin.

**Key code (`AdminProxy`):**
```java
public void updateComplaintStatus(String complaintId, ComplaintStatus status) {
    if (isAuthenticated) {
        realAdminService.updateComplaintStatus(complaintId, status);
    } else {
        System.out.println("Access Denied: Only an authorized admin can update complaints.");
    }
}
```
**Used by (GUI / Main / WebMain):**
```java
boolean isAdmin = password.equals("admin123");   // demo role check
AdminProxy adminProxy = new AdminProxy(isAdmin); // the Proxy
adminProxy.updateComplaintStatus(id, newStatus); // checked before forwarding
```
`AdminProxy` and `RealAdminService` both implement `AdminService`, so the
real service can be swapped/protected without the caller knowing.
**Honest scope:** the proxy demonstrates PERMISSION CHECKING on admin
operations; it is not a full authentication system.

---

## 5. HOW ONE SUBMISSION TRAVELS THROUGH THE CODE (trace)

1. `MainGUI.handleSubmit()` builds a `Student`, then calls
   `ComplaintFactory.createComplaint(type, student, description)`  **[FACTORY]**
2. `complaint.addObserver(new StudentObserver(student))`  **[OBSERVER]**
3. `manager.submitComplaint(complaint)` — manager obtained via
   `getInstance()`  **[SINGLETON]**
4. Manager assigns ID (C001...), stores the complaint, calls
   `chainHead.handleComplaint(complaint)`  **[CHAIN]**
5. Handlers pass it along until the matching one assigns staff and sets
   status ASSIGNED → `setStatus()` → `notifyObservers()`  **[OBSERVER fires]**
6. Admin panel: `AdminProxy.updateComplaintStatus(...)` checks the role
   **[PROXY]**, then `RealAdminService` → `manager.updateStatus(...)` →
   `complaint.setStatus(...)` → notification again.
7. Unresolved? "Escalate Complaint" → `manager.escalateComplaint(id)` →
   `EscalationChain` moves it Staff → Warden → Rector → Admin.  **[CHAIN]**

---

## 6. STATUS FLOW

```
REGISTERED -> ASSIGNED -> IN_PROGRESS -> RESOLVED
              (chain)     (admin)       (admin)
```
Escalation (for unresolved complaints): `Staff -> Warden -> Rector -> Admin`

---

## 7. HOW TO COMPILE AND RUN

```
javac -encoding UTF-8 Main.java MainGUI.java WebMain.java model/*.java factory/*.java singleton/*.java chain/*.java observer/*.java proxy/*.java

java MainGUI     # desktop GUI  (best for the demo)
java Main        # console menu
java WebMain     # browser UI at http://localhost:8080/
```
Admin password for the demo: `admin123`

---

## 8. LIVE DEMO SCRIPT (for the teacher)

1. **Submit:** GUI tab "Submit Complaint" → Name, Room, Type = Plumbing,
   Description → Submit.
   Shows: Factory created object → Singleton manager received it → Chain
   routed to "Plumbing Maintenance Staff" → status ASSIGNED → Observer
   printed the notification.
2. **Proxy (denied):** Admin tab → WRONG password + complaint ID → Update
   Status → "Access Denied" message (role check).
3. **Proxy (allowed) + Observer:** password `admin123`, status IN_PROGRESS →
   status changes and the student is notified.
4. **Observer (final):** update to RESOLVED → final notification.
5. **Escalation:** submit a fresh complaint, leave it unresolved, click
   "Escalate Complaint" repeatedly → Maintenance Staff → Warden → Rector →
   Admin.

---

## 9. ONE-LINE SUMMARY OF EACH PATTERN (viva answers)

| Pattern | Responsibility in this project |
|---|---|
| Proxy | Controls access: only an admin may update complaint status |
| Factory Method | Creates the correct complaint object from the selected type |
| Singleton | Provides ONE shared ComplaintManager instance for the app |
| Chain of Responsibility | Routes complaints to the right department and escalates unresolved ones |
| Observer | Notifies the student automatically when complaint status changes |