package com.example.demo.service.impl;

import com.example.demo.exception.NotFoundException;
import com.example.demo.model.entity.Category;
import com.example.demo.model.entity.CategoryType;
import com.example.demo.model.entity.User;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<String> getAllCategoryNames() {
        // Возвращаем отсортированный список уникальных имен (глобальных и пользовательских)
        return categoryRepository.findAll().stream()
                .map(Category::getName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public Category getCategoryByName(String name, CategoryType categoryType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }

        return categoryRepository.findByNameIgnoreCaseAndType(name.trim(), categoryType)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + name));
    }

    @Override
    public List<Category> getCategoriesByType(CategoryType type) {
        if (type == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
        return categoryRepository.findByType(type).stream()
                .sorted(Comparator.comparing(Category::getUsageCount).reversed().thenComparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public List<Category> getCategoriesByOwnerChatId(Long chatId) {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId must not be null");
        }
        return categoryRepository.findByOwnerChatId(chatId).stream()
                .sorted(Comparator.comparing(Category::getUsageCount).reversed().thenComparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Category createCategory(String name, String type, Long ownerChatId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        String trimmedName = name.trim();
        CategoryType categoryType = parseType(type);

        Category category = new Category();
        category.setName(trimmedName);
        category.setType(categoryType);

        if (ownerChatId != null) {
            // Проверка дубликата для пользователя
            if (categoryRepository.existsByNameIgnoreCaseAndOwnerChatId(trimmedName, ownerChatId)) {
                throw new IllegalArgumentException("Category already exists for user chatId=" + ownerChatId + ": " + trimmedName);
            }
            User owner = userRepository.findByChatId(ownerChatId)
                    .orElseThrow(() -> new NotFoundException("User not found by chatId=" + ownerChatId));
            category.setOwner(owner);
        } else {
            // Глобальная категория
            if (categoryRepository.existsByNameIgnoreCaseAndOwnerIsNull(trimmedName)) {
                throw new IllegalArgumentException("Global category already exists: " + trimmedName);
            }
            category.setOwner(null);
        }

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategoryByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        categoryRepository.deleteByName(name.trim());
    }

    @Override
    @Transactional
    public void deleteCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("Category must not be null and must have an ID");
        }
        if (!categoryRepository.existsById(category.getId())) {
            throw new NotFoundException("Category not found with ID: " + category.getId());
        }
        categoryRepository.delete(category);
    }

    @Override
    public boolean categoryExists(String name, Long ownerChatId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        String trimmedName = name.trim();
        if (ownerChatId != null) {
            return categoryRepository.existsByNameIgnoreCaseAndOwnerChatId(trimmedName, ownerChatId) ||
                   categoryRepository.existsByNameIgnoreCaseAndOwnerIsNull(trimmedName);
        } else {
            return categoryRepository.existsByNameIgnoreCaseAndOwnerIsNull(trimmedName);
        }
    }

    @Override
    public List<Category> getSimilarCategory(String name, Long ownerChatId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        String trimmedName = name.trim();
        return categoryRepository.findSimilarByName(trimmedName, ownerChatId);
    }

    @Override
    public void incrementCategoryUsage(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("Category must not be null and must have an ID");
        }
        Category existing = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new NotFoundException("Category not found with ID: " + category.getId()));
        existing.setUsageCount(existing.getUsageCount() + 1);
        categoryRepository.save(existing);
    }

    private CategoryType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Category type must not be blank");
        }
        try {
            return CategoryType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown category type: " + type + ". Allowed: " + List.of(CategoryType.values()), ex);
        }
    }
}
