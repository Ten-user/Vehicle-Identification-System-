package com.example.assignment;

/**
 * Abstract base class for all entities in the Vehicle Identification System.
 * Demonstrates ABSTRACTION - cannot be instantiated directly.
 * All entities share common properties: id and audit timestamps.
 */
public abstract class BaseEntity {
    protected int id;
    protected String createdAt;

    /**
     * Default constructor for BaseEntity
     */
    public BaseEntity() {
        this.createdAt = java.time.LocalDateTime.now().toString();
    }

    /**
     * Parameterized constructor for BaseEntity
     */
    public BaseEntity(int id) {
        this.id = id;
        this.createdAt = java.time.LocalDateTime.now().toString();
    }

    // Abstract method - each entity defines its own display format
    public abstract String getDisplayName();

    // Abstract method - each entity defines its own validation
    public abstract boolean isValid();

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity that = (BaseEntity) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
