import java.util.List;

/**
 * Scheduler.java  (Person 1)
 * Common contract for SJF and ACS. Main.java only talks to this interface.
 *
 * A scheduler is constructed with a SimulationEngine (which already owns the process list),
 * e.g.  new SJF(engine)  /  new ACS(engine, quantum, agingInterval).
 */
public interface Scheduler {

    /** Human readable name, e.g. "Preemptive SJF" or "ACS". */
    String getName();

    /** Runs the whole simulation until every process has finished. */
    void simulate();

    /** The processes after simulation (finishTime / firstStartTime filled). */
    List<Process> getProcesses();

    /** The Gantt timeline (RUN / CONTEXT_SWITCH / IDLE segments). */
    List<SimulationEngine.Segment> getTimeline();
}
