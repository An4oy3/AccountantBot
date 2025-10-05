package com.example.demo.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.example.demo.model.telegram.AccountantTelegramBot;
import com.example.demo.service.BotMessageService;

@Configuration
public class BotConfig {
    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Bean
    public TelegramLongPollingBot accountantTelegramBot(BotMessageService botMessageService) {
        return new AccountantTelegramBot(botUsername, botToken, botMessageService);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramLongPollingBot accountantTelegramBot) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(accountantTelegramBot);
        return botsApi;
    }
}
