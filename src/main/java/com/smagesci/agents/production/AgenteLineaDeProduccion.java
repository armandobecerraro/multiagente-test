package com.smagesci.agents.production;

import com.smagesci.agents.BaseAgent;
import com.smagesci.utils.JsonUtil;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

public class AgenteLineaDeProduccion extends BaseAgent {

    private String lineId;
    private int capacityPerHour;
    private double efficiencyRate;
    private String status; // IDLE, RUNNING, MAINTENANCE, SETUP
    private String currentProductId;
    private int setupTimeMinutes;
    private Map<String, Object> productionMetrics;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            lineId = (String) args[0];
            capacityPerHour = (Integer) args[1];
            efficiencyRate = args.length > 2 ? (Double) args[2] : 95.0;
            setupTimeMinutes = args.length > 3 ? (Integer) args[3] : 30;
        } else {
            log.error("AgenteLineaDeProduccion {} requires arguments: lineId, capacityPerHour", getAID().getName());
            doDelete();
            return;
        }

        status = "IDLE";
        productionMetrics = new HashMap<>();
        productionMetrics.put("totalProduced", 0);
        productionMetrics.put("defectiveUnits", 0);
        productionMetrics.put("downtimeMinutes", 0);

        log.info("AgenteLineaDeProduccion {} (ID: {}) initialized with capacity {} units/hour", 
                getAID().getName(), lineId, capacityPerHour);
        
        registerService("production-line-" + lineId, "production-line-" + lineId);

        addBehaviour(new ProductionControlBehaviour());
        addBehaviour(new InitializeBehaviour());
    }

    private class InitializeBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            // Notify production planner that line is ready
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("PlanificadorProduccion", AID.ISLOCALNAME));
            
            Map<String, Object> data = new HashMap<>();
            data.put("action", "LINE_READY");
            data.put("lineId", lineId);
            data.put("capacity", capacityPerHour);
            data.put("status", status);
            
            msg.setContent(JsonUtil.toJson(data));
            msg.setOntology("smagesci-production");
            send(msg);
            
            log.info("Production line {} notified planner of readiness", lineId);
        }
    }

    private class ProductionControlBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.CFP)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    String content = msg.getContent();
                    Map<String, Object> data = JsonUtil.fromJson(content, Map.class);
                    String action = (String) data.get("action");
                    
                    switch (action) {
                        case "START_PRODUCTION":
                            handleStartProduction(msg, data);
                            break;
                        case "STOP_PRODUCTION":
                            handleStopProduction(msg, data);
                            break;
                        case "SETUP_FOR_PRODUCT":
                            handleSetupForProduct(msg, data);
                            break;
                        case "REQUEST_STATUS":
                            handleStatusRequest(msg);
                            break;
                        case "MAINTENANCE_REQUEST":
                            handleMaintenanceRequest(msg, data);
                            break;
                        default:
                            log.warn("Unknown action received: {}", action);
                    }
                } catch (Exception e) {
                    log.error("Error processing message", e);
                    sendErrorResponse(msg, "Error processing request: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private void handleStartProduction(ACLMessage msg, Map<String, Object> data) {
        String productId = (String) data.get("productId");
        Integer quantity = (Integer) data.get("quantity");
        
        if (!"IDLE".equals(status) && !"SETUP".equals(status)) {
            sendErrorResponse(msg, "Production line not available for production. Current status: " + status);
            return;
        }
        
        if (currentProductId != null && !currentProductId.equals(productId)) {
            // Need setup time for product change
            status = "SETUP";
            log.info("Production line {} starting setup for product change from {} to {}", 
                    lineId, currentProductId, productId);
            // In a real system, this would trigger a timer behavior
        }
        
        currentProductId = productId;
        status = "RUNNING";
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "PRODUCTION_STARTED");
        response.put("lineId", lineId);
        response.put("productId", productId);
        response.put("plannedQuantity", quantity);
        response.put("estimatedCompletionMinutes", calculateProductionTime(quantity));
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
        
        log.info("Production line {} started producing {} units of product {}", 
                lineId, quantity, productId);
        
        // Simulate production completion (in real system, use TickerBehaviour)
        simulateProductionCompletion(productId, quantity);
    }

    private void handleStopProduction(ACLMessage msg, Map<String, Object> data) {
        if (!"RUNNING".equals(status)) {
            sendErrorResponse(msg, "Production line not currently running");
            return;
        }
        
        status = "IDLE";
        currentProductId = null;
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "PRODUCTION_STOPPED");
        response.put("lineId", lineId);
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
        
        log.info("Production line {} stopped production", lineId);
    }

    private void handleSetupForProduct(ACLMessage msg, Map<String, Object> data) {
        String productId = (String) data.get("productId");
        
        if (!"IDLE".equals(status)) {
            sendErrorResponse(msg, "Cannot setup while line is " + status);
            return;
        }
        
        status = "SETUP";
        currentProductId = productId;
        
        // Simulate setup time
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    Thread.sleep(setupTimeMinutes * 100); // Accelerated for demo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                status = "IDLE";
                log.info("Production line {} completed setup for product {}", lineId, productId);
                
                // Notify completion
                Map<String, Object> notification = new HashMap<>();
                notification.put("action", "SETUP_COMPLETED");
                notification.put("lineId", lineId);
                notification.put("productId", productId);
                
                ACLMessage setupComplete = new ACLMessage(ACLMessage.INFORM);
                setupComplete.addReceiver(msg.getSender());
                setupComplete.setContent(JsonUtil.toJson(notification));
                send(setupComplete);
            }
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "SETUP_STARTED");
        response.put("lineId", lineId);
        response.put("productId", productId);
        response.put("estimatedSetupMinutes", setupTimeMinutes);
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void handleStatusRequest(ACLMessage msg) {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("action", "STATUS_RESPONSE");
        statusData.put("lineId", lineId);
        statusData.put("status", status);
        statusData.put("currentProductId", currentProductId);
        statusData.put("capacityPerHour", capacityPerHour);
        statusData.put("efficiencyRate", efficiencyRate);
        statusData.put("metrics", productionMetrics);
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(statusData));
        send(reply);
    }

    private void handleMaintenanceRequest(ACLMessage msg, Map<String, Object> data) {
        if ("RUNNING".equals(status)) {
            sendErrorResponse(msg, "Cannot start maintenance while production is running");
            return;
        }
        
        status = "MAINTENANCE";
        String maintenanceType = (String) data.getOrDefault("type", "PREVENTIVE");
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "MAINTENANCE_STARTED");
        response.put("lineId", lineId);
        response.put("type", maintenanceType);
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
        
        log.info("Production line {} started {} maintenance", lineId, maintenanceType);
    }

    private void simulateProductionCompletion(String productId, int quantity) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    // Simulate production time (accelerated for demo)
                    int productionTimeMs = calculateProductionTime(quantity) * 100;
                    Thread.sleep(productionTimeMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Update metrics
                int currentProduced = (Integer) productionMetrics.get("totalProduced");
                productionMetrics.put("totalProduced", currentProduced + quantity);
                
                status = "IDLE";
                
                // Notify production completion
                Map<String, Object> completion = new HashMap<>();
                completion.put("action", "PRODUCTION_COMPLETED");
                completion.put("lineId", lineId);
                completion.put("productId", productId);
                completion.put("quantityProduced", quantity);
                completion.put("qualityRate", efficiencyRate);
                
                ACLMessage completionMsg = new ACLMessage(ACLMessage.INFORM);
                completionMsg.addReceiver(new AID("PlanificadorProduccion", AID.ISLOCALNAME));
                completionMsg.addReceiver(new AID("GestorInventarioPT", AID.ISLOCALNAME));
                completionMsg.setContent(JsonUtil.toJson(completion));
                completionMsg.setOntology("smagesci-production");
                send(completionMsg);
                
                log.info("Production line {} completed production of {} units of product {}", 
                        lineId, quantity, productId);
            }
        });
    }

    private int calculateProductionTime(int quantity) {
        return (int) Math.ceil((double) quantity / capacityPerHour * 60); // minutes
    }

    private void sendErrorResponse(ACLMessage originalMsg, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("action", "ERROR");
        error.put("lineId", lineId);
        error.put("message", errorMessage);
        
        ACLMessage reply = originalMsg.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        reply.setContent(JsonUtil.toJson(error));
        send(reply);
        
        log.error("Production line {}: {}", lineId, errorMessage);
    }

    @Override
    protected void takeDown() {
        log.info("AgenteLineaDeProduccion {} (ID: {}) shutting down", getAID().getName(), lineId);
        super.takeDown();
    }
}
