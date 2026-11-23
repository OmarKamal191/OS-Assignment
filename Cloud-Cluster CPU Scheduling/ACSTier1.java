import java.util.*;

public class ACSTier1 {

    public enum Outcome { FINISHED, DEMOTED }

    private final Queue<Process> queue = new LinkedList<>();
    private final int quantum;

    public ACSTier1(int quantum) {
        if (quantum <= 0) throw new IllegalArgumentException("Quantum must be > 0");
        this.quantum = quantum;
    }

    public void admit(List<Process> arrived) {
        queue.addAll(arrived);
    }

    public boolean isEmpty() { return queue.isEmpty(); }

    public Process pickNext() { return queue.poll(); }

    public Outcome runSlice(SimulationEngine engine, Process p) {
        engine.runProcess(p, quantum);
        if (p.isFinished()) {
            return Outcome.FINISHED;
        }
        p.demoted = true;
        p.tier2EntryTime = engine.getCurrentTime();
        return Outcome.DEMOTED;
    }

    public List<Process> snapshot() { return new ArrayList<>(queue); }

    public int getQuantum() { return quantum; }
}
