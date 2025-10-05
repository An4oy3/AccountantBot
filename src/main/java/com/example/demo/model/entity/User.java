package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = {"user_name"}),
        @UniqueConstraint(name = "uk_users_chat_id", columnNames = {"chat_id"})
})
public class User extends BaseEntity {

    @Column(name = "user_name", length = 64)
    private String username; // уникальный логин / handle (например, telegram username)

    @Column(name = "first_name", length = 128)
    private String firstName;

    @Column(name = "last_name", length = 128)
    private String lastName;

    @Column(name = "chat_id", nullable = false)
    private Long chatId; // Telegram chat ID for messaging

//    @ManyToMany(mappedBy = "participants")
//    private Set<Account> participantAccounts = new HashSet<>();
}

