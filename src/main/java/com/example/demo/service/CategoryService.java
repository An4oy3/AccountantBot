package com.example.demo.service;

import com.example.demo.model.entity.Category;
import com.example.demo.model.entity.CategoryType;

import java.util.List;

public interface CategoryService {
    List<String> getAllCategoryNames();
    List<Category> getAllCategories();
    Category getCategoryByName(String name);
    List<Category> getCategoriesByType(CategoryType type);
    List<Category> getCategoriesByOwnerChatId(Long chatId);
    Category createCategory(String name, String type, Long ownerChatId);
    void deleteCategoryByName(String name);
    void deleteCategory(Category category);
    boolean categoryExists(String name, Long ownerChatId);
    List<Category> getSimilarCategory(String name, Long ownerChatId);
    void incrementCategoryUsage(Category category);
}
