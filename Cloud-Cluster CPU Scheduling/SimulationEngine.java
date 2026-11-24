import java.util.*;

public class SimulationEngine {

    public enum SegmentType {
        RUN, CONTEXT_SWITCH, IDLE
    }

    public static class Segment {
        public final int start;
        public int end;
        public final SegmentType type;
        public final Process process;

        Segment(int start, int end, SegmentType type, Process process) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.process = process;
        }

        @Override
        public String toString() {
            String label;
            switch (type) {
                case RUN:
                    label = process.id;
                    break;
                case CONTEXT_SWITCH:
                    label = "CS -> " + process.id;
                    break;
                default:
                    label = "IDLE";
            }
            return start + " - " + end + " : " + label;
        }
    }

    public interface StepListener {
        void onTick(int time, Process running);

        default void onContextSwitchTick(int time, Process next) {
        }

        default void onIdleTick(int time) {
        }
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
        if (contextSwitchTime < 0)
            throw new IllegalArgumentException("CS must be >= 0");
        this.processes = processes;
        this.contextSwitchTime = contextSwitchTime;
        this.chargeCSAfterIdle = chargeCSAfterIdle;
        notArrivedYet.addAll(processes);
        notArrivedYet.sort(Comparator.comparingInt(p -> p.arrivalTime));
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public int getContextSwitchTime() {
        return contextSwitchTime;
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public void setStepListener(StepListener l) {
        this.listener = l;
    }

    public List<Process> releaseArrivals() {
        List<Process> arrived = new ArrayList<>();
        while (!notArrivedYet.isEmpty() && notArrivedYet.getFirst().arrivalTime <= currentTime) {
            arrived.add(notArrivedYet.removeFirst());
        }
        return arrived;
    }

    public boolean hasPendingArrivals() {
        return !notArrivedYet.isEmpty();
    }

    public int nextArrivalTime() {
        return notArrivedYet.isEmpty() ? -1 : notArrivedYet.getFirst().arrivalTime;
    }

    public boolean allFinished() {
        for (Process p : processes)
            if (!p.isFinished())
                return false;
        return true;
    }

    public void idleUntil(int time) {
        if (time <= currentTime)
            return;

        int start = currentTime;

        while (currentTime < time) {

            if (listener != null)
                listener.onIdleTick(currentTime);

            currentTime++;
        }

        addSegment(start, time, SegmentType.IDLE, null);

        currentTime = time;
        if (!chargeCSAfterIdle)
            lastRun = null;
    }

    public boolean needsContextSwitch(Process next) {
        return contextSwitchTime > 0 && lastRun != null && lastRun != next;
    }

    public int dispatch(Process next) {
        int spent = 0;

        if (needsContextSwitch(next)) {
            int start = currentTime;

            while (spent < contextSwitchTime) {

                if (listener != null)
                    listener.onContextSwitchTick(currentTime, next);

                currentTime++;
                spent++;

            }
            addSegment(start, currentTime, SegmentType.CONTEXT_SWITCH, next);

        }

        lastRun = next;
        return spent;
    }

    public int runProcess(Process p, int units) {
        if (p.isFinished())
            throw new IllegalStateException(p.id + " is already finished");
        dispatch(p);
        int executed = 0;
        while (executed < units && !p.isFinished()) {
            if (!p.hasStarted())
                p.firstStartTime = currentTime;
            if (listener != null)
                listener.onTick(currentTime, p);
            p.remainingTime--;
            currentTime++;
            executed++;
            addSegment(currentTime - 1, currentTime, SegmentType.RUN, p);
            if (p.isFinished())
                p.finishTime = currentTime;
        }
        return executed;
    }

    public int runToCompletion(Process p) {
        return runProcess(p, p.remainingTime);
    }

    public List<Segment> getTimeline() {
        return timeline;
    }

    private void addSegment(int start, int end, SegmentType type, Process p) {
        if (!timeline.isEmpty()) {
            Segment last = timeline.get(timeline.size() - 1);
            if (last.end == start && last.type == type && last.process == p && type != SegmentType.CONTEXT_SWITCH) {
                last.end = end;
                return;
            }
        }
        timeline.add(new Segment(start, end, type, p));
    }

    public void printTimeline() {
        for (Segment s : timeline)
            System.out.println(s);
    }
}
