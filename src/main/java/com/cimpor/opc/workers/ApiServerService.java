package com.cimpor.opc.workers;

import io.javalin.Javalin;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import java.util.concurrent.atomic.AtomicBoolean;

public class ApiServerService implements Runnable {

    private final int port;
    private final OpcUaClient opcClient; // Assume this gets injected or accessed via singleton
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Javalin app;

    public ApiServerService(int port, OpcUaClient opcClient) {
        this.port = port;
        this.opcClient = opcClient;
    }

    public void stop() {
        running.set(false);
        if (app != null) {
            app.stop();
        }
    }

    @Override
    public void run() {
        System.out.println("Starting FastAPI equivalent (Javalin) Server on port " + port);

        app = Javalin.create().start(port);

        app.post("/write", ctx -> {
            if (opcClient == null) {
                ctx.status(503).result("Not connected to OPC Server");
                return;
            }

            // Parse request body
            WriteRequest req = ctx.bodyAsClass(WriteRequest.class);

            try {
                // In a real implementation, write value to OPC node here
                // opcClient.writeValue(NodeId.parse(req.node_id), new DataValue(new Variant(req.value))).get();
                
                ctx.json(new WriteResponse("ok", req.node_id, req.value));
            } catch (Exception e) {
                ctx.status(500).result(e.getMessage());
            }
        });
        
        while (running.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // DTOs
    public static class WriteRequest {
        public String node_id;
        public Object value;
    }
    
    public static class WriteResponse {
        public String status;
        public String node;
        public Object value;
        public WriteResponse(String status, String node, Object value) {
            this.status = status;
            this.node = node;
            this.value = value;
        }
    }
}
