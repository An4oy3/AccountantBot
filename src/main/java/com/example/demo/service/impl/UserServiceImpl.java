package com.example.demo.service.impl;

import com.example.demo.exception.NotFoundException;
import com.example.demo.model.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createUser(String username, String firstName, String secondName, Long chatId) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (chatId == null) {
            throw new IllegalArgumentException("chatId is required");
        }

        if (existsByUsername(username)) {
            throw new IllegalArgumentException("User with username already exists: " + username);
        }
        if (existsByChatId(chatId)) {
            throw new IllegalArgumentException("User with chatId already exists: " + chatId);
        }
        User user = new User();
        user.setUsername(username);
        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName.trim());
        }
        if (secondName != null && !secondName.isBlank()) {
            user.setLastName(secondName.trim());
        }
        user.setChatId(chatId);
        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return userRepository.findByUsername(username.trim().toLowerCase());
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findByChatId(Long chatId) {
        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> new NotFoundException("User not found with chatId: " + chatId));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return userRepository.existsByUsername(username.trim().toLowerCase());
    }

    @Override
    public boolean existsByChatId(Long chatId) {
        if (chatId == null) return false;
        return userRepository.existsByChatId(chatId);
    }
}
