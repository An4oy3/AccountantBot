package com.example.demo.model.enums;

import lombok.Getter;

@Getter
public enum ExpenseCategory {
    NONE(""),
    GROCERIES("Продукты питания"),
    TRANSPORTATION("Транспорт"),
    UTILITIES("Коммунальные услуги"),
    HEALTHCARE("Здоровье"),
    RESTAURANTS("Рестораны и кафе"),
    CLOTHES("Одежда и обувь"),
    ENTERTAINMENT("Развлечения"),
    TRAVEL("Путешествия"),
    EDUCATION("Образование"),
    COMMUNICATION("Связь"),
    GIFT("Подарки/Праздники"),
    TAXES("Налоги"),
    PETS("Домашние животные"),
    SPORT("Спорт"),
    BEAUTY("Красота"),
    INSURANCE("Страховки"),
    INVESTMENTS("Инвестиции"),
    HOME("Дом и быт"),
    OTHER("Прочее");

    private final String text;

    ExpenseCategory(String text) { this.text = text; }

    public static ExpenseCategory fromText(String text) {
        for (ExpenseCategory category : ExpenseCategory.values()) {
            if (category.getText().equalsIgnoreCase(text)) {
                return category;
            }
        }
        return null;
    }

    public static Boolean isValidCategory(String text) {
        if (text == null) return false;
        if (fromText(text) != null) return true;
        try {
            ExpenseCategory.valueOf(text);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
