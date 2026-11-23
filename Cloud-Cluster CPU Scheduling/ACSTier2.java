
import java.util.*;

public class ACSTier2 {

    private final PriorityQueue<Process> queue;

    
    private final Comparator<Process> priorityComparator =
            Comparator.comparingInt((Process p) -> p.effectivePriority)
                    .thenComparingInt(p -> p.tier2EntryTime)
                    .thenComparingInt(p -> p.arrivalTime)
                    .thenComparing(p -> p.id);

    public ACSTier2() {
        queue = new PriorityQueue<>(priorityComparator);
    }

    
    public void admit(Process p) {
        if (p == null) {
            throw new IllegalArgumentException("Process cannot be null");
        }

        if (p.isFinished()) {
            return;
        }

        queue.offer(p);
    }

    
    public void admit(List<Process> arrived) {
        for (Process p : arrived) {
            admit(p);
        }
    }

    
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    
    public int size() {
        return queue.size();
    }

    
    public Process pickNext() {
        return queue.poll();
    }

   
    public void runProcess(SimulationEngine engine, Process p) {
        if (p == null) {
            throw new IllegalArgumentException("Process cannot be null");
        }

        if (p.isFinished()) {
            return;
        }

        engine.runToCompletion(p);
    }

    
    public void rebuildQueue() {
        List<Process> processes = new ArrayList<>(queue);

        queue.clear();

        queue.addAll(processes);
    }

    
    public List<Process> snapshot() {
        List<Process> processes = new ArrayList<>(queue);

        processes.sort(priorityComparator);

        return processes;
    }
}
