public class Process {

    public final String id;
    public final int arrivalTime;
    public final int burstTime;
    public final int initialPriority;
    public int remainingTime;
    public int effectivePriority;
    public int firstStartTime = -1;
    public int finishTime = -1;
    public boolean demoted = false;
    public int tier2EntryTime = -1;
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
    public Process copy() {
        return new Process(id, arrivalTime, burstTime, initialPriority);
    }

    @Override
    public String toString() {
        return id + " (remaining: " + remainingTime + ", Priority: " + effectivePriority + ")";
    }
}
