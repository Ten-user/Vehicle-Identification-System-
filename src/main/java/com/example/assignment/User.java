package com.example.assignment;

/**
 * Represents a User of the system (Admin, Workshop, Police, etc.).
 * Demonstrates INHERITANCE - extends Person.
 * Admin module: manages user access and permissions.
 */
public class User extends Person {
    private String username;
    private String passwordHash;
    private String role;
    private boolean isActive;

    public User() {
        super();
        this.isActive = true;
    }

    public User(int id, String username, String passwordHash, String role, String fullName, String email, String phone) {
        super(id, fullName, phone, email);
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = true;
    }

    // POLYMORPHISM - overrides getPersonType
    @Override
    public String getPersonType() {
        return "System User (" + role + ")";
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
               username != null && !username.trim().isEmpty() &&
               role != null && !role.trim().isEmpty();
    }

    /**
     * Checks if user has admin privileges (POLYMORPHISM - different behavior per role)
     */
    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(role);
    }

    /**
     * Checks if user can access police module
     */
    public boolean canAccessPoliceModule() {
        return "Admin".equalsIgnoreCase(role) || "Police".equalsIgnoreCase(role);
    }

    /**
     * Checks if user can access workshop module
     */
    public boolean canAccessWorkshopModule() {
        return "Admin".equalsIgnoreCase(role) || "Workshop".equalsIgnoreCase(role);
    }

    /**
     * Checks if user can access insurance module
     */
    public boolean canAccessInsuranceModule() {
        return "Admin".equalsIgnoreCase(role) || "Insurance".equalsIgnoreCase(role);
    }

    /**
     * Returns role-based display name with badge
     */
    public String getRoleDisplay() {
        if (role == null) return "Unknown";
        return switch (role.toLowerCase()) {
            case "admin" -> "[ADMIN] " + name;
            case "police" -> "[POLICE] " + name;
            case "workshop" -> "[WORKSHOP] " + name;
            case "insurance" -> "[INSURANCE] " + name;
            case "customer" -> "[CUSTOMER] " + name;
            default -> "[USER] " + name;
        };
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
