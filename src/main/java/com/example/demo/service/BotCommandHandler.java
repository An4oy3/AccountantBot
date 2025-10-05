package com.example.demo.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Contract for handling a specific bot command (triggered by a textual message).
 * Implementations should be stateless (or thread-safe) and annotated with a Spring stereotype (@Component/@Service)
 * so they can be auto-discovered and injected into the command registry.
 */
public interface BotCommandHandler {

    /**
     * Returns true if this handler can process the given incoming Telegram message.
     * Typical implementation compares the message text with a command keyword or button label.
     *
     * @param update incoming Telegram message (never null when invoked)
     * @return true if this handler supports the message
     */
    boolean supports(Update update);

    /**
     * Handle the message and build a response. Should not return null.
     * Any domain validation errors should be reflected in the response text.
     *
     * @param chatId
     * @param message incoming Telegram message
     * @return response message to send back to user
     */
    SendMessage handle(Long chatId, String message);
}

