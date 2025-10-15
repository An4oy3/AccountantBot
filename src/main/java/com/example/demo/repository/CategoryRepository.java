package com.example.demo.repository;

import com.example.demo.model.entity.Category;
import com.example.demo.model.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCaseAndType(String name, CategoryType type);
    List<Category> findByType(CategoryType type);
    List<Category> findByOwnerChatId(Long chatId);
    void deleteByName(String name);
    boolean existsByNameIgnoreCaseAndOwnerIsNull(String name);
    boolean existsByNameIgnoreCaseAndOwnerChatId(String name, Long ownerChatId);
    @Query("SELECT c FROM Category c LEFT JOIN c.owner o " +
            "WHERE LOWER(c.name) LIKE CONCAT('%', LOWER(:name), '%') " +
            "AND (o IS NULL OR o.chatId = :ownerChatId)")
    List<Category> findSimilarByName(String name, Long ownerChatId);
}
