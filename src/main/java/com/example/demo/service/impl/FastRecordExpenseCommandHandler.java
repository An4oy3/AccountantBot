package com.example.demo.service.impl;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.Category;
import com.example.demo.model.entity.CategoryType;
import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.service.*;
import com.example.demo.service.util.CategoryKeyboardHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.demo.model.enums.BotMainMenuButton.RECORD_FAST_EXPENSE;
import static com.example.demo.service.util.TelegramUpdateHelper.*;

@Service
@RequiredArgsConstructor
public class FastRecordExpenseCommandHandler implements BotCommandHandler {

    private static final String DEFAULT_PROMPT_TEXT = "Неизвестная команда. Пожалуйста, начните с главного меню.";
    private static final String CATEGORY_PROMPT_TEXT = "Пожалуйста, выберите категорию, используя кнопки ниже.";
    private final static String INITIAL_MESSAGE_HTML =  """
                    ✍️ Введите расход в одну строку — просто и быстро!\n
                    <b>Формат:</b>\n
                    <code>&lt;сумма&gt; &lt;категория&gt; [комментарий] [дата] [счёт]</code>\n
                    <b>Примеры:</b>\n\n
                    <code>500 еда</code>
                    <code>450 кафе обед с коллегой</code>
                    <code>1200 транспорт такси 04.10</code>
                    <code>300 продукты ужин дома 03.10 PKO</code>
                    <i>Комментарий, дата и счёт — необязательны.</i>\n
                    Если дату не укажете — возьмём <b>сегодняшнюю</b>.\n
                    Если счёт не указан — используем <b>дефолтный(наличные)</b>.
                    """;

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
        String messageText = getEffectiveText(update);
        DialogStateType stateType = dialogStateService.getStateType(chatId);

