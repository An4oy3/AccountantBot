package com.example.demo.service.impl;

import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.entity.Transaction;
import com.example.demo.model.entity.TransactionType;
import com.example.demo.model.enums.BotMainMenuButton;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.service.BotCommandHandler;
import com.example.demo.service.CategoryService;
import com.example.demo.service.DialogStateService;
import com.example.demo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.demo.service.util.TelegramUpdateHelper.*;

@Service
@RequiredArgsConstructor
public class StatisticsCommandHandler implements BotCommandHandler {

    private final DialogStateService dialogStateService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;

    @Override
    public boolean supports(Update update) {
        if (!isValid(update)) {
            return false;
        }
        String message = getEffectiveText(update);
        Long chatId = getChatId(update);
        DialogStateType currentState = dialogStateService.getStateType(chatId);
        return message.equalsIgnoreCase(BotMainMenuButton.STATISTICS.getText()) ||
                DialogStateType.AWAITING_STATS_PERIOD.equals(currentState);
    }

    @Override
    public SendMessage handle(Long chatId, String message) {
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Invalid message");
        }
        DialogStateData currentState = dialogStateService.getState(chatId);
        if (currentState == null) {
            dialogStateService.setDialogStateType(chatId, DialogStateType.IDLE);
            currentState = dialogStateService.getState(chatId);
        }
        return switch (currentState.getState()) {
            case IDLE -> initHandler(chatId, message, currentState);
            case AWAITING_STATS_PERIOD -> periodStatsHandler(chatId, message, currentState);
            default -> SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Пожалуйста, используйте меню для навигации.")
                    .build();
        };
    }

    private SendMessage initHandler(Long chatId, String message, DialogStateData currentState) {
        dialogStateService.setDialogStateType(chatId, DialogStateType.AWAITING_STATS_PERIOD);
        LocalDate now = LocalDate.now();
        String statsMessage = buildStatsMessage(chatId, now.withDayOfMonth(1), now);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(statsMessage)
                .replyMarkup(buildPeriodKeyboard())
                .build();
    }

    private SendMessage periodStatsHandler(Long chatId, String message, DialogStateData currentState) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        String period = message.split(":")[1];
        String stats = switch (period) {
            case "today" -> buildStatsMessage(chatId, now, now);
            case "month" -> buildStatsMessage(chatId, now.withDayOfMonth(1), now);
            case "last_month" -> buildStatsMessage(
                    chatId, now.minusMonths(1).withDayOfMonth(1),
                    now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth())
            );
            case "year" -> buildStatsMessage(chatId, now.withDayOfYear(1), now);
            case "last_year" -> buildStatsMessage(
                    chatId, now.minusYears(1).withDayOfYear(1),
                    now.minusYears(1).withDayOfYear(now.minusYears(1).lengthOfYear())
            );
            default -> "Неизвестный период. Пожалуйста, выберите период снова.";
        };
        stats += "\n\n\nВведите /start для возврата в главное меню.";
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(stats)
                .replyMarkup(buildPeriodKeyboard())
                .build();
    }

    private String buildStatsMessage(Long chatId, LocalDate startDate, LocalDate endDate) {
        StringBuilder stats = new StringBuilder("\uD83D\uDCCA Статистика — ");
        stats.append(startDate.getMonth()).append(" ").append(startDate.getYear());
        if (endDate != null && !endDate.equals(startDate) && endDate.isAfter(startDate)) {
            stats.append(" - ").append(endDate.getMonth()).append(" ").append(endDate.getYear());
        }
        stats.append("\n\n");

        List<Transaction> allByPeriod = transactionService.getAllByPeriod(chatId, startDate, endDate);
        stats.append("Всего транзакций: ").append(allByPeriod.size()).append("\n\n");

        BigDecimal expenses = allByPeriod.stream()
                .filter(t -> TransactionType.EXPENSE.equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        stats.append("• Расходы: ").append(expenses)
                .append(" PLN\n");

        BigDecimal incomes = allByPeriod.stream()
                .filter(t -> TransactionType.INCOME.equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        stats.append("• Доходы: ").append(incomes)
                .append(" PLN\n");

        stats.append("• Баланс: ").append(incomes.subtract(expenses))
                .append(" PLN\n\n\n");

        stats.append("Топ категорий (расходы):\n");

        AtomicInteger topCategoriesCount = new AtomicInteger();
        allByPeriod.stream()
                .filter(t -> TransactionType.EXPENSE.equals(t.getType()))
                .map(Transaction::getCategory)
                .distinct()
                .forEach(cat -> {
                    if (topCategoriesCount.getAndIncrement() >= 5) {
                        return;
                    }
                    BigDecimal total = allByPeriod.stream()
                            .filter(t -> TransactionType.EXPENSE.equals(t.getType()) && t.getCategory().equals(cat))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal::add)
                            .orElse(BigDecimal.ZERO);

                    stats.append("• ")
                            .append(cat.getName())
                            .append(": ").append(total)
                            .append(" PLN.(")
                            .append(expenses.compareTo(BigDecimal.ZERO) > 0
                                ? (total.multiply(BigDecimal.valueOf(100)).divide(expenses, 2, BigDecimal.ROUND_HALF_UP))
                                : "0")
                            .append("%)\n");

                });

        stats.append("\n\n");

        return stats.toString();
    }

    private InlineKeyboardMarkup buildPeriodKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                            .text("Сегодня")
                            .callbackData("stats_period:today")
                            .build(),
                        InlineKeyboardButton.builder()
                                .text("Этот месяц")
                                .callbackData("stats_period:month")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Прошлый месяц")
                                .callbackData("stats_period:last_month")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Этот год")
                                .callbackData("stats_period:year")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Прошлый год")
                                .callbackData("stats_period:last_year")
                                .build())))
                .build();

    }
}
