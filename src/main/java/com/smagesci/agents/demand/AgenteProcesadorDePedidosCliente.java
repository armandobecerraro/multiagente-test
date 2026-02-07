package com.smagesci.agents.demand;

import com.smagesci.agents.BaseAgent;
import com.smagesci.dao.InventoryDAO;
import com.smagesci.models.InventoryItem;
import com.smagesci.models.Order;
import com.smagesci.models.OrderItem;
import com.smagesci.utils.JsonUtil;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgenteProcesadorDePedidosCliente extends BaseAgent {
    
    private InventoryDAO inventoryDAO;
    private Map<String, Order> pendingOrders;
    private int orderCounter;

    @Override
    protected void setup() {
        super.setup();
        inventoryDAO = new InventoryDAO();
        pendingOrders = new HashMap<>();
        orderCounter = 1;
        
        log.info("AgenteProcesadorDePedidosCliente {} is ready", getAID().getName());
        registerService("customer-order-processing", "smagesci-order-processor");

        addBehaviour(new OrderProcessingBehaviour());
    }

    private class OrderProcessingBehaviour extends CyclicBehaviour {
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
                        case "PLACE_ORDER":
                            handlePlaceOrder(msg, data);
                            break;
                        case "CHECK_ORDER_STATUS":
                            handleCheckOrderStatus(msg, data);
                            break;
                        case "CANCEL_ORDER":
                            handleCancelOrder(msg, data);
                            break;
                        case "MODIFY_ORDER":
                            handleModifyOrder(msg, data);
                            break;
                        default:
                            log.warning("Unknown action received: " + action);
                            sendErrorResponse(msg, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    log.severe("Error processing order message: " + e.getMessage());
                    sendErrorResponse(msg, "Error processing order: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private void handlePlaceOrder(ACLMessage msg, Map<String, Object> data) {
        try {
            String customerId = (String) data.get("customerId");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) data.get("items");
            
            if (customerId == null || itemsData == null || itemsData.isEmpty()) {
                sendErrorResponse(msg, "Invalid order data: missing customerId or items");
                return;
            }
            
            // Generate order ID
            String orderId = "ORD" + String.format("%06d", orderCounter++);
            
            // Convert items data to OrderItem objects
            List<OrderItem> orderItems = new ArrayList<>();
            double totalAmount = 0.0;
            boolean stockAvailable = true;
            List<String> unavailableItems = new ArrayList<>();
            
            for (Map<String, Object> itemData : itemsData) {
                String productId = (String) itemData.get("productId");
                Integer quantity = (Integer) itemData.get("quantity");
                Double unitPrice = (Double) itemData.getOrDefault("unitPrice", 100.0);
                
                if (productId == null || quantity == null || quantity <= 0) {
                    sendErrorResponse(msg, "Invalid item data in order");
                    return;
                }
                
                // Check inventory availability
                try {
                    Integer itemId = Integer.parseInt(productId);
                    List<InventoryItem> inventoryItems = inventoryDAO.findByItemId(itemId);
                    int totalAvailable = inventoryItems.stream()
                        .mapToInt(item -> item.getAvailableQuantity())
                        .sum();
                    
                    if (totalAvailable < quantity) {
                        stockAvailable = false;
                        unavailableItems.add(productId + " (requested: " + quantity + ", available: " + totalAvailable + ")");
                    }
                } catch (NumberFormatException e) {
                    log.warning("Invalid product ID format: " + productId);
                    stockAvailable = false;
                    unavailableItems.add(productId + " (invalid ID format)");
                }
                
                OrderItem orderItem = new OrderItem(productId, quantity, unitPrice);
                orderItems.add(orderItem);
                totalAmount += quantity * unitPrice;
            }
            
            if (!stockAvailable) {
                Map<String, Object> response = new HashMap<>();
                response.put("action", "ORDER_REJECTED");
                response.put("orderId", orderId);
                response.put("reason", "Insufficient stock");
                response.put("unavailableItems", unavailableItems);
                
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent(JsonUtil.toJson(response));
                send(reply);
                
                log.warning("Order " + orderId + " rejected due to insufficient stock: " + unavailableItems);
                return;
            }
            
            // Create order
            Order order = new Order(orderId, customerId, LocalDateTime.now(), orderItems, "PENDING");
            pendingOrders.put(orderId, order);
            
            // Reserve inventory
            for (OrderItem item : orderItems) {
                try {
                    Integer itemId = Integer.parseInt(item.getProductId());
                    List<InventoryItem> inventoryItems = inventoryDAO.findByItemId(itemId);
                    int remainingToReserve = item.getQuantity();
                    
                    for (InventoryItem inventoryItem : inventoryItems) {
                    if (remainingToReserve <= 0) break;
                    
                    int availableInLocation = inventoryItem.getAvailableQuantity();
                    int toReserveFromLocation = Math.min(remainingToReserve, availableInLocation);
                    
                    if (toReserveFromLocation > 0) {
                        // Note: reserveQuantity method would need to be implemented in InventoryDAO
                        // For now, we'll skip the actual reservation
                        // inventoryDAO.reserveQuantity(itemId, inventoryItem.getLocation(), toReserveFromLocation);
                        remainingToReserve -= toReserveFromLocation;
                    }
                }
                } catch (NumberFormatException e) {
                    log.warning("Invalid product ID format during reservation: " + item.getProductId());
                }
            }
            
            // Send confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("action", "ORDER_CONFIRMED");
            response.put("orderId", orderId);
            response.put("customerId", customerId);
            response.put("totalAmount", totalAmount);
            response.put("estimatedDeliveryDays", 5);
            response.put("status", "PENDING");
            
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);
            
            log.info("Order " + orderId + " confirmed for customer " + customerId + " with total amount $" + totalAmount);
            
            // Notify production planner about new order
            notifyProductionPlanner(order);
            
            // Notify logistics about order fulfillment
            notifyLogistics(order);
            
        } catch (Exception e) {
            log.severe("Error processing place order request: " + e.getMessage());
            sendErrorResponse(msg, "Internal error processing order");
        }
    }

    private void handleCheckOrderStatus(ACLMessage msg, Map<String, Object> data) {
        String orderId = (String) data.get("orderId");
        
        if (orderId == null) {
            sendErrorResponse(msg, "Missing orderId");
            return;
        }
        
        Order order = pendingOrders.get(orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "ORDER_STATUS_RESPONSE");
        response.put("orderId", orderId);
        
        if (order != null) {
            response.put("status", order.getStatus());
            response.put("customerId", order.getCustomerId());
            response.put("orderDate", order.getOrderDate().toString());
            response.put("items", order.getItems());
        } else {
            response.put("status", "NOT_FOUND");
            response.put("message", "Order not found");
        }
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void handleCancelOrder(ACLMessage msg, Map<String, Object> data) {
        String orderId = (String) data.get("orderId");
        
        if (orderId == null) {
            sendErrorResponse(msg, "Missing orderId");
            return;
        }
        
        Order order = pendingOrders.get(orderId);
        
        if (order == null) {
            sendErrorResponse(msg, "Order not found: " + orderId);
            return;
        }
        
        if (!"PENDING".equals(order.getStatus()) && !"PROCESSING".equals(order.getStatus())) {
            sendErrorResponse(msg, "Cannot cancel order in status: " + order.getStatus());
            return;
        }
        
        // Release reserved inventory
        for (OrderItem item : order.getItems()) {
            try {
                Integer itemId = Integer.parseInt(item.getProductId());
                // In a real system, we'd need to track which specific reservations to release
                // For simplicity, we'll just reduce reserved quantity
                // List<InventoryItem> inventoryItems = inventoryDAO.findByItemId(itemId);
            } catch (NumberFormatException e) {
                log.warning("Invalid product ID format during cancellation: " + item.getProductId());
            }
        }
        
        order.setStatus("CANCELED");
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "ORDER_CANCELED");
        response.put("orderId", orderId);
        response.put("status", "CANCELED");
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
        
        log.info("Order " + orderId + " has been canceled");
    }

    private void handleModifyOrder(ACLMessage msg, Map<String, Object> data) {
        sendErrorResponse(msg, "Order modification not yet implemented");
    }

    private void notifyProductionPlanner(Order order) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("action", "NEW_ORDER_NOTIFICATION");
        notification.put("orderId", order.getOrderId());
        notification.put("customerId", order.getCustomerId());
        notification.put("items", order.getItems());
        notification.put("priority", "NORMAL");
        
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("PlanificadorProduccion", AID.ISLOCALNAME));
        msg.setContent(JsonUtil.toJson(notification));
        msg.setOntology("smagesci-production");
        send(msg);
        
        log.info("Notified production planner about new order " + order.getOrderId());
    }

    private void notifyLogistics(Order order) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("action", "PREPARE_FULFILLMENT");
        notification.put("orderId", order.getOrderId());
        notification.put("customerId", order.getCustomerId());
        notification.put("items", order.getItems());
        
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("DespachadorPedidos", AID.ISLOCALNAME));
        msg.setContent(JsonUtil.toJson(notification));
        msg.setOntology("smagesci-logistics");
        send(msg);
        
        log.info("Notified logistics about order fulfillment for order " + order.getOrderId());
    }

    private void sendErrorResponse(ACLMessage originalMsg, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("action", "ERROR");
        error.put("message", errorMessage);
        
        ACLMessage reply = originalMsg.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        reply.setContent(JsonUtil.toJson(error));
        send(reply);
        
        log.severe("Order processing error: " + errorMessage);
    }

    @Override
    protected void takeDown() {
        log.info("AgenteProcesadorDePedidosCliente " + getAID().getName() + " shutting down");
        super.takeDown();
    }
}