        return RECORD_FAST_EXPENSE.getText().equals(messageText) ||
                stateType == DialogStateType.AWAITING_FOR_FAST_EXPENSE ||
                stateType == DialogStateType.AWAITING_CATEGORY_CLARIFICATION;
    }

    @Override
    public SendMessage handle(Long chatId, String message) {
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Invalid message");
        }

        DialogStateData currentState = dialogStateService.getState(chatId);

        return switch (currentState.getState()) {
            case IDLE -> initHandler(chatId, currentState);
            case AWAITING_FOR_FAST_EXPENSE -> fastRecordExpenseHandler(chatId, message, currentState);
            case AWAITING_CATEGORY_CLARIFICATION -> categoryHandler(chatId, message, currentState);
            default -> defaultHandler(chatId);
        };
    }


    private SendMessage initHandler(Long chatId, DialogStateData currentState) {
        currentState.setExpense(true);
        currentState.setState(DialogStateType.AWAITING_FOR_FAST_EXPENSE);
        dialogStateService.saveOrUpdate(currentState);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(INITIAL_MESSAGE_HTML)
                .parseMode("HTML")
                .build();
    }

    private SendMessage fastRecordExpenseHandler(Long chatId, String message, DialogStateData currentState) {
        if (message.equals("/cancel")) {
            dialogStateService.clearState(chatId);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("❌ Операция отменена. Воспользуйтесь главным меню.")
                    .build();
        }
        ParseResult parseResult;
        try {
            parseResult = parseFastExpenseInput(chatId, message);
        } catch (IllegalArgumentException ex) {
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("⚠️ Не смог разобрать Ваше сообщение " + ex.getMessage() + "\n\n" + "Попробуйте снова: <code>500 еда</code> или введите /cancel")
                    .parseMode("HTML")
                    .build();
        }
        currentState.setAmount(parseResult.amount());
        currentState.setComment(parseResult.comment());
        currentState.setTransactionDate(parseResult.date());
        currentState.setAccount(parseResult.account());
        dialogStateService.saveOrUpdate(currentState);

        // Находим категорию (глобальные и пользовательские). CategoryService#getCategoryByName кидает исключение если не найдена.
        Category category;
        try {
            category = categoryService.getCategoryByName(parseResult.category());
        } catch (Exception e) {
            // Попытка подобрать из доступных по нечёткому совпадению (начало строки)
            List<Category> similarCategory = categoryService.getSimilarCategory(parseResult.category(), chatId);
            if (!similarCategory.isEmpty()) {
                dialogStateService.setDialogStateType(chatId, DialogStateType.AWAITING_CATEGORY_CLARIFICATION);
            }
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("❌ Категория не найдена: <b>" + escape(parseResult.category()) + "</b>" +
                            (!similarCategory.isEmpty() ? CATEGORY_PROMPT_TEXT : "Пожалуйста повторите ввод или введите другую категорию."))
                    .parseMode("HTML")
                    .replyMarkup(CategoryKeyboardHelper.buildCategoryKeyboard(0, similarCategory.size(), similarCategory))
                    .build();
        }

        String dateStr = parseResult.date() != null ? parseResult.date().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;

        transactionService.addExpense(chatId, parseResult.amount(), category, parseResult.comment(), dateStr, parseResult.account);

        //dialogStateService.clearState(chatId);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(successMessage(currentState))
                .parseMode("HTML")
                .build();
    }

    private SendMessage categoryHandler(Long chatId, String message, DialogStateData currentState) {
        if (message.startsWith("category_page:")) {
            int page = Integer.parseInt(message.split(":")[1]);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(CATEGORY_PROMPT_TEXT)
                    .replyMarkup(CategoryKeyboardHelper.buildCategoryKeyboard(page, 6, categoryService.getCategoriesByType(CategoryType.EXPENSE)))
                    .build();
        } else if(message.startsWith("category:")) {
            message = message.split(":")[1];
            if (categoryService.categoryExists(message, chatId)) {
                Category selectedCategory = categoryService.getCategoryByName(message);
                transactionService.addExpense(chatId, currentState.getAmount(), selectedCategory, currentState.getComment(), currentState.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE), currentState.getAccount());
            }
            dialogStateService.clearState(chatId);
            dialogStateService.setDialogStateType(chatId, DialogStateType.AWAITING_FOR_FAST_EXPENSE);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(successMessage(currentState))
                    .parseMode("HTML")
                    .build();
        }
        return new SendMessage();
    }

    private SendMessage defaultHandler(Long chatId) {
        dialogStateService.clearState(chatId);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(DEFAULT_PROMPT_TEXT)
                .build();
    }

    // ===================== PARSER =====================
    /**
     * Формат: <сумма> <категория> [комментарий...] [дата] [счёт]
     * Правила:
     *  - сумма: первый токен, число (допустимы , или . как разделитель)
     *  - категория: один или несколько следующих токенов, совпадающих с известной категорией (берём самое длинное совпадение)
     *  - дата (опционально): токен формата d.M или d.M.yyyy (если без года — текущий)
     *  - счёт (опционально): совпадение с именем счёта пользователя в конце
     *  - комментарий: всё между категорией и датой/счётом
     */
    private ParseResult parseFastExpenseInput(Long chatId, String raw) {
        String input = raw.trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Пустое сообщение");
        }
        List<String> tokens = Arrays.stream(input.split("\\s+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());
        if (tokens.size() < 2) {
            throw new IllegalArgumentException("Нужно минимум два слова: сумма и категория");
        }
        // 1. Amount
        BigDecimal amount;
        try {
            String amountToken = tokens.get(0).replace(',', '.');
            amount = new BigDecimal(amountToken);
            if (amount.signum() <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Некорректная сумма: '" + tokens.get(0) + "'");
        }

        // 2. Категория (самое длинное совпадение по названию среди expense категорий)
        List<Category> expenseCategories = categoryService.getCategoriesByType(CategoryType.EXPENSE);
        // Карта нижний регистр -> оригинальное имя
        Map<String, String> categoryNames = expenseCategories.stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), Category::getName, (a,b)->a));

        String categoryFound = null;
        int categoryEndIndex = -1; // индекс последнего токена категории
        int maxLen = tokens.size() - 1; // без первого (amount)
        for (int end = tokens.size() - 1; end >= 1; end--) {
            for (int len = Math.min(4, end); len >= 1; len--) {
                int start = end - len + 1;
                if (start < 1) continue; // пропускаем amount
                String candidate = String.join(" ", tokens.subList(start, end + 1)).toLowerCase(Locale.ROOT);
                if (categoryNames.containsKey(candidate)) {
                    categoryFound = categoryNames.get(candidate);
                    categoryEndIndex = end;
                    break; // берём первое найденное справа
                }
            }
            if (categoryFound != null) break;
        }
        if (categoryFound == null) {
            // fallback: взять второй токен как категорию (возможно пользователь создаст позже)
            categoryFound = tokens.get(1);
            categoryEndIndex = 1;
        }

        // 3. Попытка распознать дату (последний или предпоследний токен) формата d.M или d.M.yyyy
        Pattern datePatternShort = Pattern.compile("^(\\d{1,2})[./](\\d{1,2})(?:[./](\\d{2,4}))?$");
        LocalDate date = null;
        int dateTokenIndex = -1;
        for (int i = tokens.size() - 1; i > categoryEndIndex; i--) {
            String t = tokens.get(i);
            var m = datePatternShort.matcher(t);
            if (m.matches()) {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                String yearPart = m.group(3);
                int year = (yearPart == null) ? LocalDate.now().getYear() : normalizeYear(yearPart);
                try {
                    date = LocalDate.of(year, month, day);
                    dateTokenIndex = i;
                } catch (DateTimeParseException e) {
                    // игнорируем и продолжаем
                }
                break;
            }
        }
        if (date == null) {
            date = LocalDate.now();
        }

        // 4. Определение счёта (сравниваем суффикс токенов с именем счёта)
        String accountName = null;
        Account account = null;
        List<String> accountNames = accountService.getAccountsByChatId(chatId, false).stream()
                .map(a -> a.getName().toLowerCase(Locale.ROOT))
                .toList();
        int accountStartIndex = -1;
        for (int i = tokens.size() - 1; i > categoryEndIndex; i--) {
            if (i == dateTokenIndex) continue; // пропускаем токен даты
            StringBuilder sb = new StringBuilder();
            for (int start = i; start > categoryEndIndex; start--) {
                if (start == dateTokenIndex) break; // если упираемся в дату
                sb.setLength(0);
                String candidate = String.join(" ", tokens.subList(start, i + 1));
                if (accountNames.contains(candidate.toLowerCase(Locale.ROOT))) {
                    accountName = candidate;
                    accountStartIndex = start;
                    break;
                }
            }
            if (accountName != null) {
                account = accountService.findByNameAndOwnerId(accountName, chatId)
                        .orElse(accountService.findOrCreateDefaultAccount(chatId));
                break;
            }
        }
        if (account == null) {
            account = accountService.findOrCreateDefaultAccount(chatId);
        }

        // 5. Комментарий — всё между категорией и (датой/счётом) без повторного включения этих токенов
        int commentStart = categoryEndIndex + 1;
        int commentEndExclusive = tokens.size();
        Set<Integer> exclude = new HashSet<>();
        if (dateTokenIndex >= 0) exclude.add(dateTokenIndex);
        if (accountStartIndex >= 0) {
            for (int i = accountStartIndex; i < tokens.size(); i++) exclude.add(i);
        }
        if (commentStart >= tokens.size()) {
            // нет комментария
        }
        String comment = null;
        if (commentStart < tokens.size()) {
            List<String> commentTokens = new ArrayList<>();
            for (int i = commentStart; i < commentEndExclusive; i++) {
                if (exclude.contains(i)) continue;
                commentTokens.add(tokens.get(i));
            }
            if (!commentTokens.isEmpty()) {
                comment = String.join(" ", commentTokens);
            }
        }

        return new ParseResult(amount, categoryFound, comment, date, account);
    }

    private int normalizeYear(String yearPart) {
        if (yearPart.length() == 2) {
            int yy = Integer.parseInt(yearPart);
            int currentCentury = LocalDate.now().getYear() / 100 * 100;
            return currentCentury + yy; // простая логика: 2025 -> 2000 + yy
        }
        return Integer.parseInt(yearPart);
    }

    private String escape(String s) {
        if (s == null) return null;
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String successMessage(DialogStateData state) {
        StringBuilder sb = new StringBuilder("✅ Расход записан: <b>")
                .append(state.getAmount().stripTrailingZeros().toPlainString())
                .append("</b> ");
        if (state.getCategory() != null) {
            sb.append(escape(state.getCategory().getName()));
        }
        if (state.getComment() != null && !state.getComment().isBlank()) {
            sb.append(" — ").append(escape(state.getComment()));
        }
        if (state.getTransactionDate() != null) {
            sb.append(" (").append(state.getTransactionDate().format(DateTimeFormatter.ofPattern("dd.MM"))).append(")");
        }
        if (state.getAccount() != null) {
            sb.append(" \nСчёт: ").append(escape(state.getAccount().getDisplayName()));
        }
        return sb.toString();
    }

    // Используем Java record для компактного хранения результата парсинга
    private record ParseResult(BigDecimal amount, String category, String comment, LocalDate date, Account account) {}
}
