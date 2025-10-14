package com.example.demo.service;

import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.DialogStateType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotMessageServiceTest {

    @Mock
    BotCommandRegistry commandRegistry;
    @Mock
    UserService userService;
    @Mock
    DialogStateService dialogStateService;

    @InjectMocks
    BotMessageService botMessageService;

    private Update buildTextUpdate(Long chatId, String text, boolean withUser) {
        Update u = new Update();
        Message m = new Message();
        Chat c = new Chat();
        c.setId(chatId);
        m.setChat(c);
        m.setText(text);
        if (withUser) {
            User user = new User();
            user.setId(chatId); // reuse chatId as user id for test simplicity
            user.setUserName("user" + chatId);
            user.setFirstName("First" + chatId);
            user.setLastName("Last" + chatId);
            m.setFrom(user);
        }
        u.setMessage(m);
        return u;
    }

    @Test
    void startCommandBuildsMenuClearsStateAndInitializesUserAndDialog() {
        Update upd = buildTextUpdate(123L, " /start  ", true);
        when(dialogStateService.getState(123L)).thenReturn(null); // triggers initialization
        when(userService.existsByChatId(123L)).thenReturn(false);
        Optional<SendMessage> respOpt = botMessageService.handleUpdate(upd);
        assertThat(respOpt).isPresent();
        SendMessage resp = respOpt.get();
        assertThat(resp.getText()).isEqualTo("Добро пожаловать! Выберите действие:");
        assertThat(resp.getReplyMarkup()).isInstanceOf(ReplyKeyboardMarkup.class);
        ReplyKeyboardMarkup km = (ReplyKeyboardMarkup) resp.getReplyMarkup();
        assertThat(km.getKeyboard()).hasSize(3);
        verify(dialogStateService).clearState(123L);
        verify(dialogStateService).setDialogStateType(123L, DialogStateType.IDLE);
        verify(userService).createUser(eq("user123"), eq("First123"), eq("Last123"), eq(123L));
        verify(commandRegistry, never()).process(any());
    }

    @Test
    void nonStartDelegatesToCommandRegistryWhenHandlerReturnsMessage() {
        Update upd = buildTextUpdate(200L, "stats", true);
        // existing dialog state => not null => skip init setDialogStateType
        when(dialogStateService.getState(200L)).thenReturn(new DialogStateData());
        when(userService.existsByChatId(200L)).thenReturn(true);
        SendMessage handlerMsg = new SendMessage("200", "handled");
        when(commandRegistry.process(upd)).thenReturn(Optional.of(handlerMsg));
        Optional<SendMessage> respOpt = botMessageService.handleUpdate(upd);
        assertThat(respOpt).containsSame(handlerMsg);
        verify(dialogStateService, never()).setDialogStateType(anyLong(), any());
        verify(dialogStateService, never()).clearState(anyLong());
        verify(commandRegistry).process(upd);
    }

    @Test
    void nonStartNoHandlerResponseReturnsEmpty() {
        Update upd = buildTextUpdate(300L, "unknown", true);
        when(dialogStateService.getState(300L)).thenReturn(new DialogStateData());
        when(userService.existsByChatId(300L)).thenReturn(true);
        when(commandRegistry.process(upd)).thenReturn(Optional.empty());
        Optional<SendMessage> respOpt = botMessageService.handleUpdate(upd);
        assertThat(respOpt).isNotEmpty();
    }

    @Test
    void invalidUpdateReturnsEmptyWithoutInteractions() {
        Update invalid = new Update();
        Optional<SendMessage> respOpt = botMessageService.handleUpdate(invalid);
        assertThat(respOpt).isEmpty();
        verifyNoInteractions(commandRegistry, userService, dialogStateService);
    }

    @Test
    void missingUserCausesExceptionDuringInitialization() {
        Update upd = buildTextUpdate(400L, "/start", false); // no from user
        assertThatThrownBy(() -> botMessageService.handleUpdate(upd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chat ID or User cannot be null");
    }
}

