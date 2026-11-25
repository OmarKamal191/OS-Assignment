import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Tab simulationTab;
    @FXML private Tab analyticsTab;

    @FXML private ConfigurationController configViewController;
    @FXML private SimulationController simulationViewController;
    @FXML private AnalyticsController analyticsViewController;

    @FXML
    public void initialize() {
        configViewController.setOnSimulationComplete(() -> {
            simulationViewController.loadSimulationData();
            analyticsViewController.loadAnalyticsData();
            mainTabPane.getSelectionModel().select(simulationTab);
        });
    }
}