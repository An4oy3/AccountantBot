package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_owner", columnList = "owner_id")
})
public class Account extends BaseEntity {

    /**
     * Human friendly name of the account (not guaranteed to be unique per user).
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * Logical type of the account (cash, card, bank account, credit, etc.).
     * Stored as string for readability and easier migrations.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountType type;

    /**
     * ISO 4217 currency code (e.g. USD, EUR, RUB). Always uppercase, 3 letters.
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PLN";

    /**
     * Owner (primary user) who created / controls this account.
     * Mandatory. Lazy loaded to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_chat_id", referencedColumnName = "chat_id", nullable = false)
    private User owner;

    /**
     * TODO: Revisit later for potential implementation.
     * Additional participants (shared / family accounts). Optional list.
     * Many-to-many through join table 'account_participants'.
     */
//    @ManyToMany
//    @JoinTable(name = "account_participants",
//            joinColumns = @JoinColumn(name = "account_id"),
//            inverseJoinColumns = @JoinColumn(name = "user_id"))
//    private Set<User> participants = new HashSet<>();

    /**
     * Archive flag. When true the account is hidden from active selections
     * but retained for historical data integrity.
     */
    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    //TODO: Check if these fields are needed
//    /**
//     * Initial balance captured at the moment of account creation or import.
//     * Precision 19, scale 4 allows large monetary values with 4 decimal places.
//     */
//    @Column(name = "initial_balance", precision = 19, scale = 4)
//    private BigDecimal initialBalance = BigDecimal.ZERO;
//
//    /**
//     * Denormalized current balance (may be approximate). Can be recalculated
//     * asynchronously from transactions. Not a single source of truth.
//     */
//    @Column(name = "current_balance", precision = 19, scale = 4)
//    private BigDecimal currentBalance = BigDecimal.ZERO; // maintained by service logic (optional)

    public String getDisplayName() {
        return name + " (" + currency + ")";
    }

}
