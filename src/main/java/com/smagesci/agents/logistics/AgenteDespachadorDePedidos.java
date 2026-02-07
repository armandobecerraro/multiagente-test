package com.smagesci.agents.logistics;

import com.smagesci.agents.BaseAgent;
import com.smagesci.dao.InventoryDAO;
import com.smagesci.models.InventoryItem;
import com.smagesci.models.Order;
import com.smagesci.models.OrderItem;
import com.smagesci.utils.JsonUtil;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Agente responsable del despacho y entrega de pedidos.
 * Gestiona la preparación, empaque y envío de órdenes a clientes.
 */
public class AgenteDespachadorDePedidos extends BaseAgent {
    
    private InventoryDAO inventoryDAO;
    private Map<String, ShipmentInfo> activeShipments;
    private Map<String, List<String>> warehouseLocations;
    private int shipmentCounter;
    private String defaultWarehouse;

    @Override
    protected void setup() {
        super.setup();
        inventoryDAO = new InventoryDAO();
        activeShipments = new HashMap<>();
        warehouseLocations = initializeWarehouses();
        shipmentCounter = 1;
        defaultWarehouse = "MAIN-WH";

        log.info("AgenteDespachadorDePedidos " + getAID().getName() + " is ready");
        registerService("order-dispatching", "smagesci-dispatch-manager");

        addBehaviour(new DispatchingBehaviour());
        addBehaviour(new ShipmentTrackingBehaviour());
    }

