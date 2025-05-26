package com.smagesci.agents.analytics;

import com.smagesci.agents.BaseAgent;
import com.smagesci.utils.JsonUtil;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agente responsable del análisis de datos y inteligencia de negocios.
 * Genera reportes, métricas KPI y análisis predictivos para la cadena de suministro.
 */
public class AgenteAnalisisDatos extends BaseAgent {

    private Map<String, KPI> kpiMetrics;
    private List<DataPoint> historicalData;
    private Map<String, Dashboard> dashboards;
    private Map<String, Report> savedReports;
    private List<Alert> analyticsAlerts;
    private Map<String, PredictionModel> predictionModels;
    private int reportCounter;

    @Override
    protected void setup() {
        super.setup();
        kpiMetrics = initializeKPIs();
        historicalData = new ArrayList<>();
        dashboards = initializeDashboards();
        savedReports = new HashMap<>();
        analyticsAlerts = new ArrayList<>();
        predictionModels = initializePredictionModels();
        reportCounter = 1;

        log.info("AgenteAnalisisDatos {} is ready with {} KPIs and {} dashboards", 
                getAID().getName(), kpiMetrics.size(), dashboards.size());
        registerService("data-analytics", "smagesci-data-analyst");

        addBehaviour(new DataAnalyticsBehaviour());
        addBehaviour(new MetricsCollectionBehaviour(this, 60000)); // Collect metrics every minute
    }

    private Map<String, KPI> initializeKPIs() {
        Map<String, KPI> kpis = new HashMap<>();
        
        kpis.put("ORDER_FULFILLMENT_RATE", new KPI("ORDER_FULFILLMENT_RATE", 
                "Order Fulfillment Rate", "Percentage of orders fulfilled on time", 
                "PERCENTAGE", 95.0, 90.0, 98.0));
        
        kpis.put("INVENTORY_TURNOVER", new KPI("INVENTORY_TURNOVER", 
                "Inventory Turnover", "How often inventory is sold per period", 
                "RATIO", 12.0, 8.0, 15.0));
        
        kpis.put("SUPPLY_CHAIN_COST", new KPI("SUPPLY_CHAIN_COST", 
                "Supply Chain Cost Ratio", "Supply chain costs as % of revenue", 
                "PERCENTAGE", 15.0, 20.0, 12.0));
        
        kpis.put("QUALITY_SCORE", new KPI("QUALITY_SCORE", 
                "Overall Quality Score", "Average quality score across products", 
                "SCORE", 95.0, 90.0, 98.0));
        
        kpis.put("SUPPLIER_PERFORMANCE", new KPI("SUPPLIER_PERFORMANCE", 
                "Supplier Performance Index", "Weighted average of supplier ratings", 
                "SCORE", 85.0, 75.0, 95.0));
        
        kpis.put("DELIVERY_PERFORMANCE", new KPI("DELIVERY_PERFORMANCE", 
                "On-Time Delivery Rate", "Percentage of deliveries made on time", 
                "PERCENTAGE", 92.0, 85.0, 98.0));
        
        kpis.put("COST_PER_ORDER", new KPI("COST_PER_ORDER", 
                "Average Cost per Order", "Average processing cost per order", 
                "CURRENCY", 45.0, 60.0, 35.0));
        
        kpis.put("CUSTOMER_SATISFACTION", new KPI("CUSTOMER_SATISFACTION", 
                "Customer Satisfaction Score", "Average customer satisfaction rating", 
                "SCORE", 4.2, 3.5, 4.8));

        return kpis;
    }

    private Map<String, Dashboard> initializeDashboards() {
        Map<String, Dashboard> dashboards = new HashMap<>();
        
        dashboards.put("OPERATIONS", new Dashboard("OPERATIONS", "Operations Dashboard",
                Arrays.asList("ORDER_FULFILLMENT_RATE", "DELIVERY_PERFORMANCE", "COST_PER_ORDER")));
        
        dashboards.put("FINANCIAL", new Dashboard("FINANCIAL", "Financial Dashboard",
                Arrays.asList("SUPPLY_CHAIN_COST", "COST_PER_ORDER")));
        
        dashboards.put("QUALITY", new Dashboard("QUALITY", "Quality Dashboard",
                Arrays.asList("QUALITY_SCORE", "CUSTOMER_SATISFACTION")));
        
        dashboards.put("SUPPLIERS", new Dashboard("SUPPLIERS", "Supplier Dashboard",
                Arrays.asList("SUPPLIER_PERFORMANCE", "DELIVERY_PERFORMANCE")));

        return dashboards;
    }

