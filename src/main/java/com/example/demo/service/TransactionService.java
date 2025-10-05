package com.example.demo.service;

import com.example.demo.model.entity.Account;
import com.example.demo.model.enums.ExpenseCategory;

import java.math.BigDecimal;

public interface TransactionService {
    void addExpense(Long chatId, BigDecimal amount, ExpenseCategory category, String comment, String date);
    void addIncome(Long chatId, BigDecimal amount, ExpenseCategory category, String comment, String date);
    String getTodayStats(Long chatId);
    String getMonthStats(Long chatId);
    String getYearStats(Long chatId);
    String getCategoryStats(Long chatId, ExpenseCategory category, String period);
    void deleteLastTransaction(Long chatId);
}
