package com.example.demo.service.util;

import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDate;

@UtilityClass
public final class TelegramUpdateHelper {
    public static boolean isValid(Update update) {
        return update != null &&
                ((update.hasMessage() && update.getMessage().getChat() != null) ||
                (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null));
    }

    public static boolean hasTextMessage(Update update) {
        return update != null && update.hasMessage() && update.getMessage().hasText();
    }

    public static boolean hasCallback(Update update) {
        return update != null && update.hasCallbackQuery();
    }

    public static Long getChatId(Update update) {
        if (hasTextMessage(update)) {
            return update.getMessage().getChatId();
        }
        if (hasCallback(update)) {
            return update.getCallbackQuery().getFrom().getId();
        }
        return null;
    }

    public static String getEffectiveText(Update update) {
        if (update == null) return null;
        if (hasTextMessage(update)) return update.getMessage().getText();
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getData();
        return null;
    }

    public static CallbackQuery getCallback(Update update) {
        return hasCallback(update) ? update.getCallbackQuery() : null;
    }

    public static User getUser(Update update) {
        if (hasTextMessage(update)) {
            return update.getMessage().getFrom();
        }
        if (hasCallback(update)) {
            return update.getCallbackQuery().getFrom();
        }
        return null;
    }

}
