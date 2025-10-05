package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Abstract base class for all JPA entities providing an auto-generated id and
 * simple audit timestamps (createdAt / updatedAt) managed via JPA lifecycle callbacks.
 * Not versioned (no optimistic locking). Add @Version in descendants if needed.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {
    /** Surrogate primary key (database identity). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Creation timestamp (UTC). Set once on persist. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Last update timestamp (UTC). Updated on each entity modification. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Initializes audit timestamps before first persistence. */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Updates the modification timestamp prior to updating the row. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
