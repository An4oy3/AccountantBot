package com.example.demo.service;

import com.example.demo.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    void createUser(String username, String firstName, String SecondName, Long chatId);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    User findByChatId(Long chatId);
    void deleteUser(Long userId);
    boolean existsByUsername(String username);
    boolean existsByChatId(Long chatId);
}
