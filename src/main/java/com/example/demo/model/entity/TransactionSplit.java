package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Entity
@Table(name = "transaction_splits", indexes = {
        @Index(name = "idx_split_trx", columnList = "transaction_id"),
        @Index(name = "idx_split_category", columnList = "category_id")
})
public class TransactionSplit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // может быть null до категоризации

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount; // знак соответствует типу транзакции (для expense отрицательные не нужны — используем положительные суммы)

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage; // опционально — чтобы хранить долю

    @Column(name = "note", length = 512)
    private String note;

}

