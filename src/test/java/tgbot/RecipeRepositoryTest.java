package tgbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Тесты для хранилища рецептов.
 */
public class RecipeRepositoryTest {

    /**
     * Идентификатор первого пользователя.
     */
    private static final long USER_ID = 1;

    /**
     * Идентификатор второго пользователя.
     */
    private static final long OTHER_USER_ID = 2;

    /**
     * Хранилище в памяти, создаётся заново перед каждым тестом.
     */
    private RecipeRepository repository;

    /**
     * Создаёт чистое хранилище перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        repository = new RecipeRepository();
    }

    /**
     * Тестирование того, что у разных пользователей отдельные рецепты и своя нумерация.
     */
    @Test
    void testUsersHaveSeparateRecipesAndNumbers() {
        repository.add(USER_ID, "Борщ", List.of("свёкла"), "Варить");
        repository.add(USER_ID, "Щи", List.of("капуста"), "Тушить");

        Recipe other = repository.add(OTHER_USER_ID, "Окрошка", List.of("квас"), "Смешать");

        Assertions.assertEquals(1, other.getId());
        Assertions.assertEquals(1, repository.findAll(OTHER_USER_ID).size());
        Assertions.assertNull(repository.find(OTHER_USER_ID, 2));
        Assertions.assertNull(repository.findByName(OTHER_USER_ID, "Борщ"));
    }

    /**
     * Тестирование удаления из середины списка: номера следующих рецептов сдвигаются.
     */
    @Test
    void testRemoveShiftsNumbers() {
        repository.add(USER_ID, "Борщ", List.of("свёкла"), "Варить");
        repository.add(USER_ID, "Щи", List.of("капуста"), "Тушить");
        repository.add(USER_ID, "Окрошка", List.of("квас"), "Смешать");

        repository.remove(USER_ID, 2);

        Assertions.assertEquals(2, repository.findAll(USER_ID).size());
        Assertions.assertEquals("Борщ", repository.find(USER_ID, 1).getName());
        Assertions.assertEquals("Окрошка", repository.find(USER_ID, 2).getName());
        Assertions.assertEquals(2, repository.find(USER_ID, 2).getId());
        Assertions.assertNull(repository.find(USER_ID, 3));
    }

    /**
     * Тестирование удаления первого, последнего и единственного рецепта.
     */
    @Test
    void testRemoveFirstAndLast() {
        repository.add(USER_ID, "Борщ", List.of("свёкла"), "Варить");
        repository.add(USER_ID, "Щи", List.of("капуста"), "Тушить");
        repository.add(USER_ID, "Окрошка", List.of("квас"), "Смешать");

        repository.remove(USER_ID, 1);

        Assertions.assertEquals("Щи", repository.find(USER_ID, 1).getName());
        Assertions.assertEquals(1, repository.find(USER_ID, 1).getId());
        Assertions.assertEquals("Окрошка", repository.find(USER_ID, 2).getName());
        Assertions.assertEquals(2, repository.find(USER_ID, 2).getId());

        repository.remove(USER_ID, 2);
        Assertions.assertEquals(1, repository.findAll(USER_ID).size());

        repository.remove(USER_ID, 1);
        Assertions.assertTrue(repository.findAll(USER_ID).isEmpty());
        Assertions.assertEquals(1,
                repository.add(USER_ID, "Солянка", List.of("колбаса"), "Варить").getId());
    }

    /**
     * Тестирование того, что новый рецепт после удаления получает следующий номер списка.
     */
    @Test
    void testNewRecipeAfterRemoveGetsNextNumber() {
        repository.add(USER_ID, "Борщ", List.of("свёкла"), "Варить");
        repository.add(USER_ID, "Щи", List.of("капуста"), "Тушить");
        repository.add(USER_ID, "Окрошка", List.of("квас"), "Смешать");
        repository.remove(USER_ID, 2);

        Recipe added = repository.add(USER_ID, "Солянка", List.of("колбаса"), "Варить");

        Assertions.assertEquals(3, added.getId());
    }

    /**
     * Тестирование того, что при обновлении меняются данные, а номер сохраняется.
     */
    @Test
    void testUpdateKeepsNumber() {
        repository.add(USER_ID, "Борщ", List.of("свёкла"), "Варить");

        repository.update(USER_ID, 1, "Щи", List.of("капуста"), "Тушить");

        Recipe updated = repository.find(USER_ID, 1);
        Assertions.assertEquals(1, updated.getId());
        Assertions.assertEquals("Щи", updated.getName());
        Assertions.assertEquals(List.of("капуста"), updated.getIngredients());
        Assertions.assertEquals("Тушить", updated.getInstruction());
    }

    /**
     * Тестирование поиска по названию без учёта регистра: по нему бот
     * не даёт создать два рецепта с одинаковым названием.
     */
    @Test
    void testFindByNameIgnoresCase() {
        repository.add(USER_ID, "Борщ", List.of("свёкла"), "Варить");

        Assertions.assertNotNull(repository.findByName(USER_ID, "бОрЩ"));
        Assertions.assertNull(repository.findByName(USER_ID, "Щи"));
    }

