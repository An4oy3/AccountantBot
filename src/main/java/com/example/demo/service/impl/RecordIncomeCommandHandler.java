package com.example.demo.service.impl;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.Category;
import com.example.demo.model.entity.CategoryType;
import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.BotMainMenuButton;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.service.*;
import com.example.demo.service.util.CategoryKeyboardHelper;
import com.example.demo.service.util.InlineCalendarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.example.demo.model.enums.DialogStateType.AWAITING_ACCOUNT_AND_DATE;
import static com.example.demo.model.enums.DialogStateType.AWAITING_SOURCE_FOR_INCOME;
import static com.example.demo.service.util.TelegramUpdateHelper.*;

@Component
@RequiredArgsConstructor
public class RecordIncomeCommandHandler implements BotCommandHandler {

    private static final String AMOUNT_PROMPT_TEXT = "Пожалуйста, введите сумму дохода:";
    private static final String DESCRIPTION_PROMPT_TEXT = "Пожалуйста, введите описание дохода. Если хотите пропустить, нажмите \"Продолжить\".";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\s*\\d+(?:[.,]\\d{1,2})?\\s*$");
    private static final String ACCOUNT_AND_DATE_PROMPT_TEXT = "Пожалуйста, выберите счёт и дату для расхода. Если хотите использовать счёт по умолчанию и сегодняшнюю дату, нажимите \"Продолжить\".";
    private static final String CONFIRMATION_PROMPT_TEXT = "Пожалуйста, подтвердите запись дохода:\nСумма: %s\nИсточник: %s\nОписание: %s";
    private static final String SUCCESS_PROMPT_TEXT = "Доход в размере %s успешно записан. Источник дохода - %s.";
    private static final String DEFAULT_PROMPT_TEXT = "Извините, что-то пошло не так. Пожалуйста, начните запись дохода заново.";
    private static final Set<DialogStateType> SUPPORTED_STATES = Set.of(
            DialogStateType.AWAITING_AMOUNT,
            DialogStateType.AWAITING_DESCRIPTION,
            DialogStateType.AWAITING_CONFIRMATION,
            AWAITING_SOURCE_FOR_INCOME,
            AWAITING_ACCOUNT_AND_DATE
    );

    private final DialogStateService dialogStateService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;


    @Override
    public boolean supports(Update update) {
        if (!isValid(update)) {
            return false;
        }
        Long chatId = getChatId(update);
        String text = getEffectiveText(update);
        DialogStateData currentState = dialogStateService.getState(chatId);

        return text.equalsIgnoreCase(BotMainMenuButton.RECORD_INCOME.getText())
                || (currentState.isIncome() && SUPPORTED_STATES.contains(currentState.getState()));
    }

    @Override
    public SendMessage handle(Long chatId, String message) {
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Invalid message");
        }
        DialogStateData currentState = dialogStateService.getState(chatId);


