package com.cimpor.opc.ui;

import com.cimpor.opc.utils.ConfigManager;
import com.cimpor.opc.workers.ApiServerService;
import com.cimpor.opc.workers.OpcInfluxGatewayService;
import com.cimpor.opc.workers.SetpointWatcherService;
import com.cimpor.opc.workers.SimulatorService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;

import java.util.Map;

public class MainController {

    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea logArea;
    @FXML private Label opcStatusLabel;

    private OpcInfluxGatewayService gatewayService;
    private Thread gatewayThread;

    @FXML
    public void initialize() {
        logMessage("UI Initialized. Ready to start.");
        stopBtn.setDisable(true);
    }

    @FXML
    public void onStartClick() {
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        opcStatusLabel.setText("Status: Connecting...");
        
        Map<String, Object> config = ConfigManager.loadConfig();
        Map<String, Object> opcConfig = (Map<String, Object>) config.get("opc_config");
        Map<String, Object> influxConfig = (Map<String, Object>) config.get("influx_config");
        Map<String, String> selectedTags = (Map<String, String>) config.get("selected_tags");

        gatewayService = new OpcInfluxGatewayService(opcConfig, influxConfig, selectedTags);
        gatewayThread = new Thread(gatewayService);
        gatewayThread.start();
        
        logMessage("Started OPC-Influx Gateway.");
        opcStatusLabel.setText("Status: Running");
    }

    @FXML
    public void onStopClick() {
        if (gatewayService != null) {
            gatewayService.stop();
        }
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        opcStatusLabel.setText("Status: Stopped");
        logMessage("Stopped Gateway.");
    }

    public void logMessage(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }
}
