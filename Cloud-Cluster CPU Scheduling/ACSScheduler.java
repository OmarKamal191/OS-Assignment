import java.util.*;

public class ACSScheduler {

    private final SimulationEngine engine;
    private final ACSTier1 tier1;
    private final ACSTier2 tier2;
    private final AgingManager agingManager;
    private SimulationLogger logger;

    public ACSScheduler(SimulationEngine engine, ACSTier1 tier1, ACSTier2 tier2, AgingManager agingManager) {

        this.engine = engine;
        this.tier1 = tier1;
        this.tier2 = tier2;
        this.agingManager = agingManager;
    }

    public void setLogger(SimulationLogger logger) {
        this.logger = logger;
    }

    public void run() {

        while (!engine.allFinished()) {

            List<Process> arrived = engine.releaseArrivals();
            tier1.admit(arrived);

            if (!tier1.isEmpty()) {

                Process p = tier1.pickNext();

                ACSTier1.Outcome result = tier1.runSlice(engine, p);

                if (result == ACSTier1.Outcome.DEMOTED)
                    tier2.admit(p);

            }

            else if (!tier2.isEmpty()) {

                Map<Process, Integer> oldPriorities = new IdentityHashMap<>();

                for (Process p : tier2.snapshot())
                    oldPriorities.put(p, p.effectivePriority);

                int updateTime = engine.getCurrentTime();

                agingManager.updatePriorities(tier2, updateTime);

                if (logger != null) {
                    for (Process p : tier2.snapshot()) {
                        Integer oldPriority = oldPriorities.get(p);
                        int newPriority = p.effectivePriority;

                        if (oldPriority != null && oldPriority != newPriority)
                            logger.recordAgingEvent(updateTime, p, oldPriority, newPriority);
                    }
                }

                Process p = tier2.pickNext();

                engine.runToCompletion(p);
            }

            else {

                int nextArrival = engine.nextArrivalTime();

                if (nextArrival != -1)
                    engine.idleUntil(nextArrival);

            }
        }
    }
}
