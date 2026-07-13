package com.cimpor.opc.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + ".opc_influx_client_selections.json";
    private static final Gson GSON = new Gson();

    public static Map<String, Object> loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return createDefaultConfig();
        }
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> config = GSON.fromJson(reader, type);
            return config != null ? config : createDefaultConfig();
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            return createDefaultConfig();
        }
    }

    public static void saveConfig(Map<String, Object> config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    private static Map<String, Object> createDefaultConfig() {
        Map<String, Object> config = new HashMap<>();
        
        Map<String, Object> opcConfig = new HashMap<>();
        opcConfig.put("url", "opc.tcp://localhost:4840");
        opcConfig.put("username", "");
        opcConfig.put("password", "");
        opcConfig.put("use_cert_security", false);
        opcConfig.put("cert_path", "");
        opcConfig.put("key_path", "");
        
        Map<String, Object> influxConfig = new HashMap<>();
        influxConfig.put("url", "http://localhost:8086");
        influxConfig.put("token", "YOUR_TOKEN");
        influxConfig.put("org", "YOUR_ORG");
        influxConfig.put("bucket", "kiln_process_data");
        
        config.put("opc_config", opcConfig);
        config.put("influx_config", influxConfig);
        config.put("selected_tags", new HashMap<String, String>());
        config.put("tag_metadata", new HashMap<String, Object>());
        config.put("allowed_setpoints", new HashMap<String, String>());
        
        return config;
    }
}
