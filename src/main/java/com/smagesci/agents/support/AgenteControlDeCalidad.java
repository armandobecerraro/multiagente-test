package com.smagesci.agents.support;

import com.smagesci.agents.BaseAgent;
import com.smagesci.utils.JsonUtil;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Agente responsable del control de calidad en toda la cadena de suministro.
 * Monitorea estándares de calidad, gestiona inspecciones y controla defectos.
 */
public class AgenteControlDeCalidad extends BaseAgent {

    private Map<String, QualityStandard> qualityStandards;
    private Map<String, QualityInspection> activeInspections;
    private Map<String, QualityMetrics> qualityMetrics;
    private List<QualityAlert> activeAlerts;
    private int inspectionCounter;

    @Override
    protected void setup() {
        super.setup();
        qualityStandards = initializeQualityStandards();
        activeInspections = new HashMap<>();
        qualityMetrics = new HashMap<>();
        activeAlerts = new ArrayList<>();
        inspectionCounter = 1;

        log.info("AgenteControlDeCalidad {} is ready with {} quality standards", 
                getAID().getName(), qualityStandards.size());
        registerService("quality-control", "smagesci-quality-controller");

        addBehaviour(new QualityControlBehaviour());
        addBehaviour(new QualityMonitoringBehaviour(this, 120000)); // Check every 2 minutes
    }

    private Map<String, QualityStandard> initializeQualityStandards() {
        Map<String, QualityStandard> standards = new HashMap<>();
        
        // Raw material standards
        standards.put("STEEL", new QualityStandard("STEEL", "Raw Material", 
                Arrays.asList("purity >= 99.5%", "tensile_strength >= 400MPa", "surface_defects < 0.1%"),
                95.0, "visual,chemical,mechanical"));
        
        standards.put("PLASTIC_PET", new QualityStandard("PLASTIC_PET", "Raw Material",
                Arrays.asList("melt_flow_rate: 10-30", "moisture < 0.005%", "color_consistency >= 95%"),
                92.0, "thermal,moisture,visual"));
        
        // Intermediate product standards
        standards.put("COMPONENT_A", new QualityStandard("COMPONENT_A", "Component",
                Arrays.asList("dimensional_tolerance: ±0.1mm", "surface_finish: Ra < 1.6", "hardness: 45-55 HRC"),
                98.0, "dimensional,surface,hardness"));
        
        // Final product standards
        standards.put("PRODUCT_FINAL", new QualityStandard("PRODUCT_FINAL", "Final Product",
                Arrays.asList("functionality: 100%", "appearance: Grade A", "packaging: intact", "labeling: complete"),
                99.5, "functional,visual,packaging"));
        
        return standards;
    }

    private class QualityControlBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    String content = msg.getContent();
                    Map<String, Object> data = JsonUtil.fromJson(content, Map.class);
                    String action = (String) data.get("action");

