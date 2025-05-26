package com.smagesci.agents.finance;

import com.smagesci.agents.BaseAgent;
import com.smagesci.utils.JsonUtil;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Agente responsable de la gestión financiera de la cadena de suministro.
 * Maneja presupuestos, costos, facturación y análisis de rentabilidad.
 */
public class AgenteGestorFinanciero extends BaseAgent {

    private Map<String, Budget> departmentBudgets;
    private Map<String, Invoice> pendingInvoices;
    private Map<String, Payment> paymentRecords;
    private Map<String, CostCenter> costCenters;
    private List<FinancialTransaction> transactions;
    private Map<String, Double> exchangeRates;
    private int invoiceCounter;
    private int paymentCounter;

    @Override
    protected void setup() {
        super.setup();
        departmentBudgets = initializeBudgets();
        pendingInvoices = new HashMap<>();
        paymentRecords = new HashMap<>();
        costCenters = initializeCostCenters();
        transactions = new ArrayList<>();
        exchangeRates = initializeExchangeRates();
        invoiceCounter = 1;
        paymentCounter = 1;

        log.info("AgenteGestorFinanciero {} is ready with {} budgets and {} cost centers", 
                getAID().getName(), departmentBudgets.size(), costCenters.size());
        registerService("financial-management", "smagesci-financial-manager");

        addBehaviour(new FinancialManagementBehaviour());
        addBehaviour(new FinancialReportingBehaviour(this, 86400000)); // Daily reports
    }

    private Map<String, Budget> initializeBudgets() {
        Map<String, Budget> budgets = new HashMap<>();
        
        budgets.put("PROCUREMENT", new Budget("PROCUREMENT", "Procurement Department", 
                1000000.0, 250000.0, "Q1-2024"));
        budgets.put("PRODUCTION", new Budget("PRODUCTION", "Production Department", 
                2000000.0, 500000.0, "Q1-2024"));
        budgets.put("LOGISTICS", new Budget("LOGISTICS", "Logistics Department", 
                800000.0, 200000.0, "Q1-2024"));
        budgets.put("QUALITY", new Budget("QUALITY", "Quality Assurance", 
                300000.0, 75000.0, "Q1-2024"));
        budgets.put("MAINTENANCE", new Budget("MAINTENANCE", "Maintenance Department", 
                400000.0, 100000.0, "Q1-2024"));
        
        return budgets;
    }

    private Map<String, CostCenter> initializeCostCenters() {
        Map<String, CostCenter> centers = new HashMap<>();
        
        centers.put("CC001", new CostCenter("CC001", "Raw Materials", "PROCUREMENT"));
        centers.put("CC002", new CostCenter("CC002", "Manufacturing", "PRODUCTION"));
        centers.put("CC003", new CostCenter("CC003", "Warehousing", "LOGISTICS"));
        centers.put("CC004", new CostCenter("CC004", "Transportation", "LOGISTICS"));
        centers.put("CC005", new CostCenter("CC005", "Quality Testing", "QUALITY"));
        centers.put("CC006", new CostCenter("CC006", "Equipment Maintenance", "MAINTENANCE"));
        
        return centers;
    }

