package com.example.assignment;

/**
 * Represents a Customer in the Vehicle Identification System.
 * Demonstrates INHERITANCE - extends Person.
 * Customer module: provides information to the owner about vehicle condition.
 */
public class Customer extends Person {
    private String address;
    private String idNumber;

    public Customer() {
        super();
    }

    public Customer(int id, String name, String address, String phone, String email, String idNumber) {
        super(id, name, phone, email);
        this.address = address;
        this.idNumber = idNumber;
    }

    // POLYMORPHISM - overrides getPersonType
    @Override
    public String getPersonType() {
        return "Customer";
    }

    @Override
    public boolean isValid() {
        return super.isValid() && name != null && !name.trim().isEmpty();
    }

    /**
     * Returns a masked ID number for privacy (String Manipulation)
     */
    public String getMaskedIdNumber() {
        if (idNumber == null || idNumber.length() < 6) return "***";
        return idNumber.substring(0, 6) + "****" + idNumber.substring(idNumber.length() - 2);
    }

    /**
     * Formats full address with proper capitalization (String Manipulation)
     */
    public String getFormattedAddress() {
        if (address == null) return "N/A";
        String[] parts = address.split(",");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.isEmpty()) {
                // Capitalize first letter of each word
                String[] words = part.split("\\s+");
                for (int j = 0; j < words.length; j++) {
                    if (!words[j].isEmpty()) {
                        words[j] = words[j].substring(0, 1).toUpperCase() +
                                   (words[j].length() > 1 ? words[j].substring(1).toLowerCase() : "");
                    }
                }
                formatted.append(String.join(" ", words));
                if (i < parts.length - 1) formatted.append(", ");
            }
        }
        return formatted.toString();
    }

    // Getters and Setters
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
}
