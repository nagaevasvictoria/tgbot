package tgbot;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Проверяет чтение ингредиентов. */
class IngredientReaderTest {
    /** Разделителями служат запятая и перенос строки. */
    @Test
    void shouldSplitByCommaAndNewline() {
        IngredientReader reader = new IngredientReader();

        Assertions.assertEquals(List.of("макароны", "сыр"), reader.read("Макароны, сыр"));
        Assertions.assertEquals(List.of("макароны", "сыр"), reader.read("Макароны\nсыр"));
    }

    /** Пустые элементы и лишние пробелы игнорируются. */
    @Test
    void shouldIgnoreEmptyParts() {
        IngredientReader reader = new IngredientReader();

        Assertions.assertEquals(List.of("макароны", "сыр"), reader.read("  макароны,, сыр,  "));
        Assertions.assertTrue(reader.read("").isEmpty());
        Assertions.assertTrue(reader.read(" , ,").isEmpty());
    }

    /** Запятая между цифрами остаётся частью числа. */
    @Test
    void shouldKeepDecimalComma() {
        IngredientReader reader = new IngredientReader();

        Assertions.assertEquals(
                List.of("2,5 стакана муки", "сыр"),
                reader.read("2,5 стакана муки, сыр"));
    }

    /** Запятая рядом с цифрой, но не между цифрами, разделяет ингредиенты. */
    @Test
    void shouldSplitCommaNextToDigit() {
        IngredientReader reader = new IngredientReader();

        Assertions.assertEquals(List.of("яйца 2", "молоко"), reader.read("яйца 2, молоко"));
        Assertions.assertEquals(List.of("соль", "2 яйца"), reader.read("соль,2 яйца"));
    }

    /** Запятая внутри скобок не разделяет ингредиенты. */
    @Test
    void shouldNotSplitInsideParentheses() {
        IngredientReader reader = new IngredientReader();

        Assertions.assertEquals(
                List.of("мука (пшеничная, высший сорт)", "сыр"),
                reader.read("Мука (пшеничная, высший сорт), сыр"));
    }

    /** Незакрытая скобка не склеивает ингредиенты на следующих строках. */
    @Test
    void shouldResetParenthesesOnNewLine() {
        IngredientReader reader = new IngredientReader();

        Assertions.assertEquals(
                List.of("мука (пшеничная", "сыр", "масло"),
                reader.read("мука (пшеничная\nсыр,\nмасло"));
    }
}