    private Map<String, Double> initializeExchangeRates() {
        Map<String, Double> rates = new HashMap<>();
        rates.put("USD", 1.0);
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.73);
        rates.put("CAD", 1.35);
        rates.put("MXN", 17.5);
        return rates;
    }

    private class FinancialManagementBehaviour extends CyclicBehaviour {
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
                        case "CREATE_INVOICE":
                            handleCreateInvoice(msg, data);
                            break;
                        case "PROCESS_PAYMENT":
                            handleProcessPayment(msg, data);
                            break;
                        case "CHECK_BUDGET":
                            handleCheckBudget(msg, data);
                            break;
                        case "RECORD_EXPENSE":
                            handleRecordExpense(msg, data);
                            break;
                        case "GET_FINANCIAL_REPORT":
                            handleGetFinancialReport(msg, data);
                            break;
                        case "APPROVE_EXPENDITURE":
                            handleApproveExpenditure(msg, data);
                            break;
                        case "CALCULATE_PROFITABILITY":
                            handleCalculateProfitability(msg, data);
                            break;
                        case "UPDATE_EXCHANGE_RATES":
                            handleUpdateExchangeRates(msg, data);
                            break;
                        case "GET_CASH_FLOW":
                            handleGetCashFlow(msg, data);
                            break;
                        default:
                            log.warn("Unknown action received: {}", action);
                            sendErrorResponse(msg, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    log.error("Error processing financial management message", e);
                    sendErrorResponse(msg, "Error processing request: " + e.getMessage());
                }
            } else {
                block();
            }
        }
    }

    private class FinancialReportingBehaviour extends TickerBehaviour {
        public FinancialReportingBehaviour(jade.core.Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            generateDailyFinancialReport();
            updateExchangeRates();
            checkBudgetAlerts();
            processRecurringTransactions();
        }
    }

    private void handleCreateInvoice(ACLMessage msg, Map<String, Object> data) {
        try {
            String customerId = (String) data.get("customerId");
            String orderId = (String) data.get("orderId");
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            String currency = (String) data.getOrDefault("currency", "USD");

            if (customerId == null || orderId == null || items == null) {
                sendErrorResponse(msg, "Missing required invoice data");
                return;
            }

            String invoiceNumber = "INV" + String.format("%08d", invoiceCounter++);
            double subtotal = 0.0;

            List<InvoiceItem> invoiceItems = new ArrayList<>();
            for (Map<String, Object> itemData : items) {
                String productId = (String) itemData.get("productId");
                Integer quantity = (Integer) itemData.get("quantity");
                Double unitPrice = (Double) itemData.get("unitPrice");

                if (productId != null && quantity != null && unitPrice != null) {
                    InvoiceItem item = new InvoiceItem(productId, quantity, unitPrice);
                    invoiceItems.add(item);
                    subtotal += quantity * unitPrice;
                }
            }

            double tax = subtotal * 0.16; // 16% IVA
            double total = subtotal + tax;

            Invoice invoice = new Invoice(invoiceNumber, customerId, orderId, 
                    invoiceItems, subtotal, tax, total, currency);
            pendingInvoices.put(invoiceNumber, invoice);

            // Record transaction
            recordTransaction("INVOICE_CREATED", invoiceNumber, total, currency, 
                    "Invoice created for order " + orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "INVOICE_CREATED");
            response.put("invoiceNumber", invoiceNumber);
            response.put("customerId", customerId);
            response.put("orderId", orderId);
            response.put("subtotal", subtotal);
            response.put("tax", tax);
            response.put("total", total);
            response.put("currency", currency);
            response.put("dueDate", invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Created invoice {} for customer {} (${} {})", 
                    invoiceNumber, customerId, total, currency);

        } catch (Exception e) {
            log.error("Error creating invoice", e);
            sendErrorResponse(msg, "Error creating invoice: " + e.getMessage());
        }
    }

    private void handleProcessPayment(ACLMessage msg, Map<String, Object> data) {
        try {
            String invoiceNumber = (String) data.get("invoiceNumber");
            Double amount = (Double) data.get("amount");
            String paymentMethod = (String) data.getOrDefault("paymentMethod", "BANK_TRANSFER");
            String currency = (String) data.getOrDefault("currency", "USD");

            if (invoiceNumber == null || amount == null) {
                sendErrorResponse(msg, "Missing payment data");
                return;
            }

            Invoice invoice = pendingInvoices.get(invoiceNumber);
            if (invoice == null) {
                sendErrorResponse(msg, "Invoice not found: " + invoiceNumber);
                return;
            }

            // Convert currency if needed
            double convertedAmount = convertCurrency(amount, currency, invoice.getCurrency());

            String paymentId = "PAY" + String.format("%08d", paymentCounter++);
            Payment payment = new Payment(paymentId, invoiceNumber, convertedAmount, 
                    invoice.getCurrency(), paymentMethod);

            paymentRecords.put(paymentId, payment);

            // Update invoice status
            invoice.addPayment(payment);
            if (invoice.isPaidInFull()) {
                invoice.setStatus("PAID");
                pendingInvoices.remove(invoiceNumber);
            } else {
                invoice.setStatus("PARTIALLY_PAID");
            }

            // Record transaction
            recordTransaction("PAYMENT_RECEIVED", paymentId, convertedAmount, 
                    invoice.getCurrency(), "Payment for invoice " + invoiceNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "PAYMENT_PROCESSED");
            response.put("paymentId", paymentId);
            response.put("invoiceNumber", invoiceNumber);
            response.put("amount", convertedAmount);
            response.put("currency", invoice.getCurrency());
            response.put("remainingBalance", invoice.getRemainingBalance());
            response.put("invoiceStatus", invoice.getStatus());

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Processed payment {} of ${} {} for invoice {}", 
                    paymentId, convertedAmount, invoice.getCurrency(), invoiceNumber);

        } catch (Exception e) {
            log.error("Error processing payment", e);
            sendErrorResponse(msg, "Error processing payment: " + e.getMessage());
        }
    }

    private void handleCheckBudget(ACLMessage msg, Map<String, Object> data) {
        String department = (String) data.get("department");
        String costCenter = (String) data.get("costCenter");

        if (department == null) {
            sendErrorResponse(msg, "Missing department");
            return;
        }

        Budget budget = departmentBudgets.get(department);
        Map<String, Object> response = new HashMap<>();
        response.put("action", "BUDGET_STATUS");
        response.put("department", department);

        if (budget != null) {
            response.put("totalBudget", budget.getTotalAmount());
            response.put("usedBudget", budget.getUsedAmount());
            response.put("remainingBudget", budget.getRemainingAmount());
            response.put("utilizationPercent", budget.getUtilizationPercentage());
            response.put("period", budget.getPeriod());
            response.put("status", budget.getStatus());
        } else {
            response.put("status", "BUDGET_NOT_FOUND");
        }

        if (costCenter != null) {
            CostCenter cc = costCenters.get(costCenter);
            if (cc != null) {
                response.put("costCenter", Map.of(
                    "id", cc.getId(),
                    "name", cc.getName(),
                    "department", cc.getDepartment(),
                    "totalCosts", cc.getTotalCosts()
                ));
            }
        }

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);
    }

    private void handleRecordExpense(ACLMessage msg, Map<String, Object> data) {
        try {
            String department = (String) data.get("department");
            String costCenter = (String) data.get("costCenter");
            Double amount = (Double) data.get("amount");
            String description = (String) data.get("description");
            String category = (String) data.getOrDefault("category", "OPERATIONAL");
            String currency = (String) data.getOrDefault("currency", "USD");

            if (department == null || amount == null || description == null) {
                sendErrorResponse(msg, "Missing expense data");
                return;
            }

            Budget budget = departmentBudgets.get(department);
            if (budget == null) {
                sendErrorResponse(msg, "Department budget not found: " + department);
                return;
            }

            // Convert to USD if needed
            double usdAmount = convertCurrency(amount, currency, "USD");

            // Check budget availability
            if (budget.getRemainingAmount() < usdAmount) {
                sendErrorResponse(msg, "Insufficient budget for expense");
                return;
            }

            // Record expense
            budget.addExpense(usdAmount);

            // Update cost center if specified
            if (costCenter != null && costCenters.containsKey(costCenter)) {
                costCenters.get(costCenter).addCost(usdAmount);
            }

            // Record transaction
            String transactionId = recordTransaction("EXPENSE", department + "_EXP", 
                    usdAmount, "USD", description);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "EXPENSE_RECORDED");
            response.put("transactionId", transactionId);
            response.put("department", department);
            response.put("amount", usdAmount);
            response.put("remainingBudget", budget.getRemainingAmount());
            response.put("budgetUtilization", budget.getUtilizationPercentage());

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(JsonUtil.toJson(response));
            send(reply);

            log.info("Recorded expense of ${} USD for department {} ({})", 
                    usdAmount, department, description);

        } catch (Exception e) {
            log.error("Error recording expense", e);
            sendErrorResponse(msg, "Error recording expense: " + e.getMessage());
        }
    }

    private void handleGetFinancialReport(ACLMessage msg, Map<String, Object> data) {
        String reportType = (String) data.getOrDefault("reportType", "SUMMARY");
        String period = (String) data.getOrDefault("period", "CURRENT_MONTH");

        Map<String, Object> report = generateFinancialReport(reportType, period);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(report));
        send(reply);

        log.info("Generated {} financial report for period {}", reportType, period);
    }

    private void handleApproveExpenditure(ACLMessage msg, Map<String, Object> data) {
        String requestId = (String) data.get("requestId");
        Double amount = (Double) data.get("amount");
        String department = (String) data.get("department");
        Boolean approved = (Boolean) data.get("approved");

        if (requestId == null || amount == null || department == null || approved == null) {
            sendErrorResponse(msg, "Missing approval data");
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("action", "EXPENDITURE_APPROVAL");
        response.put("requestId", requestId);
        response.put("approved", approved);

        if (approved) {
            Budget budget = departmentBudgets.get(department);
            if (budget != null && budget.getRemainingAmount() >= amount) {
                response.put("status", "APPROVED");
                response.put("availableBudget", budget.getRemainingAmount());
            } else {
                response.put("status", "REJECTED");
                response.put("reason", "Insufficient budget");
                approved = false;
            }
        } else {
            response.put("status", "REJECTED");
            response.put("reason", "Not approved by financial manager");
        }

        ACLMessage reply = msg.createReply();
        reply.setPerformative(approved ? ACLMessage.AGREE : ACLMessage.REFUSE);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Expenditure request {} {} for ${} ({})", 
                requestId, approved ? "approved" : "rejected", amount, department);
    }

    private void handleCalculateProfitability(ACLMessage msg, Map<String, Object> data) {
        String productId = (String) data.get("productId");
        String orderId = (String) data.get("orderId");
        String period = (String) data.getOrDefault("period", "CURRENT_MONTH");

        Map<String, Object> profitability = calculateProfitability(productId, orderId, period);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(profitability));
        send(reply);
    }

    private void handleUpdateExchangeRates(ACLMessage msg, Map<String, Object> data) {
        Map<String, Double> newRates = (Map<String, Double>) data.get("rates");

        if (newRates != null) {
            exchangeRates.putAll(newRates);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("action", "EXCHANGE_RATES_UPDATED");
        response.put("rates", exchangeRates);
        response.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(response));
        send(reply);

        log.info("Updated exchange rates: {}", newRates != null ? newRates.keySet() : "auto-update");
    }

    private void handleGetCashFlow(ACLMessage msg, Map<String, Object> data) {
        String period = (String) data.getOrDefault("period", "CURRENT_MONTH");
        String currency = (String) data.getOrDefault("currency", "USD");

        Map<String, Object> cashFlow = generateCashFlowReport(period, currency);

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(JsonUtil.toJson(cashFlow));
        send(reply);
    }

    // Helper methods
    private String recordTransaction(String type, String reference, double amount, 
                                   String currency, String description) {
        String transactionId = "TXN" + String.format("%010d", transactions.size() + 1);
        FinancialTransaction transaction = new FinancialTransaction(
                transactionId, type, reference, amount, currency, description);
        transactions.add(transaction);
        return transactionId;
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        double fromRate = exchangeRates.getOrDefault(fromCurrency, 1.0);
        double toRate = exchangeRates.getOrDefault(toCurrency, 1.0);

        return amount * (toRate / fromRate);
    }

    private Map<String, Object> generateFinancialReport(String reportType, String period) {
        Map<String, Object> report = new HashMap<>();
        report.put("reportType", reportType);
        report.put("period", period);
        report.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Calculate totals
        double totalRevenue = calculateTotalRevenue(period);
        double totalExpenses = calculateTotalExpenses(period);
        double netProfit = totalRevenue - totalExpenses;

        report.put("totalRevenue", totalRevenue);
        report.put("totalExpenses", totalExpenses);
        report.put("netProfit", netProfit);
        report.put("profitMargin", totalRevenue > 0 ? (netProfit / totalRevenue) * 100 : 0);

        // Budget utilization
        Map<String, Double> budgetUtilization = new HashMap<>();
        for (Map.Entry<String, Budget> entry : departmentBudgets.entrySet()) {
            budgetUtilization.put(entry.getKey(), entry.getValue().getUtilizationPercentage());
        }
        report.put("budgetUtilization", budgetUtilization);

        // Pending invoices
        report.put("pendingInvoicesCount", pendingInvoices.size());
        report.put("pendingInvoicesValue", pendingInvoices.values().stream()
                .mapToDouble(Invoice::getRemainingBalance).sum());

        return report;
    }

    private Map<String, Object> calculateProfitability(String productId, String orderId, String period) {
        Map<String, Object> profitability = new HashMap<>();
        
        // Simplified profitability calculation
        double revenue = 50000.0; // Mock revenue
        double costs = 35000.0;   // Mock costs
        double profit = revenue - costs;
        double margin = (profit / revenue) * 100;

        profitability.put("action", "PROFITABILITY_ANALYSIS");
        profitability.put("productId", productId);
        profitability.put("orderId", orderId);
        profitability.put("period", period);
        profitability.put("revenue", revenue);
        profitability.put("totalCosts", costs);
        profitability.put("profit", profit);
        profitability.put("profitMargin", margin);
        profitability.put("breakEvenPoint", costs / (revenue / 100)); // Units to break even

        return profitability;
    }

    private Map<String, Object> generateCashFlowReport(String period, String currency) {
        Map<String, Object> cashFlow = new HashMap<>();
        
        double cashInflow = calculateCashInflow(period, currency);
        double cashOutflow = calculateCashOutflow(period, currency);
        double netCashFlow = cashInflow - cashOutflow;

        cashFlow.put("action", "CASH_FLOW_REPORT");
        cashFlow.put("period", period);
        cashFlow.put("currency", currency);
        cashFlow.put("cashInflow", cashInflow);
        cashFlow.put("cashOutflow", cashOutflow);
        cashFlow.put("netCashFlow", netCashFlow);
        cashFlow.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return cashFlow;
    }

    private double calculateTotalRevenue(String period) {
        return transactions.stream()
                .filter(t -> "INVOICE_CREATED".equals(t.getType()))
                .mapToDouble(t -> convertCurrency(t.getAmount(), t.getCurrency(), "USD"))
                .sum();
    }

    private double calculateTotalExpenses(String period) {
        return transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(t -> convertCurrency(t.getAmount(), t.getCurrency(), "USD"))
                .sum();
    }

    private double calculateCashInflow(String period, String currency) {
        return transactions.stream()
                .filter(t -> "PAYMENT_RECEIVED".equals(t.getType()))
                .mapToDouble(t -> convertCurrency(t.getAmount(), t.getCurrency(), currency))
                .sum();
    }

    private double calculateCashOutflow(String period, String currency) {
        return transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(t -> convertCurrency(t.getAmount(), t.getCurrency(), currency))
                .sum();
    }

    private void generateDailyFinancialReport() {
        log.info("Generating daily financial report...");
        // Auto-generate daily reports
    }

    private void updateExchangeRates() {
        // Simulate exchange rate updates
        Random random = new Random();
        for (Map.Entry<String, Double> entry : exchangeRates.entrySet()) {
            if (!"USD".equals(entry.getKey())) {
                double rate = entry.getValue();
                double fluctuation = (random.nextGaussian() * 0.01); // 1% volatility
                entry.setValue(Math.max(0.1, rate * (1 + fluctuation)));
            }
        }
    }

    private void checkBudgetAlerts() {
        for (Budget budget : departmentBudgets.values()) {
            if (budget.getUtilizationPercentage() > 90) {
                log.warn("Budget alert: {} has used {}% of budget", 
                        budget.getDepartment(), budget.getUtilizationPercentage());
            }
        }
    }

    private void processRecurringTransactions() {
        // Process any recurring financial transactions
        log.debug("Processing recurring transactions...");
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
        log.info("AgenteGestorFinanciero {} shutting down", getAID().getName());
        super.takeDown();
    }

    // Helper classes
    private static class Budget {
        private String department;
        private String description;
        private double totalAmount;
        private double usedAmount;
        private String period;
        private LocalDateTime createdAt;

        public Budget(String department, String description, double totalAmount, 
                     double usedAmount, String period) {
            this.department = department;
            this.description = description;
            this.totalAmount = totalAmount;
            this.usedAmount = usedAmount;
            this.period = period;
            this.createdAt = LocalDateTime.now();
        }

        public void addExpense(double amount) {
            this.usedAmount += amount;
        }

        public double getRemainingAmount() {
            return totalAmount - usedAmount;
        }

        public double getUtilizationPercentage() {
            return totalAmount > 0 ? (usedAmount / totalAmount) * 100 : 0;
        }

        public String getStatus() {
            double utilization = getUtilizationPercentage();
            if (utilization >= 100) return "EXCEEDED";
            if (utilization >= 90) return "CRITICAL";
            if (utilization >= 75) return "WARNING";
            return "NORMAL";
        }

        // Getters
        public String getDepartment() { return department; }
        public String getDescription() { return description; }
        public double getTotalAmount() { return totalAmount; }
        public double getUsedAmount() { return usedAmount; }
        public String getPeriod() { return period; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    private static class CostCenter {
        private String id;
        private String name;
        private String department;
        private double totalCosts;

        public CostCenter(String id, String name, String department) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.totalCosts = 0.0;
        }

        public void addCost(double cost) {
            this.totalCosts += cost;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public double getTotalCosts() { return totalCosts; }
    }

    private static class Invoice {
        private String invoiceNumber;
        private String customerId;
        private String orderId;
        private List<InvoiceItem> items;
        private double subtotal;
        private double tax;
        private double total;
        private String currency;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime dueDate;
        private List<Payment> payments;

        public Invoice(String invoiceNumber, String customerId, String orderId,
                      List<InvoiceItem> items, double subtotal, double tax, 
                      double total, String currency) {
            this.invoiceNumber = invoiceNumber;
            this.customerId = customerId;
            this.orderId = orderId;
            this.items = new ArrayList<>(items);
            this.subtotal = subtotal;
            this.tax = tax;
            this.total = total;
            this.currency = currency;
            this.status = "PENDING";
            this.createdAt = LocalDateTime.now();
            this.dueDate = LocalDateTime.now().plusDays(30);
            this.payments = new ArrayList<>();
        }

        public void addPayment(Payment payment) {
            payments.add(payment);
        }

        public double getRemainingBalance() {
            double totalPaid = payments.stream().mapToDouble(Payment::getAmount).sum();
            return total - totalPaid;
        }

        public boolean isPaidInFull() {
            return getRemainingBalance() <= 0.01; // Small tolerance for rounding
        }

        // Getters and setters
        public String getInvoiceNumber() { return invoiceNumber; }
        public String getCustomerId() { return customerId; }
        public String getOrderId() { return orderId; }
        public List<InvoiceItem> getItems() { return items; }
        public double getSubtotal() { return subtotal; }
        public double getTax() { return tax; }
        public double getTotal() { return total; }
        public String getCurrency() { return currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getDueDate() { return dueDate; }
        public List<Payment> getPayments() { return payments; }
    }

    private static class InvoiceItem {
        private String productId;
        private int quantity;
        private double unitPrice;
        private double lineTotal;

        public InvoiceItem(String productId, int quantity, double unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = quantity * unitPrice;
        }

        // Getters
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getLineTotal() { return lineTotal; }
    }

    private static class Payment {
        private String paymentId;
        private String invoiceNumber;
        private double amount;
        private String currency;
        private String paymentMethod;
        private LocalDateTime paymentDate;

        public Payment(String paymentId, String invoiceNumber, double amount, 
                      String currency, String paymentMethod) {
            this.paymentId = paymentId;
            this.invoiceNumber = invoiceNumber;
            this.amount = amount;
            this.currency = currency;
            this.paymentMethod = paymentMethod;
            this.paymentDate = LocalDateTime.now();
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getInvoiceNumber() { return invoiceNumber; }
        public double getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public LocalDateTime getPaymentDate() { return paymentDate; }
    }

    private static class FinancialTransaction {
        private String transactionId;
        private String type;
        private String reference;
        private double amount;
        private String currency;
        private String description;
        private LocalDateTime timestamp;

        public FinancialTransaction(String transactionId, String type, String reference,
                                  double amount, String currency, String description) {
            this.transactionId = transactionId;
            this.type = type;
            this.reference = reference;
            this.amount = amount;
            this.currency = currency;
            this.description = description;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getTransactionId() { return transactionId; }
        public String getType() { return type; }
        public String getReference() { return reference; }
        public double getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
