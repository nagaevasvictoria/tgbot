package tgbot;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Проверяет разбор ингредиентов. */
class IngredientParserTest {
    /** Разделителями служат запятая и перенос строки. */
    @Test
    void shouldSplitByCommaAndNewline() {
        IngredientParser parser = new IngredientParser();

        Assertions.assertEquals(List.of("макароны", "сыр"), parser.parse("Макароны, сыр"));
        Assertions.assertEquals(List.of("макароны", "сыр"), parser.parse("Макароны\nсыр"));
    }

    /** Пустые элементы и лишние пробелы игнорируются. */
    @Test
    void shouldIgnoreEmptyParts() {
        IngredientParser parser = new IngredientParser();

        Assertions.assertEquals(List.of("макароны", "сыр"), parser.parse("  макароны,, сыр,  "));
        Assertions.assertTrue(parser.parse("").isEmpty());
        Assertions.assertTrue(parser.parse(" , ,").isEmpty());
    }
}