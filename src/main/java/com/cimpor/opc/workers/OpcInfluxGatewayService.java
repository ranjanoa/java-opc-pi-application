package com.cimpor.opc.workers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OpcInfluxGatewayService implements Runnable {

    private final Map<String, Object> opcConfig;
    private final Map<String, Object> influxConfig;
    private final Map<String, String> selectedTags;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public OpcInfluxGatewayService(Map<String, Object> opcConfig, Map<String, Object> influxConfig, Map<String, String> selectedTags) {
        this.opcConfig = opcConfig;
        this.influxConfig = influxConfig;
        this.selectedTags = selectedTags;
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        System.out.println("Starting OPC-Influx Gateway Service...");
        
        String influxUrl = (String) influxConfig.get("url");
        String token = (String) influxConfig.get("token");
        String org = (String) influxConfig.get("org");
        String bucket = (String) influxConfig.get("bucket");

        try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxUrl, token.toCharArray(), org)) {
            WriteApi writeApi = influxDBClient.getWriteApi();

            String opcUrl = (String) opcConfig.get("url");
            
            // Connect to OPC UA
            List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(opcUrl).get();
            EndpointDescription endpoint = endpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals("http://opcfoundation.org/UA/SecurityPolicy#None"))
                .findFirst()
                .orElse(endpoints.get(0));

            OpcUaClient client = OpcUaClient.create(
                new OpcUaClientConfigBuilder()
                    .setApplicationName(LocalizedText.english("Data@Glance Java Archiver"))
                    .setApplicationUri("urn:eclipse:milo:client")
                    .setEndpoint(endpoint)
                    .build()
            );

            client.connect().get();
            System.out.println("Connected to OPC Server: " + opcUrl);

            // Create Subscription
            UaSubscription subscription = client.getSubscriptionManager().createSubscription(1000.0).get();

            List<MonitoredItemCreateRequest> requests = new ArrayList<>();
            AtomicInteger clientHandle = new AtomicInteger(1);

            for (String nodeIdStr : selectedTags.keySet()) {
                NodeId nodeId = NodeId.parse(nodeIdStr);
                ReadValueId readValueId = new ReadValueId(nodeId, UInteger.valueOf(13), null, null);

                MonitoringParameters parameters = new MonitoringParameters(
                    UInteger.valueOf(clientHandle.getAndIncrement()),
                    1000.0,
                    null,
                    UInteger.valueOf(10),
                    true
                );

                requests.add(new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters));
            }

            if (requests.isEmpty()) {
                System.out.println("No tags selected. Skipping subscription.");
            } else {
                List<UaMonitoredItem> items = subscription.createMonitoredItems(
                    TimestampsToReturn.Both,
                    requests,
                    (item, id) -> item.setValueConsumer(this::onDataChange)
                ).get();
                System.out.println("Successfully subscribed to " + items.size() + " tags.");
            }

            while (running.get()) {
                Thread.sleep(1000);
            }

            client.disconnect().get();

        } catch (Exception e) {
            System.err.println("Error in Gateway Service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onDataChange(UaMonitoredItem item, DataValue value) {
        String nodeId = item.getReadValueId().getNodeId().toParseableString();
        String tagName = selectedTags.getOrDefault(nodeId, nodeId);
        
        Object val = value.getValue().getValue();
        if (val instanceof Number) {
            System.out.println("Tag Changed -> " + tagName + ": " + val);
            // In a complete implementation, this batches points and writes to InfluxDB
        }
    }
}
