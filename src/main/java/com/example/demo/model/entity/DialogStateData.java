package com.example.demo.model.entity;

import com.example.demo.model.enums.DialogStateType;
import com.example.demo.model.enums.ExpenseCategory;
import com.example.demo.model.enums.IncomeSource;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@RedisHash(value = "DialogStateData", timeToLive = 43200)
public class DialogStateData implements Serializable {
    @Id
    private Long chatId;
    private DialogStateType state;
    private BigDecimal amount;
    private Category category;
    private Account account;
    private LocalDate transactionDate;
    private String comment;
    private LocalDateTime lastUpdated;
    private boolean isExpense;
    private boolean isIncome;
}
