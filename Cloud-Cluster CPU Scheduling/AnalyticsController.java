import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import java.util.*;
import javafx.stage.FileChooser;
import java.io.*;

public class AnalyticsController {

    @FXML private Label avgWaitingLabel, avgTurnaroundLabel, avgResponseLabel, cpuUtilizationLabel;
    @FXML private TableView<Process> resultsTableView;
    @FXML private TableColumn<Process, String> resPidCol;
    @FXML private TableColumn<Process, Number> resArrivalCol, resFinishCol, resTurnaroundCol, resWaitingCol, resResponseCol;
    @FXML private LineChart<String, Number> csQuantumChart;

    @FXML
    public void initialize() {
        resPidCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id));
        resArrivalCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().arrivalTime));
        resFinishCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().finishTime));
        resTurnaroundCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().turnaroundTime));
        resWaitingCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().waitingTime));
        resResponseCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().responseTime));
    }

    public void loadAnalyticsData() {
        List<Process> list = SimulationContext.currentProcesses;
        resultsTableView.setItems(FXCollections.observableArrayList(list));

        avgWaitingLabel.setText(String.format("%.2f", SchedulerMetrics.getAverageWaitingTime(list)));
        avgTurnaroundLabel.setText(String.format("%.2f", SchedulerMetrics.getAverageTurnaroundTime(list)));
        avgResponseLabel.setText(String.format("%.2f", SchedulerMetrics.getAverageResponseTime(list)));
    }

    @FXML
    private void handleRunParameterSweep() {
        csQuantumChart.getData().clear();
        XYChart.Series<String, Number> waitSeries = new XYChart.Series<>();
        waitSeries.setName("Avg Waiting Time");

        XYChart.Series<String, Number> turnaroundSeries = new XYChart.Series<>();
        turnaroundSeries.setName("Avg Turnaround Time");

        int[][] configs = {{2, 0}, {2, 1}, {3, 0}, {3, 1}, {4, 1}, {5, 2}};
        for (int[] cfg : configs) {
            int q = cfg[0], cs = cfg[1];
            List<Process> copies = new ArrayList<>();
            for (Process p : SimulationContext.currentProcesses) copies.add(p.copy());

            SimulationEngine engine = new SimulationEngine(copies, cs);
            ACSScheduler acs = new ACSScheduler(engine, new ACSTier1(q), new ACSTier2(), new AgingManager(SimulationContext.agingInterval));
            acs.run();
            SchedulerMetrics.calculateMetrics(copies);

            String label = "Q=" + q + ",CS=" + cs;
            waitSeries.getData().add(new XYChart.Data<>(label, SchedulerMetrics.getAverageWaitingTime(copies)));
            turnaroundSeries.getData().add(new XYChart.Data<>(label, SchedulerMetrics.getAverageTurnaroundTime(copies)));
        }

        csQuantumChart.getData().addAll(waitSeries, turnaroundSeries);
    }

    @FXML
    private void handleExportCsv() {
        List<Process> list = SimulationContext.currentProcesses;
        if (list == null || list.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No simulation results to export. Run a simulation first.").showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Scheduling Metrics Report");
        fileChooser.setInitialFileName("CS241_Scheduling_Results.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(resultsTableView.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Scheduler," + SimulationContext.selectedScheduler);
                writer.println("Quantum (Q)," + SimulationContext.quantum + ",Context Switch (CS)," + SimulationContext.contextSwitch);
                writer.println();
                writer.println("Process,Arrival Time,Burst Time,Finish Time,Turnaround Time,Waiting Time,Response Time");

                for (Process p : list) {
                    writer.printf("%s,%d,%d,%d,%d,%d,%d%n",
                            p.id, p.arrivalTime, p.burstTime, p.finishTime,
                            p.turnaroundTime, p.waitingTime, p.responseTime);
                }

                writer.println();
                writer.printf("Average Waiting Time,%.2f%n", SchedulerMetrics.getAverageWaitingTime(list));
                writer.printf("Average Turnaround Time,%.2f%n", SchedulerMetrics.getAverageTurnaroundTime(list));
                writer.printf("Average Response Time,%.2f%n", SchedulerMetrics.getAverageResponseTime(list));

                new Alert(Alert.AlertType.INFORMATION, "Results exported successfully to:\n" + file.getAbsolutePath()).showAndWait();
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to export CSV: " + e.getMessage()).showAndWait();
            }
        }
    }
}