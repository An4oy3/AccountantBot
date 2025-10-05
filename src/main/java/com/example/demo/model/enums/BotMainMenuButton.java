package com.example.demo.model.enums;

import lombok.Getter;

/**
 * Main menu buttons shown to user after /start command.
 */
@Getter
public enum BotMainMenuButton {
    RECORD_EXPENSE("Записать расход"),
    RECORD_INCOME("Записать доход"),
    RECORD_TRANSFER("Записать перевод"),
    VIEW_REPORTS("Просмотреть отчёты"),
    STATISTICS("Статистика"),
    EXPORT("Экспорт"),
    SETTINGS("Настройки");

    private final String text;

    BotMainMenuButton(String text) { this.text = text; }

}

