package tgbot;

import java.util.ArrayList;
import java.util.List;

/** Разбирает текст пользователя на список ингредиентов. */
public class IngredientParser {
    /**
     * Разбивает текст по запятым и переносам строк, убирает пустые элементы
     * и приводит ингредиенты к нижнему регистру.
     *
     * @param text текст пользователя
     * @return список ингредиентов, может быть пустым
     */
    public List<String> parse(String text) {
        List<String> result = new ArrayList<>();
        for (String part : text.split("[,\\n]")) {
            String ingredient = part.trim().toLowerCase();
            if (!ingredient.isEmpty()) {
                result.add(ingredient);
            }
        }
        return result;
    }
}