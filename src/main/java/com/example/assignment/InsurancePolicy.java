package com.example.assignment;

import java.math.BigDecimal;

/**
 * Represents an Insurance Policy in the Insurance Module.
 * Demonstrates INHERITANCE - extends VehicleRecord.
 */
public class InsurancePolicy extends VehicleRecord {
    private int customerId;
    private String policyNumber;
    private String provider;
    private String coverageType;
    private String startDate;
    private String endDate;
    private BigDecimal premium;
    private boolean isActive;
    private String vehicleLabel;   // for table display
    private String customerLabel;  // for table display

    public InsurancePolicy() {
        super();
        this.isActive = true;
    }

    @Override
    public String getStatusLabel() {
        return isActive ? "Active" : "Expired";
    }

    @Override
    public String getSeverityLevel() {
        if (!isActive) return "High";
        return "Low";
    }

    @Override
    public String getDisplayName() {
        return policyNumber + " - " + provider + " (" + getStatusLabel() + ")";
    }

    /** Formats premium as Lesotho Loti (M) */
    public String getFormattedPremium() {
        if (premium == null) return "0.00";
        return String.format("%,.2f", premium);
    }

    public boolean isExpired() {
        if (endDate == null) return false;
        try {
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            return end.isBefore(java.time.LocalDate.now());
        } catch (Exception e) { return false; }
    }

    public long daysUntilExpiry() {
        if (endDate == null) return -1;
        try {
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), end);
        } catch (Exception e) { return -1; }
    }

    // Getters and Setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCoverageType() { return coverageType; }
    public void setCoverageType(String coverageType) { this.coverageType = coverageType; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
    public String getCustomerLabel() { return customerLabel; }
    public void setCustomerLabel(String customerLabel) { this.customerLabel = customerLabel; }
}
