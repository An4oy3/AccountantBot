package com.example.demo.service.util;

import com.example.demo.model.entity.Account;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class InlineCalendarUtil {

    public InlineKeyboardMarkup buildAccountAndDateKeyboard(Long chatId, Account account, LocalDate selectedDate) {
        LocalDate date = selectedDate != null ? selectedDate : LocalDate.now();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка счёта (по нажатию предполагается выбор/изменение счёта)
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Счёт: " + account.getDisplayName())
                        .callbackData("account:choose")
                        .build()
        ));

        // Кнопка даты (открывает календарь для текущего месяца)
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Дата: " + date)
                        .callbackData("date:choose")
                        .build()
        ));

        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Продолжить")
                        .callbackData("proceed_account_date:"+ account.getId()+":"+date)
                        .build()
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public static InlineKeyboardMarkup handleCalendarNavigation(String callbackData) {
        LocalDate selectedDate = extractDateFromCallbackData(callbackData);
        YearMonth baseMonth = (selectedDate != null) ? YearMonth.from(selectedDate) : YearMonth.now();

        if (callbackData != null) {
            if (callbackData.startsWith("date_prev:")) {
                baseMonth = baseMonth.minusMonths(1);
            } else if (callbackData.startsWith("date_next:")) {
                baseMonth = baseMonth.plusMonths(1);
            }
        }

        return buildCalendar(baseMonth, selectedDate);
    }

    public static LocalDate extractDateFromCallbackData(String callbackData) {
        if (callbackData != null && callbackData.startsWith("date:")) {
            try {
                return LocalDate.parse(callbackData.split(":")[1]);
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        if (callbackData != null && callbackData.startsWith("date_")) {
            try {
                return YearMonth.parse(callbackData.split(":")[1]).atDay(1);
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        return null;
    }

    public static InlineKeyboardMarkup buildCalendar(YearMonth baseMonth, LocalDate selectedDate) {
        LocalDate first = baseMonth.atDay(1);
        DayOfWeek firstDow = first.getDayOfWeek();
        int shift = (firstDow.getValue() + 6) % 7; // Пн=0 ... Вс=6
        int length = baseMonth.lengthOfMonth();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Заголовок с навигацией
        rows.add(List.of(
                InlineKeyboardButton.builder().text("◀").callbackData("date_prev:" + baseMonth).build(),
                InlineKeyboardButton.builder().text(monthTitle(baseMonth)).callbackData("noop").build(),
                InlineKeyboardButton.builder().text("▶").callbackData("date_next:" + baseMonth).build()
        ));

        // Дни недели
        rows.add(List.of(
                btnLabel("Пн"), btnLabel("Вт"), btnLabel("Ср"),
                btnLabel("Чт"), btnLabel("Пт"), btnLabel("Сб"), btnLabel("Вс")
        ));

        // Сетка
        List<InlineKeyboardButton> week = new ArrayList<>();
        for (int i = 0; i < shift; i++) {
            week.add(btnEmpty());
        }
        for (int day = 1; day <= length; day++) {
            LocalDate current = baseMonth.atDay(day);
            String label = String.valueOf(day);
            if (selectedDate != null && selectedDate.equals(current)) {
                label = "▪" + label + "▪";
            }
            week.add(InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData("date:" + current)
                    .build());
            if (week.size() == 7) {
                rows.add(week);
                week = new ArrayList<>();
            }
        }
        if (!week.isEmpty()) {
            while (week.size() < 7) week.add(btnEmpty());
            rows.add(week);
        }

        // Кнопка "Сегодня"
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Сегодня")
                        .callbackData("date:" + LocalDate.now())
                        .build()
        ));

        // Accept button
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Выбрать")
                        .callbackData("date:accept:" + (selectedDate != null ? selectedDate : LocalDate.now()))
                        .build()));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton btnLabel(String text) {
        return InlineKeyboardButton.builder().text(text).callbackData("noop").build();
    }

    private InlineKeyboardButton btnEmpty() {
        return InlineKeyboardButton.builder().text(" ").callbackData("noop").build();
    }

    private String monthTitle(YearMonth ym) {
        String[] ru = {
                "Январь","Февраль","Март","Апрель","Май","Июнь",
                "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь"
        };
        return ru[ym.getMonthValue()-1] + " " + ym.getYear();
    }

    //====================================================================================================================
}
