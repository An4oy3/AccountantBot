package com.example.demo.service.impl;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.BotMainMenuButton;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.model.enums.ExpenseCategory;
import com.example.demo.service.AccountService;
import com.example.demo.service.DialogStateService;
import com.example.demo.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordExpenseCommandHandlerTest {

    @Mock
    DialogStateService dialogStateService;
    @Mock
    TransactionService transactionService;
    @Mock
    AccountService accountService;

    @InjectMocks
    RecordExpenseCommandHandler handler;

    @Captor
    ArgumentCaptor<DialogStateData> stateCaptor;

    private static final Long CHAT_ID = 12345L;

    private DialogStateData newState(DialogStateType type) {
        DialogStateData d = new DialogStateData();
        d.setChatId(CHAT_ID);
        d.setState(type);
        d.setIsExpense(true);
        return d;
    }

    private Account defaultAccount() {
        Account acc = new Account();
        acc.setName("Main");
        acc.setCurrency("RUB");
        return acc;
    }

    private void setId(Account acc, long id) {
        try {
            java.lang.reflect.Field f = acc.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(acc, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void supportsReturnsTrueWhenInSupportedStateRegardlessOfText() {
        Update update = new Update();
        Message msg = new Message();
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        msg.setChat(chat);
        msg.setText("some random text");
        update.setMessage(msg);
        DialogStateData state = newState(DialogStateType.AWAITING_AMOUNT);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        when(dialogStateService.getStateType(CHAT_ID)).thenReturn(DialogStateType.AWAITING_AMOUNT);
        assertThat(handler.supports(update)).isTrue();
    }

    @Test
    void supportsReturnsTrueWhenCallbackAndAwaitingCategory() {
        Update update = new Update();
        CallbackQuery cq = new CallbackQuery();
        cq.setId("1");
        User tUser = new User();
        tUser.setId(CHAT_ID);
        cq.setFrom(tUser);
        cq.setData("category:GROCERIES");
        update.setCallbackQuery(cq);
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        when(dialogStateService.getStateType(CHAT_ID)).thenReturn(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        assertThat(handler.supports(update)).isTrue();
    }

    @Test
    void supportsReturnsTrueWhenAwaitingAccountAndDateAndValidCategoryText() {
        Update update = new Update();
        Message msg = new Message();
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        msg.setChat(chat);
        msg.setText("GROCERIES");
        update.setMessage(msg);
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        when(dialogStateService.getStateType(CHAT_ID)).thenReturn(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        assertThat(handler.supports(update)).isTrue();
    }

    @Test
    void supportsReturnsFalseWhenAwaitingAccountAndDateAndInvalidCategoryText() {
        Update update = new Update();
        Message msg = new Message();
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        msg.setChat(chat);
        msg.setText("NOT_A_CATEGORY");
        update.setMessage(msg);
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        when(dialogStateService.getStateType(CHAT_ID)).thenReturn(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        assertThat(handler.supports(update)).isFalse();
    }

    @Test
    void supportsReturnsFalseWhenNoConditionsMatch() {
        Update update = new Update();
        Message msg = new Message();
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        msg.setChat(chat);
        msg.setText("unrelated input");
        update.setMessage(msg);
        DialogStateData state = newState(DialogStateType.IDLE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        when(dialogStateService.getStateType(CHAT_ID)).thenReturn(DialogStateType.IDLE);
        assertThat(handler.supports(update)).isFalse();
    }

    @Test
    void supportsReturnsTrueForMainMenuButtonText() {
        Update update = new Update();
        Message msg = new Message();
        msg.setText(BotMainMenuButton.RECORD_EXPENSE.getText());
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        msg.setChat(chat);
        update.setMessage(msg);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(newState(DialogStateType.IDLE));
        assertThat(handler.supports(update)).isTrue();
    }

    @Test
    void handleInvalidAmountReturnsValidationMessage() {
        DialogStateData state = newState(DialogStateType.AWAITING_AMOUNT);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "abc");
        assertThat(resp.getText()).contains("Пожалуйста, введите корректную сумму");
        verify(dialogStateService, never()).saveOrUpdate(any());
    }

    @Test
    void handleValidAmountTransitionsToCategorySelection() {
        DialogStateData state = newState(DialogStateType.AWAITING_AMOUNT);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "123.45");
        assertThat(resp.getText()).contains("Сумма записана: 123.45");
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        DialogStateData saved = stateCaptor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(saved.getState()).isEqualTo(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
    }

    @Test
    void handleCategorySelectionMovesToAccountAndDate() {
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        state.setAmount(new BigDecimal("10"));
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        when(accountService.findDefaultAccount(CHAT_ID)).thenReturn(defaultAccount());
        SendMessage resp = handler.handle(CHAT_ID, "category:TRANSPORTATION");
        assertThat(resp.getText()).contains("Пожалуйста, выберите счёт и дату");
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getState()).isEqualTo(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        assertThat(stateCaptor.getValue().getCategory()).isEqualTo(ExpenseCategory.TRANSPORTATION);
    }

    @Test
    void handleAccountChooseBranchReturnsAccountSelectionKeyboard() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        when(accountService.buildAccountSelectionKeyboard(eq(CHAT_ID), eq(false), anyString())).thenReturn(null);
        SendMessage resp = handler.handle(CHAT_ID, "account:choose");
        assertThat(resp.getText()).isEqualTo("Выберите счёт для расхода:");
        verify(accountService).buildAccountSelectionKeyboard(CHAT_ID, false, "account:");
    }

    @Test
    void handleProceedAccountDateSetsAccountAndMovesToDescription() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        Account def = defaultAccount();
        when(accountService.findById(999L)).thenReturn(Optional.empty());
        when(accountService.findDefaultAccount(CHAT_ID)).thenReturn(def);
        LocalDate date = LocalDate.now();
        SendMessage resp = handler.handle(CHAT_ID, "proceed_account_date:999:" + date);
        assertThat(resp.getText()).contains("Если хотите, можете добавить комментарий");
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        DialogStateData saved = stateCaptor.getValue();
        assertThat(saved.getAccount()).isSameAs(def);
        assertThat(saved.getTransactionDate()).isEqualTo(date);
        assertThat(saved.getState()).isEqualTo(DialogStateType.AWAITING_DESCRIPTION);
    }

    @Test
    void handleProceedAccountDateUsesFoundAccountWhenExists() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        Account found = defaultAccount();
        setId(found, 77L);
        when(accountService.findById(77L)).thenReturn(java.util.Optional.of(found));
        LocalDate date = LocalDate.of(2025,1,1);
        SendMessage resp = handler.handle(CHAT_ID, "proceed_account_date:77:" + date);
        assertThat(resp.getText()).contains("Если хотите, можете добавить комментарий");
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        DialogStateData saved = stateCaptor.getValue();
        assertThat(saved.getAccount()).isSameAs(found);
        assertThat(saved.getTransactionDate()).isEqualTo(date);
        assertThat(saved.getState()).isEqualTo(DialogStateType.AWAITING_DESCRIPTION);
    }

    @Test
    void handleAccountIdSelectionSetsAccountAndDateNow() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        Account acc = defaultAccount();
        setId(acc, 42L);
        when(accountService.getAccountsByChatId(CHAT_ID, false)).thenReturn(java.util.List.of(acc));
        SendMessage resp = handler.handle(CHAT_ID, "account:42");
        assertThat(resp.getText()).contains("Пожалуйста, выберите счёт и дату");
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        DialogStateData saved = stateCaptor.getValue();
        assertThat(saved.getAccount()).isSameAs(acc);
    }

    @Test
    void handleAccountIdNotFoundReturnsNull() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        Account acc = defaultAccount();
        setId(acc, 1L);
        when(accountService.getAccountsByChatId(CHAT_ID, false)).thenReturn(java.util.List.of(acc));
        SendMessage resp = handler.handle(CHAT_ID, "account:999");
        assertThat(resp).isNull();
        verify(dialogStateService, never()).saveOrUpdate(any());
    }

    @Test
    void accountAndDateHandlerDateChooseReturnsCalendarPrompt() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "date:choose");
        assertThat(resp.getText()).isEqualTo("Выберите дату для расхода:");
    }

    @Test
    void accountAndDateHandlerDateAcceptSetsDateAndReturnsPrompt() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        Account acc = defaultAccount();
        setId(acc, 5L);
        state.setAccount(acc);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        LocalDate chosen = LocalDate.of(2025,5,5);
        SendMessage resp = handler.handle(CHAT_ID, "date:accept:" + chosen);
        assertThat(resp.getText()).contains("Пожалуйста, выберите счёт и дату");
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getTransactionDate()).isEqualTo(chosen);
    }

    @Test
    void accountAndDateHandlerDateNavigationReturnsPrompt() {
        DialogStateData state = newState(DialogStateType.AWAITING_ACCOUNT_AND_DATE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "date:prev:2025-01");
        assertThat(resp.getText()).contains("Пожалуйста, выберите счёт и дату");
    }

    @Test
    void handleDescriptionStoresCommentAndMovesToConfirmation() {
        DialogStateData state = newState(DialogStateType.AWAITING_DESCRIPTION);
        state.setAmount(new BigDecimal("5"));
        state.setCategory(ExpenseCategory.GROCERIES);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "Молоко");
        assertThat(resp.getText()).contains("Подтвердите запись расхода");
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        DialogStateData saved = stateCaptor.getValue();
        assertThat(saved.getComment()).isEqualTo("Молоко");
        assertThat(saved.getState()).isEqualTo(DialogStateType.AWAITING_CONFIRMATION);
    }

    @Test
    void handleConfirmationConfirmPersistsExpenseAndReturnsSuccess() {
        DialogStateData state = newState(DialogStateType.AWAITING_CONFIRMATION);
        state.setAmount(new BigDecimal("7.50"));
        state.setCategory(ExpenseCategory.ENTERTAINMENT);
        state.setComment("Кино");
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "confirm");
        assertThat(resp.getText()).contains("Расход в размере 7.50");
        verify(transactionService).addExpense(eq(CHAT_ID), eq(new BigDecimal("7.50")), eq(ExpenseCategory.ENTERTAINMENT), eq("Кино"), isNull());
        verify(dialogStateService).setDialogStateType(CHAT_ID, DialogStateType.IDLE);
    }

    @Test
    void handleConfirmationCancelReturnsCancelledMessage() {
        DialogStateData state = newState(DialogStateType.AWAITING_CONFIRMATION);
        state.setAmount(new BigDecimal("3"));
        state.setCategory(ExpenseCategory.GIFT);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "cancel");
        assertThat(resp.getText()).isEqualTo("Запись расхода отменена.");
        verify(dialogStateService).setDialogStateType(CHAT_ID, DialogStateType.IDLE);
        verify(transactionService, never()).addExpense(any(), any(), any(), any(), any());
    }

    @Test
    void handleConfirmationUnknownInputRePromptsAndDoesNotPersist() {
        DialogStateData state = newState(DialogStateType.AWAITING_CONFIRMATION);
        state.setAmount(new BigDecimal("11.00"));
        state.setCategory(ExpenseCategory.GROCERIES);
        state.setComment("Хлеб");
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "something_else");
        assertThat(resp.getText()).contains("Подтвердите запись расхода");
        assertThat(resp.getText()).contains("11.00");
        assertThat(resp.getText()).contains(ExpenseCategory.GROCERIES.getText());
        assertThat(resp.getText()).contains("Хлеб");
        verify(transactionService, never()).addExpense(any(), any(), any(), any(), any());
        verify(dialogStateService, never()).setDialogStateType(anyLong(), any());
    }

    @Test
    void handleEmptyMessageThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> handler.handle(CHAT_ID, " "));
    }

    @Test
    void supportsReturnsFalseWhenUpdateIsNull() {
        assertThat(handler.supports(null)).isFalse();
        verifyNoInteractions(dialogStateService);
    }

    @Test
    void supportsReturnsFalseWhenNoMessageAndNoCallback() {
        Update update = new Update();
        assertThat(handler.supports(update)).isFalse();
        verifyNoInteractions(dialogStateService);
    }

    @Test
    void supportsReturnsFalseWhenMessageHasNoChatId() {
        Update update = new Update();
        Message msg = new Message();
        msg.setText("anything");
        // chat intentionally not set -> invalid
        update.setMessage(msg);
        assertThat(handler.supports(update)).isFalse();
        verifyNoInteractions(dialogStateService);
    }

    @Test
    void handleIdleStateTriggersInitHandler() {
        DialogStateData state = newState(DialogStateType.IDLE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "start");
        assertThat(resp.getText()).isEqualTo("Введите сумму расхода:");
        verify(dialogStateService, never()).setDialogStateType(CHAT_ID, DialogStateType.AWAITING_AMOUNT);
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getState()).isEqualTo(DialogStateType.AWAITING_AMOUNT);
    }

    @Test
    void handleNullStateInitializesAndPrompts() {
        DialogStateData idle = newState(DialogStateType.IDLE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(null, idle);
        SendMessage resp = handler.handle(CHAT_ID, "anything");
        assertThat(resp.getText()).isEqualTo("Введите сумму расхода:");
        InOrder inOrder = inOrder(dialogStateService);
        inOrder.verify(dialogStateService).getState(CHAT_ID);
        inOrder.verify(dialogStateService).setDialogStateType(CHAT_ID, DialogStateType.IDLE);
        inOrder.verify(dialogStateService).getState(CHAT_ID);
        verify(dialogStateService).saveOrUpdate(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getState()).isEqualTo(DialogStateType.AWAITING_AMOUNT);
    }

    @Test
    void handleSuccessStateClearsStateAndReturnsUnknownCommand() {
        DialogStateData state = newState(DialogStateType.SUCCESS);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "anything");
        assertThat(resp.getText()).isEqualTo("Неизвестная команда. Пожалуйста, начните с главного меню.");
        verify(dialogStateService).clearState(CHAT_ID);
        verify(dialogStateService, never()).saveOrUpdate(any());
    }

    @Test
    void categoryPageNavigationReturnsPromptWithPrevAndNextButtons() {
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        state.setAmount(new BigDecimal("50"));
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "category_page:1");
        assertThat(resp.getText()).isEqualTo("Пожалуйста, выберите категорию, используя кнопки ниже.");
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) resp.getReplyMarkup();
        assertThat(markup).isNotNull();
        assertThat(markup.getKeyboard()).isNotEmpty();
        var navRow = markup.getKeyboard().get(markup.getKeyboard().size()-1);
        assertThat(navRow).hasSize(2);
        assertThat(navRow.get(0).getCallbackData()).isEqualTo("category_page:0");
        assertThat(navRow.get(1).getCallbackData()).isEqualTo("category_page:2");
    }

    @Test
    void categoryHandlerUnknownTextRePromptsWithoutStateChange() {
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        state.setAmount(new BigDecimal("12"));
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "some-unrelated-text");
        assertThat(resp.getText()).isEqualTo("Пожалуйста, выберите категорию, используя кнопки ниже.");
        assertThat(resp.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
        verify(dialogStateService, never()).saveOrUpdate(any());
        assertThat(state.getState()).isEqualTo(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
    }

    @Test
    void categoryHandlerInvalidCategoryAfterPrefixRePromptsWithoutStateChange() {
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        state.setAmount(new BigDecimal("15"));
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "category:INVALID_CAT");
        assertThat(resp.getText()).isEqualTo("Пожалуйста, выберите категорию, используя кнопки ниже.");
        verify(dialogStateService, never()).saveOrUpdate(any());
        assertThat(state.getState()).isEqualTo(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
    }

    @Test
    void categoryPageFirstPageShowsOnlyNextButton() {
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        state.setAmount(new BigDecimal("20"));
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "category_page:0");
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) resp.getReplyMarkup();
        var navRow = markup.getKeyboard().get(markup.getKeyboard().size()-1);
        assertThat(navRow).hasSize(1);
        assertThat(navRow.get(0).getCallbackData()).isEqualTo("category_page:1");
    }

    @Test
    void categoryPageLastPageShowsOnlyPrevButton() {
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        state.setAmount(new BigDecimal("25"));
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        SendMessage resp = handler.handle(CHAT_ID, "category_page:3");
        InlineKeyboardMarkup mk = (InlineKeyboardMarkup) resp.getReplyMarkup();
        var navRow = mk.getKeyboard().get(mk.getKeyboard().size()-1);
        assertThat(navRow).hasSize(1);
        assertThat(navRow.get(0).getCallbackData()).isEqualTo("category_page:2");
    }

    @Test
    void supportsReturnsTrueForAnyCallbackWhenAwaitingCategoryState() {
        DialogStateData state = newState(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        when(dialogStateService.getState(CHAT_ID)).thenReturn(state);
        // Возвращаем состояние IDLE для getStateType, чтобы вторая часть условия была false и сработала именно третья
        when(dialogStateService.getStateType(CHAT_ID)).thenReturn(DialogStateType.AWAITING_CATEGORY_FOR_EXPENSE);
        Update update = new Update();
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cb2");
        Message msg = new Message();
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        msg.setChat(chat);
        msg.setText("some text");
        cq.setMessage(msg);
        cq.setData("some_non_category_callback");
        update.setCallbackQuery(cq);
        update.setMessage(msg);
        assertThat(handler.supports(update)).isTrue();
    }
}
