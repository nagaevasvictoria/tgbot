package tgbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Хранилище рецептов в памяти. Коллекции пользователей независимы друг от друга. */
public class RecipeRepository {
    /** Рецепты пользователей: идентификатор пользователя, затем ID рецепта. */
    private final Map<Long, Map<Integer, Recipe>> recipesByUser = new HashMap<>();
    /** ID следующего рецепта, общий для всех пользователей. */
    private int nextId = 1;

    /**
     * Добавляет рецепт в коллекцию пользователя и присваивает ему ID.
     *
     * @param userId идентификатор пользователя
     * @param name название
     * @param ingredients ингредиенты
     * @param instruction инструкция
     * @return сохранённый рецепт с присвоенным ID
     */
    public synchronized Recipe add(long userId, String name, List<String> ingredients, String instruction) {
        Recipe recipe = new Recipe(nextId, name, ingredients, instruction);
        nextId++;
        recipesByUser.computeIfAbsent(userId, key -> new LinkedHashMap<>()).put(recipe.getId(), recipe);
        return recipe;
    }

    /**
     * Ищет рецепт только в коллекции этого пользователя.
     *
     * @param userId идентификатор пользователя
     * @param id идентификатор рецепта
     * @return рецепт или пустой результат, если его нет в коллекции пользователя
     */
    public synchronized Optional<Recipe> find(long userId, int id) {
        Map<Integer, Recipe> recipes = recipesByUser.get(userId);
        if (recipes == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(recipes.get(id));
    }

    /**
     * Возвращает все рецепты пользователя в порядке добавления.
     *
     * @param userId идентификатор пользователя
     * @return список рецептов, может быть пустым
     */
    public synchronized List<Recipe> findAll(long userId) {
        Map<Integer, Recipe> recipes = recipesByUser.get(userId);
        if (recipes == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(recipes.values());
    }

    /**
     * Ищет рецепт пользователя по названию без учёта регистра.
     *
     * @param userId идентификатор пользователя
     * @param name название
     * @return рецепт или пустой результат
     */
    public synchronized Optional<Recipe> findByName(long userId, String name) {
        Map<Integer, Recipe> recipes = recipesByUser.get(userId);
        if (recipes == null) {
            return Optional.empty();
        }
        for (Recipe recipe : recipes.values()) {
            if (recipe.getName().equalsIgnoreCase(name)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /**
     * Заменяет данные рецепта, ID сохраняется.
     *
     * @param userId идентификатор пользователя
     * @param id идентификатор рецепта
     * @param name новое название
     * @param ingredients новые ингредиенты
     * @param instruction новая инструкция
     */
    public synchronized void update(long userId, int id, String name, List<String> ingredients, String instruction) {
        Map<Integer, Recipe> recipes = recipesByUser.get(userId);
        if (recipes != null && recipes.containsKey(id)) {
            recipes.put(id, new Recipe(id, name, ingredients, instruction));
        }
    }

    /**
     * Удаляет рецепт из коллекции пользователя.
     *
     * @param userId идентификатор пользователя
     * @param id идентификатор рецепта
     */
    public synchronized void remove(long userId, int id) {
        Map<Integer, Recipe> recipes = recipesByUser.get(userId);
        if (recipes != null) {
            recipes.remove(id);
        }
    }
}