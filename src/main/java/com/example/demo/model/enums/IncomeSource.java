package com.example.demo.model.enums;

import lombok.Getter;

@Getter
public enum IncomeSource {
    SALARY("Зарплата"),
    BUSINESS("Бизнес"),
    GIFT("Подарок"),
    INVESTMENTS("Инвестиции"),
    RENTAL_INCOME("Аренда"),
    FREELANCE("Фриланс"),
    DIVIDENDS("Дивиденды"),
    INTEREST("Проценты"),
    REFUND("Возврат"),
    SALE_OF_ASSETS("Продажа активов"),
    LOAN("Займ"),
    BONUS("Бонус"),
    CASHBACK("Кэшбэк"),
    OTHER("Прочее");

    private final String text;

    IncomeSource(String text) { this.text = text; }
}
