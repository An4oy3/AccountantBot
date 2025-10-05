package com.example.demo.service.util;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InlineCalendarUtilTest {

    @Test
    void extractDateFromCallbackDataParsesFullDate() {
        LocalDate d = InlineCalendarUtil.extractDateFromCallbackData("date:2025-10-05");
        assertThat(d).isEqualTo(LocalDate.of(2025,10,5));
    }

    @Test
    void extractDateFromCallbackDataParsesYearMonthFromNavigation() {
        LocalDate d = InlineCalendarUtil.extractDateFromCallbackData("date_prev:2025-11");
        assertThat(d).isEqualTo(LocalDate.of(2025,11,1));
    }

    @Test
    void extractDateFromCallbackDataReturnsNullOnInvalid() {
        assertThat(InlineCalendarUtil.extractDateFromCallbackData("date:not-a-date")).isNull();
        assertThat(InlineCalendarUtil.extractDateFromCallbackData("something_else")).isNull();
    }

    @Test
    void handleCalendarNavigationPrevMonthShiftsBaseMonth() {
        InlineKeyboardMarkup mk = InlineCalendarUtil.handleCalendarNavigation("date_prev:2025-10");
        // Header row (index 0) middle button contains month title in Russian for September 2025
        String title = mk.getKeyboard().get(0).get(1).getText();
        assertThat(title).isEqualTo("Сентябрь 2025");
    }

    @Test
    void handleCalendarNavigationNextMonthAdvancesBaseMonth() {
        InlineKeyboardMarkup mk = InlineCalendarUtil.handleCalendarNavigation("date_next:2025-10");
        String title = mk.getKeyboard().get(0).get(1).getText();
        assertThat(title).isEqualTo("Ноябрь 2025");
    }

    @Test
    void buildCalendarHighlightsSelectedDate() {
        YearMonth ym = YearMonth.of(2025, 2); // February 2025
        LocalDate selected = LocalDate.of(2025,2,14);
        InlineKeyboardMarkup mk = InlineCalendarUtil.buildCalendar(ym, selected);
        boolean found = mk.getKeyboard().stream()
                .flatMap(List::stream)
                .anyMatch(btn -> "▪14▪".equals(btn.getText()));
        assertThat(found).isTrue();
    }

    @Test
    void buildCalendarAcceptButtonUsesSelectedDate() {
        LocalDate selected = LocalDate.of(2025,3,3);
        InlineKeyboardMarkup mk = InlineCalendarUtil.buildCalendar(YearMonth.of(2025,3), selected);
        String acceptData = mk.getKeyboard().get(mk.getKeyboard().size()-1).get(0).getCallbackData();
        assertThat(acceptData).isEqualTo("date:accept:2025-03-03");
    }

    @Test
    void buildCalendarAcceptButtonDefaultsToTodayWhenNoSelection() {
        YearMonth ym = YearMonth.now();
        LocalDate today = LocalDate.now();
        InlineKeyboardMarkup mk = InlineCalendarUtil.buildCalendar(ym, null);
        String acceptData = mk.getKeyboard().get(mk.getKeyboard().size()-1).get(0).getCallbackData();
        assertThat(acceptData).isEqualTo("date:accept:" + today);
    }

    @Test
    void buildCalendarWeekAlignmentFirstDayMonday() {
        YearMonth ym = YearMonth.of(2024,1); // 2024-01-01 is Monday -> first cell should be 1
        InlineKeyboardMarkup mk = InlineCalendarUtil.buildCalendar(ym, null);
        // Row index 2 is first week row after header + weekdays
        String firstCell = mk.getKeyboard().get(2).get(0).getText();
        assertThat(firstCell).isEqualTo("1");
    }

    @Test
    void buildCalendarWeekAlignmentFirstDaySundayHasLeadingEmptyCells() {
        YearMonth ym = YearMonth.of(2024,9); // 2024-09-01 is Sunday => last column (index 6)
        InlineKeyboardMarkup mk = InlineCalendarUtil.buildCalendar(ym, null);
        List<String> weekRow = mk.getKeyboard().get(2).stream().map(InlineKeyboardButton::getText).toList();
        assertThat(weekRow.subList(0,6)).allMatch(s -> s.equals(" "));
        assertThat(weekRow.get(6)).isEqualTo("1");
    }

    @Test
    void handleCalendarNavigationKeepsSelectionHighlight() {
        // Select a date in October then move next; selection date remains the same day string in resulting keyboard highlight if still in displayed month (will not be, so highlight absent)
        InlineKeyboardMarkup mk = InlineCalendarUtil.handleCalendarNavigation("date:2025-10-05");
        boolean highlightBefore = mk.getKeyboard().stream().flatMap(List::stream).anyMatch(b -> b.getText().equals("▪5▪"));
        assertThat(highlightBefore).isTrue();
        // Navigate next month with selection 2025-10-05 (base month increments to November 2025, highlight should disappear)
        InlineKeyboardMarkup next = InlineCalendarUtil.handleCalendarNavigation("date_next:2025-10");
        boolean highlightAfter = next.getKeyboard().stream().flatMap(List::stream).anyMatch(b -> b.getText().contains("▪"));
        assertThat(highlightAfter).isFalse();
    }
}

