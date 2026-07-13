package com.cimpor.opc.workers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SetpointWatcherService implements Runnable {

    private final Map<String, Object> opcConfig;
    private final Map<String, Object> influxConfig;
    private final Map<String, String> allowedSetpointsMap;
    private final String writeBackMeas;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public SetpointWatcherService(Map<String, Object> opcConfig, Map<String, Object> influxConfig, Map<String, String> allowedSetpointsMap, String writeBackMeas) {
        this.opcConfig = opcConfig;
        this.influxConfig = influxConfig;
        this.allowedSetpointsMap = allowedSetpointsMap;
        this.writeBackMeas = writeBackMeas;
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        System.out.println("Starting Setpoint Watcher Service...");

        String influxUrl = (String) influxConfig.get("url");
        String token = (String) influxConfig.get("token");
        String org = (String) influxConfig.get("org");
        String bucket = (String) influxConfig.get("bucket");

        try (InfluxDBClient influx = InfluxDBClientFactory.create(influxUrl, token.toCharArray(), org)) {
            QueryApi queryApi = influx.getQueryApi();

            // Connect to OPC Client
            // OpcUaClient opcClient = OpcUaClient.create(...);
            // opcClient.connect().get();

            Map<String, Object> lastCmd = new HashMap<>();

            while (running.get()) {
                String query = String.format("from(bucket:\"%s\") |> range(start: -24h) |> filter(fn: (r) => r[\"_measurement\"] == \"%s\") |> last()", bucket, writeBackMeas);

                try {
                    List<FluxTable> tables = queryApi.query(query);
                    for (FluxTable table : tables) {
                        for (FluxRecord record : table.getRecords()) {
                            if (record.getValue() != null) {
                                lastCmd.put(record.getField(), record.getValue());
                            }
                        }
                    }

                    // In a full implementation, iterate over lastCmd and write back to OPC server
                    for (Map.Entry<String, Object> entry : lastCmd.entrySet()) {
                        String targetId = allowedSetpointsMap.getOrDefault(entry.getKey(), entry.getKey());
                        System.out.println("Watcher: Asserting " + targetId + " = " + entry.getValue());
                        
                        // opcClient.writeValue(NodeId.parse(targetId), new DataValue(new Variant(entry.getValue()))).get();
                    }

                } catch (Exception e) {
                    System.err.println("Query Error (InfluxDB): " + e.getMessage());
                }

                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
