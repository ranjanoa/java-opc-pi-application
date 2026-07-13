package com.cimpor.opc.workers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimulatorService implements Runnable {

    private final Map<String, Object> influxConfig;
    private final String csvFilePath;
    private final String dbMeasurement;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public SimulatorService(Map<String, Object> influxConfig, String csvFilePath, String dbMeasurement) {
        this.influxConfig = influxConfig;
        this.csvFilePath = csvFilePath;
        this.dbMeasurement = dbMeasurement;
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        System.out.println("Starting Simulator Service...");

        String influxUrl = (String) influxConfig.get("url");
        String token = (String) influxConfig.get("token");
        String org = (String) influxConfig.get("org");
        String bucket = (String) influxConfig.get("bucket");

        try (InfluxDBClient influx = InfluxDBClientFactory.create(influxUrl, token.toCharArray(), org)) {
            WriteApi writeApi = influx.getWriteApi();

            List<String[]> rows = new ArrayList<>();
            String[] headers = null;

            try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
                String line = br.readLine();
                if (line != null) {
                    headers = line.split(",");
                }
                while ((line = br.readLine()) != null) {
                    rows.add(line.split(","));
                }
            }

            if (rows.isEmpty() || headers == null) return;

            int idx = 0;
            while (running.get()) {
                String[] row = rows.get(idx);
                Point point = Point.measurement(dbMeasurement).time(Instant.now(), WritePrecision.NS);

                for (int i = 0; i < headers.length; i++) {
                    if (i < row.length) {
                        try {
                            double val = Double.parseDouble(row[i].trim());
                            point.addField(headers[i].trim(), val);
                        } catch (NumberFormatException e) {
                            // Ignore malformed numbers
                        }
                    }
                }

                writeApi.writePoint(bucket, org, point);
                System.out.println("Simulator wrote point: " + point.toLineProtocol());

                idx = (idx + 1) % rows.size();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
