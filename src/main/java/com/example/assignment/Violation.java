package com.example.assignment;

import java.math.BigDecimal;

/**
 * Represents a Traffic Violation in the Police/Admin Module.
 * Demonstrates INHERITANCE - extends VehicleRecord.
 * Tracks fines, payment status, and location of violations.
 */
public class Violation extends VehicleRecord {
    private String violationType;
    private BigDecimal fineAmount;
    private String status; // Paid, Unpaid
    private String location;
    private String officerName;
    private String paidDate;
    private String vehicleLabel; // registration number for display

    public Violation() {
        super();
        this.status = "Unpaid";
    }

    public Violation(int id, int vehicleId, String violationDate, String violationType,
                     BigDecimal fineAmount, String status, String location,
                     String officerName, String paidDate) {
        super(id, vehicleId, violationDate);
        this.violationType = violationType;
        this.fineAmount = fineAmount;
        this.status = status;
        this.location = location;
        this.officerName = officerName;
        this.paidDate = paidDate;
    }

    @Override
    public String getStatusLabel() {
        return status != null ? status : "Unpaid";
    }

    @Override
    public String getSeverityLevel() {
        if (fineAmount == null) return "Low";
        if (fineAmount.doubleValue() > 3000) return "Critical";
        if (fineAmount.doubleValue() > 1000) return "High";
        return "Medium";
    }

    @Override
    public String getDisplayName() {
        return violationType + " - R" + getFormattedFine() + " (" + getStatusLabel() + ")";
    }

    public String getFormattedFine() {
        if (fineAmount == null) return "0.00";
        return String.format("%,.2f", fineAmount);
    }

    public boolean isUnpaid() { return !isPaid(); }

    public boolean isPaid() {
        return "Paid".equalsIgnoreCase(status);
    }

    public String getSummary() {
        return String.format("%s at %s on %s | Fine: R%s | %s",
                violationType,
                location != null ? location : "Unknown",
                getFormattedDate(),
                getFormattedFine(),
                getStatusLabel());
    }

    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getOfficerName() { return officerName; }
    public void setOfficerName(String officerName) { this.officerName = officerName; }
    public String getPaidDate() { return paidDate; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
}
