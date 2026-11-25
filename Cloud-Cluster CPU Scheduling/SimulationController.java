import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SimulationController {

    @FXML private Label currentTimeLabel, runningCpuLabel;
    @FXML private Slider speedSlider;
    @FXML private Button playPauseBtn;
    @FXML private HBox ganttChartContainer, tier1QueueBox, tier2QueueBox;
    @FXML private TableView<SimulationContext.TickSnapshot> executionTableView;
    @FXML private TableColumn<SimulationContext.TickSnapshot, String> logTimeCol;
    @FXML private TableColumn<SimulationContext.TickSnapshot, String> logCpuCol;
    @FXML private TableColumn<SimulationContext.TickSnapshot, String> logTier1Col;
    @FXML private TableColumn<SimulationContext.TickSnapshot, String> logTier2Col;
    private int currentStepIndex = 0;
    private Timeline playTimer;
    private boolean isPlaying = false;

    @FXML
    public void initialize() {
        logTimeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty("T = " + d.getValue().time));
        logCpuCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().runningLabel));
        logTier1Col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().tier1Text));
        logTier2Col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().tier2Text));

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isPlaying) {
                restartTimerWithCurrentSpeed();
            }
        });
    }

    public void loadSimulationData() {
        stopTimer();
        currentStepIndex = 0;
        if (!SimulationContext.tickSnapshots.isEmpty()) {
            renderUpToStep(0);
        } else {
            ganttChartContainer.getChildren().clear();
            tier1QueueBox.getChildren().clear();
            tier2QueueBox.getChildren().clear();
            executionTableView.getItems().clear();
        }
    }

    private void renderUpToStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= SimulationContext.tickSnapshots.size()) return;

        SimulationContext.TickSnapshot currentSnap = SimulationContext.tickSnapshots.get(stepIndex);
        int maxTimeShown = currentSnap.time + 1;

        currentTimeLabel.setText("Current Time: T = " + currentSnap.time + " → " + maxTimeShown);
        runningCpuLabel.setText("CPU State: " + currentSnap.runningLabel);

        ganttChartContainer.getChildren().clear();
        for (SimulationEngine.Segment seg : SimulationContext.currentTimeline) {
            if (seg.start >= maxTimeShown) break;

            int visibleEnd = Math.min(seg.end, maxTimeShown);
            int duration = visibleEnd - seg.start;
            if (duration <= 0) continue;

            VBox block = new VBox(4);
            block.setAlignment(Pos.CENTER);
            double width = Math.max(68, duration * 42);
            block.setMinWidth(width);
            block.setPrefWidth(width);
            block.setPrefHeight(65);

            String color = switch (seg.type) {
                case CONTEXT_SWITCH -> "#cf222e";
                case IDLE -> "#8c959f";
                case RUN -> "#0969da";
            };
            String title = (seg.type == SimulationEngine.SegmentType.RUN) ? seg.process.id
                    : (seg.type == SimulationEngine.SegmentType.CONTEXT_SWITCH ? "CS" : "IDLE");

            block.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6; -fx-padding: 5;");
            Label nameLbl = new Label(title);
            nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            Label timeLbl = new Label(seg.start + "→" + visibleEnd);
            timeLbl.setStyle("-fx-text-fill: #f0f6fc; -fx-font-size: 11px;");

            block.getChildren().addAll(nameLbl, timeLbl);
            ganttChartContainer.getChildren().add(block);
        }

        tier1QueueBox.getChildren().clear();
        for (String item : currentSnap.tier1List) {
            Label badge = new Label(item);
            badge.setStyle("-fx-background-color: #1a7f37; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6; -fx-font-weight: bold;");
            tier1QueueBox.getChildren().add(badge);
        }

        tier2QueueBox.getChildren().clear();
        for (String item : currentSnap.tier2List) {
            Label badge = new Label(item);
            badge.setStyle("-fx-background-color: #9a6700; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6; -fx-font-weight: bold;");
            tier2QueueBox.getChildren().add(badge);
        }

        StringBuilder sb = new StringBuilder();
        executionTableView.getItems().setAll(SimulationContext.tickSnapshots.subList(0, stepIndex + 1));
        executionTableView.scrollTo(stepIndex);
    }

    @FXML
    private void handleNextStep() {
        if (currentStepIndex + 1 < SimulationContext.tickSnapshots.size()) {
            currentStepIndex++;
            renderUpToStep(currentStepIndex);
        } else {
            stopTimer();
        }
    }

    @FXML
    private void handlePrevStep() {
        stopTimer();
        if (currentStepIndex > 0) {
            currentStepIndex--;
            renderUpToStep(currentStepIndex);
        }
    }

    @FXML
    private void handlePlayPause() {
        if (isPlaying) {
            stopTimer();
        } else {
            if (currentStepIndex >= SimulationContext.tickSnapshots.size() - 1) {
                currentStepIndex = 0;
                renderUpToStep(0);
            }
            isPlaying = true;
            playPauseBtn.setText("⏸ Pause");
            restartTimerWithCurrentSpeed();
        }
    }

    private void restartTimerWithCurrentSpeed() {
        if (playTimer != null) {
            playTimer.stop();
        }
        double speedVal = speedSlider.getValue();
        double delayMillis = 1320 - (speedVal * 12);

        playTimer = new Timeline(new KeyFrame(Duration.millis(delayMillis), e -> {
            if (currentStepIndex + 1 < SimulationContext.tickSnapshots.size()) {
                currentStepIndex++;
                renderUpToStep(currentStepIndex);
            } else {
                stopTimer();
            }
        }));
        playTimer.setCycleCount(Timeline.INDEFINITE);
        playTimer.play();
    }

    private void stopTimer() {
        if (playTimer != null) {
            playTimer.stop();
        }
        isPlaying = false;
        if (playPauseBtn != null) {
            playPauseBtn.setText("▶ Play");
        }
    }

    @FXML
    private void handleReset() {
        stopTimer();
        currentStepIndex = 0;
        if (!SimulationContext.tickSnapshots.isEmpty()) {
            renderUpToStep(0);
        }
    }
}