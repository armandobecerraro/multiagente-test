package com.smagesci.agents.supply;

import com.smagesci.agents.BaseAgent;
import com.smagesci.dao.InventoryDAO;
import com.smagesci.models.InventoryItem;
import com.smagesci.models.RawMaterial;
import com.smagesci.utils.JsonUtil;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Agente responsable de la gestión de compras y adquisiciones.
 * Evalúa proveedores, negocia precios y gestiona órdenes de compra.
 */
public class AgenteGestorDeCompras extends BaseAgent {
    
    private Map<String, SupplierInfo> suppliers;
    private Map<String, PurchaseOrder> activePurchaseOrders;
    private Map<String, Double> materialPrices;
    private InventoryDAO inventoryDAO;
    private int purchaseOrderCounter;

    @Override
    protected void setup() {
        super.setup();
        inventoryDAO = new InventoryDAO();
        suppliers = initializeSuppliers();
        activePurchaseOrders = new HashMap<>();
        materialPrices = initializePrices();
        purchaseOrderCounter = 1;

        log.info("AgenteGestorDeCompras {} is ready with {} suppliers", 
                getAID().getName(), suppliers.size());
        registerService("procurement-management", "smagesci-procurement-manager");

        addBehaviour(new ProcurementBehaviour());
        addBehaviour(new PriceMonitoringBehaviour(this, 300000)); // Check prices every 5 minutes
    }

    private Map<String, SupplierInfo> initializeSuppliers() {
        Map<String, SupplierInfo> supplierMap = new HashMap<>();
        
        supplierMap.put("SUP001", new SupplierInfo("SUP001", "MetalCorp Inc.", 
                Arrays.asList("STEEL", "ALUMINUM", "COPPER"), 95.0, 3, "A"));
        supplierMap.put("SUP002", new SupplierInfo("SUP002", "PlasticWorks Ltd.", 
                Arrays.asList("PLASTIC_PET", "PLASTIC_PP", "PLASTIC_ABS"), 88.0, 5, "B"));
        supplierMap.put("SUP003", new SupplierInfo("SUP003", "ChemSupply Co.", 
                Arrays.asList("CHEMICAL_A", "CHEMICAL_B", "SOLVENT"), 92.0, 7, "A"));
        supplierMap.put("SUP004", new SupplierInfo("SUP004", "FastMetal Solutions", 
                Arrays.asList("STEEL", "ALUMINUM"), 85.0, 1, "B"));
        supplierMap.put("SUP005", new SupplierInfo("SUP005", "Premium Materials", 
                Arrays.asList("RUBBER", "FABRIC", "GLASS"), 98.0, 4, "A"));
        
        return supplierMap;
    }

    private Map<String, Double> initializePrices() {
        Map<String, Double> prices = new HashMap<>();
        
        // Metal prices (per kg)
        prices.put("STEEL", 2.50);
        prices.put("ALUMINUM", 4.20);
        prices.put("COPPER", 8.75);
        
        // Plastic prices (per kg)
        prices.put("PLASTIC_PET", 1.20);
        prices.put("PLASTIC_PP", 1.05);
        prices.put("PLASTIC_ABS", 1.80);
        
        // Chemical prices (per liter)
        prices.put("CHEMICAL_A", 15.30);
        prices.put("CHEMICAL_B", 12.80);
        prices.put("SOLVENT", 8.90);
        
        // Other materials
        prices.put("RUBBER", 3.40);
        prices.put("FABRIC", 6.20);
        prices.put("GLASS", 2.80);
        
        return prices;
    }