    private Map<String, PredictionModel> initializePredictionModels() {
        Map<String, PredictionModel> models = new HashMap<>();
        
        models.put("DEMAND_FORECAST", new PredictionModel("DEMAND_FORECAST", 
                "Demand Forecasting", "Predicts future demand based on historical data"));
        
        models.put("INVENTORY_OPTIMIZATION", new PredictionModel("INVENTORY_OPTIMIZATION", 
                "Inventory Optimization", "Optimizes inventory levels"));
        
        models.put("PRICE_PREDICTION", new PredictionModel("PRICE_PREDICTION", 
                "Price Prediction", "Predicts future material prices"));
        
        models.put("QUALITY_PREDICTION", new PredictionModel("QUALITY_PREDICTION", 
                "Quality Prediction", "Predicts quality issues"));

        return models;
    }

    private class DataAnalyticsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    String content = msg.getContent();
                    Map<String, Object> data = JsonUtil.fromJson(content, Map.class);
                    String action = (String) data.get("action");

                    switch (action) {
                        case "GENERATE_REPORT":
                            handleGenerateReport(msg, data);
                            break;
                        case "GET_KPI_STATUS":
                            handleGetKPIStatus(msg, data);
                            break;
                        case "UPDATE_METRICS":
                            handleUpdateMetrics(msg, data);
                            break;
                        case "GET_DASHBOARD":
                            handleGetDashboard(msg, data);
                            break;
                        case "ANALYZE_TREND":
                            handleAnalyzeTrend(msg, data);
                            break;
                        case "PREDICT_DEMAND":
                            handlePredictDemand(msg, data);
                            break;
                        case "OPTIMIZE_INVENTORY":
                            handleOptimizeInventory(msg, data);
                            break;
                        case "ANOMALY_DETECTION":
                            handleAnomalyDetection(msg, data);
                            break;
                        case "GET_INSIGHTS":
                            handleGetInsights(msg, data);
                            break;
                        case "COMPARE_PERIODS":
                            handleComparePeriods(msg, data);
                            break;
                        default:
                            log.warn("Unknown action received: {}", action);
                            sendErrorResponse(msg, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    log.error("Error processing analytics message", e);
                    sendErrorResponse(msg, "Error processing request: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private class MetricsCollectionBehaviour extends TickerBehaviour {
        public MetricsCollectionBehaviour(jade.core.Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            collectRealTimeMetrics();
            updateKPIs();
            detectAnomalies();
            generateAlerts();
        }
    }

    private void handleGenerateReport(ACLMessage msg, Map<String, Object> data) {
        try {
            String reportType = (String) data.get("reportType");
            String timeframe = (String) data.getOrDefault("timeframe", "LAST_30_DAYS");
            List<String> metrics = (List<String>) data.get("metrics");
            String format = (String) data.getOrDefault("format", "JSON");

            if (reportType == null) {
                sendErrorResponse(msg, "Missing reportType");
                return;
            }

            String reportId = "RPT" + String.format("%06d", reportCounter++);
            Report report = generateReport(reportId, reportType, timeframe, metrics, format);
            savedReports.put(reportId, report);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "REPORT_GENERATED");
            response.put("reportId", reportId);
            response.put("reportType", reportType);
            response.put("timeframe", timeframe);
            response.put("generatedAt", report.getGeneratedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("data", report.getData());
            response.put("summary", report.getSummary());

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Generated {} report {} covering {}", reportType, reportId, timeframe);

        } catch (Exception e) {
            log.error("Error generating report", e);
            sendErrorResponse(msg, "Error generating report: " + e.getMessage());
        }
    }

    private void handleGetKPIStatus(ACLMessage msg, Map<String, Object> data) {
        String kpiId = (String) data.get("kpiId");
        String category = (String) data.get("category");

        Map<String, Object> response = new HashMap<>();
        response.put("action", "KPI_STATUS");

        if (kpiId != null) {
            KPI kpi = kpiMetrics.get(kpiId);
            if (kpi != null) {
                response.put("kpi", createKPIResponse(kpi));
            } else {
                response.put("error", "KPI not found: " + kpiId);
            }
        } else {
            // Return all KPIs or by category
            Map<String, Object> allKPIs = new HashMap<>();
            for (Map.Entry<String, KPI> entry : kpiMetrics.entrySet()) {
                allKPIs.put(entry.getKey(), createKPIResponse(entry.getValue()));
            }
            response.put("kpis", allKPIs);
        }

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void handleUpdateMetrics(ACLMessage msg, Map<String, Object> data) {
        try {
            String metricName = (String) data.get("metricName");
            Double value = (Double) data.get("value");
            String source = (String) data.getOrDefault("source", "SYSTEM");

            if (metricName == null || value == null) {
                sendErrorResponse(msg, "Missing metricName or value");
                return;
            }

            // Add data point to historical data
            DataPoint dataPoint = new DataPoint(metricName, value, source);
            historicalData.add(dataPoint);

            // Update KPI if exists
            KPI kpi = kpiMetrics.get(metricName);
            if (kpi != null) {
                kpi.updateValue(value);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("action", "METRICS_UPDATED");
            response.put("metricName", metricName);
            response.put("value", value);
            response.put("status", kpi != null ? kpi.getStatus() : "RECORDED");

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Updated metric {}: {} ({})", metricName, value, kpi != null ? kpi.getStatus() : "NEW");

        } catch (Exception e) {
            log.error("Error updating metrics", e);
            sendErrorResponse(msg, "Error updating metrics: " + e.getMessage());
        }
    }

    private void handleGetDashboard(ACLMessage msg, Map<String, Object> data) {
        String dashboardId = (String) data.get("dashboardId");

        if (dashboardId == null) {
            sendErrorResponse(msg, "Missing dashboardId");
            return;
        }

        Dashboard dashboard = dashboards.get(dashboardId);
        if (dashboard == null) {
            sendErrorResponse(msg, "Dashboard not found: " + dashboardId);
            return;
        }

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("action", "DASHBOARD_DATA");
        dashboardData.put("dashboardId", dashboardId);
        dashboardData.put("name", dashboard.getName());
        dashboardData.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> kpiData = new HashMap<>();
        for (String kpiId : dashboard.getKpiIds()) {
            KPI kpi = kpiMetrics.get(kpiId);
            if (kpi != null) {
                kpiData.put(kpiId, createKPIResponse(kpi));
            }
        }
        dashboardData.put("kpis", kpiData);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(dashboardData));
        send(reply);

        log.info("Provided dashboard data for {}", dashboardId);
    }

    private void handleAnalyzeTrend(ACLMessage msg, Map<String, Object> data) {
        String metricName = (String) data.get("metricName");
        String timeframe = (String) data.getOrDefault("timeframe", "LAST_30_DAYS");
        String analysisType = (String) data.getOrDefault("analysisType", "LINEAR");

        if (metricName == null) {
            sendErrorResponse(msg, "Missing metricName");
            return;
        }

        TrendAnalysis trend = analyzeTrend(metricName, timeframe, analysisType);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "TREND_ANALYSIS");
        response.put("metricName", metricName);
        response.put("timeframe", timeframe);
        response.put("direction", trend.getDirection());
        response.put("strength", trend.getStrength());
        response.put("slope", trend.getSlope());
        response.put("correlation", trend.getCorrelation());
        response.put("forecast", trend.getForecast());
        response.put("confidence", trend.getConfidence());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Analyzed trend for {} over {}: {} trend with {} strength", 
                metricName, timeframe, trend.getDirection(), trend.getStrength());
    }

    private void handlePredictDemand(ACLMessage msg, Map<String, Object> data) {
        String productId = (String) data.get("productId");
        Integer forecastDays = (Integer) data.getOrDefault("forecastDays", 30);
        String model = (String) data.getOrDefault("model", "ARIMA");

        if (productId == null) {
            sendErrorResponse(msg, "Missing productId");
            return;
        }

        DemandForecast forecast = predictDemand(productId, forecastDays, model);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "DEMAND_PREDICTION");
        response.put("productId", productId);
        response.put("forecastDays", forecastDays);
        response.put("model", model);
        response.put("predictions", forecast.getPredictions());
        response.put("confidence", forecast.getConfidence());
        response.put("seasonality", forecast.getSeasonality());
        response.put("accuracy", forecast.getAccuracy());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Generated demand forecast for {} ({} days, {} confidence)", 
                productId, forecastDays, forecast.getConfidence());
    }

    private void handleOptimizeInventory(ACLMessage msg, Map<String, Object> data) {
        String warehouseId = (String) data.get("warehouseId");
        String optimizationGoal = (String) data.getOrDefault("optimizationGoal", "MINIMIZE_COST");

        InventoryOptimization optimization = optimizeInventory(warehouseId, optimizationGoal);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "INVENTORY_OPTIMIZATION");
        response.put("warehouseId", warehouseId);
        response.put("optimizationGoal", optimizationGoal);
        response.put("recommendations", optimization.getRecommendations());
        response.put("potentialSavings", optimization.getPotentialSavings());
        response.put("riskLevel", optimization.getRiskLevel());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Generated inventory optimization for {} (goal: {}, savings: ${})", 
                warehouseId, optimizationGoal, optimization.getPotentialSavings());
    }

    private void handleAnomalyDetection(ACLMessage msg, Map<String, Object> data) {
        String metricName = (String) data.get("metricName");
        String algorithm = (String) data.getOrDefault("algorithm", "STATISTICAL");
        Double sensitivity = (Double) data.getOrDefault("sensitivity", 0.95);

        List<Anomaly> anomalies = detectAnomalies(metricName, algorithm, sensitivity);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "ANOMALY_DETECTION_RESULT");
        response.put("metricName", metricName);
        response.put("algorithm", algorithm);
        response.put("sensitivity", sensitivity);
        response.put("anomaliesFound", anomalies.size());
        response.put("anomalies", anomalies.stream().map(this::createAnomalyResponse).collect(Collectors.toList()));

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Detected {} anomalies for {} using {} algorithm", 
                anomalies.size(), metricName, algorithm);
    }

    private void handleGetInsights(ACLMessage msg, Map<String, Object> data) {
        String category = (String) data.getOrDefault("category", "ALL");
        String timeframe = (String) data.getOrDefault("timeframe", "LAST_7_DAYS");

        List<BusinessInsight> insights = generateInsights(category, timeframe);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "BUSINESS_INSIGHTS");
        response.put("category", category);
        response.put("timeframe", timeframe);
        response.put("insightsCount", insights.size());
        response.put("insights", insights.stream().map(this::createInsightResponse).collect(Collectors.toList()));

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Generated {} business insights for category {} ({})", 
                insights.size(), category, timeframe);
    }

    private void handleComparePeriods(ACLMessage msg, Map<String, Object> data) {
        String metric = (String) data.get("metric");
        String period1 = (String) data.get("period1");
        String period2 = (String) data.get("period2");

        if (metric == null || period1 == null || period2 == null) {
            sendErrorResponse(msg, "Missing comparison parameters");
            return;
        }

        PeriodComparison comparison = comparePeriods(metric, period1, period2);

        Map<String, Object> response = new HashMap<>();
        response.put("action", "PERIOD_COMPARISON");
        response.put("metric", metric);
        response.put("period1", period1);
        response.put("period2", period2);
        response.put("value1", comparison.getValue1());
        response.put("value2", comparison.getValue2());
        response.put("difference", comparison.getDifference());
        response.put("percentageChange", comparison.getPercentageChange());
        response.put("direction", comparison.getDirection());
        response.put("significance", comparison.getSignificance());

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Compared {} between {} and {}: {}% change", 
                metric, period1, period2, comparison.getPercentageChange());
    }

    // Helper methods
    private Report generateReport(String reportId, String reportType, String timeframe, 
                                List<String> metrics, String format) {
        Map<String, Object> reportData = new HashMap<>();
        Map<String, String> summary = new HashMap<>();

        // Generate report based on type
        switch (reportType) {
            case "KPI_SUMMARY":
                reportData = generateKPISummaryReport(timeframe, metrics);
                summary.put("description", "Summary of key performance indicators");
                break;
            case "OPERATIONAL":
                reportData = generateOperationalReport(timeframe);
                summary.put("description", "Operational performance metrics");
                break;
            case "FINANCIAL":
                reportData = generateFinancialReport(timeframe);
                summary.put("description", "Financial performance analysis");
                break;
            case "QUALITY":
                reportData = generateQualityReport(timeframe);
                summary.put("description", "Quality metrics and trends");
                break;
            default:
                reportData.put("error", "Unknown report type");
                summary.put("description", "Error generating report");
        }

        return new Report(reportId, reportType, timeframe, format, reportData, summary);
    }

    private Map<String, Object> generateKPISummaryReport(String timeframe, List<String> metrics) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> kpiSummary = new HashMap<>();

        for (Map.Entry<String, KPI> entry : kpiMetrics.entrySet()) {
            if (metrics == null || metrics.contains(entry.getKey())) {
                kpiSummary.put(entry.getKey(), createKPIResponse(entry.getValue()));
            }
        }

        data.put("kpis", kpiSummary);
        data.put("overallStatus", calculateOverallStatus());
        data.put("criticalKPIs", getCriticalKPIs());

        return data;
    }

    private Map<String, Object> generateOperationalReport(String timeframe) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("orderFulfillmentRate", 95.2);
        data.put("avgProcessingTime", 2.8);
        data.put("deliveryPerformance", 92.1);
        data.put("resourceUtilization", 78.5);
        data.put("throughput", 1250);
        data.put("efficiency", 87.3);

        return data;
    }

    private Map<String, Object> generateFinancialReport(String timeframe) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("totalRevenue", 2450000.0);
        data.put("operatingCosts", 1835000.0);
        data.put("netProfit", 615000.0);
        data.put("profitMargin", 25.1);
        data.put("costPerOrder", 45.75);
        data.put("roi", 33.5);

        return data;
    }

    private Map<String, Object> generateQualityReport(String timeframe) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("overallQualityScore", 94.8);
        data.put("defectRate", 1.2);
        data.put("customerSatisfaction", 4.3);
        data.put("qualityIncidents", 8);
        data.put("passRate", 98.8);
        data.put("improvementTrend", "POSITIVE");

        return data;
    }

    private Map<String, Object> createKPIResponse(KPI kpi) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", kpi.getId());
        response.put("name", kpi.getName());
        response.put("description", kpi.getDescription());
        response.put("currentValue", kpi.getCurrentValue());
        response.put("targetValue", kpi.getTargetValue());
        response.put("status", kpi.getStatus());
        response.put("trend", kpi.getTrend());
        response.put("lastUpdated", kpi.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }

    private TrendAnalysis analyzeTrend(String metricName, String timeframe, String analysisType) {
        // Simplified trend analysis
        List<DataPoint> relevantData = historicalData.stream()
                .filter(dp -> dp.getMetricName().equals(metricName))
                .collect(Collectors.toList());

        Random random = new Random();
        String direction = random.nextBoolean() ? "UPWARD" : "DOWNWARD";
        String strength = random.nextDouble() > 0.5 ? "STRONG" : "WEAK";
        double slope = random.nextGaussian() * 2.0;
        double correlation = 0.7 + random.nextDouble() * 0.3;
        List<Double> forecast = Arrays.asList(120.0, 125.0, 130.0, 128.0, 135.0);
        double confidence = 0.8 + random.nextDouble() * 0.15;

        return new TrendAnalysis(direction, strength, slope, correlation, forecast, confidence);
    }

    private DemandForecast predictDemand(String productId, int forecastDays, String model) {
        // Simplified demand prediction
        Random random = new Random();
        List<Double> predictions = new ArrayList<>();
        
        double baseValue = 100.0;
        for (int i = 0; i < forecastDays; i++) {
            double seasonality = Math.sin(2 * Math.PI * i / 7) * 10; // Weekly pattern
            double trend = i * 0.5; // Slight upward trend
            double noise = random.nextGaussian() * 5;
            predictions.add(baseValue + seasonality + trend + noise);
        }

        return new DemandForecast(predictions, 0.85, "WEEKLY", 0.92);
    }

    private InventoryOptimization optimizeInventory(String warehouseId, String optimizationGoal) {
        // Simplified inventory optimization
        List<String> recommendations = Arrays.asList(
                "Reduce safety stock for Product A by 15%",
                "Increase reorder point for Product B",
                "Implement just-in-time for Category C",
                "Consolidate slow-moving inventory"
        );

        return new InventoryOptimization(recommendations, 25000.0, "LOW");
    }

    private List<Anomaly> detectAnomalies(String metricName, String algorithm, double sensitivity) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        // Simulate anomaly detection
        Random random = new Random();
        if (random.nextDouble() < 0.3) { // 30% chance of anomaly
            anomalies.add(new Anomaly(metricName, LocalDateTime.now().minusHours(2), 
                    "OUTLIER", "Value significantly higher than expected", 0.95));
        }

        return anomalies;
    }

    private List<BusinessInsight> generateInsights(String category, String timeframe) {
        List<BusinessInsight> insights = new ArrayList<>();
        
        insights.add(new BusinessInsight("COST_OPTIMIZATION", 
                "High Priority", "Transportation costs increased 15% this month",
                "Consider optimizing delivery routes", LocalDateTime.now()));
        
        insights.add(new BusinessInsight("DEMAND_PATTERN", 
                "Medium Priority", "Product A shows strong seasonal trend",
                "Adjust inventory planning for upcoming season", LocalDateTime.now()));
        
        insights.add(new BusinessInsight("SUPPLIER_PERFORMANCE", 
                "Low Priority", "Supplier X has improved delivery times",
                "Consider increasing order volume", LocalDateTime.now()));

        return insights;
    }

    private PeriodComparison comparePeriods(String metric, String period1, String period2) {
        // Simplified period comparison
        Random random = new Random();
        double value1 = 100.0 + random.nextGaussian() * 10;
        double value2 = 105.0 + random.nextGaussian() * 10;
        double difference = value2 - value1;
        double percentageChange = (difference / value1) * 100;
        String direction = difference > 0 ? "INCREASE" : "DECREASE";
        String significance = Math.abs(percentageChange) > 5 ? "SIGNIFICANT" : "MINOR";

        return new PeriodComparison(value1, value2, difference, percentageChange, direction, significance);
    }

    private Map<String, Object> createAnomalyResponse(Anomaly anomaly) {
        Map<String, Object> response = new HashMap<>();
        response.put("metricName", anomaly.getMetricName());
        response.put("timestamp", anomaly.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("type", anomaly.getType());
        response.put("description", anomaly.getDescription());
        response.put("confidence", anomaly.getConfidence());
        return response;
    }

    private Map<String, Object> createInsightResponse(BusinessInsight insight) {
        Map<String, Object> response = new HashMap<>();
        response.put("category", insight.getCategory());
        response.put("priority", insight.getPriority());
        response.put("observation", insight.getObservation());
        response.put("recommendation", insight.getRecommendation());
        response.put("timestamp", insight.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }

    private void collectRealTimeMetrics() {
        // Simulate real-time metric collection
        Random random = new Random();
        
        for (KPI kpi : kpiMetrics.values()) {
            double variation = random.nextGaussian() * 2.0;
            double newValue = kpi.getCurrentValue() + variation;
            kpi.updateValue(Math.max(0, newValue));
        }
    }

    private void updateKPIs() {
        // Update KPI trends and statuses
        for (KPI kpi : kpiMetrics.values()) {
            kpi.updateTrend();
        }
    }

    private void detectAnomalies() {
        // Detect anomalies in real-time data
        for (KPI kpi : kpiMetrics.values()) {
            if ("CRITICAL".equals(kpi.getStatus())) {
                Alert alert = new Alert("ANOMALY", "KPI_CRITICAL", 
                        kpi.getName() + " is in critical state", "HIGH");
                analyticsAlerts.add(alert);
                log.warn("Anomaly detected: {} is critical", kpi.getName());
            }
        }
    }

    private void generateAlerts() {
        // Generate alerts based on analytics
        analyticsAlerts.removeIf(alert -> 
                alert.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24)));
    }

    private String calculateOverallStatus() {
        long criticalCount = kpiMetrics.values().stream()
                .mapToLong(kpi -> "CRITICAL".equals(kpi.getStatus()) ? 1 : 0)
                .sum();
        
        if (criticalCount > 2) return "CRITICAL";
        if (criticalCount > 0) return "WARNING";
        return "GOOD";
    }

    private List<String> getCriticalKPIs() {
        return kpiMetrics.entrySet().stream()
                .filter(entry -> "CRITICAL".equals(entry.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void sendErrorResponse(ACLMessage originalMsg, String errorMessage) {
        ACLMessage reply = originalMsg.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        Map<String, Object> error = Map.of("error", errorMessage);
        reply.setContent(JsonUtil.toJson(error));
        send(reply);
    }

    @Override
    protected void takeDown() {
        log.info("AgenteAnalisisDatos {} shutting down", getAID().getName());
        super.takeDown();
    }

    // Helper classes
    private static class KPI {
        private String id;
        private String name;
        private String description;
        private String unit;
        private double currentValue;
        private double targetValue;
        private double thresholdMin;
        private double thresholdMax;
        private LocalDateTime lastUpdated;
        private String trend;

        public KPI(String id, String name, String description, String unit, 
                  double currentValue, double thresholdMin, double thresholdMax) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.unit = unit;
            this.currentValue = currentValue;
            this.targetValue = thresholdMax;
            this.thresholdMin = thresholdMin;
            this.thresholdMax = thresholdMax;
            this.lastUpdated = LocalDateTime.now();
            this.trend = "STABLE";
        }

        public void updateValue(double newValue) {
            this.currentValue = newValue;
            this.lastUpdated = LocalDateTime.now();
        }

        public void updateTrend() {
            // Simplified trend calculation
            Random random = new Random();
            double r = random.nextDouble();
            if (r < 0.33) this.trend = "UPWARD";
            else if (r < 0.66) this.trend = "DOWNWARD";
            else this.trend = "STABLE";
        }

        public String getStatus() {
            if (currentValue < thresholdMin) return "CRITICAL";
            if (currentValue > thresholdMax) return "EXCELLENT";
            if (currentValue < thresholdMin * 1.1) return "WARNING";
            return "GOOD";
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getUnit() { return unit; }
        public double getCurrentValue() { return currentValue; }
        public double getTargetValue() { return targetValue; }
        public double getThresholdMin() { return thresholdMin; }
        public double getThresholdMax() { return thresholdMax; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public String getTrend() { return trend; }
    }

    private static class DataPoint {
        private String metricName;
        private double value;
        private String source;
        private LocalDateTime timestamp;

        public DataPoint(String metricName, double value, String source) {
            this.metricName = metricName;
            this.value = value;
            this.source = source;
            this.timestamp = LocalDateTime.now();
        }

        public String getMetricName() { return metricName; }
        public double getValue() { return value; }
        public String getSource() { return source; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    private static class Dashboard {
        private String id;
        private String name;
        private List<String> kpiIds;

        public Dashboard(String id, String name, List<String> kpiIds) {
            this.id = id;
            this.name = name;
            this.kpiIds = new ArrayList<>(kpiIds);
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public List<String> getKpiIds() { return kpiIds; }
    }

    private static class Report {
        private String reportId;
        private String reportType;
        private String timeframe;
        private String format;
        private Map<String, Object> data;
        private Map<String, String> summary;
        private LocalDateTime generatedAt;

        public Report(String reportId, String reportType, String timeframe, String format,
                     Map<String, Object> data, Map<String, String> summary) {
            this.reportId = reportId;
            this.reportType = reportType;
            this.timeframe = timeframe;
            this.format = format;
            this.data = new HashMap<>(data);
            this.summary = new HashMap<>(summary);
            this.generatedAt = LocalDateTime.now();
        }

        public String getReportId() { return reportId; }
        public String getReportType() { return reportType; }
        public String getTimeframe() { return timeframe; }
        public String getFormat() { return format; }
        public Map<String, Object> getData() { return data; }
        public Map<String, String> getSummary() { return summary; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
    }

    private static class TrendAnalysis {
        private String direction;
        private String strength;
        private double slope;
        private double correlation;
        private List<Double> forecast;
        private double confidence;

        public TrendAnalysis(String direction, String strength, double slope, double correlation,
                           List<Double> forecast, double confidence) {
            this.direction = direction;
            this.strength = strength;
            this.slope = slope;
            this.correlation = correlation;
            this.forecast = new ArrayList<>(forecast);
            this.confidence = confidence;
        }

        public String getDirection() { return direction; }
        public String getStrength() { return strength; }
        public double getSlope() { return slope; }
        public double getCorrelation() { return correlation; }
        public List<Double> getForecast() { return forecast; }
        public double getConfidence() { return confidence; }
    }

    private static class DemandForecast {
        private List<Double> predictions;
        private double confidence;
        private String seasonality;
        private double accuracy;

        public DemandForecast(List<Double> predictions, double confidence, String seasonality, double accuracy) {
            this.predictions = new ArrayList<>(predictions);
            this.confidence = confidence;
            this.seasonality = seasonality;
            this.accuracy = accuracy;
        }

        public List<Double> getPredictions() { return predictions; }
        public double getConfidence() { return confidence; }
        public String getSeasonality() { return seasonality; }
        public double getAccuracy() { return accuracy; }
    }

    private static class InventoryOptimization {
        private List<String> recommendations;
        private double potentialSavings;
        private String riskLevel;

        public InventoryOptimization(List<String> recommendations, double potentialSavings, String riskLevel) {
            this.recommendations = new ArrayList<>(recommendations);
            this.potentialSavings = potentialSavings;
            this.riskLevel = riskLevel;
        }

        public List<String> getRecommendations() { return recommendations; }
        public double getPotentialSavings() { return potentialSavings; }
        public String getRiskLevel() { return riskLevel; }
    }

    private static class Anomaly {
        private String metricName;
        private LocalDateTime timestamp;
        private String type;
        private String description;
        private double confidence;

        public Anomaly(String metricName, LocalDateTime timestamp, String type, String description, double confidence) {
            this.metricName = metricName;
            this.timestamp = timestamp;
            this.type = type;
            this.description = description;
            this.confidence = confidence;
        }

        public String getMetricName() { return metricName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public double getConfidence() { return confidence; }
    }

    private static class BusinessInsight {
        private String category;
        private String priority;
        private String observation;
        private String recommendation;
        private LocalDateTime timestamp;

        public BusinessInsight(String category, String priority, String observation, String recommendation, LocalDateTime timestamp) {
            this.category = category;
            this.priority = priority;
            this.observation = observation;
            this.recommendation = recommendation;
            this.timestamp = timestamp;
        }

        public String getCategory() { return category; }
        public String getPriority() { return priority; }
        public String getObservation() { return observation; }
        public String getRecommendation() { return recommendation; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    private static class PeriodComparison {
        private double value1;
        private double value2;
        private double difference;
        private double percentageChange;
        private String direction;
        private String significance;

        public PeriodComparison(double value1, double value2, double difference, double percentageChange, String direction, String significance) {
            this.value1 = value1;
            this.value2 = value2;
            this.difference = difference;
            this.percentageChange = percentageChange;
            this.direction = direction;
            this.significance = significance;
        }

        public double getValue1() { return value1; }
        public double getValue2() { return value2; }
        public double getDifference() { return difference; }
        public double getPercentageChange() { return percentageChange; }
        public String getDirection() { return direction; }
        public String getSignificance() { return significance; }
    }

    private static class PredictionModel {
        private String id;
        private String name;
        private String description;

        public PredictionModel(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    private static class Alert {
        private String type;
        private String category;
        private String message;
        private String severity;
        private LocalDateTime createdAt;

        public Alert(String type, String category, String message, String severity) {
            this.type = type;
            this.category = category;
            this.message = message;
            this.severity = severity;
            this.createdAt = LocalDateTime.now();
        }

        public String getType() { return type; }
        public String getCategory() { return category; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
