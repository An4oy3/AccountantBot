package com.example.demo.service.impl;

import com.example.demo.exception.NotFoundException;
import com.example.demo.model.entity.*;
import com.example.demo.model.enums.ExpenseCategory;
import com.example.demo.repository.*;
import com.example.demo.service.AccountService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    private static final DateTimeFormatter[] DATE_PATTERNS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    @Override
    @Transactional
    public void addExpense(Long chatId, BigDecimal amount, Category category, String comment, String date, Account account) {
        addTransaction(chatId, amount, category, comment, date, TransactionType.EXPENSE, account);
    }

    @Override
    @Transactional
    public void addIncome(Long chatId, BigDecimal amount, Category category, String comment, String date, Account account) {
        addTransaction(chatId, amount, category, comment, date, TransactionType.INCOME, account);
    }

    @Override
    public List<Transaction> getAllByPeriod(Long chatId, LocalDate startDate, LocalDate endDate) {
        User user = findUserByChatId(chatId);
        return transactionRepository.findByOwnerAndOperationTimeBetween(
                user,
                startDate.atStartOfDay().toInstant(ZoneOffset.UTC),
                endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
    }

    @Override
    public String getTodayStats(Long chatId) {
        return "Функция пока не реализована";
    }

    @Override
    public String getMonthStats(Long chatId) {
        return "Функция пока не реализована";
    }

    @Override
    public String getYearStats(Long chatId) {
        return "Функция пока не реализована";
    }

    @Override
    public String getCategoryStats(Long chatId, ExpenseCategory category, String period) {
        return "Функция пока не реализована";
    }

    @Override
    @Transactional
    public void deleteLastTransaction(Long chatId) {
        User user = findUserByChatId(chatId);
        List<Transaction> transactions = transactionRepository.findByOwnerOrderByPostedTimeDesc(user);
        if (transactions.isEmpty()) {
            throw new NotFoundException("Нет транзакций для удаления");
        }
        transactionRepository.delete(transactions.get(0));
    }

    /* ===================== Internal helpers ===================== */

    private void addTransaction(Long chatId, BigDecimal amount, Category category, String comment, String dateStr, TransactionType type, Account account) {
        User user = findUserByChatId(chatId);
        Instant opTime = parseDateOrNow(dateStr);

        Transaction trx = new Transaction();
        trx.setOwner(user);
        trx.setType(type);
        trx.setStatus(TransactionStatus.POSTED);
        trx.setAccount(account);
        trx.setAmount(amount);
        trx.setCurrency(account.getCurrency());
        trx.setDescription(comment != null && !comment.isBlank() ? truncate(comment) : null);
        trx.setOperationTime(opTime);
        trx.setPostedTime(Instant.now());
        trx.setCategory(category);
        transactionRepository.save(trx);
        categoryService.incrementCategoryUsage(category);
    }

    private User findUserByChatId(Long chatId) {
        if (chatId == null) throw new IllegalArgumentException("chatId is required");
        return userService.findByChatId(chatId);
    }

    private Instant parseDateOrNow(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return Instant.now();
        String trimmed = dateStr.trim();
        for (DateTimeFormatter fmt : DATE_PATTERNS) {
            try {
                LocalDate d = LocalDate.parse(trimmed, fmt);
                return d.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException ignored) {
            }
        }
        // fallback try ISO_INSTANT / ISO_LOCAL_DATE_TIME
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }
        return Instant.now();
    }

    private String truncate(String s) {
        int max = 512;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