        return switch (currentState.getState()) {
            case IDLE -> initHandler(chatId, currentState);
            case AWAITING_AMOUNT -> amountHandler(chatId, message, currentState);
            case AWAITING_SOURCE_FOR_INCOME -> IncomeSorceHandler(chatId, message, currentState);
            case AWAITING_ACCOUNT_AND_DATE -> accountAndDateHandler(chatId, message, currentState);
            case AWAITING_DESCRIPTION -> descriptionHandler(chatId, message, currentState);
            case AWAITING_CONFIRMATION -> confirmationHandler(chatId, message, currentState);
            default -> defaultHandler(chatId);
        };
    }


    private SendMessage initHandler(Long chatId, DialogStateData currentState) {
        currentState.setIncome(true);
        currentState.setState(DialogStateType.AWAITING_AMOUNT);
        dialogStateService.saveOrUpdate(currentState);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(AMOUNT_PROMPT_TEXT)
                .build();
    }

    private SendMessage amountHandler(Long chatId, String message, DialogStateData currentState) {
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
                .replyMarkup(CategoryKeyboardHelper.buildCategoryKeyboard(0, 6, categoryService.getCategoriesByType(CategoryType.INCOME)))
                .build();
    }

    private SendMessage IncomeSorceHandler(Long chatId, String message, DialogStateData currentState) {
        if (message.startsWith("category_page:")) {
            int page = Integer.parseInt(message.split(":")[1]);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Пожалуйста, выберите источник дохода:")
                    .replyMarkup(CategoryKeyboardHelper.buildCategoryKeyboard(page, 6, categoryService.getCategoriesByType(CategoryType.INCOME)))
                    .build();
        } else if (message.startsWith("category:")) {
            message = message.split(":")[1];
            String responseText;
            if (categoryService.categoryExists(message, chatId)) {
                Category selectedSource = categoryService.getCategoryByName(message);
                if (!selectedSource.isIncome()) {
                    return SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("Пожалуйста, выберите корректный источник дохода из списка.")
                            .replyMarkup(CategoryKeyboardHelper.buildCategoryKeyboard(0, 6, categoryService.getCategoriesByType(CategoryType.INCOME)))
                            .build();
                }
                currentState.setCategory(selectedSource);
                responseText = ACCOUNT_AND_DATE_PROMPT_TEXT;
            } else {
                Category newSource = categoryService.createCategory(message, "INCOME", chatId);
                currentState.setCategory(newSource);
                responseText = String.format("Создан новый источник дохода: %s\n\n%s", newSource.getName(), ACCOUNT_AND_DATE_PROMPT_TEXT);
            }
            currentState.setState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
            dialogStateService.saveOrUpdate(currentState);
            Account account = accountService.findOrCreateDefaultAccount(chatId);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(responseText)
                    .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(chatId, account, null))
                    .build();
        }
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Пожалуйста, выберите источник дохода:")
                .replyMarkup(CategoryKeyboardHelper.buildCategoryKeyboard(0, 6, categoryService.getCategoriesByType(CategoryType.INCOME)))
                .build();
    }

    // Fragment from `RecordExpenseCommandHandler.java`
    private SendMessage accountAndDateHandler(Long chatId, String message, DialogStateData currentState) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        if (message.startsWith("proceed_account_date:")) {
            return handleProceedAccountDate(chatId, message, currentState);
        }
        if (message.startsWith("account:")) {
            return handleAccountAction(chatId, message, currentState);
        }
        if (message.startsWith("date")) {
            return handleDateAction(chatId, message, currentState);
        }
        return null;
    }

    private SendMessage handleProceedAccountDate(Long chatId, String message, DialogStateData state) {
        // proceed_account_date:<accountId>:<yyyy-MM-dd>
        String[] parts = message.split(":", 3);
        if (parts.length < 3) {
            return null;
        }
        long accountId;
        LocalDate date;
        try {
            accountId = Long.parseLong(parts[1]);
            date = LocalDate.parse(parts[2]);
        } catch (Exception ex) {
            return null;
        }
        state.setAccount(accountService.findById(accountId).orElse(accountService.findOrCreateDefaultAccount(chatId)));
        state.setTransactionDate(date);
        state.setState(DialogStateType.AWAITING_DESCRIPTION);
        dialogStateService.saveOrUpdate(state);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(DESCRIPTION_PROMPT_TEXT)
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(List.of(
                                List.of(InlineKeyboardButton.builder()
                                        .text("Продолжить")
                                        .callbackData("skip_description")
                                        .build())
                        ))
                        .build())
                .build();
    }

    private SendMessage handleAccountAction(Long chatId, String message, DialogStateData state) {
        // account:choose | account:<id>
        String[] parts = message.split(":", 2);
        if (parts.length < 2) {
            return null;
        }
        String suffix = parts[1];
        if ("choose".equalsIgnoreCase(suffix)) {
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Выберите счёт для расхода:")
                    .replyMarkup(accountService.buildAccountSelectionKeyboard(chatId, false, "account:"))
                    .build();
        }
        Account account = accountService.getAccountsByChatId(chatId, false).stream()
                .filter(acc -> acc.getId().toString().equalsIgnoreCase(suffix))
                .findFirst()
                .orElse(null);
        if (account == null) {
            return null;
        }
        state.setAccount(account);
        dialogStateService.saveOrUpdate(state);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(chatId, account, state.getTransactionDate()))
                .build();
    }

    private SendMessage handleDateAction(Long chatId, String message, DialogStateData state) {
        // date:choose | date:accept:<yyyy-MM-dd> | navigation
        String[] parts = message.split(":", 3);
        if (parts.length < 2) {
            return null;
        }
        String action = parts[1];

        if ("choose".equalsIgnoreCase(action)) {
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Выберите дату для расхода:")
                    .replyMarkup(InlineCalendarUtil.buildCalendar(YearMonth.now(), null))
                    .build();
        }
        if ("accept".equalsIgnoreCase(action) && parts.length == 3) {
            try {
                LocalDate selectedDate = LocalDate.parse(parts[2]);
                state.setTransactionDate(selectedDate);
                dialogStateService.saveOrUpdate(state);
                Account account = state.getAccount() != null ? state.getAccount() : accountService.findOrCreateDefaultAccount(chatId);
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                        .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(chatId, account, selectedDate))
                        .build();
            } catch (Exception ignored) {
                return null;
            }
        }
        // Calendar navigation fallback
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                .replyMarkup(InlineCalendarUtil.handleCalendarNavigation(message))
                .build();
    }

    private SendMessage descriptionHandler(Long chatId, String message, DialogStateData currentState) {
        String comment = StringUtils.hasText(message) && !message.equalsIgnoreCase("skip_description") ? message.trim() : "";
        currentState.setComment(comment);
        currentState.setState(DialogStateType.AWAITING_CONFIRMATION);
        dialogStateService.saveOrUpdate(currentState);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(String.format(CONFIRMATION_PROMPT_TEXT, currentState.getAmount(), currentState.getCategory().getName(), comment))
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(List.of(
                                List.of(InlineKeyboardButton.builder().text("Подтвердить").callbackData("confirm").build()),
                                List.of(InlineKeyboardButton.builder().text("Отменить").callbackData("cancel").build())
                        ))
                        .build())
                .build();
    }

    private SendMessage confirmationHandler(Long chatId, String message, DialogStateData currentState) {
        String text = message.trim().toLowerCase();
        if (text.equals("confirm")) {
            dialogStateService.clearState(chatId);
            transactionService.addIncome(chatId, currentState.getAmount(), currentState.getCategory(), currentState.getComment(), currentState.getTransactionDate() != null ? currentState.getTransactionDate().toString() : null, currentState.getAccount());

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(String.format(SUCCESS_PROMPT_TEXT, currentState.getAmount(), currentState.getCategory().getName()))
                    .build();
        } else if (text.equals("cancel")) {
            dialogStateService.clearState(chatId);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Запись дохода отменена.")
                    .build();
        } else {
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(String.format(CONFIRMATION_PROMPT_TEXT, currentState.getAmount(), currentState.getCategory().getName(), currentState.getComment()))
                    .build();
        }
    }

    private SendMessage defaultHandler(Long chatId) {
        dialogStateService.clearState(chatId);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(DEFAULT_PROMPT_TEXT)
                .build();
    }
}
