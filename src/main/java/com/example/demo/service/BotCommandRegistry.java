package com.example.demo.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

/**
 * Registry abstraction that routes incoming messages to a matching {@link BotCommandHandler}.
 * Separate interface allows multiple strategies (in-memory list, cached, prioritized, etc.).
 */
public interface BotCommandRegistry {

    /**
     * Try to process the message via the first handler that supports it.
     * @param update telegram update object (may be null)
     * @return optional response
     */
    Optional<SendMessage> process(Update update);
}

