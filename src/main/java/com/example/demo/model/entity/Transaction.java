package com.example.demo.model.entity;

import com.example.demo.model.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a financial transaction performed by a user.
 * <p>
 * Supported transaction types include income, expense and transfer operations.
 * A transaction always belongs to an {@link Account} (the source account for expenses / the target for income).
 * For transfers a {@link #counterAccount} may be set representing the second side of the movement.
 * <p>
 * Currency is denormalized from the account at the moment of creation to preserve historical correctness
 * even if the account currency changes later. For transfers with currency conversion the {@link #exchangeRate}
 * and {@link #counterAmount} fields are used.
 * <p>
 * Idempotency is enforced per owner via the composite unique constraint (owner_id, idempotency_key).
 * Status transitions typically go from PENDING -> POSTED (or FAILED / CANCELED if you add such states later).
 */
@Setter
@Getter
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_trx_account", columnList = "account_id"),
        @Index(name = "idx_trx_owner", columnList = "owner_id"),
        @Index(name = "idx_trx_status", columnList = "status"), })
//TODO: Uncomment when implementing idempotency
//}, uniqueConstraints = {
//        @UniqueConstraint(name = "uk_trx_owner_idemp", columnNames = {"owner_id"})
//})
public class Transaction extends BaseEntity {

    /**
     * Owner (user) who initiated / owns the transaction. Mandatory.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * High level transaction type (e.g. INCOME, EXPENSE, TRANSFER).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    /**
     * Current lifecycle status of the transaction. Defaults to PENDING until posted / finalized.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * Primary account affected by this transaction.
     * For EXPENSE funds leave this account, for INCOME they enter, for TRANSFER this is the source account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * TODO: For later implementation of transfers.
     * Counter account for transfer transactions (destination when moving funds). Null for non-transfer types.
     */
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "counter_account_id")
//    private Account counterAccount; // для transfer

    /**
     * TODO: Revisit later for potential implementation.
     * Optional merchant / vendor associated with the transaction (mostly for expenses).
     */
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "merchant_id")
//    private Merchant merchant;

    /**
     * Monetary amount in the account's currency (positive value). Precision 19, scale 4.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount; // сумма в валюте account

    /**
     * Counter amount for transfers when currency conversion occurs (amount in counter account currency).
     */
//    @Column(name = "counter_amount", precision = 19, scale = 4)
//    private BigDecimal counterAmount; // для transfer

    /**
     * ISO 4217 currency code captured at creation time (denormalized from account).
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency; // денормализация account.currency

    /**
     * Exchange rate applied when converting between account and counter account currencies (if applicable).
     * Typically expressed as counterAmount / amount.
     */
    @Column(name = "exchange_rate", precision = 19, scale = 8)
    private BigDecimal exchangeRate; // курс при конверсии

    /**
     * Short human readable description (e.g. merchant provided or user input). Up to 512 chars.
     */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * User's personal note or memo. Up to 1024 chars.
     */
    @Column(name = "note", length = 1024)
    private String note;

    /**
     * The time the operation logically occurred (client or server assigned). Used for ordering / reporting.
     */
    @Column(name = "operation_time", nullable = false)
    private Instant operationTime = Instant.now();

    /**
     * Time when the transaction was posted / finalized (e.g. after confirmation or reconciliation). Nullable.
     */
    @Column(name = "posted_time")
    private Instant postedTime;

    /**
     * External system reference / identifier (e.g. bank statement ID, payment processor ID).
     */
    @Column(name = "external_ref", length = 128)
    private String externalRef;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "category", length = 32)
//    private ExpenseCategory category;
    @ManyToOne()
    @JoinColumn(name = "category_id")
    private Category category;
    /**
     * TODO: For later implementation of idempotency.
     * Optional splits breaking down this transaction into multiple categories / budgets.
     * Orphan removal keeps the collection in sync with persistence.
     */
//    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<TransactionSplit> splits = new HashSet<>();

    /**
     * Attachments (receipts, invoices, images) linked to the transaction.
     */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attachment> attachments = new HashSet<>();

}
