package tgbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Хранилище рецептов в SQLite.
 * Коллекции пользователей независимы друг от друга.
 */
public class RecipeRepository {

    /**
     * Разделитель ингредиентов в одной ячейке
     * (парсер не пропускает переносы строк внутри ингредиента).
     */
    private static final String SEPARATOR = "\n";

    /**
     * Путь для базы, которая живёт только в памяти
     * (удобно для тестов).
     */
    private static final String IN_MEMORY = ":memory:";

    /**
     * Единственное соединение с базой на всё время работы.
     */
    private final Connection connection;

    /**
     * Создаёт хранилище в памяти без сохранения на диск.
     */
    public RecipeRepository() {
        this(IN_MEMORY);
    }

    /**
     * Открывает базу данных и создаёт таблицу, если её ещё нет.
     *
     * @param dbPath путь к файлу базы, например "recipes.db"
     */
    public RecipeRepository(String dbPath) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS recipes (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            user_id INTEGER NOT NULL,
                            name TEXT NOT NULL,
                            ingredients TEXT NOT NULL,
                            instruction TEXT NOT NULL
                        )""");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_recipes_user ON recipes(user_id)");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось открыть базу рецептов: " + dbPath, e);
        }
    }

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
        String sql = "INSERT INTO recipes (user_id, name, ingredients, instruction) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, String.join(SEPARATOR, ingredients));
            statement.setString(4, instruction);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new Recipe(keys.getInt(1), name, ingredients, instruction);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить рецепт", e);
        }
    }

    /**
     * Ищет рецепт только в коллекции этого пользователя.
     *
     * @param userId идентификатор пользователя
     * @param id идентификатор рецепта
     * @return рецепт или пустой результат, если его нет в коллекции пользователя
     */
    public synchronized Optional<Recipe> find(long userId, int id) {
        String sql = "SELECT id, name, ingredients, instruction FROM recipes WHERE user_id = ? AND id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(readRecipe(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать рецепт", e);
        }
    }

    /**
     * Возвращает все рецепты пользователя в порядке добавления.
     *
     * @param userId идентификатор пользователя
     * @return список рецептов, может быть пустым
     */
    public synchronized List<Recipe> findAll(long userId) {
        String sql = "SELECT id, name, ingredients, instruction FROM recipes WHERE user_id = ? ORDER BY id";
        List<Recipe> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(readRecipe(rows));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать рецепты", e);
        }
        return result;
    }

    /**
     * Ищет рецепт пользователя по названию без учёта регистра.
     * Сравнение делается в Java, потому что SQLite
     * не умеет сравнивать без учёта регистра кириллицу.
     *
     * @param userId идентификатор пользователя
     * @param name название
     * @return рецепт или пустой результат
     */
    public synchronized Optional<Recipe> findByName(long userId, String name) {
        for (Recipe recipe : findAll(userId)) {
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
        String sql = "UPDATE recipes SET name = ?, ingredients = ?, instruction = ? WHERE user_id = ? AND id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, String.join(SEPARATOR, ingredients));
            statement.setString(3, instruction);
            statement.setLong(4, userId);
            statement.setInt(5, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось обновить рецепт", e);
        }
    }

    /**
     * Удаляет рецепт из коллекции пользователя.
     *
     * @param userId идентификатор пользователя
     * @param id идентификатор рецепта
     */
    public synchronized void remove(long userId, int id) {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM recipes WHERE user_id = ? AND id = ?")) {
            statement.setLong(1, userId);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось удалить рецепт", e);
        }
    }

    /**
     * Собирает рецепт из текущей строки результата.
     */
    private Recipe readRecipe(ResultSet rows) throws SQLException {
        List<String> ingredients = List.of(rows.getString("ingredients").split(SEPARATOR));
        return new Recipe(rows.getInt("id"), rows.getString("name"), ingredients, rows.getString("instruction"));
    }
}