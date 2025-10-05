package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_budget_owner_category_period_start", columnNames = {"owner_id", "category_id", "period_type", "start_date"})
}, indexes = {
        @Index(name = "idx_budget_owner", columnList = "owner_id"),
        @Index(name = "idx_budget_category", columnList = "category_id"),
        @Index(name = "idx_budget_period", columnList = "period_type,start_date")
})
/**
 * Budget links a user, a category and a time period with a spending (or earning) limit.
 * spentAmount can be periodically recalculated from transactions; rollover permits
 * carrying over unused remainder into the next period.
 */
public class Budget extends BaseEntity {

    /** Owner of this budget configuration. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** Target category the budget applies to (expense or income). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Period granularity type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 16)
    private BudgetPeriodType periodType;

    /** Start date (inclusive) of the budget period. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // начало периода (например 2025-09-01 для месяца)

    /** Allowed limit for the period (absolute monetary value). */
    @Column(name = "amount_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountLimit; // лимит

    /** Cached aggregated amount spent so far in the same currency. */
    @Column(name = "spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal spentAmount = BigDecimal.ZERO; // вычисляемая / кешируемая

    /** Currency code (should match related transactions). */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency; // для агрегирования (совпадает с валютой транзакций)

    /** Optional note or description. */
    @Column(name = "note", length = 512)
    private String note;
}
