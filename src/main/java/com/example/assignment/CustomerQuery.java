package com.example.assignment;

/**
 * Represents a Customer Query in the Customer Module.
 * Demonstrates INHERITANCE - extends VehicleRecord.
 * Provides information to the owner about vehicle condition and queries.
 */
public class CustomerQuery extends VehicleRecord {
    private int customerId;
    private String customerName;
    private String queryText;
    private String responseText;
    private String status; // Pending, In Progress, Resolved, Closed

    public CustomerQuery() {
        super();
        this.status = "Pending";
    }

    public CustomerQuery(int id, int customerId, int vehicleId, String queryDate,
                         String queryText, String responseText, String status) {
        super(id, vehicleId, queryDate);
        this.customerId = customerId;
        this.queryText = queryText;
        this.responseText = responseText;
        this.status = status;
    }

    // POLYMORPHISM - overrides
    @Override
    public String getStatusLabel() {
        return status != null ? status : "Pending";
    }

    @Override
    public String getSeverityLevel() {
        if ("Pending".equals(status)) return "High";
        if ("In Progress".equals(status)) return "Medium";
        return "Low"; // Resolved, Closed
    }

    @Override
    public String getDisplayName() {
        return "Query #" + id + " - " + getStatusLabel() + " (" + getFormattedDate() + ")";
    }

    /**
     * Returns a truncated preview of the query text (String Manipulation)
     */
    public String getQueryPreview() {
        if (queryText == null) return "";
        return queryText.length() > 80 ? queryText.substring(0, 77) + "..." : queryText;
    }

    /**
     * Returns true if the query is resolved
     */
    public boolean isResolved() {
        return "Resolved".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status);
    }

    // Getters and Setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    public String getResponseText() { return responseText; }
    public void setResponseText(String responseText) { this.responseText = responseText; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
