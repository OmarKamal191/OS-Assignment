import java.util.*;

/**
 * TestCase.java  (Person 1)
 * Holds the input of one test: process list + Q + CS + aging interval.
 * getProcesses() ALWAYS returns fresh copies, so each scheduler gets its own objects.
 */
public class TestCase {

    public final String name;
    public final int quantum;
    public final int contextSwitch;
    public final int agingInterval;
    /** Expected ACS finish times from the assignment (null if unknown). Used by Person 5 to verify. */
    public final Map<String, Integer> expectedAcsFinish;

    private final List<Process> template = new ArrayList<>();

    public TestCase(String name, int quantum, int contextSwitch, int agingInterval,
                    Map<String, Integer> expectedAcsFinish) {
        this.name = name;
        this.quantum = quantum;
        this.contextSwitch = contextSwitch;
        this.agingInterval = agingInterval;
        this.expectedAcsFinish = expectedAcsFinish;
    }

    public TestCase add(String id, int arrival, int burst, int priority) {
        template.add(new Process(id, arrival, burst, priority));
        return this;
    }

    /** Fresh, untouched copies of all processes (arrival order = insertion order). */
    public List<Process> getProcesses() {
        List<Process> copies = new ArrayList<>();
        for (Process p : template) copies.add(p.copy());
        return copies;
    }

    /** Same processes with different Q / CS (for the report's CS-vs-Q experiments). */
    public TestCase withParams(int newQuantum, int newCS) {
        TestCase t = new TestCase(name + " (Q=" + newQuantum + ", CS=" + newCS + ")",
                newQuantum, newCS, agingInterval, null);
        for (Process p : template) t.add(p.id, p.arrivalTime, p.burstTime, p.initialPriority);
        return t;
    }

    /** Builds a test from lines like "P1 0 4 2" (id arrival burst priority). */
    public static TestCase fromText(String name, int q, int cs, int agingInterval, String... lines) {
        TestCase t = new TestCase(name, q, cs, agingInterval, null);
        for (String line : lines) {
            String[] f = line.trim().split("\\s+");
            if (f.length != 4) throw new IllegalArgumentException("Bad line: " + line);
            t.add(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]));
        }
        return t;
    }

    // ------------------------------------------------------------ assignment test cases

    public static TestCase testCase1() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("P1", 9); expected.put("P2", 13); expected.put("P3", 8);
        return new TestCase("Test Case 1", 3, 0, 4, expected)
                .add("P1", 0, 4, 2)
                .add("P2", 1, 7, 3)
                .add("P3", 3, 2, 1);
    }

    public static TestCase testCase2() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("P1", 18); expected.put("P2", 15); expected.put("P3", 20);
        return new TestCase("Test Case 2", 3, 1, 4, expected)
                .add("P1", 0, 5, 4)
                .add("P2", 1, 6, 2)
                .add("P3", 4, 4, 3);
    }

    public static List<TestCase> all() {
        return Arrays.asList(testCase1(), testCase2());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name + "  [Q=" + quantum + ", CS=" + contextSwitch
                + ", AgingInterval=" + agingInterval + "]\n");
        sb.append(String.format("  %-8s %-8s %-8s %-8s%n", "Process", "Arrival", "Burst", "Priority"));
        for (Process p : template)
            sb.append(String.format("  %-8s %-8d %-8d %-8d%n", p.id, p.arrivalTime, p.burstTime, p.initialPriority));
        return sb.toString();
    }
}
