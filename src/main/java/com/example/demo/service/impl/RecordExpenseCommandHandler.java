package com.example.demo.service.impl;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.BotMainMenuButton;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.model.enums.ExpenseCategory;
import com.example.demo.service.AccountService;
import com.example.demo.service.BotCommandHandler;
import com.example.demo.service.DialogStateService;
import com.example.demo.service.TransactionService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.example.demo.model.enums.DialogStateType.AWAITING_ACCOUNT_AND_DATE;
import static com.example.demo.model.enums.DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE;
import static com.example.demo.service.util.TelegramUpdateHelper.*;

/**
 * Handles the main menu action "Record Expense" ("Записать расход").
 * When the user presses the corresponding reply keyboard button or types the same text, this handler is invoked.
 */
@Component
@RequiredArgsConstructor
public class RecordExpenseCommandHandler implements BotCommandHandler {

    private static final String AMOUNT_PROMPT_TEXT = "Введите сумму расхода:";
    private static final String DEFAULT_PROMPT_TEXT = "Неизвестная команда. Пожалуйста, начните с главного меню.";
    private static final String ACCOUNT_AND_DATE_PROMPT_TEXT = "Пожалуйста, выберите счёт и дату для расхода. Если хотите использовать счёт по умолчанию и сегодняшнюю дату, нажимите \"Продолжить\".";
    private static final String DESCRIPTION_PROMPT_TEXT = "Если хотите, можете добавить комментарий к расходу, или нажмите \"Продолжить\" чтобы пропустить этот шаг.";
    private static final String CATEGORY_PROMPT_TEXT = "Пожалуйста, выберите категорию, используя кнопки ниже.";
    private static final String CONFIRMATION_PROMPT_TEXT = "Подтвердите запись расхода в размере %s по категории %s с комментарием: \"%s\".\n\nЕсли всё верно, нажмите \"Подтвердить\", иначе \"Отменить\".";
    private static final String SUCCESS_PROMPT_TEXT = "Расход в размере %s по категории %s записан.";
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\s*\\d+(?:[.,]\\d{1,2})?\\s*$");
    private static final Set<DialogStateType> SUPPORTED_STATES = Set.of(
            DialogStateType.AWAITING_AMOUNT,
            DialogStateType.AWAITING_DESCRIPTION,
            DialogStateType.AWAITING_CONFIRMATION
    );


    private final DialogStateService dialogStateService;
    private final TransactionService transactionService;
    private final AccountService accountService;


