package com.example.demo.model.telegram;

import com.example.demo.service.BotMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountantTelegramBotTest {

    @Mock
    BotMessageService botMessageService;

    private Update buildUpdate(Long chatId, String text) {
        Update u = new Update();
        Message m = new Message();
        Chat c = new Chat();
        c.setId(chatId);
        m.setChat(c);
        m.setText(text);
        u.setMessage(m);
        return u;
    }

    @Test
    void getBotCredentialsReturnProvidedValues() {
        AccountantTelegramBot bot = new AccountantTelegramBot("user_name", "tok123", botMessageService);
        assertThat(bot.getBotUsername()).isEqualTo("user_name");
        assertThat(bot.getBotToken()).isEqualTo("tok123");
    }

    @Test
    void onUpdateReceivedExecutesMessageWhenPresent() throws Exception {
        AccountantTelegramBot bot = Mockito.spy(new AccountantTelegramBot("u","t", botMessageService));
        Update upd = buildUpdate(1L, "hi");
        SendMessage outbound = new SendMessage("1", "hello");
        when(botMessageService.handleUpdate(upd)).thenReturn(Optional.of(outbound));
        doReturn(new Message()).when(bot).execute(outbound);
        bot.onUpdateReceived(upd);
        verify(bot).execute(outbound);
    }

    @Test
    void onUpdateReceivedSkipsExecuteWhenEmptyOptional() throws Exception {
        AccountantTelegramBot bot = Mockito.spy(new AccountantTelegramBot("u","t", botMessageService));
        Update upd = buildUpdate(2L, "hi");
        when(botMessageService.handleUpdate(upd)).thenReturn(Optional.empty());
        bot.onUpdateReceived(upd);
        verify(bot, never()).execute(any(SendMessage.class));
    }

    @Test
    void onUpdateReceivedCatchesExecuteException() throws Exception {
        AccountantTelegramBot bot = Mockito.spy(new AccountantTelegramBot("u","t", botMessageService));
        Update upd = buildUpdate(3L, "hi");
        SendMessage sm = new SendMessage("3", "reply");
        when(botMessageService.handleUpdate(upd)).thenReturn(Optional.of(sm));
        doThrow(new TelegramApiException("fail")).when(bot).execute(sm);
        assertDoesNotThrow(() -> bot.onUpdateReceived(upd));
    }

    @Test
    void onUpdateReceivedCatchesServiceException() {
        AccountantTelegramBot bot = new AccountantTelegramBot("u","t", botMessageService);
        Update upd = buildUpdate(4L, "boom");
        when(botMessageService.handleUpdate(upd)).thenThrow(new RuntimeException("err"));
        assertDoesNotThrow(() -> bot.onUpdateReceived(upd));
    }
}

