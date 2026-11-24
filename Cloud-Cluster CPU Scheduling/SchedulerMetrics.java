import java.util.List;

public class SchedulerMetrics {

    public static void calculateMetrics(List<Process> processes) {

        for (Process p : processes) {

            if (p.finishTime < 0 || p.firstStartTime < 0)
                throw new IllegalStateException("Process has not finised: " + p.id);

            p.turnaroundTime = p.finishTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;
            p.responseTime = p.firstStartTime - p.arrivalTime;
        }
    };

    public static double getAverageWaitingTime(List<Process> processes) {

        if (processes.isEmpty())
            return 0;

        double total = 0;

        for (Process p : processes)
            total += p.waitingTime;

        return total / processes.size();
    };

    public static double getAverageTurnaroundTime(List<Process> processes) {

        if (processes.isEmpty())
            return 0;

        double total = 0;

        for (Process p : processes)
            total += p.turnaroundTime;

        return total / processes.size();

    };

    public static double getAverageResponseTime(List<Process> processes) {
        if (processes.isEmpty())
            return 0;

        double total = 0;

        for (Process p : processes)
            total += p.responseTime;

        return total / processes.size();
    };

    public static void printResults(List<Process> processes) {

        System.out.printf(
                "%-10s %-8s %-8s %-8s %-8s %-12s %-8s%n",
                "Process", "Arrival", "Burst", "Finish",
                "Waiting", "Turnaround", "Response");

        for (Process p : processes) {
            System.out.printf(
                    "%-10s %-8d %-8d %-8d %-8d %-12d %-8d%n",
                    p.id,
                    p.arrivalTime,
                    p.burstTime,
                    p.finishTime,
                    p.waitingTime,
                    p.turnaroundTime,
                    p.responseTime);
        }

        System.out.println("\nAverage Metrics:");

        System.out.printf(
                "Average Waiting Time: %.2f%n",
                getAverageWaitingTime(processes));

        System.out.printf(
                "Average Turnaround Time: %.2f%n",
                getAverageTurnaroundTime(processes));

        System.out.printf(
                "Average Response Time: %.2f%n",
                getAverageResponseTime(processes));
    };
}