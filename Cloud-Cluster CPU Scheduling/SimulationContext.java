import java.util.*;

public class SimulationContext {
    public static class TickSnapshot {
        public final int time;
        public final String runningLabel;
        public final List<String> tier1List;
        public final List<String> tier2List;
        public final String tier1Text;
        public final String tier2Text;

        public TickSnapshot(int time, String runningLabel, List<String> tier1List, List<String> tier2List) {
            this.time = time;
            this.runningLabel = runningLabel;
            this.tier1List = tier1List;
            this.tier2List = tier2List;
            this.tier1Text = tier1List.isEmpty() ? "" : String.join(", ", tier1List);
            this.tier2Text = tier2List.isEmpty() ? "" : String.join(", ", tier2List);
        }
    }

    public static List<Process> currentProcesses = new ArrayList<>();
    public static List<SimulationEngine.Segment> currentTimeline = new ArrayList<>();
    public static List<TickSnapshot> tickSnapshots = new ArrayList<>();
    public static String selectedScheduler = "Adaptive Container Scheduler (ACS)";
    public static int quantum = 3;
    public static int contextSwitch = 1;
    public static int agingInterval = 4;
}