package com.smagesci.agents.logistics;

import com.smagesci.agents.BaseAgent;
import com.smagesci.dao.InventoryDAO;
import com.smagesci.models.InventoryItem;
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
 * Agente responsable de la gestión de transporte y rutas de entrega.
 * Optimiza rutas, gestiona flotas de vehículos y coordina entregas.
 */
public class AgenteGestorDeTransporte extends BaseAgent {

    private Map<String, VehicleInfo> vehicleFleet;
    private Map<String, RouteInfo> activeRoutes;
    private Map<String, DeliveryZone> deliveryZones;
    private InventoryDAO inventoryDAO;
    private int routeCounter;

    @Override
    protected void setup() {
        super.setup();
        inventoryDAO = new InventoryDAO();
        vehicleFleet = initializeVehicleFleet();
        activeRoutes = new HashMap<>();
        deliveryZones = initializeDeliveryZones();
        routeCounter = 1;

        log.info("AgenteGestorDeTransporte {} is ready with {} vehicles", 
                getAID().getName(), vehicleFleet.size());
        registerService("transport-management", "smagesci-transport-manager");

        addBehaviour(new TransportManagementBehaviour());
        addBehaviour(new VehicleMonitoringBehaviour(this, 60000)); // Check every minute
    }

    private Map<String, VehicleInfo> initializeVehicleFleet() {
        Map<String, VehicleInfo> fleet = new HashMap<>();
        
        // Delivery trucks
        fleet.put("TRK001", new VehicleInfo("TRK001", "TRUCK", 5000, "AVAILABLE", "MAIN-WH"));
        fleet.put("TRK002", new VehicleInfo("TRK002", "TRUCK", 5000, "AVAILABLE", "MAIN-WH"));
        fleet.put("TRK003", new VehicleInfo("TRK003", "TRUCK", 5000, "AVAILABLE", "NORTH-WH"));
        
        // Vans for local delivery
        fleet.put("VAN001", new VehicleInfo("VAN001", "VAN", 2000, "AVAILABLE", "MAIN-WH"));
        fleet.put("VAN002", new VehicleInfo("VAN002", "VAN", 2000, "AVAILABLE", "SOUTH-WH"));
        fleet.put("VAN003", new VehicleInfo("VAN003", "VAN", 2000, "MAINTENANCE", "MAIN-WH"));
        
        return fleet;
    }

    private Map<String, DeliveryZone> initializeDeliveryZones() {
        Map<String, DeliveryZone> zones = new HashMap<>();
        
        zones.put("NORTH", new DeliveryZone("NORTH", "North District", 25, Arrays.asList("TRK003", "VAN001")));
        zones.put("SOUTH", new DeliveryZone("SOUTH", "South District", 30, Arrays.asList("VAN002")));
        zones.put("CENTRAL", new DeliveryZone("CENTRAL", "Central District", 15, Arrays.asList("TRK001", "TRK002")));
        zones.put("EAST", new DeliveryZone("EAST", "East District", 35, Arrays.asList("TRK001", "TRK002")));
        zones.put("WEST", new DeliveryZone("WEST", "West District", 40, Arrays.asList("TRK003")));
        
        return zones;
    }

