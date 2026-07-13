package com.cimpor.opc.ui;

import com.cimpor.opc.utils.ConfigManager;
import com.cimpor.opc.workers.ApiServerService;
import com.cimpor.opc.workers.OpcInfluxGatewayService;
import com.cimpor.opc.workers.SetpointWatcherService;
import com.cimpor.opc.workers.SimulatorService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea logArea;
    @FXML private Label opcStatusLabel;

    // OPC UA Config
    @FXML private TextField opcEndpointInput;
    @FXML private TextField opcUsernameInput;
    @FXML private PasswordField opcPasswordInput;
    @FXML private TextField opcBrowsePathInput;
    @FXML private TextField opcMeasurementInput;
    @FXML private CheckBox useCertSecurityChk;

    // InfluxDB Config
    @FXML private TextField influxUrlInput;
    @FXML private PasswordField influxTokenInput;
    @FXML private TextField influxOrgInput;
    @FXML private TextField influxBucketInput;
    @FXML private Spinner<Integer> writeIntervalSpinner;

    // PI System & Simulator
    @FXML private TextField piUrlInput;
    @FXML private TextField piUsernameInput;
    @FXML private PasswordField piPasswordInput;
    @FXML private TextField csvPathInput;

    private OpcInfluxGatewayService gatewayService;
    private Thread gatewayThread;

    @FXML
    public void initialize() {
        logMessage("UI Initialized. Loading configuration...");
        stopBtn.setDisable(true);
        
        // Setup Spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 60000, 1000, 100);
        writeIntervalSpinner.setValueFactory(valueFactory);

        loadConfigToUI();
    }

    private void loadConfigToUI() {
        Map<String, Object> config = ConfigManager.loadConfig();
        if (config == null || config.isEmpty()) return;

        Map<String, Object> opcConfig = (Map<String, Object>) config.getOrDefault("opc_config", new HashMap<>());
        opcEndpointInput.setText((String) opcConfig.getOrDefault("url", "opc.tcp://localhost:4840"));
        opcUsernameInput.setText((String) opcConfig.getOrDefault("username", ""));
        opcPasswordInput.setText((String) opcConfig.getOrDefault("password", ""));
        useCertSecurityChk.setSelected((Boolean) opcConfig.getOrDefault("use_cert_security", false));
        opcBrowsePathInput.setText((String) config.getOrDefault("opc_browse_path", ""));
        opcMeasurementInput.setText((String) config.getOrDefault("opc_measurement", "kiln1_opc"));

        Map<String, Object> influxConfig = (Map<String, Object>) config.getOrDefault("influx_config", new HashMap<>());
        influxUrlInput.setText((String) influxConfig.getOrDefault("url", "http://localhost:8086"));
        influxTokenInput.setText((String) influxConfig.getOrDefault("token", ""));
        influxOrgInput.setText((String) influxConfig.getOrDefault("org", "my-org"));
        influxBucketInput.setText((String) influxConfig.getOrDefault("bucket", "my-bucket"));
        
        if (config.containsKey("write_interval")) {
            writeIntervalSpinner.getValueFactory().setValue(((Number) config.get("write_interval")).intValue());
        }

        piUrlInput.setText((String) config.getOrDefault("pi_url", ""));
        piUsernameInput.setText((String) config.getOrDefault("pi_username", ""));
        piPasswordInput.setText((String) config.getOrDefault("pi_password", ""));
        csvPathInput.setText((String) config.getOrDefault("csv_file_path", ""));
    }

    private void saveConfigFromUI() {
        Map<String, Object> config = ConfigManager.loadConfig();
        if (config == null) config = new HashMap<>();

        Map<String, Object> opcConfig = new HashMap<>();
        opcConfig.put("url", opcEndpointInput.getText());
        opcConfig.put("username", opcUsernameInput.getText());
        opcConfig.put("password", opcPasswordInput.getText());
        opcConfig.put("use_cert_security", useCertSecurityChk.isSelected());
        config.put("opc_config", opcConfig);

        Map<String, Object> influxConfig = new HashMap<>();
        influxConfig.put("url", influxUrlInput.getText());
        influxConfig.put("token", influxTokenInput.getText());
        influxConfig.put("org", influxOrgInput.getText());
        influxConfig.put("bucket", influxBucketInput.getText());
        config.put("influx_config", influxConfig);

        config.put("opc_browse_path", opcBrowsePathInput.getText());
        config.put("opc_measurement", opcMeasurementInput.getText());
        config.put("write_interval", writeIntervalSpinner.getValue());
        config.put("pi_url", piUrlInput.getText());
        config.put("pi_username", piUsernameInput.getText());
        config.put("pi_password", piPasswordInput.getText());
        config.put("csv_file_path", csvPathInput.getText());

        ConfigManager.saveConfig(config);
    }

    @FXML
    public void onStartClick() {
        saveConfigFromUI();
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        opcStatusLabel.setText("Status: Connecting...");
        
        Map<String, Object> config = ConfigManager.loadConfig();
        Map<String, Object> opcConfig = (Map<String, Object>) config.get("opc_config");
        Map<String, Object> influxConfig = (Map<String, Object>) config.get("influx_config");
        Map<String, String> selectedTags = (Map<String, String>) config.getOrDefault("selected_tags", new HashMap<>());

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
