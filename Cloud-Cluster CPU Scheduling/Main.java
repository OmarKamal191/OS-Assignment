import java.util.List;

public class Main {

    public static void main(String[] args) {

        for (TestCase testCase : TestCase.all()) {
            System.out.println("\n====================================");
            System.out.println(testCase);
            System.out.println("\n====================================");

            runSJF(testCase);
            runACS(testCase);
        }
    }

    private static void runSJF(TestCase testCase) {

        System.out.println("\n========== SJF ==========");

        List<Process> processes = testCase.getProcesses();

        SJF sjf = new SJF(processes, testCase.contextSwitch);

        sjf.simulate();
        sjf.printResults();
    }

    private static void runACS(TestCase testCase) {

        System.out.println("\n========== ACS ==========");

        List<Process> processes = testCase.getProcesses();

        SimulationEngine engine = new SimulationEngine(processes, testCase.contextSwitch);

        ACSTier1 tier1 = new ACSTier1(testCase.quantum);
        ACSTier2 tier2 = new ACSTier2();

        AgingManager agingManager = new AgingManager(testCase.agingInterval);

        SimulationLogger logger = new SimulationLogger(engine, tier1, tier2);

        ACSScheduler acs = new ACSScheduler(engine, tier1, tier2, agingManager);

        acs.setLogger(logger);

        acs.run();

        logger.printLog();

        SchedulerMetrics.calculateMetrics(processes);
        SchedulerMetrics.printResults(processes);

        if (testCase.expectedAcsFinish != null) {

            System.out.println("\n===== Finish Time Validation =====");

            for (Process p : processes) {

                int expected = testCase.expectedAcsFinish.get(p.id);

                System.out.printf(
                        "%s | Expected: %d | Actual: %d | %s%n",
                        p.id, expected, p.finishTime, expected == p.finishTime ? "PASS" : "FAIL");
            }
        }
    }
}