    private class TransportManagementBehaviour extends CyclicBehaviour {
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
                        case "SCHEDULE_DELIVERY":
                            handleScheduleDelivery(msg, data);
                            break;
                        case "OPTIMIZE_ROUTE":
                            handleOptimizeRoute(msg, data);
                            break;
                        case "TRACK_VEHICLE":
                            handleTrackVehicle(msg, data);
                            break;
                        case "UPDATE_VEHICLE_STATUS":
                            handleUpdateVehicleStatus(msg, data);
                            break;
                        case "GET_FLEET_STATUS":
                            handleGetFleetStatus(msg, data);
                            break;
                        case "SCHEDULE_MAINTENANCE":
                            handleScheduleMaintenance(msg, data);
                            break;
                        default:
                            log.warn("Unknown action received: {}", action);
                            sendErrorResponse(msg, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    log.error("Error processing transport message", e);
                    sendErrorResponse(msg, "Error processing request: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private class VehicleMonitoringBehaviour extends TickerBehaviour {
        public VehicleMonitoringBehaviour(jade.core.Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            // Monitor vehicle status and update routes
            updateVehiclePositions();
            checkDeliveryCompletions();
            optimizeFleetUtilization();
        }
    }

    private void handleScheduleDelivery(ACLMessage msg, Map<String, Object> data) {
        try {
            String shipmentId = (String) data.get("shipmentId");
            String destination = (String) data.get("destination");
            Double weight = (Double) data.getOrDefault("weight", 100.0);
            String priority = (String) data.getOrDefault("priority", "STANDARD");
            String deliveryZone = (String) data.getOrDefault("zone", "CENTRAL");

            if (shipmentId == null || destination == null) {
                sendErrorResponse(msg, "Missing required delivery data");
                return;
            }

            // Find suitable vehicle
            VehicleInfo selectedVehicle = selectBestVehicle(weight, deliveryZone, priority);

            if (selectedVehicle == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("action", "DELIVERY_REJECTED");
                response.put("shipmentId", shipmentId);
                response.put("reason", "No suitable vehicle available");

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent(JsonUtil.toJson(response));
                send(reply);

                log.warn("Delivery rejected for shipment {} - no suitable vehicle", shipmentId);
                return;
            }

            // Create route
            String routeId = "ROUTE" + String.format("%04d", routeCounter++);
            RouteInfo route = new RouteInfo(routeId, selectedVehicle.getVehicleId(), shipmentId, destination, deliveryZone);
            route.addDeliveryStop(destination, shipmentId, weight);

            activeRoutes.put(routeId, route);
            selectedVehicle.setStatus("ON_ROUTE");
            selectedVehicle.setCurrentRoute(routeId);

            // Calculate estimated delivery time
            DeliveryZone zone = deliveryZones.get(deliveryZone);
            int estimatedMinutes = zone != null ? zone.getAverageDeliveryTimeMinutes() : 60;

            Map<String, Object> response = new HashMap<>();
            response.put("action", "DELIVERY_SCHEDULED");
            response.put("shipmentId", shipmentId);
            response.put("routeId", routeId);
            response.put("vehicleId", selectedVehicle.getVehicleId());
            response.put("estimatedDeliveryMinutes", estimatedMinutes);
            response.put("destination", destination);

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Delivery scheduled for shipment {} with vehicle {} on route {}", 
                    shipmentId, selectedVehicle.getVehicleId(), routeId);

        } catch (Exception e) {
            log.error("Error scheduling delivery", e);
            sendErrorResponse(msg, "Error scheduling delivery: " + e.getMessage());
        }
    }

