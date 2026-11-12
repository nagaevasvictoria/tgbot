package tgbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище рецептов в JSON-файле. Коллекции пользователей независимы друг от друга.
 * Номер рецепта - его позиция в списке пользователя (1, 2, 3...),
 * поэтому при удалении номера следующих рецептов сдвигаются.
 */
public class RecipeRepository {
    /** Тип данных в файле: рецепты каждого пользователя. */
    private static final Type DATA_TYPE = new TypeToken<Map<Long, List<Recipe>>>() { }.getType();

    /** Преобразование данных в JSON и обратно. */
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /** Файл с рецептами, null если данные хранятся только в памяти. */
    private final Path file;
    /** Рецепты всех пользователей, ключ - идентификатор пользователя. */
    private final Map<Long, List<Recipe>> data;

    /** Создаёт хранилище без файла: данные живут только пока работает программа. */
    public RecipeRepository() {
        file = null;
        data = new HashMap<>();
    }

    /**
     * Создаёт хранилище и загружает рецепты из файла, если он уже есть.
     *
     * @param path путь к файлу, например "recipes.json"
     */
    public RecipeRepository(String path) {
        file = Path.of(path);
        data = load();
    }

    /**
     * Добавляет рецепт в конец коллекции пользователя.
     *
     * @param userId идентификатор пользователя
     * @param name название
     * @param ingredients ингредиенты
     * @param instruction инструкция
     * @return сохранённый рецепт; его номер - позиция в списке пользователя
     */
    public Recipe add(long userId, String name, List<String> ingredients, String instruction) {
        List<Recipe> list = recipesOf(userId);
        Recipe recipe = new Recipe(list.size() + 1, name, ingredients, instruction);
        list.add(recipe);
        save();
        return recipe;
    }

    /**
     * Ищет рецепт по номеру в коллекции этого пользователя.
     *
     * @param userId идентификатор пользователя
     * @param number номер рецепта (с 1)
     * @return рецепт или null, если такого номера нет
     */
    public Recipe find(long userId, int number) {
        List<Recipe> list = recipesOf(userId);
        if (number < 1 || number > list.size()) {
            return null;
        }
        return list.get(number - 1);
    }

    /**
     * Возвращает все рецепты пользователя в порядке добавления.
     *
     * @param userId идентификатор пользователя
     * @return список рецептов, может быть пустым
     */
    public List<Recipe> findAll(long userId) {
        return new ArrayList<>(recipesOf(userId));
    }

    /**
     * Ищет рецепт пользователя по названию без учёта регистра.
     *
     * @param userId идентификатор пользователя
     * @param name название
     * @return рецепт или null, если такого нет
     */
    public Recipe findByName(long userId, String name) {
        for (Recipe recipe : recipesOf(userId)) {
            if (recipe.getName().equalsIgnoreCase(name)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Заменяет данные рецепта, номер сохраняется.
     *
     * @param userId идентификатор пользователя
     * @param number номер рецепта
     * @param name новое название
     * @param ingredients новые ингредиенты
     * @param instruction новая инструкция
     */
    public void update(long userId, int number, String name, List<String> ingredients, String instruction) {
        List<Recipe> list = recipesOf(userId);
        if (number < 1 || number > list.size()) {
            return;
        }
        list.set(number - 1, new Recipe(number, name, ingredients, instruction));
        save();
    }

    /**
     * Удаляет рецепт из коллекции пользователя. Номера следующих рецептов сдвигаются.
     *
     * @param userId идентификатор пользователя
     * @param number номер рецепта
     */
    public void remove(long userId, int number) {
        List<Recipe> list = recipesOf(userId);
        if (number < 1 || number > list.size()) {
            return;
        }
        list.remove(number - 1);
        renumber(list, number - 1);
        save();
    }

    /** Возвращает список рецептов пользователя, при первом обращении создаёт пустой. */
    private List<Recipe> recipesOf(long userId) {
        List<Recipe> list = data.get(userId);
        if (list == null) {
            list = new ArrayList<>();
            data.put(userId, list);
        }
        return list;
    }

    /**
     * Приводит номера рецептов, начиная с позиции from, к их положению в списке (1, 2, 3...).
     */
    private static void renumber(List<Recipe> list, int from) {
        for (int i = from; i < list.size(); i++) {
            Recipe recipe = list.get(i);
            list.set(i, new Recipe(i + 1, recipe.getName(), recipe.getIngredients(), recipe.getInstruction()));
        }
    }

    /**
     * Читает рецепты из файла. Если файла нет или он пустой, возвращает пустые данные.
     * Номера рецептов приводятся к позициям в списке, поэтому номера, записанные в файле
     * раньше (с пропусками или повторами), не ломают нумерацию.
     */
    private Map<Long, List<Recipe>> load() {
        if (!Files.exists(file)) {
            return new HashMap<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Map<Long, List<Recipe>> loaded = gson.fromJson(json, DATA_TYPE);
            if (loaded == null) {
                return new HashMap<>();
            }
            for (List<Recipe> list : loaded.values()) {
                renumber(list, 0);
            }
            return loaded;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл рецептов: " + file, e);
        }
    }

    /**
     * Записывает все рецепты в файл. Без файла ничего не делает.
     * Данные сначала пишутся во временный файл, который затем атомарно заменяет основной,
     * поэтому сбой посреди записи не портит уже сохранённые рецепты.
     */
    private void save() {
        if (file == null) {
            return;
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temp, gson.toJson(data, DATA_TYPE), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить рецепты в файл: " + file, e);
        }
    }
}
/** ya vse eto nenavizhu/