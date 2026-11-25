# Cloud-Cluster CPU Scheduling Simulator

**Course:** CS241/SCS241 – Operating Systems  
**Assignment 2:** Cloud-Cluster CPU Scheduling Simulator  
**Faculty of Computing and Artificial Intelligence, Cairo University**

| Name | ID |
|------|----|
| Omar Ahmed | 20240360 |
| Adham Amr | 20240070 |
| Mohamed Tarek | 20240508 |
| Mohamed Atef | 20240513 |
| Mariam Ashraf | 20240572 |

---

## 1. Overview

This project simulates a discrete-time **Cloud-Cluster CPU Scheduling Engine** in Java with both a **Command-Line Interface (CLI)** and an interactive **JavaFX Graphical User Interface (GUI)**. It models and compares two scheduling algorithms under **Context Switch ($CS$) overhead**:

1. **Preemptive Shortest Job First (SJF):** Dispatches the arrived process with the shortest remaining burst time, preempting the running process when a strictly shorter job arrives.
2. **Adaptive Container Scheduler (ACS):** A three-tier feedback scheduler designed to balance fast response times for short interactive tasks with high CPU utilization for long batch jobs:
   - **Tier 1 (Round Robin Queue):** Serves newly arrived interactive tasks using a fixed time quantum ($Q$). If a process does not complete within $Q$, it is heuristically demoted to Tier 2.
   - **Tier 2 (Non-Preemptive Priority Queue):** Executes demoted batch jobs in priority order (lower numerical value = higher priority) non-preemptively until completion, only when Tier 1 is empty.
   - **Tier 3 (Starvation Prevention via Aging):** Periodically boosts the effective priority of waiting processes in Tier 2 based on their accumulated waiting time relative to an **Aging Interval**.

---

## 2. How to Compile and Run

### 2.1 Console Mode (Automated Test Cases & Validation)

```bash
javac *.java
java Main
```

Running `Main` automatically executes the assignment's pre-configured test cases on both **SJF** and **ACS**, prints the step-by-step heuristic demotion matrices, computes scheduling metrics, and validates finish times (`PASS / FAIL`).

### 2.2 JavaFX GUI Mode (Interactive Simulator)

1. Open the project in **IntelliJ IDEA** and ensure `src` is marked as the **Sources** root.
2. Configure JavaFX (either use **BellSoft Liberica Full JDK**, or link your local JavaFX SDK `lib` folder with VM options `--module-path "<PATH_TO_JAVAFX_SDK>\lib" --add-modules javafx.controls,javafx.fxml`).
3. Run `MainApp.java` to launch the three-tab interactive dashboard (`Configuration & Processes`, `Live Simulation & Queues`, and `Metrics & Analytics`).

---

## 3. Project Structure

| File | Responsibility |
|------|----------------|
| `Main.java` | CLI entry point: runs assignment test cases for SJF and ACS, logs matrices, and validates expected finish times. |
| `MainApp.java` | JavaFX application entry point: loads `MainDashboard.fxml` in maximized light-mode view. |
| `Process.java` | Process Control Block (PCB) storing arrival time, burst time, remaining time, initial/effective priority, demotion state, and timing metrics. |
| `SimulationEngine.java` | Discrete-time simulation clock managing timeline segments (`RUN`, `CONTEXT_SWITCH`, `IDLE`) and notifying step listeners. |
| `SimulationContext.java` | Shared state container holding active processes, timeline segments, and per-tick queue snapshots for GUI synchronization. |
| `SJF.java` | Preemptive Shortest Job First scheduler with context switch tracking and result reporting. |
| `ACSScheduler.java` | Coordinates the three-tier ACS lifecycle: arrivals, Tier 1 RR execution, demotion to Tier 2, aging updates, and context switches. |
| `ACSTier1.java` | FIFO Round Robin queue managing quantum tracking and preemption/demotion decisions. |
| `ACSTier2.java` | Non-preemptive priority queue ordering demoted batch jobs by effective priority (breaking ties by arrival time / FIFO). |
| `AgingManager.java` | Computes effective priority updates for waiting Tier 2 jobs to prevent starvation. |
| `SchedulerMetrics.java` | Calculates individual and average Turnaround, Waiting, Response times, and CPU Utilization. |
| `SimulationLogger.java` | Records and prints step-by-step heuristic demotion matrices and aging events in console mode. |
| `TestCase.java` | Encapsulates assignment test cases (`Test Case 1` and `Test Case 2`) and custom benchmark workloads. |
| `ConfigurationController.java` | Controls Tab 1: configures $Q$, $CS$, Aging Interval, scheduler selection, manual process entry, and preset loaders. |
| `SimulationController.java` | Controls Tab 2: renders incremental Gantt Chart blocks, live Tier 1/Tier 2 badges, playback controls (`Play/Pause`, `Next`, `Prev`, `Reset`, `Speed`), and the execution matrix table. |
| `AnalyticsController.java` | Controls Tab 3: displays KPI summary cards, process metrics table, one-click CSV export, and the $(Q, CS)$ sensitivity line chart. |
| `gui_views/*.fxml & style.css` | Scene Builder FXML layouts (`MainDashboard`, `ConfigurationView`, `SimulationView`, `AnalyticsView`) and light-mode stylesheet. |

---

