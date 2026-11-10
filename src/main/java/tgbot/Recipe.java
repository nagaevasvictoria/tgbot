package tgbot;

import java.util.List;

/** Рецепт пользователя. */
public class Recipe {
    /** Уникальный идентификатор рецепта. */
    private final int id;
    /** Название рецепта. */
    private final String name;
    /** Список ингредиентов. */
    private final List<String> ingredients;
    /** Инструкция приготовления. */
    private final String instruction;

    /**
     * Создаёт рецепт с заданными данными.
     *
     * @param id идентификатор рецепта
     * @param name название
     * @param ingredients ингредиенты
     * @param instruction инструкция приготовления
     */
    public Recipe(int id, String name, List<String> ingredients, String instruction) {
        this.id = id;
        this.name = name;
        this.ingredients = List.copyOf(ingredients);
        this.instruction = instruction;
    }

    /** @return идентификатор рецепта */
    public int getId() {
        return id;
    }

    /** @return название рецепта */
    public String getName() {
        return name;
    }

    /** @return ингредиенты рецепта */
    public List<String> getIngredients() {
        return ingredients;
    }

    /** @return инструкция приготовления */
    public String getInstruction() {
        return instruction;
    }
}