    private class ProcurementBehaviour extends CyclicBehaviour {
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
                        case "REQUEST_QUOTATION":
                            handleRequestQuotation(msg, data);
                            break;
                        case "CREATE_PURCHASE_ORDER":
                            handleCreatePurchaseOrder(msg, data);
                            break;
                        case "TRACK_PURCHASE_ORDER":
                            handleTrackPurchaseOrder(msg, data);
                            break;
                        case "EVALUATE_SUPPLIERS":
                            handleEvaluateSuppliers(msg, data);
                            break;
                        case "UPDATE_SUPPLIER_PERFORMANCE":
                            handleUpdateSupplierPerformance(msg, data);
                            break;
                        case "NEGOTIATE_PRICE":
                            handleNegotiatePrice(msg, data);
                            break;
                        case "CHECK_INVENTORY_LEVELS":
                            handleCheckInventoryLevels(msg, data);
                            break;
                        default:
                            log.warn("Unknown action received: {}", action);
                            sendErrorResponse(msg, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    log.error("Error processing procurement message", e);
                    sendErrorResponse(msg, "Error processing request: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private class PriceMonitoringBehaviour extends TickerBehaviour {
        public PriceMonitoringBehaviour(jade.core.Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            // Monitor market prices and update supplier quotes
            updateMarketPrices();
            checkLowInventoryLevels();
            reviewSupplierPerformance();
        }
    }

    private void handleRequestQuotation(ACLMessage msg, Map<String, Object> data) {
        try {
            String materialId = (String) data.get("materialId");
            Integer quantity = (Integer) data.get("quantity");
            String urgency = (String) data.getOrDefault("urgency", "NORMAL");

            if (materialId == null || quantity == null || quantity <= 0) {
                sendErrorResponse(msg, "Missing or invalid quotation data");
                return;
            }

            // Find suppliers for this material
            List<QuotationOption> quotations = new ArrayList<>();
            
            for (SupplierInfo supplier : suppliers.values()) {
                if (supplier.getAvailableMaterials().contains(materialId)) {
                    double basePrice = materialPrices.getOrDefault(materialId, 10.0);
                    double supplierMultiplier = getSupplierPriceMultiplier(supplier, urgency);
                    double unitPrice = basePrice * supplierMultiplier;
                    double totalPrice = unitPrice * quantity;
                    
                    int deliveryDays = calculateDeliveryTime(supplier, quantity, urgency);
                    
                    quotations.add(new QuotationOption(
                        supplier.getSupplierId(),
                        supplier.getName(),
                        materialId,
                        quantity,
                        unitPrice,
                        totalPrice,
                        deliveryDays,
                        supplier.getQualityRating()
                    ));
                }
            }

            // Sort by best value (considering price, quality, and delivery time)
            quotations.sort((q1, q2) -> Double.compare(calculateQuotationScore(q1), calculateQuotationScore(q2)));

            Map<String, Object> response = new HashMap<>();
            response.put("action", "QUOTATION_RESPONSE");
            response.put("materialId", materialId);
            response.put("quantity", quantity);
            response.put("quotations", quotations);
            response.put("recommendedSupplier", quotations.isEmpty() ? null : quotations.get(0).getSupplierId());

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Provided {} quotations for {} units of {}", quotations.size(), quantity, materialId);

        } catch (Exception e) {
            log.error("Error processing quotation request", e);
            sendErrorResponse(msg, "Error processing quotation: " + e.getMessage());
        }
    }

    private void handleCreatePurchaseOrder(ACLMessage msg, Map<String, Object> data) {
        try {
            String supplierId = (String) data.get("supplierId");
            String materialId = (String) data.get("materialId");
            Integer quantity = (Integer) data.get("quantity");
            Double agreedPrice = (Double) data.get("agreedPrice");
            String priority = (String) data.getOrDefault("priority", "NORMAL");

            if (supplierId == null || materialId == null || quantity == null || agreedPrice == null) {
                sendErrorResponse(msg, "Missing required purchase order data");
                return;
            }

            SupplierInfo supplier = suppliers.get(supplierId);
            if (supplier == null) {
                sendErrorResponse(msg, "Supplier not found: " + supplierId);
                return;
            }

            if (!supplier.getAvailableMaterials().contains(materialId)) {
                sendErrorResponse(msg, "Supplier does not provide material: " + materialId);
                return;
            }

            // Create purchase order
            String poNumber = "PO" + String.format("%06d", purchaseOrderCounter++);
            PurchaseOrder po = new PurchaseOrder(
                poNumber, supplierId, materialId, quantity, agreedPrice, priority
            );

            activePurchaseOrders.put(poNumber, po);

            // Calculate delivery date
            int deliveryDays = calculateDeliveryTime(supplier, quantity, priority);
            LocalDateTime expectedDelivery = LocalDateTime.now().plusDays(deliveryDays);

            // Send PO to supplier (simulate)
            sendPurchaseOrderToSupplier(po, supplier);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "PURCHASE_ORDER_CREATED");
            response.put("poNumber", poNumber);
            response.put("supplierId", supplierId);
            response.put("materialId", materialId);
            response.put("quantity", quantity);
            response.put("totalAmount", quantity * agreedPrice);
            response.put("expectedDelivery", expectedDelivery.format(DateTimeFormatter.ISO_LOCAL_DATE));
            response.put("status", "SENT_TO_SUPPLIER");

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Created purchase order {} for {} units of {} from supplier {}", 
                    poNumber, quantity, materialId, supplierId);

        } catch (Exception e) {
            log.error("Error creating purchase order", e);
            sendErrorResponse(msg, "Error creating purchase order: " + e.getMessage());
        }
    }

