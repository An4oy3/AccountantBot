package com.example.demo.service.impl;

import com.example.demo.service.BotCommandHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotCommandRegistryImplTest {

    private Update validUpdate(String text) {
        Update u = new Update();
        Message m = new Message();
        Chat c = new Chat();
        c.setId(123L);
        m.setChat(c);
        m.setText(text);
        u.setMessage(m);
        return u;
    }

    @Test
    void processReturnsEmptyWhenUpdateInvalid() {
        BotCommandHandler h = mock(BotCommandHandler.class);
        BotCommandRegistryImpl registry = new BotCommandRegistryImpl(List.of(h));
        Update invalid = new Update(); // no message / callback -> invalid
        Optional<SendMessage> result = registry.process(invalid);
        assertThat(result).isEmpty();
        verifyNoInteractions(h);
    }

    @Test
    void processReturnsFirstHandlerResultAndStopsFurtherProcessing() {
        BotCommandHandler h1 = mock(BotCommandHandler.class);
        BotCommandHandler h2 = mock(BotCommandHandler.class);
        when(h1.supports(any())).thenReturn(true);
        when(h1.handle(eq(123L), any())).thenReturn(new SendMessage("123", "ok1"));
        BotCommandRegistryImpl registry = new BotCommandRegistryImpl(List.of(h1, h2));
        Optional<SendMessage> result = registry.process(validUpdate("cmd"));
        assertThat(result).isPresent();
        assertThat(result.get().getText()).isEqualTo("ok1");
        verify(h1).handle(eq(123L), eq("cmd"));
        verify(h2, never()).supports(any());
    }

    @Test
    void processSkipsHandlersWithSupportsException() {
        BotCommandHandler bad = mock(BotCommandHandler.class);
        BotCommandHandler good = mock(BotCommandHandler.class);
        when(bad.supports(any())).thenThrow(new RuntimeException("boom"));
        when(good.supports(any())).thenReturn(true);
        when(good.handle(anyLong(), any())).thenReturn(new SendMessage("123", "good"));
        BotCommandRegistryImpl registry = new BotCommandRegistryImpl(List.of(bad, good));
        Optional<SendMessage> result = registry.process(validUpdate("cmd"));
        assertThat(result).isPresent();
        assertThat(result.get().getText()).isEqualTo("good");
        verify(good).handle(eq(123L), eq("cmd"));
    }

    @Test
    void processFallsBackToNextHandlerWhenFirstHandleThrows() {
        BotCommandHandler h1 = mock(BotCommandHandler.class);
        BotCommandHandler h2 = mock(BotCommandHandler.class);
        when(h1.supports(any())).thenReturn(true);
        when(h1.handle(anyLong(), any())).thenThrow(new RuntimeException("fail"));
        when(h2.supports(any())).thenReturn(true);
        when(h2.handle(anyLong(), any())).thenReturn(new SendMessage("123", "ok2"));
        BotCommandRegistryImpl registry = new BotCommandRegistryImpl(List.of(h1, h2));
        Optional<SendMessage> result = registry.process(validUpdate("cmd"));
        assertThat(result).isPresent();
        assertThat(result.get().getText()).isEqualTo("ok2");
        verify(h2).handle(eq(123L), eq("cmd"));
    }

    @Test
    void processReturnsEmptyWhenNoHandlerSupports() {
        BotCommandHandler h1 = mock(BotCommandHandler.class);
        BotCommandHandler h2 = mock(BotCommandHandler.class);
        when(h1.supports(any())).thenReturn(false);
        when(h2.supports(any())).thenReturn(false);
        BotCommandRegistryImpl registry = new BotCommandRegistryImpl(List.of(h1, h2));
        Optional<SendMessage> result = registry.process(validUpdate("cmd"));
        assertThat(result).isEmpty();
        verify(h1).supports(any());
        verify(h2).supports(any());
        verify(h1, never()).handle(anyLong(), any());
        verify(h2, never()).handle(anyLong(), any());
    }
}

