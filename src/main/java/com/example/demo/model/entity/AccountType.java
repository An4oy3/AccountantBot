package com.example.demo.model.entity;

import lombok.Getter;

@Getter
public enum AccountType {
    CASH("Наличные"),
    M_BANK_CARD("Карта MBank"),
    PKO_CARD("Карта PKO"),
    CREDIT("Кредитка/Рассрочка"),
    CRYPTO_WALLET("Криптокошелек"),
    OTHER("Другое");

    private String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }
}

