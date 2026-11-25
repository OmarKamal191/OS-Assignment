import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.*;

public class ConfigurationController {

    @FXML private ComboBox<String> schedulerComboBox;
    @FXML private Spinner<Integer> quantumSpinner;
    @FXML private Spinner<Integer> csSpinner;
    @FXML private Spinner<Integer> agingSpinner;

    @FXML private TextField pidField, arrivalField, burstField, priorityField;
    @FXML private TableView<Process> processTableView;
    @FXML private TableColumn<Process, String> colPid;
    @FXML private TableColumn<Process, Number> colArrival, colBurst, colPriority;

    private final ObservableList<Process> processList = FXCollections.observableArrayList();
    private Runnable onSimulationComplete;

    public void setOnSimulationComplete(Runnable callback) {
        this.onSimulationComplete = callback;
    }

    @FXML
    public void initialize() {
        schedulerComboBox.setItems(FXCollections.observableArrayList(
                "Adaptive Container Scheduler (ACS)",
                "Preemptive SJF"
        ));
        schedulerComboBox.getSelectionModel().selectFirst();

        quantumSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 3));
        csSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 1));
        agingSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 4));

        colPid.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().id));
        colArrival.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().arrivalTime));
        colBurst.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().burstTime));
        colPriority.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().initialPriority));

        processTableView.setItems(processList);
    }

    @FXML
    private void handleLoadTestCase1() {
        loadFromTestCase(TestCase.testCase1());
    }

    @FXML
    private void handleLoadTestCase2() {
        loadFromTestCase(TestCase.testCase2());
    }

    private void loadFromTestCase(TestCase tc) {
        quantumSpinner.getValueFactory().setValue(tc.quantum);
        csSpinner.getValueFactory().setValue(tc.contextSwitch);
        agingSpinner.getValueFactory().setValue(tc.agingInterval);
        processList.setAll(tc.getProcesses());
    }

    @FXML
    private void handleAddProcess() {
        try {
            String pid = pidField.getText().trim();
            int arr = Integer.parseInt(arrivalField.getText().trim());
            int burst = Integer.parseInt(burstField.getText().trim());
            int prio = Integer.parseInt(priorityField.getText().trim());
            processList.add(new Process(pid, arr, burst, prio));
            pidField.clear();
            arrivalField.clear();
            burstField.clear();
            priorityField.clear();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Invalid process input: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleRemoveSelected() {
        Process selected = processTableView.getSelectionModel().getSelectedItem();
        if (selected != null) processList.remove(selected);
    }

    @FXML
    private void handleClearProcesses() {
        processList.clear();
    }

    @FXML
    private void handleRunSimulation() {
        if (processList.isEmpty()) return;

        List<Process> copies = new ArrayList<>();
        for (Process p : processList) copies.add(p.copy());

        int q = quantumSpinner.getValue();
        int cs = csSpinner.getValue();
        int aging = agingSpinner.getValue();
        String algo = schedulerComboBox.getValue();

        SimulationContext.quantum = q;
        SimulationContext.contextSwitch = cs;
        SimulationContext.agingInterval = aging;
        SimulationContext.selectedScheduler = algo;
        SimulationContext.tickSnapshots.clear();

        if (algo.contains("SJF")) {
            SJF sjf = new SJF(copies, cs);
            sjf.simulate();
            SimulationContext.currentProcesses = sjf.getProcesses();
            SimulationContext.currentTimeline = sjf.getTimeline();

            for (SimulationEngine.Segment seg : sjf.getTimeline()) {
                for (int t = seg.start; t < seg.end; t++) {
                    String label = (seg.type == SimulationEngine.SegmentType.RUN) ? seg.process.id
                            : (seg.type == SimulationEngine.SegmentType.CONTEXT_SWITCH ? "CS -> " + seg.process.id : "IDLE");
                    SimulationContext.tickSnapshots.add(
                            new SimulationContext.TickSnapshot(t, label, Collections.emptyList(), Collections.emptyList())
                    );
                }
            }
        } else {
            SimulationEngine engine = new SimulationEngine(copies, cs);
            ACSTier1 tier1 = new ACSTier1(q);
            ACSTier2 tier2 = new ACSTier2();
            AgingManager agingManager = new AgingManager(aging);

            engine.setStepListener(new SimulationEngine.StepListener() {
                @Override
                public void onTick(int time, Process running) {
                    captureTick(time, running.id, running, copies, tier1, tier2);
                }

                @Override
                public void onContextSwitchTick(int time, Process next) {
                    captureTick(time, "CS -> " + next.id, null, copies, tier1, tier2);
                }

                @Override
                public void onIdleTick(int time) {
                    captureTick(time, "IDLE", null, copies, tier1, tier2);
                }
            });

            ACSScheduler acs = new ACSScheduler(engine, tier1, tier2, agingManager);
            acs.run();
            SchedulerMetrics.calculateMetrics(copies);

            SimulationContext.currentProcesses = copies;
            SimulationContext.currentTimeline = engine.getTimeline();
        }

        if (onSimulationComplete != null) onSimulationComplete.run();
    }

    private void captureTick(int time, String label, Process runningProcess, List<Process> allProcesses, ACSTier1 t1, ACSTier2 t2) {
        List<String> q1 = new ArrayList<>();
        List<String> q2 = new ArrayList<>();

        if (runningProcess != null) {
            if (!runningProcess.demoted) {
                q1.add(runningProcess.toString());
            } else {
                q2.add(runningProcess.toString());
            }
        }

        Set<Process> seenT1 = Collections.newSetFromMap(new IdentityHashMap<>());
        if (runningProcess != null && !runningProcess.demoted) seenT1.add(runningProcess);

        for (Process p : t1.snapshot()) {
            if (seenT1.add(p)) q1.add(p.toString());
        }
        for (Process p : allProcesses) {
            if (p.arrivalTime <= time && !p.isFinished() && !p.demoted && seenT1.add(p)) {
                q1.add(p.toString());
            }
        }

        for (Process p : t2.snapshot()) {
            if (p != runningProcess) {
                q2.add(p.toString());
            }
        }

        SimulationContext.tickSnapshots.add(new SimulationContext.TickSnapshot(time, label, q1, q2));
    }
}