    private void handleTrackPurchaseOrder(ACLMessage msg, Map<String, Object> data) {
        String poNumber = (String) data.get("poNumber");

        if (poNumber == null) {
            sendErrorResponse(msg, "Missing poNumber");
            return;
        }

        PurchaseOrder po = activePurchaseOrders.get(poNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "PURCHASE_ORDER_STATUS");
        response.put("poNumber", poNumber);

        if (po != null) {
            response.put("status", po.getStatus());
            response.put("supplierId", po.getSupplierId());
            response.put("materialId", po.getMaterialId());
            response.put("quantity", po.getQuantity());
            response.put("unitPrice", po.getUnitPrice());
            response.put("totalAmount", po.getTotalAmount());
            response.put("createdAt", po.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("expectedDelivery", po.getExpectedDelivery());

            if (po.getActualDelivery() != null) {
                response.put("actualDelivery", po.getActualDelivery().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        } else {
            response.put("status", "NOT_FOUND");
            response.put("message", "Purchase order not found");
        }

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void handleEvaluateSuppliers(ACLMessage msg, Map<String, Object> data) {
        String materialId = (String) data.get("materialId");
        
        List<SupplierEvaluation> evaluations = new ArrayList<>();
        
        for (SupplierInfo supplier : suppliers.values()) {
            if (materialId == null || supplier.getAvailableMaterials().contains(materialId)) {
                double score = calculateSupplierScore(supplier);
                evaluations.add(new SupplierEvaluation(
                    supplier.getSupplierId(),
                    supplier.getName(),
                    supplier.getQualityRating(),
                    supplier.getDeliveryTimeAvgDays(),
                    supplier.getRating(),
                    score
                ));
            }
        }

        // Sort by score (best first)
        evaluations.sort((e1, e2) -> Double.compare(e2.getOverallScore(), e1.getOverallScore()));

        Map<String, Object> response = new HashMap<>();
        response.put("action", "SUPPLIER_EVALUATION");
        response.put("materialId", materialId);
        response.put("evaluations", evaluations);
        response.put("bestSupplier", evaluations.isEmpty() ? null : evaluations.get(0).getSupplierId());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Evaluated {} suppliers for material {}", evaluations.size(), materialId);
    }

    private void handleUpdateSupplierPerformance(ACLMessage msg, Map<String, Object> data) {
        String supplierId = (String) data.get("supplierId");
        String performanceType = (String) data.get("type"); // DELIVERY, QUALITY, PRICE
        Double score = (Double) data.get("score"); // 0-100

        if (supplierId == null || performanceType == null || score == null) {
            sendErrorResponse(msg, "Missing performance update data");
            return;
        }

        SupplierInfo supplier = suppliers.get(supplierId);
        if (supplier == null) {
            sendErrorResponse(msg, "Supplier not found: " + supplierId);
            return;
        }

        // Update supplier performance metrics
        updateSupplierMetrics(supplier, performanceType, score);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "SUPPLIER_PERFORMANCE_UPDATED");
        response.put("supplierId", supplierId);
        response.put("performanceType", performanceType);
        response.put("newScore", score);
        response.put("updatedRating", supplier.getRating());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Updated supplier {} performance: {} = {}", supplierId, performanceType, score);
    }

    private void handleNegotiatePrice(ACLMessage msg, Map<String, Object> data) {
        String supplierId = (String) data.get("supplierId");
        String materialId = (String) data.get("materialId");
        Integer quantity = (Integer) data.get("quantity");
        Double targetPrice = (Double) data.get("targetPrice");

        if (supplierId == null || materialId == null || quantity == null || targetPrice == null) {
            sendErrorResponse(msg, "Missing negotiation data");
            return;
        }

        SupplierInfo supplier = suppliers.get(supplierId);
        if (supplier == null) {
            sendErrorResponse(msg, "Supplier not found: " + supplierId);
            return;
        }

        double currentPrice = materialPrices.getOrDefault(materialId, 10.0);
        double supplierMultiplier = getSupplierPriceMultiplier(supplier, "NORMAL");
        double currentQuote = currentPrice * supplierMultiplier;

        // Simple negotiation logic
        boolean negotiationSuccessful = false;
        double finalPrice = currentQuote;
        String negotiationResult = "REJECTED";

        if (targetPrice >= currentQuote * 0.85) { // Accept if within 15% of original price
            negotiationSuccessful = true;
            finalPrice = Math.max(targetPrice, currentQuote * 0.85);
            negotiationResult = "ACCEPTED";
        } else if (quantity >= 1000) { // Volume discount for large orders
            negotiationSuccessful = true;
            finalPrice = currentQuote * 0.90; // 10% discount
            negotiationResult = "COUNTER_OFFER";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("action", "PRICE_NEGOTIATION_RESULT");
        response.put("supplierId", supplierId);
        response.put("materialId", materialId);
        response.put("quantity", quantity);
        response.put("originalPrice", currentQuote);
        response.put("targetPrice", targetPrice);
        response.put("finalPrice", finalPrice);
        response.put("result", negotiationResult);
        response.put("successful", negotiationSuccessful);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(negotiationSuccessful ? ACLMessage.AGREE : ACLMessage.REFUSE);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Price negotiation with {} for {}: {} (original: ${}, final: ${})", 
                supplierId, materialId, negotiationResult, currentQuote, finalPrice);
    }

    private void handleCheckInventoryLevels(ACLMessage msg, Map<String, Object> data) {
        try {
            // Check inventory levels and suggest reorders
            Map<String, Integer> currentLevels = new HashMap<>();
            Map<String, Integer> reorderPoints = new HashMap<>();
            List<String> lowStockMaterials = new ArrayList<>();

            // Get current inventory levels (simplified)
            for (String materialId : materialPrices.keySet()) {
                try {
                    Integer itemId = Integer.parseInt(materialId);
                    List<InventoryItem> inventoryItems = inventoryDAO.findByItemId(itemId);
                    int totalStock = inventoryItems.stream()
                            .mapToInt(item -> item.getAvailableQuantity())
                            .sum();
                    
                    currentLevels.put(materialId, totalStock);
                    
                    // Set reorder point (simplified logic)
                    int reorderPoint = 100; // Minimum stock level
                    reorderPoints.put(materialId, reorderPoint);
                    
                    if (totalStock < reorderPoint) {
                        lowStockMaterials.add(materialId);
                    }
                } catch (NumberFormatException e) {
                    log.warning("Invalid material ID format: " + materialId);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("action", "INVENTORY_LEVELS_REPORT");
            response.put("currentLevels", currentLevels);
            response.put("reorderPoints", reorderPoints);
            response.put("lowStockMaterials", lowStockMaterials);
            response.put("reorderRecommendations", generateReorderRecommendations(lowStockMaterials));

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            if (!lowStockMaterials.isEmpty()) {
                log.warn("Low stock detected for {} materials: {}", lowStockMaterials.size(), lowStockMaterials);
                // Auto-trigger reorder process for critical materials
                autoTriggerReorders(lowStockMaterials);
            }

        } catch (Exception e) {
            log.error("Error checking inventory levels", e);
            sendErrorResponse(msg, "Error checking inventory: " + e.getMessage());
        }
    }

    private double getSupplierPriceMultiplier(SupplierInfo supplier, String urgency) {
        double baseMultiplier = 1.0;
        
        // Quality premium/discount
        if ("A".equals(supplier.getRating())) {
            baseMultiplier += 0.05; // 5% premium for A-rated suppliers
        } else if ("C".equals(supplier.getRating())) {
            baseMultiplier -= 0.05; // 5% discount for C-rated suppliers
        }
        
        // Urgency premium
        if ("URGENT".equals(urgency)) {
            baseMultiplier += 0.15; // 15% premium for urgent orders
        } else if ("EXPRESS".equals(urgency)) {
            baseMultiplier += 0.25; // 25% premium for express orders
        }
        
        return baseMultiplier;
    }

    private int calculateDeliveryTime(SupplierInfo supplier, int quantity, String priority) {
        int baseTime = supplier.getDeliveryTimeAvgDays();
        
        // Adjust for quantity
        if (quantity > 1000) {
            baseTime += 2; // Extra days for large orders
        }
        
        // Adjust for priority
        if ("URGENT".equals(priority)) {
            baseTime = Math.max(1, baseTime - 2);
        } else if ("EXPRESS".equals(priority)) {
            baseTime = Math.max(1, baseTime - 3);
        }
        
        return baseTime;
    }

    private double calculateQuotationScore(QuotationOption quotation) {
        // Score based on price (40%), quality (30%), delivery time (30%)
        double priceScore = 100.0 / quotation.getUnitPrice(); // Lower price = higher score
        double qualityScore = quotation.getQualityRating();
        double deliveryScore = 100.0 / quotation.getDeliveryDays(); // Faster delivery = higher score
        
        return (priceScore * 0.4) + (qualityScore * 0.3) + (deliveryScore * 0.3);
    }

    private double calculateSupplierScore(SupplierInfo supplier) {
        // Overall supplier score considering multiple factors
        double qualityScore = supplier.getQualityRating();
        double deliveryScore = 100.0 / supplier.getDeliveryTimeAvgDays();
        double ratingScore = "A".equals(supplier.getRating()) ? 100 : 
                            "B".equals(supplier.getRating()) ? 80 : 60;
        
        return (qualityScore * 0.4) + (deliveryScore * 0.3) + (ratingScore * 0.3);
    }

    private void updateSupplierMetrics(SupplierInfo supplier, String performanceType, double score) {
        switch (performanceType) {
            case "DELIVERY":
                // Update delivery performance (simplified)
                if (score >= 90) {
                    supplier.setDeliveryTimeAvgDays(Math.max(1, supplier.getDeliveryTimeAvgDays() - 1));
                } else if (score < 70) {
                    supplier.setDeliveryTimeAvgDays(supplier.getDeliveryTimeAvgDays() + 1);
                }
                break;
            case "QUALITY":
                supplier.setQualityRating((supplier.getQualityRating() + score) / 2);
                break;
            case "PRICE":
                // Price competitiveness affects rating
                if (score >= 90) {
                    // Good price competitiveness
                } else if (score < 70) {
                    // Poor price competitiveness
                }
                break;
        }
        
        // Update overall rating
        updateOverallRating(supplier);
    }

    private void updateOverallRating(SupplierInfo supplier) {
        double overallScore = calculateSupplierScore(supplier);
        if (overallScore >= 85) {
            supplier.setRating("A");
        } else if (overallScore >= 70) {
            supplier.setRating("B");
        } else {
            supplier.setRating("C");
        }
    }

    private void sendPurchaseOrderToSupplier(PurchaseOrder po, SupplierInfo supplier) {
        // Simulate sending PO to supplier and receiving confirmation
        // In real system, this would integrate with supplier's system
        
        // Simulate supplier response time
        Random random = new Random();
        if (random.nextDouble() < 0.9) { // 90% acceptance rate
            po.setStatus("CONFIRMED");
        } else {
            po.setStatus("REJECTED");
        }
        
        log.info("Sent purchase order {} to supplier {} - Status: {}", 
                po.getPoNumber(), supplier.getName(), po.getStatus());
    }

    private void updateMarketPrices() {
        // Simulate market price fluctuations
        Random random = new Random();
        for (Map.Entry<String, Double> entry : materialPrices.entrySet()) {
            double currentPrice = entry.getValue();
            double fluctuation = (random.nextGaussian() * 0.02); // 2% standard deviation
            double newPrice = currentPrice * (1 + fluctuation);
            entry.setValue(Math.max(0.1, newPrice)); // Minimum price of 0.1
        }
    }

    private void checkLowInventoryLevels() {
        // Check for materials running low and trigger alerts
        try {
            for (String materialId : materialPrices.keySet()) {
                try {
                    Integer itemId = Integer.parseInt(materialId);
                    List<InventoryItem> inventoryItems = inventoryDAO.findByItemId(itemId);
                    int totalStock = inventoryItems.stream()
                            .mapToInt(item -> item.getAvailableQuantity())
                            .sum();
                    
                    if (totalStock < 50) { // Critical level
                        log.warning("Critical inventory level for " + materialId + ": " + totalStock + " units remaining");
                        // Could trigger automatic reorder here
                        autoTriggerReorders(Arrays.asList(materialId));
                    }
                } catch (NumberFormatException e) {
                    log.warning("Invalid material ID format: " + materialId);
                }
            }
        } catch (Exception e) {
            log.error("Error checking inventory levels", e);
        }
    }

    private void reviewSupplierPerformance() {
        // Periodic review of supplier performance
        for (SupplierInfo supplier : suppliers.values()) {
            double score = calculateSupplierScore(supplier);
            if (score < 60) {
                log.warn("Supplier {} performance below threshold: {}", supplier.getName(), score);
            }
        }
    }

    private List<ReorderRecommendation> generateReorderRecommendations(List<String> lowStockMaterials) {
        List<ReorderRecommendation> recommendations = new ArrayList<>();
        
        for (String materialId : lowStockMaterials) {
            // Find best supplier for this material
            SupplierInfo bestSupplier = null;
            double bestScore = 0;
            
            for (SupplierInfo supplier : suppliers.values()) {
                if (supplier.getAvailableMaterials().contains(materialId)) {
                    double score = calculateSupplierScore(supplier);
                    if (score > bestScore) {
                        bestScore = score;
                        bestSupplier = supplier;
                    }
                }
            }
            
            if (bestSupplier != null) {
                int recommendedQuantity = 500; // Standard reorder quantity
                double unitPrice = materialPrices.get(materialId) * getSupplierPriceMultiplier(bestSupplier, "NORMAL");
                
                recommendations.add(new ReorderRecommendation(
                    materialId,
                    bestSupplier.getSupplierId(),
                    bestSupplier.getName(),
                    recommendedQuantity,
                    unitPrice,
                    "LOW_STOCK"
                ));
            }
        }
        
        return recommendations;
    }

    private void autoTriggerReorders(List<String> criticalMaterials) {
        // Auto-trigger reorders for critical materials
        for (String materialId : criticalMaterials) {
            if (materialPrices.containsKey(materialId)) {
                // Create auto-reorder message to self
                Map<String, Object> autoReorder = new HashMap<>();
                autoReorder.put("action", "AUTO_REORDER");
                autoReorder.put("materialId", materialId);
                autoReorder.put("priority", "URGENT");
                
                ACLMessage autoMsg = new ACLMessage(ACLMessage.REQUEST);
                autoMsg.addReceiver(getAID());
                autoMsg.setContent(JsonUtil.toJson(autoReorder));
                send(autoMsg);
                
                log.info("Auto-triggered reorder for critical material: {}", materialId);
            }
        }
    }

    private void sendErrorResponse(ACLMessage originalMsg, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("action", "ERROR");
        error.put("message", errorMessage);

        ACLMessage reply = originalMsg.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        reply.setContent(JsonUtil.toJson(error));
        send(reply);

        log.error("Procurement error: {}", errorMessage);
    }

    @Override
    protected void takeDown() {
        log.info("AgenteGestorDeCompras {} shutting down", getAID().getName());
        super.takeDown();
    }

    // Helper classes
    private static class SupplierInfo {
        private String supplierId;
        private String name;
        private List<String> availableMaterials;
        private double qualityRating; // 0-100
        private int deliveryTimeAvgDays;
        private String rating; // A, B, C

        public SupplierInfo(String supplierId, String name, List<String> availableMaterials, 
                           double qualityRating, int deliveryTimeAvgDays, String rating) {
            this.supplierId = supplierId;
            this.name = name;
            this.availableMaterials = new ArrayList<>(availableMaterials);
            this.qualityRating = qualityRating;
            this.deliveryTimeAvgDays = deliveryTimeAvgDays;
            this.rating = rating;
        }

        // Getters and setters
        public String getSupplierId() { return supplierId; }
        public String getName() { return name; }
        public List<String> getAvailableMaterials() { return availableMaterials; }
        public double getQualityRating() { return qualityRating; }
        public void setQualityRating(double qualityRating) { this.qualityRating = qualityRating; }
        public int getDeliveryTimeAvgDays() { return deliveryTimeAvgDays; }
        public void setDeliveryTimeAvgDays(int deliveryTimeAvgDays) { this.deliveryTimeAvgDays = deliveryTimeAvgDays; }
        public String getRating() { return rating; }
        public void setRating(String rating) { this.rating = rating; }
    }

    private static class PurchaseOrder {
        private String poNumber;
        private String supplierId;
        private String materialId;
        private int quantity;
        private double unitPrice;
        private String priority;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime expectedDelivery;
        private LocalDateTime actualDelivery;

        public PurchaseOrder(String poNumber, String supplierId, String materialId, 
                           int quantity, double unitPrice, String priority) {
            this.poNumber = poNumber;
            this.supplierId = supplierId;
            this.materialId = materialId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.priority = priority;
            this.status = "CREATED";
            this.createdAt = LocalDateTime.now();
        }

        public double getTotalAmount() {
            return quantity * unitPrice;
        }

        // Getters and setters
        public String getPoNumber() { return poNumber; }
        public String getSupplierId() { return supplierId; }
        public String getMaterialId() { return materialId; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public String getPriority() { return priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpectedDelivery() { return expectedDelivery; }
        public void setExpectedDelivery(LocalDateTime expectedDelivery) { this.expectedDelivery = expectedDelivery; }
        public LocalDateTime getActualDelivery() { return actualDelivery; }
        public void setActualDelivery(LocalDateTime actualDelivery) { this.actualDelivery = actualDelivery; }
    }

    private static class QuotationOption {
        private String supplierId;
        private String supplierName;
        private String materialId;
        private int quantity;
        private double unitPrice;
        private double totalPrice;
        private int deliveryDays;
        private double qualityRating;

        public QuotationOption(String supplierId, String supplierName, String materialId, 
                             int quantity, double unitPrice, double totalPrice, 
                             int deliveryDays, double qualityRating) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.materialId = materialId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
            this.deliveryDays = deliveryDays;
            this.qualityRating = qualityRating;
        }

        // Getters
        public String getSupplierId() { return supplierId; }
        public String getSupplierName() { return supplierName; }
        public String getMaterialId() { return materialId; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getTotalPrice() { return totalPrice; }
        public int getDeliveryDays() { return deliveryDays; }
        public double getQualityRating() { return qualityRating; }
    }

    private static class SupplierEvaluation {
        private String supplierId;
        private String supplierName;
        private double qualityRating;
        private int deliveryTimeAvgDays;
        private String rating;
        private double overallScore;

        public SupplierEvaluation(String supplierId, String supplierName, double qualityRating, 
                                int deliveryTimeAvgDays, String rating, double overallScore) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.qualityRating = qualityRating;
            this.deliveryTimeAvgDays = deliveryTimeAvgDays;
            this.rating = rating;
            this.overallScore = overallScore;
        }

        // Getters
        public String getSupplierId() { return supplierId; }
        public String getSupplierName() { return supplierName; }
        public double getQualityRating() { return qualityRating; }
        public int getDeliveryTimeAvgDays() { return deliveryTimeAvgDays; }
        public String getRating() { return rating; }
        public double getOverallScore() { return overallScore; }
    }

    private static class ReorderRecommendation {
        private String materialId;
        private String recommendedSupplierId;
        private String supplierName;
        private int recommendedQuantity;
        private double estimatedUnitPrice;
        private String reason;

        public ReorderRecommendation(String materialId, String recommendedSupplierId, 
                                   String supplierName, int recommendedQuantity, 
                                   double estimatedUnitPrice, String reason) {
            this.materialId = materialId;
            this.recommendedSupplierId = recommendedSupplierId;
            this.supplierName = supplierName;
            this.recommendedQuantity = recommendedQuantity;
            this.estimatedUnitPrice = estimatedUnitPrice;
            this.reason = reason;
        }

        // Getters
        public String getMaterialId() { return materialId; }
        public String getRecommendedSupplierId() { return recommendedSupplierId; }
        public String getSupplierName() { return supplierName; }
        public int getRecommendedQuantity() { return recommendedQuantity; }
        public double getEstimatedUnitPrice() { return estimatedUnitPrice; }
        public String getReason() { return reason; }
    }
}
