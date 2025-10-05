package com.example.demo.service.util;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.*;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramUpdateHelperTest {

    private Update messageUpdate(Long chatId, String text, boolean withFrom) {
        Update u = new Update();
        Message m = new Message();
        Chat c = new Chat();
        c.setId(chatId);
        m.setChat(c);
        m.setText(text);
        if (withFrom) {
            User from = new User();
            from.setId(chatId);
            from.setUserName("user" + chatId);
            m.setFrom(from);
        }
        u.setMessage(m);
        return u;
    }

    private Update callbackUpdate(Long fromId, String data) {
        Update u = new Update();
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cb1");
        User from = new User();
        from.setId(fromId);
        from.setUserName("cb" + fromId);
        cq.setFrom(from);
        cq.setData(data);
        u.setCallbackQuery(cq);
        return u;
    }

    @Test
    void isValidReturnsFalseForNull() {
        assertThat(TelegramUpdateHelper.isValid(null)).isFalse();
    }

    @Test
    void isValidReturnsTrueForTextMessageWithChat() {
        Update u = messageUpdate(10L, "hello", true);
        assertThat(TelegramUpdateHelper.isValid(u)).isTrue();
    }

    @Test
    void isValidReturnsFalseWhenMessageWithoutChat() {
        Update u = new Update();
        Message m = new Message();
        m.setText("hi");
        u.setMessage(m);
        assertThat(TelegramUpdateHelper.isValid(u)).isFalse();
    }

    @Test
    void isValidReturnsTrueForCallbackWithFrom() {
        Update u = callbackUpdate(22L, "data");
        assertThat(TelegramUpdateHelper.isValid(u)).isTrue();
    }

    @Test
    void hasTextMessageTrueWhenMessageHasText() {
        Update u = messageUpdate(1L, "text", true);
        assertThat(TelegramUpdateHelper.hasTextMessage(u)).isTrue();
    }

    @Test
    void hasTextMessageFalseWhenNoText() {
        Update u = messageUpdate(1L, null, true);
        assertThat(TelegramUpdateHelper.hasTextMessage(u)).isFalse();
    }

    @Test
    void hasCallbackTrueWhenCallbackPresent() {
        Update u = callbackUpdate(5L, "x");
        assertThat(TelegramUpdateHelper.hasCallback(u)).isTrue();
    }

    @Test
    void hasCallbackFalseWhenAbsent() {
        Update u = messageUpdate(2L, "hi", true);
        assertThat(TelegramUpdateHelper.hasCallback(u)).isFalse();
    }

    @Test
    void getChatIdFromMessage() {
        Update u = messageUpdate(77L, "msg", true);
        assertThat(TelegramUpdateHelper.getChatId(u)).isEqualTo(77L);
    }

    @Test
    void getChatIdFromCallbackUsesFromId() {
        Update u = callbackUpdate(88L, "cb");
        assertThat(TelegramUpdateHelper.getChatId(u)).isEqualTo(88L);
    }

    @Test
    void getChatIdReturnsNullWhenNoMessageOrCallback() {
        Update u = new Update();
        assertThat(TelegramUpdateHelper.getChatId(u)).isNull();
    }

    @Test
    void getEffectiveTextPrefersMessageTextOverCallback() {
        Update u = messageUpdate(11L, "messageText", true);
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cb");
        User f = new User(); f.setId(11L); cq.setFrom(f);
        cq.setData("callbackData");
        u.setCallbackQuery(cq); // both present
        assertThat(TelegramUpdateHelper.getEffectiveText(u)).isEqualTo("messageText");
    }

    @Test
    void getEffectiveTextReturnsCallbackDataWhenNoMessageText() {
        Update u = callbackUpdate(44L, "cb_data");
        assertThat(TelegramUpdateHelper.getEffectiveText(u)).isEqualTo("cb_data");
    }

    @Test
    void getEffectiveTextReturnsNullWhenEmptyUpdate() {
        assertThat(TelegramUpdateHelper.getEffectiveText(new Update())).isNull();
    }

    @Test
    void getEffectiveTextReturnsNullWhenNullUpdate() {
        assertThat(TelegramUpdateHelper.getEffectiveText(null)).isNull();
    }

    @Test
    void getCallbackReturnsCallback() {
        Update u = callbackUpdate(3L, "d");
        assertThat(TelegramUpdateHelper.getCallback(u)).isNotNull();
    }

    @Test
    void getCallbackReturnsNullWhenAbsent() {
        Update u = messageUpdate(9L, "t", true);
        assertThat(TelegramUpdateHelper.getCallback(u)).isNull();
    }

    @Test
    void getUserReturnsFromMessage() {
        Update u = messageUpdate(101L, "hi", true);
        assertThat(TelegramUpdateHelper.getUser(u)).isNotNull();
        assertThat(TelegramUpdateHelper.getUser(u).getId()).isEqualTo(101L);
    }

    @Test
    void getUserReturnsFromCallback() {
        Update u = callbackUpdate(202L, "data");
        assertThat(TelegramUpdateHelper.getUser(u)).isNotNull();
        assertThat(TelegramUpdateHelper.getUser(u).getId()).isEqualTo(202L);
    }

    @Test
    void getUserReturnsNullWhenNone() {
        assertThat(TelegramUpdateHelper.getUser(new Update())).isNull();
    }
}

