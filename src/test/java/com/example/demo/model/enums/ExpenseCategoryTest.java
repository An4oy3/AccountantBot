package com.example.demo.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseCategoryTest {

    @Test
    void fromTextReturnsEnumForExactRussianText() {
        assertThat(ExpenseCategory.fromText("Продукты питания")).isEqualTo(ExpenseCategory.GROCERIES);
    }

    @Test
    void fromTextReturnsEnumIgnoringCase() {
        assertThat(ExpenseCategory.fromText("тРансПоРт")).isEqualTo(ExpenseCategory.TRANSPORTATION);
    }

    @Test
    void fromTextReturnsNoneForEmptyString() {
        assertThat(ExpenseCategory.fromText(""))
                .isEqualTo(ExpenseCategory.NONE);
    }

    @Test
    void fromTextReturnsNullForUnknownText() {
        assertThat(ExpenseCategory.fromText("Несуществующая категория")).isNull();
    }

    @Test
    void fromTextReturnsNullForNullInput() {
        assertThat(ExpenseCategory.fromText(null)).isNull();
    }

    @Test
    void isValidCategoryReturnsFalseForNull() {
        assertThat(ExpenseCategory.isValidCategory(null)).isFalse();
    }

    @Test
    void isValidCategoryReturnsTrueForRussianTextIgnoringCase() {
        assertThat(ExpenseCategory.isValidCategory("продукты питания")).isTrue();
    }

    @Test
    void isValidCategoryReturnsTrueForEnumName() {
        assertThat(ExpenseCategory.isValidCategory("GROCERIES")).isTrue();
    }

    @Test
    void isValidCategoryReturnsFalseForEnumNameWrongCase() {
        // lowercase constant name does not match translation or enum constant (valueOf is case-sensitive)
        assertThat(ExpenseCategory.isValidCategory("groceries")).isFalse();
    }

    @Test
    void isValidCategoryReturnsTrueForEmptyStringMatchingNone() {
        assertThat(ExpenseCategory.isValidCategory(""))
                .isTrue();
    }

    @Test
    void isValidCategoryReturnsFalseForUnknown() {
        assertThat(ExpenseCategory.isValidCategory("UNKNOWN_CAT"))
                .isFalse();
    }
}

