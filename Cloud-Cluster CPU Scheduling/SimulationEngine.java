import java.util.*;

/**
 * SimulationEngine.java  (Person 1)
 * Shared clock + CPU helper used by every scheduler. It does NOT decide WHICH process runs;
 * that logic belongs to the scheduler owner. It only provides:
 *   - the clock, arrival release, idle handling
 *   - running a process (1 unit at a time internally)
 *   - context-switch accounting
 *   - timeline (Gantt) recording and an optional per-tick listener (for Person 5's logger)
 *
 * ===== AGREED POLICIES (change the flags / constants here, nowhere else) =====
 *  1. All times are integers; 1 tick = 1 time unit.
 *  2. A context switch (CS) costs `contextSwitchTime` units when the CPU goes from process A to a
 *     DIFFERENT process B, including when A just finished (matches assignment Test Case 2).
 *  3. No CS for the very first process, and (default) no CS when coming out of an idle gap.
 *     Set chargeCSAfterIdle = true to charge it when the CPU was idle after a process ran.
 *  4. A CS cannot be interrupted. The next process is committed before the CS starts.
 *     Processes that arrive during the CS are NOT lost: call releaseArrivals() after runProcess/dispatch
 *     and they will be returned in arrival order (ties: input order).
 *  5. CPU is released immediately when a process finishes (no wasted tick).
 */
public class SimulationEngine {

    public enum SegmentType { RUN, CONTEXT_SWITCH, IDLE }

    /** One block of the Gantt chart. For CONTEXT_SWITCH, `process` is the process being loaded. */
    public static class Segment {
        public final int start;
        public int end;
        public final SegmentType type;
        public final Process process;       // null for IDLE

        Segment(int start, int end, SegmentType type, Process process) {
            this.start = start; this.end = end; this.type = type; this.process = process;
        }

        @Override
        public String toString() {
            String label;
            switch (type) {
                case RUN:            label = process.id; break;
                case CONTEXT_SWITCH: label = "CS -> " + process.id; break;
                default:             label = "IDLE";
            }
            return start + " - " + end + " : " + label;
        }
    }

    /** Called once per executed time unit, BEFORE the clock advances (time = start of that unit). */
    public interface StepListener {
        void onTick(int time, Process running);
    }

    private final List<Process> processes;
    private final LinkedList<Process> notArrivedYet = new LinkedList<>();
    private final int contextSwitchTime;
    private final boolean chargeCSAfterIdle;
    private final List<Segment> timeline = new ArrayList<>();

    private int currentTime = 0;
    private Process lastRun = null;
    private StepListener listener = null;

    public SimulationEngine(List<Process> processes, int contextSwitchTime) {
        this(processes, contextSwitchTime, false);
    }

    public SimulationEngine(List<Process> processes, int contextSwitchTime, boolean chargeCSAfterIdle) {
        if (contextSwitchTime < 0) throw new IllegalArgumentException("CS must be >= 0");
        this.processes = processes;
        this.contextSwitchTime = contextSwitchTime;
        this.chargeCSAfterIdle = chargeCSAfterIdle;
        notArrivedYet.addAll(processes);
        notArrivedYet.sort(Comparator.comparingInt(p -> p.arrivalTime)); // stable: ties keep input order
    }

    // ------------------------------------------------------------ clock & arrivals

    public int getCurrentTime() { return currentTime; }
    public int getContextSwitchTime() { return contextSwitchTime; }
    public List<Process> getProcesses() { return processes; }
    public void setStepListener(StepListener l) { this.listener = l; }

    /** Returns (and removes from the pending list) every process with arrivalTime <= currentTime, in order. */
    public List<Process> releaseArrivals() {
        List<Process> arrived = new ArrayList<>();
        while (!notArrivedYet.isEmpty() && notArrivedYet.getFirst().arrivalTime <= currentTime) {
            arrived.add(notArrivedYet.removeFirst());
        }
        return arrived;
    }

    public boolean hasPendingArrivals() { return !notArrivedYet.isEmpty(); }

    /** Arrival time of the next process that has not been released yet, or -1 if none. */
    public int nextArrivalTime() {
        return notArrivedYet.isEmpty() ? -1 : notArrivedYet.getFirst().arrivalTime;
    }

    public boolean allFinished() {
        for (Process p : processes) if (!p.isFinished()) return false;
        return true;
    }

    // ------------------------------------------------------------ idle

    /** CPU idle until `time` (no-op if time <= now). Typical use: idleUntil(nextArrivalTime()). */
    public void idleUntil(int time) {
        if (time <= currentTime) return;
        addSegment(currentTime, time, SegmentType.IDLE, null);
        currentTime = time;
        if (!chargeCSAfterIdle) lastRun = null;
    }

    // ------------------------------------------------------------ context switch

    /** True if loading `next` right now would cost a context switch. */
    public boolean needsContextSwitch(Process next) {
        return contextSwitchTime > 0 && lastRun != null && lastRun != next;
    }

    /**
     * Charges a CS if needed (see policy 2-3). Returns the CS time spent (0 if none).
     * runProcess() calls this automatically, so schedulers usually don't need to call it.
     */
    public int dispatch(Process next) {
        int spent = 0;
        if (needsContextSwitch(next)) {
            addSegment(currentTime, currentTime + contextSwitchTime, SegmentType.CONTEXT_SWITCH, next);
            currentTime += contextSwitchTime;
            spent = contextSwitchTime;
        }
        lastRun = next;
        return spent;
    }

    // ------------------------------------------------------------ running a process

    /**
     * Runs `p` for up to `units` time units (stops early if p finishes: CPU released immediately).
     * Automatically: charges CS, sets firstStartTime, decrements remainingTime, sets finishTime,
     * records the timeline and fires the tick listener.
     *
     * NOTE: the CS happens first and the first-start time is taken AFTER the CS
     * (response time = time the process actually starts executing - arrival).
     *
     * @return number of units actually executed (not counting the CS).
     */
    public int runProcess(Process p, int units) {
        if (p.isFinished()) throw new IllegalStateException(p.id + " is already finished");
        dispatch(p);
        int executed = 0;
        while (executed < units && !p.isFinished()) {
            if (!p.hasStarted()) p.firstStartTime = currentTime;
            if (listener != null) listener.onTick(currentTime, p);
            p.remainingTime--;
            currentTime++;
            executed++;
            addSegment(currentTime - 1, currentTime, SegmentType.RUN, p);
            if (p.isFinished()) p.finishTime = currentTime;
        }
        return executed;
    }

    /** Convenience: run p until it finishes (non-preemptive). */
    public int runToCompletion(Process p) { return runProcess(p, p.remainingTime); }

    // ------------------------------------------------------------ timeline

    public List<Segment> getTimeline() { return timeline; }

    private void addSegment(int start, int end, SegmentType type, Process p) {
        if (!timeline.isEmpty()) {
            Segment last = timeline.get(timeline.size() - 1);
            // merge consecutive RUN / IDLE blocks of the same process; never merge CS blocks
            if (last.end == start && last.type == type && last.process == p && type != SegmentType.CONTEXT_SWITCH) {
                last.end = end;
                return;
            }
        }
        timeline.add(new Segment(start, end, type, p));
    }

    public void printTimeline() {
        for (Segment s : timeline) System.out.println(s);
    }
}
