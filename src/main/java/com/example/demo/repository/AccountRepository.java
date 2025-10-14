package com.example.demo.repository;

import com.example.demo.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwnerChatId(Long ownerId);
    Optional<Account> findByNameAndOwnerChatId(String name, Long ownerId);
    List<Account> findByOwnerChatIdAndArchivedFalse(Long ownerId);
}