    @Override
    public boolean supports(Update update) {
        if (!isValid(update)) {
            return false;
        }
        Long chatId = getChatId(update);
        DialogStateData state = dialogStateService.getState(chatId);
        String text = getEffectiveText(update);

        return text.equalsIgnoreCase(BotMainMenuButton.RECORD_EXPENSE.getText())
                || (SUPPORTED_STATES.contains(dialogStateService.getStateType(chatId)) && state.getIsExpense())
                || (hasCallback(update) && AWAITING_CATEGORY_FOR_EXPENSE.equals(state.getState()))
                || (AWAITING_ACCOUNT_AND_DATE.equals(state.getState()) && (ExpenseCategory.isValidCategory(text) ||
                                                                            text.startsWith("account:") ||
                                                                            text.startsWith("date:") ||
                                                                            text.startsWith("proceed_account_date:")));
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
            case IDLE -> initHandler(chatId, currentState);
            case AWAITING_AMOUNT -> amountHandler(chatId, message, currentState);
            case AWAITING_CATEGORY_FOR_EXPENSE -> categoryHandler(chatId, message, currentState);
            case AWAITING_ACCOUNT_AND_DATE -> accountAndDateHandler(chatId, message, currentState);
            case AWAITING_DESCRIPTION -> descriptionHandler(chatId, message, currentState);
            case AWAITING_CONFIRMATION -> confirmationHandler(chatId, message, currentState);
            default -> defaultHandler(chatId);
        };
    }


    private SendMessage amountHandler(Long chatId, String message, DialogStateData currentState) {
        if (!StringUtils.hasText(message) || !AMOUNT_PATTERN.matcher(message.trim()).matches()) {
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Пожалуйста, введите корректную сумму (только цифры, допустимы разделители \".\" или \",\", до двух знаков после разделителя).")
                    .build();
        }
        currentState.setAmount(new BigDecimal(message.trim().replace(',', '.')));
        currentState.setState(AWAITING_CATEGORY_FOR_EXPENSE);
        dialogStateService.saveOrUpdate(currentState);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Сумма записана: " + currentState.getAmount() + ". Пожалуйста выберите категорию расхода.")
                .replyMarkup(buildCategoryKeyboard(0, 6))
                .build();
    }

    private SendMessage categoryHandler(Long chatId, String text, DialogStateData currentState) {
        if (text.startsWith("category_page:")) {
            int page = Integer.parseInt(text.split(":")[1]);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(CATEGORY_PROMPT_TEXT)
                    .replyMarkup(buildCategoryKeyboard(page, 6))
                    .build();
        } else if(text.startsWith("category:")) {
            text = text.split(":")[1];

            if (ExpenseCategory.isValidCategory(text)) {
                currentState.setCategory(ExpenseCategory.valueOf(text));
                currentState.setState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
                dialogStateService.saveOrUpdate(currentState);
                Account account = accountService.findDefaultAccount(chatId);
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                        .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(chatId, account, null))
                        .build();
            }
        }

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(CATEGORY_PROMPT_TEXT)
                .replyMarkup(buildCategoryKeyboard(0, 6))
                .build();
    }

    private SendMessage descriptionHandler(Long chatId, String message, DialogStateData currentState) {
        String comment = StringUtils.hasText(message) && !message.equalsIgnoreCase("skip_description") ? message.trim() : "";
        currentState.setComment(comment);
        currentState.setState(DialogStateType.AWAITING_CONFIRMATION);
        dialogStateService.saveOrUpdate(currentState);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(String.format(CONFIRMATION_PROMPT_TEXT, currentState.getAmount(), currentState.getCategory().getText(), comment))
                .replyMarkup(buildConfirmationKeyboard())
                .build();
    }

    private SendMessage confirmationHandler(Long chatId, String message, DialogStateData currentState) {
        String text = message.trim().toLowerCase();
        if (text.equals("confirm")) {
            currentState.setIsComplete(true);
            // Here you would typically save the expense to the database
            dialogStateService.setDialogStateType(chatId, DialogStateType.IDLE);
            transactionService.addExpense(chatId, currentState.getAmount(), currentState.getCategory(), currentState.getComment(), null);

            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(String.format(SUCCESS_PROMPT_TEXT, currentState.getAmount(), currentState.getCategory().getText()))
                    .build();
        } else if (text.equals("cancel")) {
            dialogStateService.setDialogStateType(chatId, DialogStateType.IDLE);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Запись расхода отменена.")
                    .build();
        } else {
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(String.format(CONFIRMATION_PROMPT_TEXT, currentState.getAmount(), currentState.getCategory().getText(), currentState.getComment()))
                    .build();
        }
    }

    private SendMessage accountAndDateHandler(Long chatId, String message, DialogStateData currentState) {
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
            if (accountPrefix.equals("choose")) {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Выберите счёт для расхода:")
                        .replyMarkup(accountService.buildAccountSelectionKeyboard(chatId, false, "account:"))
                        .build();
            }
            Account account = accountService.getAccountsByChatId(chatId, false).stream()
                    .filter(acc -> accountPrefix.equalsIgnoreCase(acc.getId().toString()))
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
        } else if (StringUtils.hasText(message) && message.startsWith("date")) {
            String[] calenderData = message.split(":");
            String calPrefix = calenderData[1];
            if (calPrefix.equals("choose")) {
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Выберите дату для расхода:")
                        .replyMarkup(InlineCalendarUtil.buildCalendar(
                                YearMonth.now(),
                                null))
                        .build();
            } else if (calPrefix.equals("accept")) {
                LocalDate selectedDate = LocalDate.parse(calenderData[2]);
                currentState.setTransactionDate(selectedDate);
                dialogStateService.saveOrUpdate(currentState);
                Account selectedAccount = currentState.getAccount() != null ? currentState.getAccount() : accountService.findDefaultAccount(chatId);
                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(ACCOUNT_AND_DATE_PROMPT_TEXT)
                        .replyMarkup(InlineCalendarUtil.buildAccountAndDateKeyboard(chatId, selectedAccount, selectedDate))
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


    private SendMessage defaultHandler(Long chatId) {
        dialogStateService.clearState(chatId);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(DEFAULT_PROMPT_TEXT)
                .build();
    }

    private SendMessage initHandler(Long chatId, DialogStateData dialogStateData) {
        dialogStateData.setIsExpense(true);
        dialogStateData.setState(DialogStateType.AWAITING_AMOUNT);
        dialogStateService.saveOrUpdate(dialogStateData);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(AMOUNT_PROMPT_TEXT)
                .build();
    }

    private InlineKeyboardMarkup buildConfirmationKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(InlineKeyboardButton.builder().text("Подтвердить").callbackData("confirm").build()),
                        List.of(InlineKeyboardButton.builder().text("Отменить").callbackData("cancel").build())
                ))
                .build();
    }

    private InlineKeyboardMarkup buildCategoryKeyboard(int page, int pageSize) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        ExpenseCategory[] categories = ExpenseCategory.values();
        int totalCategories = (int) Arrays.stream(categories)
                .filter(c -> c != ExpenseCategory.NONE)
                .count();
        int totalPages = (int) Math.ceil((double) totalCategories / pageSize);

        int startIdx = page * pageSize;
        int endIdx = Math.min(startIdx + pageSize, totalCategories);

        List<ExpenseCategory> filtered = Arrays.stream(categories)
                .filter(c -> c != ExpenseCategory.NONE)
                .toList();

        for (int i = startIdx; i < endIdx; i++) {
            ExpenseCategory category = filtered.get(i);
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(category.getText())
                    .callbackData("category:" + category.name())
                    .build();
            rows.add(List.of(button));
        }

        List<InlineKeyboardButton> navRow = new ArrayList<>();
        if (page > 0) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("⬅️ Назад")
                    .callbackData("category_page:" + (page - 1))
                    .build());
        }
        if (page < totalPages - 1) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("Вперёд ➡️")
                    .callbackData("category_page:" + (page + 1))
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

