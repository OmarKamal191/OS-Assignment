import java.util.List;

public interface Scheduler {

    String getName();
    void simulate();
    List<Process> getProcesses();
    List<SimulationEngine.Segment> getTimeline();
}
