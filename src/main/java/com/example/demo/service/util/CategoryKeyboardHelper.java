package com.example.demo.service.util;

import com.example.demo.model.entity.Category;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CategoryKeyboardHelper {

    public InlineKeyboardMarkup buildCategoryKeyboard(int page, int pageSize, List<Category> categories) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        int totalCategories = categories.size();
        int totalPages = (int) Math.ceil((double) totalCategories / pageSize);

        int startIdx = page * pageSize;
        int endIdx = Math.min(startIdx + pageSize, totalCategories);

        for (int i = startIdx; i < endIdx; i++) {
            Category category = categories.get(i);
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(category.getName())
                    .callbackData("category:" + category.getName())
                    .build();
            rows.add(List.of(button));
        }

        List<InlineKeyboardButton> navRow = new ArrayList<>();
        if (page > 0) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("⬅️ Назад")
                    .callbackData("category_page:" + (page - 1))
                    .build());
        }
        if (page < totalPages - 1) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("Вперёд ➡️")
                    .callbackData("category_page:" + (page + 1))
                    .build());
        }
        if (!navRow.isEmpty()) {
            rows.add(navRow);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }
}
