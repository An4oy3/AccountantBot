package com.example.demo.model.telegram;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.example.demo.service.BotMessageService;

@RequiredArgsConstructor
public class AccountantTelegramBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(AccountantTelegramBot.class);

    private final String username;
    private final String token;
    private final BotMessageService botMessageService;


    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            botMessageService.handleUpdate(update).ifPresent(msg -> {
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    log.error("Send failed", e);
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
    }
}
