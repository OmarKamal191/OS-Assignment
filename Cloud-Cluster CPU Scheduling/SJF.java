import java.util.*;

public class SJF implements Scheduler {

    private final List<Process> processes;
    private final int contextSwitchTime;
    private SimulationEngine engine;
    public SJF(List<Process> processes, int contextSwitchTime) {
        this.processes = processes;
        this.contextSwitchTime = contextSwitchTime;
    }

    @Override
    public String getName() {
        return "Preemptive SJF";
    }

    @Override
    public void simulate() {
        engine = new SimulationEngine(processes, contextSwitchTime);
        while (!engine.allFinished()) {
            int time = engine.getCurrentTime();
            Process shortest = null;
            for (Process p : processes) {
                if (p.arrivalTime <= time && !p.isFinished()) {
                    if (shortest == null ||
                            p.remainingTime < shortest.remainingTime ||
                            (p.remainingTime == shortest.remainingTime &&
                                    p.arrivalTime < shortest.arrivalTime)) {
                        shortest = p;
                    }
                }
            }
            if (shortest == null) {
                int nextArrival = engine.nextArrivalTime();
                if (nextArrival != -1) {
                    engine.idleUntil(nextArrival);
                }
                continue;
            }
            engine.runProcess(shortest, 1);
        }
        calculateMetrics();
    }

    private void calculateMetrics() {
        double totalWaiting = 0;
        double totalTurnaround = 0;
        double totalResponse = 0;
        for (Process p : processes) {
            p.turnaroundTime = p.finishTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;
            p.responseTime = p.firstStartTime - p.arrivalTime;
            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
            totalResponse += p.responseTime;
        }

        int n = processes.size();
        System.out.printf("Average Waiting Time: %.2f%n", totalWaiting / n);
        System.out.printf("Average Turnaround Time: %.2f%n", totalTurnaround / n);
        System.out.printf("Average Response Time: %.2f%n", totalResponse / n);
    }

    @Override
    public List<Process> getProcesses() {
        return processes;
    }

    @Override
    public List<SimulationEngine.Segment> getTimeline() {
        return engine == null ? Collections.emptyList() : engine.getTimeline();
    }

    public void printResults() {
        System.out.println("\n===== " + getName() + " =====");
        System.out.println("\nGantt Chart:");
        for (SimulationEngine.Segment s : getTimeline()) {
            System.out.println(s);
        }
        System.out.println("\nProcess Metrics:");
        System.out.printf("%-10s %-10s %-10s %-10s %-10s %-12s %-10s%n",
                "Process", "Arrival", "Burst", "Finish", "Waiting", "Turnaround", "Response");
        for (Process p : processes) {
            System.out.printf("%-10s %-10d %-10d %-10d %-10d %-12d %-10d%n",
                    p.id, p.arrivalTime, p.burstTime, p.finishTime,
                    p.waitingTime, p.turnaroundTime, p.responseTime);
        }

        System.out.println("\nAverage Metrics:");
        double totalWaiting = 0;
        double totalTurnaround = 0;
        double totalResponse = 0;

        for (Process p : processes) {
            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
            totalResponse += p.responseTime;
        }

        int n = processes.size();
        System.out.printf("Average Waiting Time: %.2f%n", totalWaiting / n);
        System.out.printf("Average Turnaround Time: %.2f%n", totalTurnaround / n);
        System.out.printf("Average Response Time: %.2f%n", totalResponse / n);
    }
}