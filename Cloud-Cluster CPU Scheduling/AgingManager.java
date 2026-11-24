import java.util.List;

public class AgingManager {

    private final int aginginterval;

    public AgingManager(int agingInterval) {
        if (agingInterval <= 0) {
            throw new IllegalArgumentException("Aging interval must be > 0");
        }
        this.aginginterval = agingInterval;
    }

    public void updatePriorities(ACSTier2 tier2, int currentTime) {
        if (tier2 == null) {
            throw new IllegalArgumentException("Tier 2 cannot be null");
        }

        List<Process> waitingProcesses = tier2.snapshot();

        for (Process p : waitingProcesses) {
            if (p.tier2EntryTime < 0 || currentTime < p.tier2EntryTime) {
                throw new IllegalStateException("Invalid Tier 2 entry time for " + p.id);
            }

            int waitintTime = currentTime - p.tier2EntryTime;

            int aging = waitintTime / aginginterval;

            p.effectivePriority = p.initialPriority - aging;
        }

        tier2.rebuildQueue();
    }
}