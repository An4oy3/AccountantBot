package com.example.demo.service.impl;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.BotMainMenuButton;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.model.enums.IncomeSource;
import com.example.demo.service.*;
import com.example.demo.service.util.InlineCalendarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.example.demo.model.enums.DialogStateType.AWAITING_SOURCE_FOR_INCOME;
import static com.example.demo.service.util.TelegramUpdateHelper.*;

@Component
@RequiredArgsConstructor
public class RecordIncomeCommandHandler implements BotCommandHandler {

    private static final String AMOUNT_PROMPT_TEXT = "Пожалуйста, введите сумму дохода:";
    private static final String DESCRIPTION_PROMPT_TEXT = "Пожалуйста, введите описание дохода. Если хотите пропустить, нажмите \"Продолжить\".";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\s*\\d+(?:[.,]\\d{1,2})?\\s*$");
    private static final String ACCOUNT_AND_DATE_PROMPT_TEXT = "Пожалуйста, выберите счёт и дату для расхода. Если хотите использовать счёт по умолчанию и сегодняшнюю дату, нажимите \"Продолжить\".";
    private static final Set<DialogStateType> SUPPORTED_STATES = Set.of(
            DialogStateType.AWAITING_AMOUNT,
            DialogStateType.AWAITING_DESCRIPTION,
            DialogStateType.AWAITING_CONFIRMATION
    );

    private final DialogStateService dialogStateService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final UserService userService;


    @Override
    public boolean supports(Update update) {
        if (!isValid(update)) {
            return false;
        }
        Long chatId = getChatId(update);
        String text = getEffectiveText(update);
        DialogStateData currentState = dialogStateService.getState(chatId);

        return text.equalsIgnoreCase(BotMainMenuButton.RECORD_INCOME.getText())
                || currentState.getIsIncome() && SUPPORTED_STATES.contains(currentState.getState())
                || (hasCallback(update) && AWAITING_SOURCE_FOR_INCOME.equals(currentState.getState()));
    }

    @Override
    public SendMessage handle(Long chatId, String message) {
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Invalid message");
        }
        DialogStateData currentState = dialogStateService.getState(chatId);


