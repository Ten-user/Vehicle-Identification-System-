package com.example.assignment;

/**
 * Represents a person in the system (Customer, Officer, etc.).
 * Demonstrates INHERITANCE - extends BaseEntity.
 * Demonstrates ENCAPSULATION - private fields with controlled access.
 */
public abstract class Person extends BaseEntity {
    protected String name;
    protected String phone;
    protected String email;

    public Person() {
        super();
    }

    public Person(int id, String name, String phone, String email) {
        super(id);
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    // POLYMORPHISM - Each person type validates differently
    public abstract String getPersonType();

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public boolean isValid() {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Formats the contact information for display.
     * Demonstrates STRING MANIPULATION.
     */
    public String getFormattedContact() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(name != null ? name : "N/A");
        if (phone != null && !phone.trim().isEmpty()) {
            sb.append(" | Phone: ").append(phone);
        }
        if (email != null && !email.trim().isEmpty()) {
            sb.append(" | Email: ").append(email.toLowerCase().trim());
        }
        return sb.toString();
    }

    /**
     * Validates email format using string manipulation
     */
    public boolean hasValidEmail() {
        if (email == null || email.trim().isEmpty()) return false;
        String trimmed = email.trim().toLowerCase();
        return trimmed.contains("@") && trimmed.contains(".") && trimmed.indexOf("@") < trimmed.lastIndexOf(".");
    }

    /**
     * Validates phone number format (South African)
     */
    public boolean hasValidPhone() {
        if (phone == null || phone.trim().isEmpty()) return false;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() >= 10;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
