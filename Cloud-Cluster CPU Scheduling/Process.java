/**
 * Process.java  (Person 1)
 * Represents one process in the simulation. Fields are public on purpose so every
 * team member can read them quickly (e.g. Comparator.comparingInt(p -> p.effectivePriority)).
 *
 * RULE: never share Process objects between schedulers. Use copy() / TestCase.getProcesses().
 */
public class Process {

    // ---- Input data (never changes) ----
    public final String id;
    public final int arrivalTime;
    public final int burstTime;
    public final int initialPriority;      // smaller number = higher priority

    // ---- Runtime state (changes during simulation) ----
    public int remainingTime;
    public int effectivePriority;          // initialPriority minus aging (Person 5)
    public int firstStartTime = -1;        // -1 = has not started yet
    public int finishTime = -1;            // -1 = not finished yet

    // ---- ACS helpers ----
    public boolean demoted = false;        // true once moved Tier 1 -> Tier 2 (Person 3 sets it)
    public int tier2EntryTime = -1;        // time the process entered Tier 2 (Person 3 sets, Person 5 reads for aging)

    // ---- Metrics (filled by SchedulerMetrics, Person 5) ----
    public int waitingTime;
    public int turnaroundTime;
    public int responseTime;

    public Process(String id, int arrivalTime, int burstTime, int initialPriority) {
        if (burstTime <= 0) throw new IllegalArgumentException("Burst time must be > 0 for " + id);
        if (arrivalTime < 0) throw new IllegalArgumentException("Arrival time must be >= 0 for " + id);
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.initialPriority = initialPriority;
        this.remainingTime = burstTime;
        this.effectivePriority = initialPriority;
    }

    public boolean isFinished() { return remainingTime == 0; }
    public boolean hasStarted() { return firstStartTime >= 0; }

    /** Fresh copy built from the ORIGINAL input data (runtime state is NOT copied). */
    public Process copy() {
        return new Process(id, arrivalTime, burstTime, initialPriority);
    }

    /** Same format as the assignment tables: "P1 (remaining: 4, Priority: 2)". */
    @Override
    public String toString() {
        return id + " (remaining: " + remainingTime + ", Priority: " + effectivePriority + ")";
    }
}