    private Map<String, List<String>> initializeWarehouses() {
        Map<String, List<String>> warehouses = new HashMap<>();
        warehouses.put("MAIN-WH", Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2"));
        warehouses.put("NORTH-WH", Arrays.asList("N1", "N2", "N3"));
        warehouses.put("SOUTH-WH", Arrays.asList("S1", "S2", "S3"));
        return warehouses;
    }

    private class DispatchingBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                try {
                    String content = msg.getContent();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) JsonUtil.fromJson(content, Map.class);
                    String action = (String) data.get("action");

                    switch (action) {
                        case "PREPARE_FULFILLMENT":
                            handlePrepareFulfillment(msg, data);
                            break;
                        case "TRACK_SHIPMENT":
                            handleTrackShipment(msg, data);
                            break;
                        case "UPDATE_DELIVERY_STATUS":
                            handleUpdateDeliveryStatus(msg, data);
                            break;
                        case "CALCULATE_SHIPPING_COST":
                            handleCalculateShippingCost(msg, data);
                            break;
                        default:
                            log.warning("Unknown action received: " + action);
                            sendErrorResponse(msg, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    log.severe("Error processing dispatch message: " + e.getMessage());
                    sendErrorResponse(msg, "Error processing request: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private class ShipmentTrackingBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Simulate shipment status updates
            try {
                Thread.sleep(30000); // Check every 30 seconds
                updateShipmentStatuses();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handlePrepareFulfillment(ACLMessage msg, Map<String, Object> data) {
        try {
            String orderId = (String) data.get("orderId");
            String customerId = (String) data.get("customerId");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) data.get("items");

            if (orderId == null || customerId == null || itemsData == null) {
                sendErrorResponse(msg, "Missing required fulfillment data");
                return;
            }

            // Create shipment
            String shipmentId = "SHIP" + String.format("%06d", shipmentCounter++);
            ShipmentInfo shipment = new ShipmentInfo(shipmentId, orderId, customerId);

            // Verify inventory availability and prepare picking list
            List<PickingItem> pickingList = new ArrayList<>();
            boolean allItemsAvailable = true;
            List<String> unavailableItems = new ArrayList<>();

            for (Map<String, Object> itemData : itemsData) {
                String productId = (String) itemData.get("productId");
                Integer quantity = (Integer) itemData.get("quantity");

                try {
                    Integer itemId = Integer.parseInt(productId);
                    List<InventoryItem> inventoryItems = inventoryDAO.findByItemId(itemId);
                    int totalAvailable = inventoryItems.stream()
                        .mapToInt(item -> item.getAvailableQuantity())
                        .sum();

                    if (totalAvailable >= quantity) {
                        // Create picking instructions
                        int remainingToPick = quantity;
                        for (InventoryItem inventoryItem : inventoryItems) {
                            if (remainingToPick <= 0) break;

                            int availableInLocation = inventoryItem.getAvailableQuantity();
                            int toPickFromLocation = Math.min(remainingToPick, availableInLocation);

                            if (toPickFromLocation > 0) {
                                pickingList.add(new PickingItem(
                                    productId,
                                    inventoryItem.getLocation(),
                                    toPickFromLocation
                                ));
                                remainingToPick -= toPickFromLocation;
                            }
                        }
                    } else {
                        allItemsAvailable = false;
                        unavailableItems.add(productId + " (needed: " + quantity + ", available: " + totalAvailable + ")");
                    }
                } catch (NumberFormatException e) {
                    log.warning("Invalid product ID format: " + productId);
                    allItemsAvailable = false;
                    unavailableItems.add(productId + " (invalid ID format)");
                }
            }

            if (!allItemsAvailable) {
                Map<String, Object> response = new HashMap<>();
                response.put("action", "FULFILLMENT_REJECTED");
                response.put("orderId", orderId);
                response.put("shipmentId", shipmentId);
                response.put("reason", "Insufficient inventory");
                response.put("unavailableItems", unavailableItems);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent(JsonUtil.toJson(response));
                send(reply);

                log.warning("Fulfillment rejected for order " + orderId + " due to insufficient inventory");
                return;
            }

            // Schedule picking and packing
            shipment.setPickingList(pickingList);
            shipment.setStatus("PICKING_SCHEDULED");
            activeShipments.put(shipmentId, shipment);

            // Start picking process
            simulatePickingProcess(shipmentId);

            // Send confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("action", "FULFILLMENT_ACCEPTED");
            response.put("orderId", orderId);
            response.put("shipmentId", shipmentId);
            response.put("estimatedPickingTime", calculatePickingTime(pickingList));
            response.put("estimatedShippingDays", 2);

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Fulfillment scheduled for order " + orderId + " with shipment " + shipmentId);

        } catch (Exception e) {
            log.severe("Error preparing fulfillment: " + e.getMessage());
            sendErrorResponse(msg, "Error preparing fulfillment: " + e.getMessage());
        }
    }

    private void handleTrackShipment(ACLMessage msg, Map<String, Object> data) {
        String shipmentId = (String) data.get("shipmentId");

        if (shipmentId == null) {
            sendErrorResponse(msg, "Missing shipmentId");
            return;
        }

        ShipmentInfo shipment = activeShipments.get(shipmentId);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "SHIPMENT_STATUS");
        response.put("shipmentId", shipmentId);

        if (shipment != null) {
            response.put("status", shipment.getStatus());
            response.put("orderId", shipment.getOrderId());
            response.put("customerId", shipment.getCustomerId());
            response.put("createdAt", shipment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("trackingNumber", shipment.getTrackingNumber());
            response.put("estimatedDeliveryDate", shipment.getEstimatedDeliveryDate());
        } else {
            response.put("status", "NOT_FOUND");
            response.put("message", "Shipment not found");
        }

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void handleUpdateDeliveryStatus(ACLMessage msg, Map<String, Object> data) {
        String shipmentId = (String) data.get("shipmentId");
        String newStatus = (String) data.get("status");

        if (shipmentId == null || newStatus == null) {
            sendErrorResponse(msg, "Missing shipmentId or status");
            return;
        }

        ShipmentInfo shipment = activeShipments.get(shipmentId);

        if (shipment == null) {
            sendErrorResponse(msg, "Shipment not found: " + shipmentId);
            return;
        }

        shipment.setStatus(newStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "DELIVERY_STATUS_UPDATED");
        response.put("shipmentId", shipmentId);
        response.put("newStatus", newStatus);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Updated shipment " + shipmentId + " status to " + newStatus);

        // Notify customer of status change
        notifyCustomerOfStatusUpdate(shipment, newStatus);
    }

    private void handleCalculateShippingCost(ACLMessage msg, Map<String, Object> data) {
        String destination = (String) data.get("destination");
        Double weight = (Double) data.getOrDefault("weight", 1.0);
        String priority = (String) data.getOrDefault("priority", "STANDARD");

        double baseCost = 10.0;
        double weightMultiplier = weight * 2.0;
        double priorityMultiplier = "EXPRESS".equals(priority) ? 1.5 : 1.0;

        double totalCost = (baseCost + weightMultiplier) * priorityMultiplier;

        Map<String, Object> response = new HashMap<>();
        response.put("action", "SHIPPING_COST_CALCULATED");
        response.put("destination", destination);
        response.put("weight", weight);
        response.put("priority", priority);
        response.put("cost", totalCost);
        response.put("estimatedDays", "EXPRESS".equals(priority) ? 1 : 3);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void simulatePickingProcess(String shipmentId) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    // Simulate picking time
                    Thread.sleep(5000); // 5 seconds for demo

                    ShipmentInfo shipment = activeShipments.get(shipmentId);
                    if (shipment != null) {
                        shipment.setStatus("PICKED");
                        
                        // Update inventory (reduce available quantities)
                        // Note: reduceQuantity method would need to be implemented in InventoryDAO
                        // for (PickingItem item : shipment.getPickingList()) {
                        //     inventoryDAO.reduceQuantity(Integer.parseInt(item.getProductId()), item.getLocation(), item.getQuantity());
                        // }

                        // Start packing process
                        simulatePackingProcess(shipmentId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void simulatePackingProcess(String shipmentId) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    Thread.sleep(3000); // 3 seconds for demo

                    ShipmentInfo shipment = activeShipments.get(shipmentId);
                    if (shipment != null) {
                        shipment.setStatus("PACKED");
                        shipment.setTrackingNumber("TRK" + shipmentId + System.currentTimeMillis());
                        
                        // Schedule shipment
                        simulateShipping(shipmentId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void simulateShipping(String shipmentId) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    Thread.sleep(2000); // 2 seconds for demo

                    ShipmentInfo shipment = activeShipments.get(shipmentId);
                    if (shipment != null) {
                        shipment.setStatus("SHIPPED");
                        shipment.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
                        
                        log.info("Shipment " + shipmentId + " has been shipped with tracking number " + shipment.getTrackingNumber());

                        // Notify order processor
                        notifyOrderShipped(shipment);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void updateShipmentStatuses() {
        for (ShipmentInfo shipment : activeShipments.values()) {
            if ("SHIPPED".equals(shipment.getStatus())) {
                // Simulate delivery progression
                Random random = new Random();
                if (random.nextDouble() < 0.1) { // 10% chance to advance to delivery
                    shipment.setStatus("DELIVERED");
                    log.info("Shipment " + shipment.getShipmentId() + " has been delivered");
                    notifyCustomerOfStatusUpdate(shipment, "DELIVERED");
                }
            }
        }
    }

    private int calculatePickingTime(List<PickingItem> pickingList) {
        // Estimate 2 minutes per location + 1 minute per item
        Set<String> uniqueLocations = new HashSet<>();
        int totalItems = 0;
        
        for (PickingItem item : pickingList) {
            uniqueLocations.add(item.getLocation());
            totalItems += item.getQuantity();
        }
        
        return uniqueLocations.size() * 2 + totalItems;
    }

    private void notifyOrderShipped(ShipmentInfo shipment) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("action", "ORDER_SHIPPED");
        notification.put("orderId", shipment.getOrderId());
        notification.put("shipmentId", shipment.getShipmentId());
        notification.put("trackingNumber", shipment.getTrackingNumber());
        notification.put("estimatedDeliveryDate", shipment.getEstimatedDeliveryDate());

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("ProcesadorPedidosCliente", AID.ISLOCALNAME));
        msg.setContent(JsonUtil.toJson(notification));
        msg.setOntology("smagesci-logistics");
        send(msg);
    }

    private void notifyCustomerOfStatusUpdate(ShipmentInfo shipment, String newStatus) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("action", "DELIVERY_STATUS_UPDATE");
        notification.put("shipmentId", shipment.getShipmentId());
        notification.put("orderId", shipment.getOrderId());
        notification.put("status", newStatus);
        notification.put("trackingNumber", shipment.getTrackingNumber());

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(shipment.getCustomerId(), AID.ISLOCALNAME));
        msg.setContent(JsonUtil.toJson(notification));
        msg.setOntology("smagesci-customer");
        send(msg);

        log.info("Notified customer {} of shipment status update: {}", shipment.getCustomerId(), newStatus);
    }

    private void sendErrorResponse(ACLMessage originalMsg, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("action", "ERROR");
        error.put("message", errorMessage);

        ACLMessage reply = originalMsg.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        reply.setContent(JsonUtil.toJson(error));
        send(reply);

        log.error("Dispatch error: {}", errorMessage);
    }

    @Override
    protected void takeDown() {
        log.info("AgenteDespachadorDePedidos {} shutting down", getAID().getName());
        super.takeDown();
    }

    // Helper classes
    private static class ShipmentInfo {
        private String shipmentId;
        private String orderId;
        private String customerId;
        private String status;
        private LocalDateTime createdAt;
        private String trackingNumber;
        private String estimatedDeliveryDate;
        private List<PickingItem> pickingList;

        public ShipmentInfo(String shipmentId, String orderId, String customerId) {
            this.shipmentId = shipmentId;
            this.orderId = orderId;
            this.customerId = customerId;
            this.status = "CREATED";
            this.createdAt = LocalDateTime.now();
            this.pickingList = new ArrayList<>();
        }

        // Getters and setters
        public String getShipmentId() { return shipmentId; }
        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
        public void setEstimatedDeliveryDate(String estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }
        public List<PickingItem> getPickingList() { return pickingList; }
        public void setPickingList(List<PickingItem> pickingList) { this.pickingList = pickingList; }
    }

    private static class PickingItem {
        private String productId;
        private String location;
        private int quantity;

        public PickingItem(String productId, String location, int quantity) {
            this.productId = productId;
            this.location = location;
            this.quantity = quantity;
        }

        public String getProductId() { return productId; }
        public String getLocation() { return location; }
        public int getQuantity() { return quantity; }
    }
}
