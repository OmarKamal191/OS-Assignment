
import java.util.*;

public class SimulationLogger implements SimulationEngine.StepListener {

    private final SimulationEngine engine;
    private final ACSTier1 tier1;
    private final ACSTier2 tier2;

    private final List<String> snapshots = new ArrayList<>();
    private final List<String> events = new ArrayList<>();

    private final Set<String> detectedDemotions = new HashSet<>();

    public SimulationLogger(SimulationEngine engine, ACSTier1 tier1, ACSTier2 tier2) {

        this.engine = engine;
        this.tier1 = tier1;
        this.tier2 = tier2;

        engine.setStepListener(this);
    }

    @Override
    public void onTick(int time, Process running) {
        recordSnapshot(time, running.id, running);
    }

    @Override
    public void onContextSwitchTick(int time, Process next) {
        recordSnapshot(time, "CONTEXT SWITCH -> " + next.id, next);
    }

    @Override
    public void onIdleTick(int time) {
        recordSnapshot(time, "IDLE", null);
    }

    private void recordSnapshot(int time, String runningLabel, Process runningProcess) {

        List<Process> tier1Processes = getTier1Processes(time, runningProcess);

        List<Process> tier2Processes = tier2.snapshot();

        List<Process> observed = new ArrayList<>();

        if (runningProcess != null)
            observed.add(runningProcess);

        observed.addAll(tier1Processes);
        observed.addAll(tier2Processes);

        for (Process p : observed) {

            if (p.demoted && detectedDemotions.add(p.id))
                events.add("T=" + p.tier2EntryTime + ": " + p.id + " was demoted to Tier 2");

        }

        StringBuilder snapshot = new StringBuilder();

        snapshot.append("\nTime: ").append(time);
        snapshot.append("\nRunning: ").append(runningLabel);

        snapshot.append("\nTier 1: ").append(formatQueue(tier1Processes));

        snapshot.append("\nTier 2: ").append(formatQueue(tier2Processes));

        snapshots.add(snapshot.toString());
    }

    private List<Process> getTier1Processes(int time, Process running) {

        List<Process> result = tier1.snapshot();

        Set<Process> included = Collections.newSetFromMap(new IdentityHashMap<>());

        included.addAll(result);

        List<Process> all = new ArrayList<>(engine.getProcesses());

        all.sort(Comparator.comparingInt(p -> p.arrivalTime));

        for (Process p : all)
            if (p != running && p.arrivalTime <= time && !p.isFinished() && !p.demoted && included.add(p))
                result.add(p);

        return result;
    }

    public void recordAgingEvent(int time, Process p, int oldPriority, int newPriority) {

        events.add("T=" + time + ": Aging updated " + p.id + " priority from " + oldPriority + " to " + newPriority);
    }

    private String formatQueue(List<Process> processes) {

        if (processes.isEmpty())
            return "[Empty]";

        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < processes.size(); i++) {

            result.append(processes.get(i));

            if (i < processes.size() - 1)
                result.append(", ");

        }

        result.append("]");

        return result.toString();
    }

    public void printLog() {

        System.out.println("\n===== Scheduling Matrices =====");

        for (String snapshot : snapshots) {
            System.out.println(snapshot);
        }

        System.out.println("\n===== Scheduling Events =====");

        if (events.isEmpty()) {
            System.out.println("No events recorded.");
        } else {
            for (String event : events) {
                System.out.println(event);
            }
        }

        System.out.println("\n===== Scheduling Timeline =====");

        engine.printTimeline();
    }
}