    /**
     * Тестирование того, что рецепты нескольких пользователей читаются из файла после перезапуска.
     */
    @Test
    void testRecipesSurviveRestart(@TempDir Path dir) {
        String file = dir.resolve("recipes.json").toString();
        RecipeRepository first = new RecipeRepository(file);
        first.add(USER_ID, "Борщ", List.of("свёкла", "2,5 л воды"), "Варить час");
        first.add(OTHER_USER_ID, "Щи", List.of("капуста"), "Тушить");

        RecipeRepository second = new RecipeRepository(file);

        Recipe borsch = second.find(USER_ID, 1);
        Assertions.assertEquals("Борщ", borsch.getName());
        Assertions.assertEquals(List.of("свёкла", "2,5 л воды"), borsch.getIngredients());
        Assertions.assertEquals("Варить час", borsch.getInstruction());
        Assertions.assertEquals("Щи", second.find(OTHER_USER_ID, 1).getName());
    }

    /**
     * Тестирование того, что обновление и удаление сохраняются в файл,
     * а нумерация после перезапуска продолжается.
     */
    @Test
    void testUpdateAndRemoveAreSaved(@TempDir Path dir) {
        String file = dir.resolve("recipes.json").toString();
        RecipeRepository first = new RecipeRepository(file);
        first.add(USER_ID, "Борщ", List.of("свёкла"), "Варить");
        first.add(USER_ID, "Щи", List.of("капуста"), "Тушить");
        first.add(USER_ID, "Окрошка", List.of("квас"), "Смешать");
        first.update(USER_ID, 1, "Солянка", List.of("колбаса"), "Варить");
        first.remove(USER_ID, 2);

        RecipeRepository second = new RecipeRepository(file);

        Assertions.assertEquals("Солянка", second.find(USER_ID, 1).getName());
        Assertions.assertEquals("Окрошка", second.find(USER_ID, 2).getName());
        Assertions.assertEquals(2, second.find(USER_ID, 2).getId());
        Assertions.assertNull(second.find(USER_ID, 3));
        Assertions.assertEquals(3,
                second.add(USER_ID, "Рассольник", List.of("огурцы"), "Варить").getId());
    }

    /**
     * Тестирование первого запуска: файла ещё нет, хранилище пустое.
     */
    @Test
    void testMissingFileGivesEmptyRepository(@TempDir Path dir) {
        RecipeRepository fresh = new RecipeRepository(dir.resolve("recipes.json").toString());

        Assertions.assertTrue(fresh.findAll(USER_ID).isEmpty());
    }

    /**
     * Тестирование пустого файла: хранилище пустое.
     */
    @Test
    void testEmptyFileGivesEmptyRepository(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("recipes.json");
        Files.writeString(file, "");

        RecipeRepository fresh = new RecipeRepository(file.toString());

        Assertions.assertTrue(fresh.findAll(USER_ID).isEmpty());
    }

    /**
     * Тестирование того, что текст, который вводят пользователи (кавычки, переносы
     * строк, эмодзи), после записи и чтения не меняется.
     */
    @Test
    void testFreeTextSurvivesRestart(@TempDir Path dir) {
        String file = dir.resolve("recipes.json").toString();
        String name = "Борщ \"по-домашнему\"";
        String instruction = "Шаг 1: нарезать\nШаг 2: варить \"до готовности\" 🍲";
        new RecipeRepository(file).add(USER_ID, name, List.of("соль"), instruction);

        Recipe loaded = new RecipeRepository(file).find(USER_ID, 1);

        Assertions.assertEquals(name, loaded.getName());
        Assertions.assertEquals(instruction, loaded.getInstruction());
    }

    /**
     * Тестирование повреждённого файла: запуск завершается ошибкой, а файл не меняется,
     * чтобы следующее сохранение не затёрло остатки данных.
     */
    @Test
    void testCorruptedFileIsNotTreatedAsEmpty(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("recipes.json");
        String broken = "{\"1\": [{\"id\": 1,";
        Files.writeString(file, broken);

        Assertions.assertThrows(RuntimeException.class,
                () -> new RecipeRepository(file.toString()));

        Assertions.assertEquals(broken, Files.readString(file));
    }

    /**
     * Тестирование файла с устаревшими номерами (пропуски после удалений): при чтении
     * номера приводятся к позициям, а новый рецепт не получает уже занятый номер.
     */
    @Test
    void testStaleNumbersInFileAreRenumbered(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("recipes.json");
        Files.writeString(file, """
                {
                  "1": [
                    {"id": 1, "name": "Борщ", "ingredients": ["свёкла"], "instruction": "Варить"},
                    {"id": 3, "name": "Щи", "ingredients": ["капуста"], "instruction": "Тушить"},
                    {"id": 4, "name": "Окрошка", "ingredients": ["квас"], "instruction": "Смешать"},
                    {"id": 5, "name": "Солянка", "ingredients": ["колбаса"], "instruction": "Варить"}
                  ]
                }""");

        RecipeRepository loaded = new RecipeRepository(file.toString());

        for (int number = 1; number <= 4; number++) {
            Assertions.assertEquals(number, loaded.find(USER_ID, number).getId());
        }
        Recipe added = loaded.add(USER_ID, "Рассольник", List.of("огурцы"), "Варить");
        Assertions.assertEquals(5, added.getId());
        Assertions.assertEquals(5, loaded.findAll(USER_ID).size());
    }
}