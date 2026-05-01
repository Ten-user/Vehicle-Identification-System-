package com.example.assignment;

/**
 * Represents a Vehicle in the Vehicle Identification System.
 * Demonstrates INHERITANCE - extends BaseEntity.
 * Workshop module: stores vehicle registration details and service information.
 */
public class Vehicle extends BaseEntity {
    private String registrationNumber;
    private String make;
    private String model;
    private int year;
    private String color;
    private String engineNumber;
    private String chassisNumber;
    private int ownerId;
    private String ownerName;
    private String registrationDate;
    private boolean isStolen;

    public Vehicle() {
        super();
        this.isStolen = false;
    }

    public Vehicle(int id, String registrationNumber, String make, String model, int year, int ownerId) {
        super(id);
        this.registrationNumber = registrationNumber;
        this.make = make;
        this.model = model;
        this.year = year;
        this.ownerId = ownerId;
        this.isStolen = false;
    }

    @Override
    public String getDisplayName() {
        return year + " " + make + " " + model + " (" + registrationNumber + ")";
    }

    @Override
    public boolean isValid() {
        return registrationNumber != null && !registrationNumber.trim().isEmpty()
            && make != null && !make.trim().isEmpty()
            && model != null && !model.trim().isEmpty()
            && year >= 1900 && year <= java.time.Year.now().getValue() + 1;
    }

    /**
     * Returns the full vehicle description (String Manipulation)
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(year).append(" ").append(make.toUpperCase()).append(" ").append(model);
        if (color != null) sb.append(" - ").append(color);
        sb.append(" [").append(registrationNumber.toUpperCase()).append("]");
        if (isStolen) sb.append(" *** STOLEN ***");
        return sb.toString();
    }

    /**
     * Returns formatted registration number (String Manipulation)
     */
    public String getFormattedRegNumber() {
        if (registrationNumber == null) return "N/A";
        return registrationNumber.toUpperCase().replaceAll("\\s+", "");
    }

    /**
     * Calculates vehicle age
     */
    public int getVehicleAge() {
        return java.time.Year.now().getValue() - year;
    }

    // Getters and Setters
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }
    public String getChassisNumber() { return chassisNumber; }
    public void setChassisNumber(String chassisNumber) { this.chassisNumber = chassisNumber; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }
    public boolean isStolen() { return isStolen; }
    public void setStolen(boolean stolen) { isStolen = stolen; }
}
