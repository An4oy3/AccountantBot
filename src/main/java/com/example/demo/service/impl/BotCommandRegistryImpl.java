package com.example.demo.service.impl;

import com.example.demo.service.BotCommandHandler;
import com.example.demo.service.BotCommandRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.example.demo.service.util.TelegramUpdateHelper.*;

/**
 * In-memory thread-safe implementation of BotCommandRegistry.
 * Spring автоматически инжектит все бины BotCommandHandler через конструктор.
 */
@Slf4j
@Service
public class BotCommandRegistryImpl implements BotCommandRegistry {

    private final CopyOnWriteArrayList<BotCommandHandler> handlers;

    @Autowired
    public BotCommandRegistryImpl(List<BotCommandHandler> handlers) {
        this.handlers = new CopyOnWriteArrayList<>(handlers);
    }

    @Override
    public Optional<SendMessage> process(Update update) {
        if (!isValid(update)) {
            return Optional.empty();
        }
        for (BotCommandHandler handler : handlers) {
            if (safeSupports(handler, update)) {
                try {
                    return Optional.of(handler.handle(getChatId(update), getEffectiveText(update)));
                } catch (Exception e) {
                    log.error("Handler {} failed while handling message: {}", handler.getClass().getSimpleName(), e.getMessage(), e);
                    // Переходим к следующему обработчику как fallback
                }
            }
        }
        return Optional.empty();
    }


    private boolean safeSupports(BotCommandHandler handler, Update update) {
        try {
            return handler.supports(update);
        } catch (Exception e) {
            log.warn("Handler {} threw exception in supports(): {}", handler.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}
