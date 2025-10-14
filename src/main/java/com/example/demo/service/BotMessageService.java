package com.example.demo.service;

import com.example.demo.model.enums.DialogStateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import java.util.List;
import java.util.Optional;

import com.example.demo.model.enums.BotMainMenuButton;

import static com.example.demo.service.util.TelegramUpdateHelper.*;

/**
 * Service responsible for handling incoming Telegram updates and building responses.
 */
@Service
@RequiredArgsConstructor
public class BotMessageService {
    private final BotCommandRegistry commandRegistry;
    private final UserService userService;
    private final DialogStateService dialogStateService;

    /**
     * Process an update and return a SendMessage if a response is needed.
     */
    public Optional<SendMessage> handleUpdate(Update update) {
        if (!isValid(update)) {
            return Optional.empty();
        }
        initializeDialogIfNeeded(update);

        Long chatId = getChatId(update);

        SendMessage response;
        if (hasTextMessage(update) && "/start".equalsIgnoreCase(update.getMessage().getText().trim()) && chatId != null) {
            dialogStateService.clearState(chatId);
            response = SendMessage.builder()
                    .chatId(chatId)
                    .text("Добро пожаловать! Выберите действие:")
                    .replyMarkup(buildStartKeyboard())
                    .build();
        } else {
            // Delegate to command registry for other commands
            response = commandRegistry.process(update).orElse(null);
        }
        if (response == null) {
            dialogStateService.clearState(chatId);
            return Optional.of(SendMessage.builder()
                    .chatId(chatId)
                    .text("Извините, я не понимаю вашу команду. Пожалуйста, выберите действие из меню.")
                    .replyMarkup(buildStartKeyboard())
                    .build());
        }

        return Optional.of(response);
    }

    public static ReplyKeyboardMarkup buildStartKeyboard() {
        // Build the main menu keyboard
        KeyboardRow row1 = new KeyboardRow(List.of(
                KeyboardButton.builder().text(BotMainMenuButton.RECORD_EXPENSE.getText()).build(),
                KeyboardButton.builder().text(BotMainMenuButton.RECORD_INCOME.getText()).build()
        ));
        KeyboardRow row2 = new KeyboardRow(List.of(
                KeyboardButton.builder().text(BotMainMenuButton.STATISTICS.getText()).build(),
                KeyboardButton.builder().text(BotMainMenuButton.EXPORT.getText()).build()
        ));
        KeyboardRow row3 = new KeyboardRow(List.of(
                KeyboardButton.builder().text(BotMainMenuButton.SETTINGS.getText()).build(),
                KeyboardButton.builder().text(BotMainMenuButton.RECORD_FAST_EXPENSE.getText()).build()
        ));

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3))
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    private void initializeDialogIfNeeded(Update update) {
        Long chatId = getChatId(update);
        User tUser = getUser(update);
        if (chatId == null || tUser == null) {
            throw new IllegalArgumentException("Chat ID or User cannot be null");
        }
        if (dialogStateService.getState(chatId) == null) {
            dialogStateService.setDialogStateType(chatId, DialogStateType.IDLE);
        }
        if (!userService.existsByChatId(chatId)) {
            userService.createUser(
                    tUser.getUserName(),
                    tUser.getFirstName(),
                    tUser.getLastName(),
                    chatId
            );
        }
    }
}
