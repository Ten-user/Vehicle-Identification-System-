package com.example.assignment;

/**
 * Represents a Police Report in the Police Module.
 * Demonstrates INHERITANCE - extends VehicleRecord.
 * Tracks police-related records for vehicles.
 */
public class PoliceReport extends VehicleRecord {
    private String reportType; // Accident, Theft, Recovery, Investigation, Other
    private String officerName;
    private String station;
    private String caseNumber;

    public PoliceReport() {
        super();
    }

    public PoliceReport(int id, int vehicleId, String reportDate, String reportType,
                        String description, String officerName, String station, String caseNumber) {
        super(id, vehicleId, reportDate);
        this.reportType = reportType;
        this.description = description;
        this.officerName = officerName;
        this.station = station;
        this.caseNumber = caseNumber;
    }

    // POLYMORPHISM - overrides
    @Override
    public String getStatusLabel() {
        return reportType != null ? reportType : "Unknown";
    }

    @Override
    public String getSeverityLevel() {
        if (reportType == null) return "Low";
        return switch (reportType.toLowerCase()) {
            case "theft" -> "Critical";
            case "accident" -> "High";
            case "investigation" -> "Medium";
            default -> "Low";
        };
    }

    @Override
    public String getDisplayName() {
        return reportType + " Report #" + id + " - " + getFormattedDate();
    }

    /**
     * Returns formatted case reference (String Manipulation)
     */
    public String getFormattedCaseRef() {
        if (caseNumber == null) return "No Case Number";
        return caseNumber.toUpperCase().replaceAll("\\s+", "-");
    }

    /**
     * Returns true if this is a theft report
     */
    public boolean isTheftReport() {
        return "Theft".equalsIgnoreCase(reportType);
    }

    /**
     * Returns true if this is a recovery report
     */
    public boolean isRecoveryReport() {
        return "Recovery".equalsIgnoreCase(reportType);
    }

    // Getters and Setters
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getOfficerName() { return officerName; }
    public void setOfficerName(String officerName) { this.officerName = officerName; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
}