        return switch (currentState.getState()) {
            case IDLE -> initHandler(chatId, currentState);
            case AWAITING_AMOUNT -> handleAmountInput(chatId, message, currentState);
            case AWAITING_SOURCE_FOR_INCOME -> handleIncomeSourceInput(chatId, message, currentState);
            case AWAITING_ACCOUNT_AND_DATE -> handleAccountAndDateInput(chatId, message, currentState);
            case AWAITING_DESCRIPTION -> handleDescriptionInput(chatId, message);
            case AWAITING_CONFIRMATION -> handleConfirmationInput(chatId, message);
            default -> defaultHandler(chatId);
        };
    }


    private SendMessage initHandler(Long chatId, DialogStateData currentState) {
        currentState.setIsIncome(true);
        currentState.setState(DialogStateType.AWAITING_AMOUNT);
        dialogStateService.saveOrUpdate(currentState);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(AMOUNT_PROMPT_TEXT)
                .build();
    }

    private SendMessage handleAmountInput(Long chatId, String message, DialogStateData currentState) {
        if (!StringUtils.hasText(message) || !AMOUNT_PATTERN.matcher(message.trim()).matches()) {
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Пожалуйста, введите корректную сумму (только цифры, допустимы разделители \".\" или \",\", до двух знаков после разделителя).")
                    .build();
        }
        currentState.setAmount(new BigDecimal(message.trim().replace(',', '.')));
        currentState.setState(AWAITING_SOURCE_FOR_INCOME);
        dialogStateService.saveOrUpdate(currentState);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Пожалуйста, выберите источник дохода:")
                .replyMarkup(buildIncomeSourceKeyboard(0, 6))
                .build();
    }

    private SendMessage handleIncomeSourceInput(Long chatId, String message, DialogStateData currentState) {
        if (message.startsWith("source_page:")) {
            int page = Integer.parseInt(message.split(":")[1]);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Пожалуйста, выберите источник дохода:")
                    .replyMarkup(buildIncomeSourceKeyboard(page, 6))
                    .build();
        } else if (message.startsWith("source:")) {
            String categoryStr = message.split(":")[1];
            try {
                IncomeSource source = IncomeSource.valueOf(categoryStr);
                currentState.setIncomeSource(source);
                currentState.setState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
                dialogStateService.saveOrUpdate(currentState);
                Account account = accountService.findDefaultAccount(chatId);
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                        .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(chatId, account, null))
                        .build();
            } catch (IllegalArgumentException e) {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Пожалуйста, выберите корректный источник дохода из списка.")
                        .replyMarkup(buildIncomeSourceKeyboard(0, 6))
                        .build();
            }
        }
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Пожалуйста, выберите источник дохода:")
                .replyMarkup(buildIncomeSourceKeyboard(0, 6))
                .build();
    }

    private SendMessage handleDescriptionInput(Long chatId, String message) {
        return null;
    }

    private SendMessage handleAccountAndDateInput(Long chatId, String message, DialogStateData currentState) {
        if (StringUtils.hasText(message) && message.startsWith("proceed_account_date:")) {
            String[] parts = message.split(":");
            Long selectedAccountId = Long.parseLong(parts[1]);
            LocalDate selectedDate = LocalDate.parse(parts[2]);
            currentState.setAccount(accountService.findById(selectedAccountId).orElse(accountService.findDefaultAccount(chatId)));
            currentState.setTransactionDate(selectedDate);
            currentState.setState(DialogStateType.AWAITING_DESCRIPTION);
            dialogStateService.saveOrUpdate(currentState);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(DESCRIPTION_PROMPT_TEXT)
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboard(List.of(
                                    List.of(InlineKeyboardButton.builder().text("Продолжить").callbackData("skip_description").build())
                            ))
                            .build())
                    .build();
        } else if (StringUtils.hasText(message) && message.startsWith("account:")) {
            String accountPrefix = message.split(":")[1];
            if ("choose".equals(accountPrefix)) {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Выберите счёт для дохода:")
                        .replyMarkup(accountService.buildAccountSelectionKeyboard(chatId, false, "account:"))
                        .build();
            }
            Account account = accountService.getAccountsByChatId(chatId, false).stream()
                    .filter(acc -> acc.getId().toString().equals(accountPrefix))
                    .findFirst()
                    .orElse(null);
            if (account != null) {
                currentState.setAccount(account);
                dialogStateService.saveOrUpdate(currentState);
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                        .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(chatId, account, currentState.getTransactionDate()))
                        .build();
            }
        } else if (StringUtils.hasText(message) && message.startsWith("date:")) {
            String[] calenderData = message.split(":");
            String calPrefix = calenderData[1];
            if ("choose".equals(calPrefix)) {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Выберите дату для дохода:")
                        .replyMarkup(InlineCalendarUtil.buildCalendar(
                                YearMonth.now(),
                                null))
                        .build();
            } else if ("accept".equalsIgnoreCase(calPrefix)) {
                LocalDate selectedDate = LocalDate.parse(calenderData[2]);
                currentState.setTransactionDate(selectedDate);
                dialogStateService.saveOrUpdate(currentState);
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                        .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(
                                chatId,
                                currentState.getAccount() != null ? currentState.getAccount() : accountService.findDefaultAccount(chatId),
                                selectedDate))
                        .build();
            } else {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                        .replyMarkup(InlineCalendarUtil.handleCalendarNavigation(message))
                        .build();
            }
        }
        return null;
    }

    private SendMessage handleConfirmationInput(Long chatId, String message) {
         return null;
    }

    private SendMessage defaultHandler(Long chatId) {
        return null;
    }



    private ReplyKeyboard buildIncomeSourceKeyboard(int page, int pageSize) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        IncomeSource[] categories = IncomeSource.values();
        int totalCategories = (int) Arrays.stream(categories).count();
        int totalPages = (int) Math.ceil((double) totalCategories / pageSize);

        int startIdx = page * pageSize;
        int endIdx = Math.min(startIdx + pageSize, totalCategories);

        List<IncomeSource> filtered = Arrays.stream(categories).toList();

        for (int i = startIdx; i < endIdx; i++) {
            IncomeSource source = filtered.get(i);
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(source.getText())
                    .callbackData("source:" + source.name())
                    .build();
            rows.add(List.of(button));
        }

        List<InlineKeyboardButton> navRow = new ArrayList<>();
        if (page > 0) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("⬅️ Назад")
                    .callbackData("source_page:" + (page - 1))
                    .build());
        }
        if (page < totalPages - 1) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("Вперёд ➡️")
                    .callbackData("source_page:" + (page + 1))
                    .build());
        }
        if (!navRow.isEmpty()) {
            rows.add(navRow);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }
}
