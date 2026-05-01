package com.example.assignment;

import java.math.BigDecimal;

/**
 * Represents a Service Record in the Workshop Module.
 * Demonstrates INHERITANCE - extends VehicleRecord.
 * Stores vehicle registration details and service information.
 */
public class ServiceRecord extends VehicleRecord {
    private String serviceType;
    private BigDecimal cost;
    private String workshopName;
    private int mileage;
    private String nextServiceDate;

    public ServiceRecord() {
        super();
    }

    public ServiceRecord(int id, int vehicleId, String serviceDate, String serviceType,
                         String description, BigDecimal cost) {
        super(id, vehicleId, serviceDate);
        this.serviceType = serviceType;
        this.description = description;
        this.cost = cost;
    }

    // POLYMORPHISM - overrides
    @Override
    public String getStatusLabel() {
        return "Completed";
    }

    @Override
    public String getSeverityLevel() {
        if (cost == null) return "Low";
        if (cost.doubleValue() > 5000) return "High";
        if (cost.doubleValue() > 2000) return "Medium";
        return "Low";
    }

    @Override
    public String getDisplayName() {
        return serviceType + " - " + getFormattedDate() + " (R" + getFormattedCost() + ")";
    }

    /**
     * Formats cost as South African Rand (String Manipulation)
     */
    public String getFormattedCost() {
        if (cost == null) return "0.00";
        return String.format("%,.2f", cost);
    }

    /**
     * Determines if service is overdue based on next service date
     */
    public boolean isServiceOverdue() {
        if (nextServiceDate == null) return false;
        try {
            java.time.LocalDate next = java.time.LocalDate.parse(nextServiceDate);
            return next.isBefore(java.time.LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    // Getters and Setters
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getWorkshopName() { return workshopName; }
    public void setWorkshopName(String workshopName) { this.workshopName = workshopName; }
    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
    public String getNextServiceDate() { return nextServiceDate; }
    public void setNextServiceDate(String nextServiceDate) { this.nextServiceDate = nextServiceDate; }
}
