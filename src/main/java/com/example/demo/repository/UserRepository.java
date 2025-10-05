package com.example.demo.repository;

import com.example.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    // Added for lookup by Telegram chat id
    Optional<User> findByChatId(Long chatId);
    boolean existsByUsername(String username);
    boolean existsByChatId(Long chatId);
}