    private void handleOptimizeRoute(ACLMessage msg, Map<String, Object> data) {
        String routeId = (String) data.get("routeId");

        if (routeId == null) {
            sendErrorResponse(msg, "Missing routeId");
            return;
        }

        RouteInfo route = activeRoutes.get(routeId);

        if (route == null) {
            sendErrorResponse(msg, "Route not found: " + routeId);
            return;
        }

        // Simple optimization algorithm (in real system, use more sophisticated algorithms)
        List<DeliveryStop> optimizedStops = optimizeDeliveryOrder(route.getDeliveryStops());
        route.setDeliveryStops(optimizedStops);

        // Calculate new estimated time and distance
        double totalDistance = calculateTotalDistance(optimizedStops);
        int estimatedTime = calculateEstimatedTime(optimizedStops);

        route.setEstimatedDistance(totalDistance);
        route.setEstimatedTimeMinutes(estimatedTime);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "ROUTE_OPTIMIZED");
        response.put("routeId", routeId);
        response.put("optimizedStops", optimizedStops.size());
        response.put("estimatedDistance", totalDistance);
        response.put("estimatedTimeMinutes", estimatedTime);
        response.put("timeSaved", route.getOriginalEstimatedTime() - estimatedTime);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Route {} optimized - {} stops, {} km, {} minutes", 
                routeId, optimizedStops.size(), totalDistance, estimatedTime);
    }

    private void handleTrackVehicle(ACLMessage msg, Map<String, Object> data) {
        String vehicleId = (String) data.get("vehicleId");

        if (vehicleId == null) {
            sendErrorResponse(msg, "Missing vehicleId");
            return;
        }

        VehicleInfo vehicle = vehicleFleet.get(vehicleId);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "VEHICLE_TRACKING");
        response.put("vehicleId", vehicleId);

        if (vehicle != null) {
            response.put("status", vehicle.getStatus());
            response.put("currentLocation", vehicle.getCurrentLocation());
            response.put("currentRoute", vehicle.getCurrentRoute());
            response.put("fuelLevel", vehicle.getFuelLevel());
            response.put("lastUpdate", vehicle.getLastUpdate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // If on route, add route progress
            if (vehicle.getCurrentRoute() != null) {
                RouteInfo route = activeRoutes.get(vehicle.getCurrentRoute());
                if (route != null) {
                    response.put("routeProgress", route.getProgress());
                    response.put("nextStop", route.getNextStop());
                    response.put("estimatedArrival", route.getEstimatedArrival());
                }
            }
        } else {
            response.put("status", "NOT_FOUND");
            response.put("message", "Vehicle not found");
        }

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void handleUpdateVehicleStatus(ACLMessage msg, Map<String, Object> data) {
        String vehicleId = (String) data.get("vehicleId");
        String newStatus = (String) data.get("status");
        String location = (String) data.get("location");

        if (vehicleId == null || newStatus == null) {
            sendErrorResponse(msg, "Missing vehicleId or status");
            return;
        }

        VehicleInfo vehicle = vehicleFleet.get(vehicleId);

        if (vehicle == null) {
            sendErrorResponse(msg, "Vehicle not found: " + vehicleId);
            return;
        }

        vehicle.setStatus(newStatus);
        if (location != null) {
            vehicle.setCurrentLocation(location);
        }
        vehicle.setLastUpdate(LocalDateTime.now());

        // If delivery completed, update route
        if ("DELIVERED".equals(newStatus) && vehicle.getCurrentRoute() != null) {
            RouteInfo route = activeRoutes.get(vehicle.getCurrentRoute());
            if (route != null) {
                route.completeCurrentDelivery();
                if (route.isCompleted()) {
                    vehicle.setStatus("RETURNING");
                    vehicle.setCurrentRoute(null);
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("action", "VEHICLE_STATUS_UPDATED");
        response.put("vehicleId", vehicleId);
        response.put("newStatus", newStatus);
        response.put("location", location);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Updated vehicle {} status to {} at location {}", vehicleId, newStatus, location);
    }

    private void handleGetFleetStatus(ACLMessage msg, Map<String, Object> data) {
        Map<String, Object> fleetStatus = new HashMap<>();
        Map<String, Object> vehicleStatuses = new HashMap<>();
        Map<String, Integer> statusCounts = new HashMap<>();

        for (VehicleInfo vehicle : vehicleFleet.values()) {
            Map<String, Object> vehicleData = new HashMap<>();
            vehicleData.put("type", vehicle.getType());
            vehicleData.put("status", vehicle.getStatus());
            vehicleData.put("location", vehicle.getCurrentLocation());
            vehicleData.put("capacity", vehicle.getCapacityKg());
            vehicleData.put("fuelLevel", vehicle.getFuelLevel());

            vehicleStatuses.put(vehicle.getVehicleId(), vehicleData);

            // Count by status
            statusCounts.put(vehicle.getStatus(), 
                    statusCounts.getOrDefault(vehicle.getStatus(), 0) + 1);
        }

        fleetStatus.put("action", "FLEET_STATUS");
        fleetStatus.put("totalVehicles", vehicleFleet.size());
        fleetStatus.put("statusCounts", statusCounts);
        fleetStatus.put("vehicles", vehicleStatuses);
        fleetStatus.put("activeRoutes", activeRoutes.size());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(fleetStatus));
        send(reply);
    }

    private void handleScheduleMaintenance(ACLMessage msg, Map<String, Object> data) {
        String vehicleId = (String) data.get("vehicleId");
        String maintenanceType = (String) data.getOrDefault("type", "ROUTINE");

        if (vehicleId == null) {
            sendErrorResponse(msg, "Missing vehicleId");
            return;
        }

        VehicleInfo vehicle = vehicleFleet.get(vehicleId);

        if (vehicle == null) {
            sendErrorResponse(msg, "Vehicle not found: " + vehicleId);
            return;
        }

        if ("ON_ROUTE".equals(vehicle.getStatus())) {
            sendErrorResponse(msg, "Cannot schedule maintenance for vehicle on route");
            return;
        }

        vehicle.setStatus("MAINTENANCE");

        Map<String, Object> response = new HashMap<>();
        response.put("action", "MAINTENANCE_SCHEDULED");
        response.put("vehicleId", vehicleId);
        response.put("maintenanceType", maintenanceType);
        response.put("estimatedDuration", "ROUTINE".equals(maintenanceType) ? 4 : 8);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Scheduled {} maintenance for vehicle {}", maintenanceType, vehicleId);
    }

    private VehicleInfo selectBestVehicle(double weight, String deliveryZone, String priority) {
        DeliveryZone zone = deliveryZones.get(deliveryZone);
        List<String> preferredVehicles = zone != null ? zone.getPreferredVehicles() : new ArrayList<>();

        // First try preferred vehicles for the zone
        for (String vehicleId : preferredVehicles) {
            VehicleInfo vehicle = vehicleFleet.get(vehicleId);
            if (vehicle != null && "AVAILABLE".equals(vehicle.getStatus()) && 
                vehicle.getCapacityKg() >= weight) {
                return vehicle;
            }
        }

        // If no preferred vehicle available, find any suitable vehicle
        for (VehicleInfo vehicle : vehicleFleet.values()) {
            if ("AVAILABLE".equals(vehicle.getStatus()) && vehicle.getCapacityKg() >= weight) {
                return vehicle;
            }
        }

        return null;
    }

    private List<DeliveryStop> optimizeDeliveryOrder(List<DeliveryStop> stops) {
        // Simple nearest neighbor optimization (in real system, use more sophisticated algorithms)
        if (stops.size() <= 1) return stops;

        List<DeliveryStop> optimized = new ArrayList<>();
        List<DeliveryStop> remaining = new ArrayList<>(stops);

        // Start with the first stop
        DeliveryStop current = remaining.remove(0);
        optimized.add(current);

        while (!remaining.isEmpty()) {
            // Find nearest stop
            DeliveryStop nearest = findNearestStop(current, remaining);
            remaining.remove(nearest);
            optimized.add(nearest);
            current = nearest;
        }

        return optimized;
    }

    private DeliveryStop findNearestStop(DeliveryStop current, List<DeliveryStop> candidates) {
        // Simplified distance calculation (in real system, use actual GPS coordinates)
        return candidates.get(0); // For demo, just return first
    }

    private double calculateTotalDistance(List<DeliveryStop> stops) {
        // Simplified calculation (in real system, use actual routing API)
        return stops.size() * 5.0; // 5 km per stop on average
    }

    private int calculateEstimatedTime(List<DeliveryStop> stops) {
        // Simplified calculation (in real system, consider traffic, road conditions)
        return stops.size() * 20; // 20 minutes per stop on average
    }

    private void updateVehiclePositions() {
        for (VehicleInfo vehicle : vehicleFleet.values()) {
            if ("ON_ROUTE".equals(vehicle.getStatus())) {
                // Simulate movement (in real system, get from GPS tracker)
                Random random = new Random();
                vehicle.setFuelLevel(Math.max(10, vehicle.getFuelLevel() - random.nextInt(5)));
                vehicle.setLastUpdate(LocalDateTime.now());
            }
        }
    }

    private void checkDeliveryCompletions() {
        // Simulate delivery completions
        Random random = new Random();
        for (RouteInfo route : activeRoutes.values()) {
            if ("IN_PROGRESS".equals(route.getStatus()) && random.nextDouble() < 0.05) { // 5% chance
                VehicleInfo vehicle = vehicleFleet.get(route.getVehicleId());
                if (vehicle != null) {
                    route.completeCurrentDelivery();
                    if (route.isCompleted()) {
                        vehicle.setStatus("RETURNING");
                        vehicle.setCurrentRoute(null);
                        log.info("Route {} completed by vehicle {}", route.getRouteId(), vehicle.getVehicleId());
                    }
                }
            }
        }
    }

    private void optimizeFleetUtilization() {
        // Monitor fleet utilization and suggest optimizations
        long availableVehicles = vehicleFleet.values().stream()
                .filter(v -> "AVAILABLE".equals(v.getStatus()))
                .count();

        long onRouteVehicles = vehicleFleet.values().stream()
                .filter(v -> "ON_ROUTE".equals(v.getStatus()))
                .count();

        double utilizationRate = (double) onRouteVehicles / vehicleFleet.size();

        if (utilizationRate > 0.8) {
            log.warn("High fleet utilization: {}% - consider adding more vehicles", utilizationRate * 100);
        } else if (utilizationRate < 0.3) {
            log.info("Low fleet utilization: {}% - opportunity for route consolidation", utilizationRate * 100);
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

        log.error("Transport error: {}", errorMessage);
    }

    @Override
    protected void takeDown() {
        log.info("AgenteGestorDeTransporte {} shutting down", getAID().getName());
        super.takeDown();
    }

    // Helper classes
    private static class VehicleInfo {
        private String vehicleId;
        private String type;
        private double capacityKg;
        private String status;
        private String currentLocation;
        private String currentRoute;
        private double fuelLevel;
        private LocalDateTime lastUpdate;

        public VehicleInfo(String vehicleId, String type, double capacityKg, String status, String currentLocation) {
            this.vehicleId = vehicleId;
            this.type = type;
            this.capacityKg = capacityKg;
            this.status = status;
            this.currentLocation = currentLocation;
            this.fuelLevel = 100.0;
            this.lastUpdate = LocalDateTime.now();
        }

        // Getters and setters
        public String getVehicleId() { return vehicleId; }
        public String getType() { return type; }
        public double getCapacityKg() { return capacityKg; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCurrentLocation() { return currentLocation; }
        public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
        public String getCurrentRoute() { return currentRoute; }
        public void setCurrentRoute(String currentRoute) { this.currentRoute = currentRoute; }
        public double getFuelLevel() { return fuelLevel; }
        public void setFuelLevel(double fuelLevel) { this.fuelLevel = fuelLevel; }
        public LocalDateTime getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
    }

    private static class RouteInfo {
        private String routeId;
        private String vehicleId;
        private String primaryShipmentId;
        private String destination;
        private String zone;
        private String status;
        private List<DeliveryStop> deliveryStops;
        private int currentStopIndex;
        private double estimatedDistance;
        private int estimatedTimeMinutes;
        private int originalEstimatedTime;
        private LocalDateTime createdAt;

        public RouteInfo(String routeId, String vehicleId, String primaryShipmentId, String destination, String zone) {
            this.routeId = routeId;
            this.vehicleId = vehicleId;
            this.primaryShipmentId = primaryShipmentId;
            this.destination = destination;
            this.zone = zone;
            this.status = "CREATED";
            this.deliveryStops = new ArrayList<>();
            this.currentStopIndex = 0;
            this.createdAt = LocalDateTime.now();
        }

        public void addDeliveryStop(String destination, String shipmentId, double weight) {
            deliveryStops.add(new DeliveryStop(destination, shipmentId, weight));
        }

        public void completeCurrentDelivery() {
            if (currentStopIndex < deliveryStops.size()) {
                deliveryStops.get(currentStopIndex).setCompleted(true);
                currentStopIndex++;
                
                if (currentStopIndex >= deliveryStops.size()) {
                    status = "COMPLETED";
                } else {
                    status = "IN_PROGRESS";
                }
            }
        }

        public boolean isCompleted() {
            return "COMPLETED".equals(status);
        }

        public String getNextStop() {
            if (currentStopIndex < deliveryStops.size()) {
                return deliveryStops.get(currentStopIndex).getDestination();
            }
            return null;
        }

        public double getProgress() {
            if (deliveryStops.isEmpty()) return 0.0;
            return (double) currentStopIndex / deliveryStops.size() * 100.0;
        }

        public String getEstimatedArrival() {
            if (estimatedTimeMinutes > 0) {
                return LocalDateTime.now().plusMinutes(estimatedTimeMinutes)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return null;
        }

        // Getters and setters
        public String getRouteId() { return routeId; }
        public String getVehicleId() { return vehicleId; }
        public String getStatus() { return status; }
        public List<DeliveryStop> getDeliveryStops() { return deliveryStops; }
        public void setDeliveryStops(List<DeliveryStop> deliveryStops) { this.deliveryStops = deliveryStops; }
        public double getEstimatedDistance() { return estimatedDistance; }
        public void setEstimatedDistance(double estimatedDistance) { this.estimatedDistance = estimatedDistance; }
        public int getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
        public void setEstimatedTimeMinutes(int estimatedTimeMinutes) { this.estimatedTimeMinutes = estimatedTimeMinutes; }
        public int getOriginalEstimatedTime() { return originalEstimatedTime; }
    }

    private static class DeliveryStop {
        private String destination;
        private String shipmentId;
        private double weight;
        private boolean completed;
        private LocalDateTime completedAt;

        public DeliveryStop(String destination, String shipmentId, double weight) {
            this.destination = destination;
            this.shipmentId = shipmentId;
            this.weight = weight;
            this.completed = false;
        }

        public String getDestination() { return destination; }
        public String getShipmentId() { return shipmentId; }
        public double getWeight() { return weight; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { 
            this.completed = completed;
            if (completed) {
                this.completedAt = LocalDateTime.now();
            }
        }
    }

    private static class DeliveryZone {
        private String zoneId;
        private String name;
        private int averageDeliveryTimeMinutes;
        private List<String> preferredVehicles;

        public DeliveryZone(String zoneId, String name, int averageDeliveryTimeMinutes, List<String> preferredVehicles) {
            this.zoneId = zoneId;
            this.name = name;
            this.averageDeliveryTimeMinutes = averageDeliveryTimeMinutes;
            this.preferredVehicles = preferredVehicles;
        }

        public String getZoneId() { return zoneId; }
        public String getName() { return name; }
        public int getAverageDeliveryTimeMinutes() { return averageDeliveryTimeMinutes; }
        public List<String> getPreferredVehicles() { return preferredVehicles; }
    }
}
