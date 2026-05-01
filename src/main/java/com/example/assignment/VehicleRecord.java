package com.example.assignment;

/**
 * Abstract base class for all records in the system (Service, Police, Violation, etc.)
 * Demonstrates INHERITANCE and POLYMORPHISM.
 * All records share: vehicle reference, date, and description.
 */
public abstract class VehicleRecord extends BaseEntity {
    protected int vehicleId;
    protected String recordDate;
    protected String description;

    public VehicleRecord() {
        super();
    }

    public VehicleRecord(int id, int vehicleId, String recordDate) {
        super(id);
        this.vehicleId = vehicleId;
        this.recordDate = recordDate;
    }

    // POLYMORPHISM - each record type has different status semantics
    public abstract String getStatusLabel();

    // POLYMORPHISM - each record type has different severity
    public abstract String getSeverityLevel();

    /**
     * Returns formatted date (String Manipulation)
     */
    public String getFormattedDate() {
        if (recordDate == null) return "N/A";
        try {
            String[] parts = recordDate.split("-");
            if (parts.length == 3) {
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                if (month >= 1 && month <= 12) {
                    return day + " " + months[month - 1] + " " + parts[0];
                }
            }
        } catch (NumberFormatException e) {
            // Return raw date if parsing fails
        }
        return recordDate;
    }

    @Override
    public boolean isValid() {
        return vehicleId > 0 && recordDate != null && !recordDate.trim().isEmpty();
    }

    // Getters and Setters
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
