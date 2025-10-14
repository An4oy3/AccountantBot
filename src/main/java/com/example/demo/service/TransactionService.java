package com.example.demo.service;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.Category;
import com.example.demo.model.entity.Transaction;
import com.example.demo.model.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionService {
    void addExpense(Long chatId, BigDecimal amount, Category category, String comment, String date, Account account);
    void addIncome(Long chatId, BigDecimal amount, Category category, String comment, String date, Account account);

    List<Transaction> getAllByPeriod(Long chatId, LocalDate startDate, LocalDate endDate);
    String getTodayStats(Long chatId);
    String getMonthStats(Long chatId);
    String getYearStats(Long chatId);
    String getCategoryStats(Long chatId, ExpenseCategory category, String period);
    void deleteLastTransaction(Long chatId);
}
