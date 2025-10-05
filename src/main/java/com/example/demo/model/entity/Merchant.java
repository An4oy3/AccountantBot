package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "merchants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_owner_name", columnNames = {"owner_id", "name"})
}, indexes = {
        @Index(name = "idx_merchant_owner", columnList = "owner_id"),
        @Index(name = "idx_merchant_normalized", columnList = "normalized_name")
})
/**
 * Merchant (vendor / payee) entity used for analytics, receipt matching and normalization.
 * Can be global (owner=null) so multiple users benefit from normalized naming.
 */
//TODO: define if entity is needed
public class Merchant extends BaseEntity {

    /** Owner of merchant record; null indicates shared/global scope. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner; // null => глобальный

    /** Display name captured from source (may be raw). */
    @Column(nullable = false, length = 256)
    private String name;

    /** Normalized / cleaned name used for deduplication & search. */
    @Column(name = "normalized_name", length = 256)
    private String normalizedName;

    /** External reference ID from receipt / bank / catalog service. */
    @Column(name = "external_ref", length = 128)
    private String externalRef; // например ID из чек-сервиса

    /** Archive flag to hide merchant while keeping historical linkage. */
    @Column(name = "archived", nullable = false)
    private boolean archived = false;

}
