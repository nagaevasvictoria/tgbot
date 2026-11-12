package tgbot;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты для чтения ингредиентов из текста.
 */
public class IngredientReaderTest {

    /**
     * Читатель ингредиентов.
     */
    private final IngredientReader reader = new IngredientReader();

    /**
     * Тестирование разделения по запятым и переносам строк.
     */
    @Test
    void testSplitsByCommaAndNewLine() {
        Assertions.assertEquals(List.of("молоко", "яйца", "мука"),
                reader.read("молоко, яйца\nмука"));
    }

    /**
     * Тестирование приведения к нижнему регистру и удаления лишних пробелов.
     */
    @Test
    void testLowersCaseAndTrimsSpaces() {
        Assertions.assertEquals(List.of("сахар белый", "соль"),
                reader.read("  Сахар   БЕЛЫЙ ,  СОЛЬ "));
    }

    /**
     * Тестирование того, что запятая между цифрами не разделяет ингредиенты,
     * а запятая рядом с буквой или пробелом разделяет.
     */
    @Test
    void testCommaSeparatesUnlessBetweenDigits() {
        Assertions.assertEquals(List.of("1,5 л молока", "0,5 кг муки"),
                reader.read("1,5 л молока, 0,5 кг муки"));
        Assertions.assertEquals(List.of("молоко 2", "3 яйца"),
                reader.read("молоко 2, 3 яйца"));
        Assertions.assertEquals(List.of("яйца", "3 шт"),
                reader.read("яйца,3 шт"));
    }

    /**
     * Тестирование того, что пустые элементы (двойная запятая, пустые строки) пропускаются.
     */
    @Test
    void testEmptyItemsAreSkipped() {
        Assertions.assertEquals(List.of("мука", "соль"),
                reader.read("мука,, ,\n\nсоль,"));
    }
}