package com.example.demo.service.impl;

import com.example.demo.exception.NotFoundException;
import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.Transaction;
import com.example.demo.model.entity.User;
import com.example.demo.model.enums.ExpenseCategory;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.TransactionSplitRepository;
import com.example.demo.service.AccountService;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl service;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setChatId(111L);
        account = new Account();
        account.setCurrency("RUB");
        account.setName("Main");
    }

    @Test
    void addExpensePersistsPostedExpenseWithCategoryAndComment() {
        when(accountService.findDefaultAccount(111L)).thenReturn(account);
        service.addExpense(111L, new BigDecimal("123.45"), ExpenseCategory.GROCERIES, "Покупка еды", "2024-12-31");
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction t = transactionCaptor.getValue();
        assertThat(t.getType().name()).isEqualTo("EXPENSE");
        assertThat(t.getStatus().name()).isEqualTo("POSTED");
        assertThat(t.getCategory()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(t.getCurrency()).isEqualTo("RUB");
        assertThat(t.getDescription()).isEqualTo("Покупка еды");
        Instant expected = LocalDate.of(2024,12,31).atStartOfDay(ZoneOffset.UTC).toInstant();
        assertThat(t.getOperationTime()).isEqualTo(expected);
        assertThat(t.getPostedTime()).isNotNull();
    }

    @Test
    void addIncomePersistsPostedIncomeWithNullDescriptionWhenCommentBlank() {
        when(accountService.findDefaultAccount(111L)).thenReturn(account);
        service.addIncome(111L, new BigDecimal("10"), ExpenseCategory.OTHER, "  ", null);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction t = transactionCaptor.getValue();
        assertThat(t.getType().name()).isEqualTo("INCOME");
        assertThat(t.getDescription()).isNull();
        assertThat(t.getCategory()).isEqualTo(ExpenseCategory.OTHER);
        assertThat(t.getOperationTime()).isNotNull();
    }

    @Test
    void addExpenseTruncatesLongCommentTo512Chars() {
        when(accountService.findDefaultAccount(111L)).thenReturn(account);
        String longComment = "x".repeat(600);
        service.addExpense(111L, new BigDecimal("1"), ExpenseCategory.OTHER, longComment, null);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction t = transactionCaptor.getValue();
        assertThat(t.getDescription()).hasSize(512);
        assertThat(longComment.startsWith(t.getDescription())).isTrue();
    }

    @Test
    void addExpenseParsesDifferentDatePatterns() {
        when(accountService.findDefaultAccount(111L)).thenReturn(account);
        service.addExpense(111L, new BigDecimal("1"), ExpenseCategory.HOME, "a", "25.01.2025");
        service.addExpense(111L, new BigDecimal("1"), ExpenseCategory.HOME, "b", "05/10/2025");
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<Transaction> saved = transactionCaptor.getAllValues();
        Instant firstExpected = LocalDate.of(2025,1,25).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant secondExpected = LocalDate.of(2025,10,5).atStartOfDay(ZoneOffset.UTC).toInstant();
        assertThat(saved.get(0).getOperationTime()).isEqualTo(firstExpected);
        assertThat(saved.get(1).getOperationTime()).isEqualTo(secondExpected);
    }

    @Test
    void addExpenseParsesIsoInstant() {
        Instant now = Instant.now();
        when(accountService.findDefaultAccount(111L)).thenReturn(account);
        service.addExpense(111L, new BigDecimal("2"), ExpenseCategory.HOME, null, now.toString());
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getOperationTime()).isEqualTo(now);
    }

    @Test
    void addExpenseInvalidDateFallsBackToCurrentTime() {
        when(accountService.findDefaultAccount(111L)).thenReturn(account);
        Instant before = Instant.now();
        service.addExpense(111L, new BigDecimal("3"), ExpenseCategory.HOME, null, "not-a-date");
        Instant after = Instant.now();
        verify(transactionRepository).save(transactionCaptor.capture());
        Instant op = transactionCaptor.getValue().getOperationTime();
        assertThat(op).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void addExpenseWithNullChatIdThrowsException() {
        assertThatThrownBy(() -> service.addExpense(null, BigDecimal.ONE, ExpenseCategory.OTHER, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chatId");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteLastTransactionDeletesMostRecent() {
        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        t2.setPostedTime(Instant.now());
        when(userService.findByChatId(111L)).thenReturn(user);
        when(transactionRepository.findByOwnerOrderByPostedTimeDesc(user)).thenReturn(List.of(t2, t1));
        service.deleteLastTransaction(111L);
        verify(transactionRepository).delete(t2);
    }

    @Test
    void deleteLastTransactionNoTransactionsThrowsNotFound() {
        assertThatThrownBy(() -> service.deleteLastTransaction(111L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Нет транзакций");
    }

    @Test
    void parseDateOrNowReturnsCurrentInstantForNull() throws Exception {
        Method m = TransactionServiceImpl.class.getDeclaredMethod("parseDateOrNow", String.class);
        m.setAccessible(true);
        Instant before = Instant.now();
        Instant parsed = (Instant) m.invoke(service, (Object) null);
        Instant after = Instant.now();
        assertThat(parsed).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void parseDateOrNowReturnsCurrentInstantForBlank() throws Exception {
        Method m = TransactionServiceImpl.class.getDeclaredMethod("parseDateOrNow", String.class);
        m.setAccessible(true);
        Instant before = Instant.now();
        Instant parsed = (Instant) m.invoke(service, "   ");
        Instant after = Instant.now();
        assertThat(parsed).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void parseDateOrNowParsesIsoLocalDateTime() throws Exception {
        Method m = TransactionServiceImpl.class.getDeclaredMethod("parseDateOrNow", String.class);
        m.setAccessible(true);
        String isoLocal = "2025-10-05T14:30:45"; // ISO_LOCAL_DATE_TIME
        Instant parsed = (Instant) m.invoke(service, isoLocal);
        Instant expected = LocalDateTime.parse(isoLocal).toInstant(ZoneOffset.UTC);
        assertThat(parsed).isEqualTo(expected);
    }

    @Test
    void statsMethodsReturnNotImplementedPlaceholders() {
        assertThat(service.getTodayStats(111L)).isEqualTo("Функция пока не реализована");
        assertThat(service.getMonthStats(111L)).isEqualTo("Функция пока не реализована");
        assertThat(service.getYearStats(111L)).isEqualTo("Функция пока не реализована");
        assertThat(service.getCategoryStats(111L, ExpenseCategory.GROCERIES, "MONTH"))
                .isEqualTo("Функция пока не реализована");
    }
}
