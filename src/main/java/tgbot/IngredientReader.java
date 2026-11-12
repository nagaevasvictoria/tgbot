package tgbot;

import java.util.ArrayList;
import java.util.List;

/** Читает текст пользователя и превращает его в список ингредиентов. */
public class IngredientReader {
    /**
     * Разбивает текст по запятым и переносам строк, убирает пустые элементы
     * и приводит ингредиенты к нижнему регистру. Запятая между двумя цифрами
     * (десятичная дробь) и запятая внутри скобок разделителями не считаются.
     *
     * @param text текст пользователя
     * @return список ингредиентов, может быть пустым
     */
    public List<String> read(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char symbol = text.charAt(i);
            if (symbol == '\n') {
                addIfNotEmpty(result, current);
                current.setLength(0);
                depth = 0;
                continue;
            }
            if (symbol == '(') {
                depth++;
            } else if (symbol == ')' && depth > 0) {
                depth--;
            }
            if (symbol == ',' && depth == 0 && !isDecimalComma(text, i)) {
                addIfNotEmpty(result, current);
                current.setLength(0);
            } else {
                current.append(symbol);
            }
        }
        addIfNotEmpty(result, current);
        return result;
    }

    /** Проверяет, что запятая стоит между двумя цифрами, например в «2,5». */
    private boolean isDecimalComma(String text, int index) {
        return index > 0
                && index < text.length() - 1
                && Character.isDigit(text.charAt(index - 1))
                && Character.isDigit(text.charAt(index + 1));
    }

    /** Добавляет очищенный ингредиент в список, если он не пустой. */
    private void addIfNotEmpty(List<String> result, StringBuilder current) {
        String ingredient = current.toString().trim().toLowerCase();
        if (!ingredient.isEmpty()) {
            result.add(ingredient);
        }
    }
}