## 4. Scheduling Design & Mathematical Model

### 4.1 Three-Tier ACS Execution Rules

1. **Dispatch Order:** At each decision point, if Tier 1 is non-empty, the head of Tier 1 is dispatched for up to $Q$ time units. Tier 2 is dispatched **only when Tier 1 is empty**.
2. **Heuristic Demotion:** If a process in Tier 1 consumes $Q$ time units and still has `remainingTime > 0`, it is flagged as `demoted = true` and inserted into Tier 2.
3. **Non-Preemptive Tier 2:** Once a Tier 2 process starts executing, it runs continuously to completion without being preempted by newly arriving Tier 1 processes.
4. **Priority Aging Formula (Tier 3):** Before selecting the next process from Tier 2, waiting durations are evaluated:

$$\text{Effective Priority} = \max\left(1,\ \text{Initial Priority} - \left\lfloor \frac{\text{Wait Time}}{\text{Aging Interval}} \right\rfloor\right)$$

### 4.2 Context Switch ($CS$) Handling

- Whenever the CPU switches from executing process $P_i$ to a different process $P_j$ (including after demotion), a `CONTEXT_SWITCH` segment of duration $CS$ is inserted into the timeline before $P_j$ begins execution.
- If the CPU was previously `IDLE` or continues executing the same process without interruption, no context switch overhead is incurred.

### 4.3 Performance Metrics

For each process $P_i$:

- **Turnaround Time:** $TAT_i = \text{Finish Time}_i - \text{Arrival Time}_i$
- **Waiting Time:** $WT_i = TAT_i - \text{Burst Time}_i$
- **Response Time:** $RT_i = \text{First Start Time}_i - \text{Arrival Time}_i$
- **CPU Utilization:** $\frac{\sum \text{Burst Time}}{\text{Total Simulation Time}} \times 100\%$

---

## 5. GUI Features & Interactive Controls

- **Empty-Start Custom Input:** Launches with a clean workload table so custom processes (`PID`, `Arrival`, `Burst`, `Priority`) can be added manually, or assignment presets (`Load Test Case 1` / `Load Test Case 2`) can be loaded with one click.
- **Step-by-Step Timeline & Gantt Chart:** Steps forward (`Next Step`), backward (`Prev Step`), or auto-plays (`Play / Pause` with adjustable `Speed`) while incrementally drawing color-coded Gantt blocks (`RUN` in blue, `CS` in red, `IDLE` in gray).
- **Live Queue & Matrix View:** Displays active Tier 1 and Tier 2 queues alongside a structured 4-column `Heuristic Demotion & Execution Matrix` (`Time | CPU | Tier 1 Queue | Tier 2 Queue`).
- **CS & Q Sensitivity Sweep + CSV Export:** Automatically runs 6 combinations of $(Q, CS)$ to plot their effect on Average Waiting and Turnaround times, and exports all metrics to `.csv` for reporting.

---

## 6. Sample Run (CLI Validation — Test Case 2)

**Configuration:** `Quantum (Q) = 3`, `Context Switch (CS) = 1`, `Aging Interval = 4`

```text
====================================
Test Case 2 | Q=3 | CS=1 | Aging=4
====================================

========== ACS ==========
===== Process Metrics =====
Process    Arrival    Burst      Finish     Waiting    Turnaround   Response  
P1         0          5          18         13         18           0         
P2         1          6          15         8          14           3         
P3         4          4          20         12         16           4         

Average Waiting Time: 11.00
Average Turnaround Time: 16.00
Average Response Time: 2.33

===== Finish Time Validation =====
P1 | Expected: 18 | Actual: 18 | PASS
P2 | Expected: 15 | Actual: 15 | PASS
P3 | Expected: 20 | Actual: 20 | PASS
```

---

## 7. Testing

The simulator was verified against the following test scenarios:

| Test Case | Parameters ($Q, CS, \text{Aging}$) | Processes | Purpose |
|-----------|-----------------------------------|-----------|---------|
| **Test Case 1** | $Q=3, CS=0, \text{Aging}=4$ | P1(0,4,2), P2(1,7,3), P3(3,2,1) | Baseline validation without context switch overhead (`PASS`). |
| **Test Case 2** | $Q=3, CS=1, \text{Aging}=4$ | P1(0,5,4), P2(1,6,2), P3(4,4,3) | Full validation with context switches, Tier 2 demotion, and priority aging (`PASS`). |
| **Mixed Workload** | $Q=3, CS=1, \text{Aging}=4$ | 2 heavy batch jobs + 3 short interactive arrivals | Demonstrates fast response times for Tier 1 interactive jobs over SJF. |
| **Aging Stress** | $Q=2, CS=1, \text{Aging}=3$ | Low-priority early arrival (`Priority=6`) vs. high-priority jobs | Verifies starvation prevention via effective priority boosting in Tier 3. |
| **Idle & Non-Preemptive** | $Q=3, CS=1, \text{Aging}=4$ | Arrivals separated by idle gaps and Tier 2 execution | Confirms proper `IDLE` handling and non-preemptive lock in Tier 2. |
| **CS & Q Sweep** | $Q \in [2..5], CS \in [0..2]$ | 4 concurrent medium/long processes | Evaluates sensitivity of Average Waiting and Turnaround times to $Q$ and $CS$. |