                    switch (action) {
                        case "SCHEDULE_INSPECTION":
                            handleScheduleInspection(msg, data);
                            break;
                        case "RECORD_QUALITY_DATA":
                            handleRecordQualityData(msg, data);
                            break;
                        case "REQUEST_QUALITY_REPORT":
                            handleRequestQualityReport(msg, data);
                            break;
                        case "UPDATE_QUALITY_STANDARD":
                            handleUpdateQualityStandard(msg, data);
                            break;
                        case "REPORT_DEFECT":
                            handleReportDefect(msg, data);
                            break;
                        case "APPROVE_BATCH":
                            handleApproveBatch(msg, data);
                            break;
                        case "REJECT_BATCH":
                            handleRejectBatch(msg, data);
                            break;
                        case "GET_QUALITY_ALERTS":
                            handleGetQualityAlerts(msg, data);
                            break;
                        default:
                            log.warn("Unknown action received: {}", action);
                            sendErrorResponse(msg, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    log.error("Error processing quality control message", e);
                    sendErrorResponse(msg, "Error processing request: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private class QualityMonitoringBehaviour extends TickerBehaviour {
        public QualityMonitoringBehaviour(jade.core.Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            // Monitor quality metrics and trigger alerts
            updateQualityMetrics();
            checkQualityThresholds();
            processScheduledInspections();
        }
    }

    private void handleScheduleInspection(ACLMessage msg, Map<String, Object> data) {
        try {
            String productId = (String) data.get("productId");
            String batchId = (String) data.get("batchId");
            String inspectionType = (String) data.getOrDefault("inspectionType", "ROUTINE");
            String priority = (String) data.getOrDefault("priority", "NORMAL");

            if (productId == null) {
                sendErrorResponse(msg, "Missing productId");
                return;
            }

            QualityStandard standard = qualityStandards.get(productId);
            if (standard == null) {
                // Create default standard for unknown products
                standard = new QualityStandard(productId, "Unknown", 
                        Arrays.asList("basic_quality_check"), 85.0, "visual");
                qualityStandards.put(productId, standard);
            }

            // Create inspection
            String inspectionId = "QI" + String.format("%06d", inspectionCounter++);
            QualityInspection inspection = new QualityInspection(
                inspectionId, productId, batchId, inspectionType, priority, standard
            );

            activeInspections.put(inspectionId, inspection);

            // Schedule inspection execution
            scheduleInspectionExecution(inspection);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "INSPECTION_SCHEDULED");
            response.put("inspectionId", inspectionId);
            response.put("productId", productId);
            response.put("batchId", batchId);
            response.put("inspectionType", inspectionType);
            response.put("estimatedDuration", estimateInspectionDuration(standard));
            response.put("scheduledTime", inspection.getScheduledTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Scheduled {} inspection {} for product {} batch {}", 
                    inspectionType, inspectionId, productId, batchId);

        } catch (Exception e) {
            log.error("Error scheduling inspection", e);
            sendErrorResponse(msg, "Error scheduling inspection: " + e.getMessage());
        }
    }

    private void handleRecordQualityData(ACLMessage msg, Map<String, Object> data) {
        try {
            String inspectionId = (String) data.get("inspectionId");
            String productId = (String) data.get("productId");
            Map<String, Object> measurements = (Map<String, Object>) data.get("measurements");
            String inspectorId = (String) data.getOrDefault("inspectorId", "AUTO_SYSTEM");

            if (inspectionId == null && productId == null) {
                sendErrorResponse(msg, "Missing inspectionId or productId");
                return;
            }

            QualityInspection inspection = null;
            if (inspectionId != null) {
                inspection = activeInspections.get(inspectionId);
            }

            if (inspection == null && productId != null) {
                // Create ad-hoc inspection for quality data recording
                inspectionId = "QI" + String.format("%06d", inspectionCounter++);
                QualityStandard standard = qualityStandards.get(productId);
                if (standard != null) {
                    inspection = new QualityInspection(inspectionId, productId, 
                            "BATCH_" + System.currentTimeMillis(), "AD_HOC", "NORMAL", standard);
                    activeInspections.put(inspectionId, inspection);
                }
            }

            if (inspection == null) {
                sendErrorResponse(msg, "Inspection not found or cannot be created");
                return;
            }

            // Record quality measurements
            inspection.recordMeasurements(measurements, inspectorId);

            // Evaluate against standards
            QualityResult result = evaluateQuality(inspection);
            inspection.setResult(result);

            // Update quality metrics
            updateProductQualityMetrics(inspection.getProductId(), result);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "QUALITY_DATA_RECORDED");
            response.put("inspectionId", inspectionId);
            response.put("productId", inspection.getProductId());
            response.put("qualityScore", result.getOverallScore());
            response.put("passed", result.isPassed());
            response.put("failedCriteria", result.getFailedCriteria());

            if (!result.isPassed()) {
                // Create quality alert for failed inspection
                createQualityAlert(inspection, result);
            }

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Recorded quality data for inspection {} - Score: {}, Passed: {}", 
                    inspectionId, result.getOverallScore(), result.isPassed());

        } catch (Exception e) {
            log.error("Error recording quality data", e);
            sendErrorResponse(msg, "Error recording quality data: " + e.getMessage());
        }
    }

    private void handleRequestQualityReport(ACLMessage msg, Map<String, Object> data) {
        String reportType = (String) data.getOrDefault("reportType", "SUMMARY");
        String productId = (String) data.get("productId");
        String timeRange = (String) data.getOrDefault("timeRange", "LAST_7_DAYS");

        Map<String, Object> report = generateQualityReport(reportType, productId, timeRange);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(report));
        send(reply);

        log.info("Generated {} quality report for product {} ({})", reportType, productId, timeRange);
    }

    private void handleUpdateQualityStandard(ACLMessage msg, Map<String, Object> data) {
        try {
            String productId = (String) data.get("productId");
            String category = (String) data.get("category");
            List<String> criteria = (List<String>) data.get("criteria");
            Double minScore = (Double) data.get("minScore");
            String testMethods = (String) data.get("testMethods");

            if (productId == null || criteria == null) {
                sendErrorResponse(msg, "Missing required quality standard data");
                return;
            }

            QualityStandard standard = qualityStandards.get(productId);
            if (standard == null) {
                standard = new QualityStandard(productId, category, criteria, 
                        minScore != null ? minScore : 85.0, testMethods);
                qualityStandards.put(productId, standard);
            } else {
                standard.updateStandard(category, criteria, minScore, testMethods);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("action", "QUALITY_STANDARD_UPDATED");
            response.put("productId", productId);
            response.put("minScore", standard.getMinimumScore());
            response.put("criteriaCount", standard.getCriteria().size());

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Updated quality standard for product {}", productId);

        } catch (Exception e) {
            log.error("Error updating quality standard", e);
            sendErrorResponse(msg, "Error updating quality standard: " + e.getMessage());
        }
    }

    private void handleReportDefect(ACLMessage msg, Map<String, Object> data) {
        try {
            String productId = (String) data.get("productId");
            String batchId = (String) data.get("batchId");
            String defectType = (String) data.get("defectType");
            String severity = (String) data.getOrDefault("severity", "MEDIUM");
            String description = (String) data.get("description");
            String reportedBy = (String) data.getOrDefault("reportedBy", "SYSTEM");

            if (productId == null || defectType == null) {
                sendErrorResponse(msg, "Missing required defect data");
                return;
            }

            // Create defect record
            String defectId = "DEF" + String.format("%06d", System.currentTimeMillis() % 1000000);
            DefectRecord defect = new DefectRecord(defectId, productId, batchId, 
                    defectType, severity, description, reportedBy);

            // Create quality alert
            QualityAlert alert = new QualityAlert("DEFECT_REPORTED", severity, 
                    "Defect reported: " + defectType + " in " + productId, 
                    Map.of("defectId", defectId, "productId", productId, "batchId", batchId));
            activeAlerts.add(alert);

            // Update quality metrics
            QualityMetrics metrics = qualityMetrics.computeIfAbsent(productId, 
                    k -> new QualityMetrics(productId));
            metrics.recordDefect(severity);

            // Notify relevant agents
            notifyDefectToProduction(defect);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "DEFECT_RECORDED");
            response.put("defectId", defectId);
            response.put("productId", productId);
            response.put("severity", severity);
            response.put("alertCreated", true);

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.warn("Defect reported: {} - {} severity in product {} batch {}", 
                    defectType, severity, productId, batchId);

        } catch (Exception e) {
            log.error("Error reporting defect", e);
            sendErrorResponse(msg, "Error reporting defect: " + e.getMessage());
        }
    }

    private void handleApproveBatch(ACLMessage msg, Map<String, Object> data) {
        String batchId = (String) data.get("batchId");
        String productId = (String) data.get("productId");
        String approvedBy = (String) data.getOrDefault("approvedBy", "QC_AGENT");

        if (batchId == null || productId == null) {
            sendErrorResponse(msg, "Missing batchId or productId");
            return;
        }

        // Record batch approval
        BatchApproval approval = new BatchApproval(batchId, productId, approvedBy, "APPROVED");

        Map<String, Object> response = new HashMap<>();
        response.put("action", "BATCH_APPROVED");
        response.put("batchId", batchId);
        response.put("productId", productId);
        response.put("approvedBy", approvedBy);
        response.put("approvalTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Notify production and inventory
        notifyBatchStatus(batchId, productId, "APPROVED");

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Batch {} of product {} approved by {}", batchId, productId, approvedBy);
    }

    private void handleRejectBatch(ACLMessage msg, Map<String, Object> data) {
        String batchId = (String) data.get("batchId");
        String productId = (String) data.get("productId");
        String reason = (String) data.get("reason");
        String rejectedBy = (String) data.getOrDefault("rejectedBy", "QC_AGENT");

        if (batchId == null || productId == null || reason == null) {
            sendErrorResponse(msg, "Missing batchId, productId, or reason");
            return;
        }

        // Record batch rejection
        BatchApproval rejection = new BatchApproval(batchId, productId, rejectedBy, "REJECTED", reason);

        // Create quality alert
        QualityAlert alert = new QualityAlert("BATCH_REJECTED", "HIGH",
                "Batch " + batchId + " rejected: " + reason,
                Map.of("batchId", batchId, "productId", productId, "reason", reason));
        activeAlerts.add(alert);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "BATCH_REJECTED");
        response.put("batchId", batchId);
        response.put("productId", productId);
        response.put("reason", reason);
        response.put("rejectedBy", rejectedBy);
        response.put("rejectionTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Notify production and inventory
        notifyBatchStatus(batchId, productId, "REJECTED");

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.warn("Batch {} of product {} rejected by {}: {}", batchId, productId, rejectedBy, reason);
    }

    private void handleGetQualityAlerts(ACLMessage msg, Map<String, Object> data) {
        String severity = (String) data.get("severity");
        Boolean activeOnly = (Boolean) data.getOrDefault("activeOnly", true);

        List<QualityAlert> filteredAlerts = activeAlerts.stream()
                .filter(alert -> severity == null || severity.equals(alert.getSeverity()))
                .filter(alert -> !activeOnly || alert.isActive())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("action", "QUALITY_ALERTS");
        response.put("alerts", filteredAlerts);
        response.put("totalCount", filteredAlerts.size());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void scheduleInspectionExecution(QualityInspection inspection) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    // Simulate inspection time
                    Thread.sleep(5000); // 5 seconds for demo
                    
                    // Auto-generate some quality measurements for demo
                    Map<String, Object> autoMeasurements = generateAutoMeasurements(inspection.getStandard());
                    inspection.recordMeasurements(autoMeasurements, "AUTO_INSPECTOR");
                    
                    QualityResult result = evaluateQuality(inspection);
                    inspection.setResult(result);
                    inspection.setStatus("COMPLETED");
                    
                    log.info("Auto-completed inspection {} with score {}", 
                            inspection.getInspectionId(), result.getOverallScore());
                            
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private int estimateInspectionDuration(QualityStandard standard) {
        // Estimate based on number of criteria and test methods
        int baseDuration = 30; // minutes
        int criteriaFactor = standard.getCriteria().size() * 5;
        int methodsFactor = standard.getTestMethods().split(",").length * 10;
        
        return baseDuration + criteriaFactor + methodsFactor;
    }

    private QualityResult evaluateQuality(QualityInspection inspection) {
        QualityStandard standard = inspection.getStandard();
        Map<String, Object> measurements = inspection.getMeasurements();
        
        List<String> failedCriteria = new ArrayList<>();
        double totalScore = 0.0;
        int evaluatedCriteria = 0;
        
        // Evaluate each criterion
        for (String criterion : standard.getCriteria()) {
            double criterionScore = evaluateCriterion(criterion, measurements);
            totalScore += criterionScore;
            evaluatedCriteria++;
            
            if (criterionScore < 70.0) { // Fail threshold
                failedCriteria.add(criterion);
            }
        }
        
        double overallScore = evaluatedCriteria > 0 ? totalScore / evaluatedCriteria : 0.0;
        boolean passed = overallScore >= standard.getMinimumScore() && failedCriteria.isEmpty();
        
        return new QualityResult(overallScore, passed, failedCriteria, 
                "Quality evaluation completed", measurements);
    }

    private double evaluateCriterion(String criterion, Map<String, Object> measurements) {
        // Simplified evaluation logic (in real system, would parse criterion and check measurements)
        Random random = new Random();
        return 70.0 + random.nextDouble() * 30.0; // Random score between 70-100
    }

    private Map<String, Object> generateAutoMeasurements(QualityStandard standard) {
        Map<String, Object> measurements = new HashMap<>();
        Random random = new Random();
        
        // Generate measurements based on test methods
        String[] methods = standard.getTestMethods().split(",");
        for (String method : methods) {
            switch (method.trim()) {
                case "visual":
                    measurements.put("surface_defects", random.nextDouble() * 0.2);
                    measurements.put("color_consistency", 90 + random.nextDouble() * 10);
                    break;
                case "dimensional":
                    measurements.put("tolerance_deviation", random.nextGaussian() * 0.05);
                    measurements.put("dimensional_accuracy", 95 + random.nextDouble() * 5);
                    break;
                case "chemical":
                    measurements.put("purity", 98 + random.nextDouble() * 2);
                    measurements.put("composition", "within_spec");
                    break;
                case "mechanical":
                    measurements.put("tensile_strength", 400 + random.nextDouble() * 100);
                    measurements.put("hardness", 45 + random.nextDouble() * 10);
                    break;
                default:
                    measurements.put("general_quality", 85 + random.nextDouble() * 15);
            }
        }
        
        return measurements;
    }

    private void updateQualityMetrics() {
        // Update quality metrics for all products
        for (QualityMetrics metrics : qualityMetrics.values()) {
            metrics.calculateTrends();
        }
    }

    private void checkQualityThresholds() {
        // Check for quality threshold violations
        for (QualityMetrics metrics : qualityMetrics.values()) {
            if (metrics.getAverageScore() < 85.0) {
                createQualityAlert(null, null, "LOW_QUALITY_TREND", "MEDIUM",
                        "Quality trend declining for product " + metrics.getProductId());
            }
            
            if (metrics.getDefectRate() > 0.05) { // 5% defect rate threshold
                createQualityAlert(null, null, "HIGH_DEFECT_RATE", "HIGH",
                        "High defect rate detected for product " + metrics.getProductId());
            }
        }
    }

    private void processScheduledInspections() {
        // Process inspections that are due
        LocalDateTime now = LocalDateTime.now();
        for (QualityInspection inspection : activeInspections.values()) {
            if ("SCHEDULED".equals(inspection.getStatus()) && 
                inspection.getScheduledTime().isBefore(now)) {
                // Auto-execute inspection
                scheduleInspectionExecution(inspection);
                inspection.setStatus("IN_PROGRESS");
            }
        }
    }

    private void updateProductQualityMetrics(String productId, QualityResult result) {
        QualityMetrics metrics = qualityMetrics.computeIfAbsent(productId, 
                k -> new QualityMetrics(productId));
        metrics.recordInspection(result.getOverallScore(), result.isPassed());
    }

    private void createQualityAlert(QualityInspection inspection, QualityResult result) {
        if (inspection != null && result != null) {
            String message = String.format("Quality inspection failed for %s - Score: %.1f", 
                    inspection.getProductId(), result.getOverallScore());
            QualityAlert alert = new QualityAlert("INSPECTION_FAILED", "HIGH", message,
                    Map.of("inspectionId", inspection.getInspectionId(), 
                           "productId", inspection.getProductId(),
                           "score", result.getOverallScore()));
            activeAlerts.add(alert);
        }
    }

    private void createQualityAlert(QualityInspection inspection, QualityResult result, 
                                  String type, String severity, String message) {
        QualityAlert alert = new QualityAlert(type, severity, message, new HashMap<>());
        activeAlerts.add(alert);
    }

    private Map<String, Object> generateQualityReport(String reportType, String productId, String timeRange) {
        Map<String, Object> report = new HashMap<>();
        report.put("action", "QUALITY_REPORT");
        report.put("reportType", reportType);
        report.put("productId", productId);
        report.put("timeRange", timeRange);
        report.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if ("SUMMARY".equals(reportType)) {
            // Generate summary report
            Map<String, Object> summary = new HashMap<>();
            
            if (productId != null) {
                QualityMetrics metrics = qualityMetrics.get(productId);
                if (metrics != null) {
                    summary.put("averageScore", metrics.getAverageScore());
                    summary.put("passRate", metrics.getPassRate());
                    summary.put("defectRate", metrics.getDefectRate());
                    summary.put("totalInspections", metrics.getTotalInspections());
                }
            } else {
                // Overall summary
                double totalScore = 0;
                int totalInspections = 0;
                int totalPassed = 0;
                
                for (QualityMetrics metrics : qualityMetrics.values()) {
                    totalScore += metrics.getAverageScore() * metrics.getTotalInspections();
                    totalInspections += metrics.getTotalInspections();
                    totalPassed += (int)(metrics.getPassRate() * metrics.getTotalInspections() / 100);
                }
                
                summary.put("overallAverageScore", totalInspections > 0 ? totalScore / totalInspections : 0);
                summary.put("overallPassRate", totalInspections > 0 ? (double)totalPassed / totalInspections * 100 : 0);
                summary.put("totalInspections", totalInspections);
                summary.put("activeAlerts", activeAlerts.size());
            }
            
            report.put("summary", summary);
        }

        return report;
    }

    private void notifyDefectToProduction(DefectRecord defect) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("action", "DEFECT_NOTIFICATION");
        notification.put("defectId", defect.getDefectId());
        notification.put("productId", defect.getProductId());
        notification.put("batchId", defect.getBatchId());
        notification.put("defectType", defect.getDefectType());
        notification.put("severity", defect.getSeverity());

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("PlanificadorProduccion", AID.ISLOCALNAME));
        msg.setContent(JsonUtil.toJson(notification));
        msg.setOntology("smagesci-quality");
        send(msg);

        log.info("Notified production of defect {} in product {}", defect.getDefectId(), defect.getProductId());
    }

    private void notifyBatchStatus(String batchId, String productId, String status) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("action", "BATCH_STATUS_UPDATE");
        notification.put("batchId", batchId);
        notification.put("productId", productId);
        notification.put("qualityStatus", status);

        // Notify production
        ACLMessage prodMsg = new ACLMessage(ACLMessage.INFORM);
        prodMsg.addReceiver(new AID("PlanificadorProduccion", AID.ISLOCALNAME));
        prodMsg.setContent(JsonUtil.toJson(notification));
        prodMsg.setOntology("smagesci-quality");
        send(prodMsg);

        // Notify inventory
        ACLMessage invMsg = new ACLMessage(ACLMessage.INFORM);
        invMsg.addReceiver(new AID("GestorInventarioPT", AID.ISLOCALNAME));
        invMsg.setContent(JsonUtil.toJson(notification));
        invMsg.setOntology("smagesci-quality");
        send(invMsg);
    }

    private void sendErrorResponse(ACLMessage originalMsg, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("action", "ERROR");
        error.put("message", errorMessage);

        ACLMessage reply = originalMsg.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        reply.setContent(JsonUtil.toJson(error));
        send(reply);

        log.error("Quality control error: {}", errorMessage);
    }

    @Override
    protected void takeDown() {
        log.info("AgenteControlDeCalidad {} shutting down", getAID().getName());
        super.takeDown();
    }

    // Helper classes
    private static class QualityStandard {
        private String productId;
        private String category;
        private List<String> criteria;
        private double minimumScore;
        private String testMethods;

        public QualityStandard(String productId, String category, List<String> criteria, 
                              double minimumScore, String testMethods) {
            this.productId = productId;
            this.category = category;
            this.criteria = new ArrayList<>(criteria);
            this.minimumScore = minimumScore;
            this.testMethods = testMethods;
        }

        public void updateStandard(String category, List<String> criteria, Double minScore, String testMethods) {
            if (category != null) this.category = category;
            if (criteria != null) this.criteria = new ArrayList<>(criteria);
            if (minScore != null) this.minimumScore = minScore;
            if (testMethods != null) this.testMethods = testMethods;
        }

        // Getters
        public String getProductId() { return productId; }
        public String getCategory() { return category; }
        public List<String> getCriteria() { return criteria; }
        public double getMinimumScore() { return minimumScore; }
        public String getTestMethods() { return testMethods; }
    }

    private static class QualityInspection {
        private String inspectionId;
        private String productId;
        private String batchId;
        private String inspectionType;
        private String priority;
        private String status;
        private QualityStandard standard;
        private LocalDateTime scheduledTime;
        private LocalDateTime completedTime;
        private Map<String, Object> measurements;
        private QualityResult result;

        public QualityInspection(String inspectionId, String productId, String batchId, 
                               String inspectionType, String priority, QualityStandard standard) {
            this.inspectionId = inspectionId;
            this.productId = productId;
            this.batchId = batchId;
            this.inspectionType = inspectionType;
            this.priority = priority;
            this.standard = standard;
            this.status = "SCHEDULED";
            this.scheduledTime = LocalDateTime.now().plusMinutes("URGENT".equals(priority) ? 5 : 30);
            this.measurements = new HashMap<>();
        }

        public void recordMeasurements(Map<String, Object> measurements, String inspectorId) {
            this.measurements.putAll(measurements);
            this.measurements.put("inspectorId", inspectorId);
            this.measurements.put("recordedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        // Getters and setters
        public String getInspectionId() { return inspectionId; }
        public String getProductId() { return productId; }
        public String getBatchId() { return batchId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public QualityStandard getStandard() { return standard; }
        public LocalDateTime getScheduledTime() { return scheduledTime; }
        public Map<String, Object> getMeasurements() { return measurements; }
        public QualityResult getResult() { return result; }
        public void setResult(QualityResult result) { this.result = result; }
    }

    private static class QualityResult {
        private double overallScore;
        private boolean passed;
        private List<String> failedCriteria;
        private String comments;
        private Map<String, Object> measurements;

        public QualityResult(double overallScore, boolean passed, List<String> failedCriteria, 
                           String comments, Map<String, Object> measurements) {
            this.overallScore = overallScore;
            this.passed = passed;
            this.failedCriteria = new ArrayList<>(failedCriteria);
            this.comments = comments;
            this.measurements = new HashMap<>(measurements);
        }

        // Getters
        public double getOverallScore() { return overallScore; }
        public boolean isPassed() { return passed; }
        public List<String> getFailedCriteria() { return failedCriteria; }
        public String getComments() { return comments; }
        public Map<String, Object> getMeasurements() { return measurements; }
    }

    private static class QualityMetrics {
        private String productId;
        private double averageScore;
        private double passRate;
        private double defectRate;
        private int totalInspections;
        private int passedInspections;
        private int totalDefects;
        private LocalDateTime lastUpdated;

        public QualityMetrics(String productId) {
            this.productId = productId;
            this.averageScore = 0.0;
            this.passRate = 100.0;
            this.defectRate = 0.0;
            this.totalInspections = 0;
            this.passedInspections = 0;
            this.totalDefects = 0;
            this.lastUpdated = LocalDateTime.now();
        }

        public void recordInspection(double score, boolean passed) {
            // Update running averages
            averageScore = (averageScore * totalInspections + score) / (totalInspections + 1);
            totalInspections++;
            if (passed) passedInspections++;
            passRate = (double) passedInspections / totalInspections * 100;
            lastUpdated = LocalDateTime.now();
        }

        public void recordDefect(String severity) {
            totalDefects++;
            // Weight defects by severity
            int weight = "HIGH".equals(severity) ? 3 : "MEDIUM".equals(severity) ? 2 : 1;
            defectRate = (double) (totalDefects * weight) / Math.max(1, totalInspections) * 100;
            lastUpdated = LocalDateTime.now();
        }

        public void calculateTrends() {
            // Calculate quality trends (simplified)
            lastUpdated = LocalDateTime.now();
        }

        // Getters
        public String getProductId() { return productId; }
        public double getAverageScore() { return averageScore; }
        public double getPassRate() { return passRate; }
        public double getDefectRate() { return defectRate; }
        public int getTotalInspections() { return totalInspections; }
    }

    private static class QualityAlert {
        private String type;
        private String severity;
        private String message;
        private Map<String, Object> details;
        private LocalDateTime createdAt;
        private boolean active;

        public QualityAlert(String type, String severity, String message, Map<String, Object> details) {
            this.type = type;
            this.severity = severity;
            this.message = message;
            this.details = new HashMap<>(details);
            this.createdAt = LocalDateTime.now();
            this.active = true;
        }

        // Getters
        public String getType() { return type; }
        public String getSeverity() { return severity; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    private static class DefectRecord {
        private String defectId;
        private String productId;
        private String batchId;
        private String defectType;
        private String severity;
        private String description;
        private String reportedBy;
        private LocalDateTime reportedAt;

        public DefectRecord(String defectId, String productId, String batchId, 
                          String defectType, String severity, String description, String reportedBy) {
            this.defectId = defectId;
            this.productId = productId;
            this.batchId = batchId;
            this.defectType = defectType;
            this.severity = severity;
            this.description = description;
            this.reportedBy = reportedBy;
            this.reportedAt = LocalDateTime.now();
        }

        // Getters
        public String getDefectId() { return defectId; }
        public String getProductId() { return productId; }
        public String getBatchId() { return batchId; }
        public String getDefectType() { return defectType; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getReportedBy() { return reportedBy; }
        public LocalDateTime getReportedAt() { return reportedAt; }
    }

    private static class BatchApproval {
        private String batchId;
        private String productId;
        private String approvedBy;
        private String status;
        private String reason;
        private LocalDateTime timestamp;

        public BatchApproval(String batchId, String productId, String approvedBy, String status) {
            this(batchId, productId, approvedBy, status, null);
        }

        public BatchApproval(String batchId, String productId, String approvedBy, String status, String reason) {
            this.batchId = batchId;
            this.productId = productId;
            this.approvedBy = approvedBy;
            this.status = status;
            this.reason = reason;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getBatchId() { return batchId; }
        public String getProductId() { return productId; }
        public String getApprovedBy() { return approvedBy; }
        public String getStatus() { return status; }
        public String getReason() { return reason; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
