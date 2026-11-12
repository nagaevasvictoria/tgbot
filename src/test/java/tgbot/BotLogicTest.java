package tgbot;

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Тесты для логики работы бота
 */
public class BotLogicTest {

    /**
     * Идентификатор первого пользователя.
     */
    private static final long USER_ID = 1;

    /**
     * Идентификатор второго пользователя.
     */
    private static final long OTHER_USER_ID = 2;

    /**
     * Ответ бота на текст вне диалога.
     */
    private static final String NOT_UNDERSTOOD = "Не понял. Список команд: /help";

    /**
     * Ответ бота, когда у пользователя нет рецептов.
     */
    private static final String NO_RECIPES =
            "У вас пока нет рецептов. Добавьте первый командой /add <название>.";

    /**
     * Логика бота с чистыми хранилищами в памяти.
     */
    private final BotLogic botLogic = new BotLogic(new SessionStorage(),
            new RecipeRepository(), new IngredientReader());

    /**
     * Добавляет рецепт через диалог: команда /add, ингредиенты, инструкция.
     */
    private void addRecipe(long userId, String name, String ingredients,
                           String instruction) {
        addRecipe(botLogic, userId, name, ingredients, instruction);
    }

    /**
     * Добавляет рецепт через диалог в указанной логике бота.
     */
    private static void addRecipe(BotLogic logic, long userId, String name,
                                  String ingredients, String instruction) {
        logic.handle(userId, "/add " + name);
        logic.handle(userId, ingredients);
        logic.handle(userId, instruction);
    }

    /**
     * Тестирование неизвестной команды.
     */
    @Test
    void testUnknownCommand() {
        Assertions.assertEquals("Неизвестная команда. Список команд: /help",
                botLogic.handle(USER_ID, "/foo"));
    }

    /**
     * Тестирование текста вне диалога.
     */
    @Test
    void testTextWithoutDialog() {
        Assertions.assertEquals(NOT_UNDERSTOOD, botLogic.handle(USER_ID, "привет"));
    }

    /**
     * Тестирование того, что пробелы по краям сообщения игнорируются.
     */
    @Test
    void testMessageIsTrimmed() {
        Assertions.assertEquals(botLogic.handle(USER_ID, "/help"),
                botLogic.handle(USER_ID, "   /help  \n"));
    }

    /**
     * Тестирование команд без ID: бот просит ID и не начинает диалог.
     */
    @Test
    void testCommandsWithoutId() {
        Assertions.assertEquals("Укажите ID рецепта: /recipe <id>",
                botLogic.handle(USER_ID, "/recipe"));
        Assertions.assertEquals("Укажите ID рецепта: /delete <id>",
                botLogic.handle(USER_ID, "/delete"));
        Assertions.assertEquals("Укажите ID рецепта: /edit <id>",
                botLogic.handle(USER_ID, "/edit"));
        Assertions.assertEquals(NOT_UNDERSTOOD, botLogic.handle(USER_ID, "текст"));
    }

    /**
     * Тестирование добавления рецепта по шагам диалога.
     */
    @Test
    void testAddRecipe() {
        Assertions.assertEquals("Укажите ингредиенты через запятую.",
                botLogic.handle(USER_ID, "/add Борщ"));
        Assertions.assertEquals("Введите инструкцию приготовления.",
                botLogic.handle(USER_ID, "свёкла"));
        Assertions.assertEquals("Рецепт «Борщ» сохранён.",
                botLogic.handle(USER_ID, "Варить час"));
        Assertions.assertEquals("""
                                1. Борщ
                                Ингредиенты: свёкла
                                Инструкция: Варить час""", botLogic.handle(USER_ID, "/recipe 1"));
    }

    /**
     * Тестирование названия из нескольких слов.
     */
    @Test
    void testAddNameWithSeveralWords() {
        addRecipe(USER_ID, "Борщ с говядиной", "свёкла", "Варить");
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ с говядиной""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование разбора ингредиентов: запятые, переносы строк, десятичная
     * запятая и нижний регистр.
     */
    @Test
    void testAddIngredientsParsing() {
        addRecipe(USER_ID, "Каша", "Молоко, 2,5 стакана воды\nСОЛЬ", "Варить");
        Assertions.assertEquals("""
                                1. Каша
                                Ингредиенты: молоко, 2,5 стакана воды, соль
                                Инструкция: Варить""", botLogic.handle(USER_ID, "/recipe 1"));
    }

    /**
     * Тестирование добавления без названия.
     */
    @Test
    void testAddWithoutName() {
        Assertions.assertEquals("Укажите название: /add <название>",
                botLogic.handle(USER_ID, "/add"));
        Assertions.assertEquals(NOT_UNDERSTOOD, botLogic.handle(USER_ID, "свёкла"));
        Assertions.assertEquals(NO_RECIPES, botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование добавления рецепта с уже существующим названием
     * (без учёта регистра).
     */
    @Test
    void testAddDuplicateName() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        Assertions.assertEquals("Рецепт «Борщ» уже есть в вашей книге (ID: 1). "
                        + "Чтобы создать новый, выберите другое название "
                        + "или сначала удалите старый командой /delete <id>.",
                botLogic.handle(USER_ID, "/add бОрЩ"));
        Assertions.assertEquals(NOT_UNDERSTOOD, botLogic.handle(USER_ID, "капуста"));
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование того, что команда посреди добавления отменяет диалог.
     */
    @Test
    void testCommandCancelsAdd() {
        botLogic.handle(USER_ID, "/add Борщ");
        botLogic.handle(USER_ID, "свёкла");
        botLogic.handle(USER_ID, "/list");
        Assertions.assertEquals(NOT_UNDERSTOOD, botLogic.handle(USER_ID, "Варить"));
        Assertions.assertEquals(NO_RECIPES, botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование списка рецептов.
     */
    @Test
    void testList() {
        Assertions.assertEquals(NO_RECIPES, botLogic.handle(USER_ID, "/list"));
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        addRecipe(USER_ID, "Щи", "капуста", "Тушить");
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ
                                2. Щи""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование просмотра несуществующего рецепта.
     */
    @Test
    void testShowErrors() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        Assertions.assertEquals("Рецепт с ID 2 не найден.",
                botLogic.handle(USER_ID, "/recipe 2"));
        Assertions.assertEquals("Рецепт с ID 0 не найден.",
                botLogic.handle(USER_ID, "/recipe 0"));
        Assertions.assertEquals("Рецепт с ID -1 не найден.",
                botLogic.handle(USER_ID, "/recipe -1"));
        Assertions.assertEquals("Рецепт с ID abc не найден.",
                botLogic.handle(USER_ID, "/recipe abc"));
        Assertions.assertEquals("Рецепт с ID 99999999999 не найден.",
                botLogic.handle(USER_ID, "/recipe 99999999999"));
    }

    /**
     * Тестирование того, что у разных пользователей разные рецепты.
     */
    @Test
    void testUsersHaveSeparateRecipes() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        Assertions.assertEquals(NO_RECIPES, botLogic.handle(OTHER_USER_ID, "/list"));
        Assertions.assertEquals("Рецепт с ID 1 не найден.",
                botLogic.handle(OTHER_USER_ID, "/recipe 1"));
        Assertions.assertEquals("Рецепт с ID 1 не найден.",
                botLogic.handle(OTHER_USER_ID, "/delete 1"));
        Assertions.assertEquals("Рецепт с ID 1 не найден.",
                botLogic.handle(OTHER_USER_ID, "/edit 1"));
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование того, что диалоги разных пользователей не мешают друг другу.
     */
    @Test
    void testUsersHaveSeparateDialogs() {
        botLogic.handle(USER_ID, "/add Борщ");
        botLogic.handle(OTHER_USER_ID, "/add Щи");
        botLogic.handle(USER_ID, "свёкла");
        botLogic.handle(OTHER_USER_ID, "капуста");
        botLogic.handle(OTHER_USER_ID, "Тушить");
        botLogic.handle(USER_ID, "Варить");
        Assertions.assertEquals("""
                                1. Борщ
                                Ингредиенты: свёкла
                                Инструкция: Варить""", botLogic.handle(USER_ID, "/recipe 1"));
        Assertions.assertEquals("""
                                1. Щи
                                Ингредиенты: капуста
                                Инструкция: Тушить""", botLogic.handle(OTHER_USER_ID, "/recipe 1"));
    }

    /**
     * Тестирование удаления: номера следующих рецептов сдвигаются.
     */
    @Test
    void testDeleteShiftsNumbers() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        addRecipe(USER_ID, "Щи", "капуста", "Тушить");
        addRecipe(USER_ID, "Окрошка", "квас", "Смешать");
        Assertions.assertEquals("Рецепт «Щи» удалён.",
                botLogic.handle(USER_ID, "/delete 2"));
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ
                                2. Окрошка""", botLogic.handle(USER_ID, "/list"));
        Assertions.assertEquals("""
                                2. Окрошка
                                Ингредиенты: квас
                                Инструкция: Смешать""", botLogic.handle(USER_ID, "/recipe 2"));
        Assertions.assertEquals("Рецепт с ID 3 не найден.",
                botLogic.handle(USER_ID, "/recipe 3"));
    }

    /**
     * Тестирование удаления несуществующего рецепта.
     */
    @Test
    void testDeleteErrors() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        Assertions.assertEquals("Рецепт с ID 5 не найден.",
                botLogic.handle(USER_ID, "/delete 5"));
        Assertions.assertEquals("Рецепт с ID x не найден.",
                botLogic.handle(USER_ID, "/delete x"));
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование изменения всех полей рецепта.
     */
    @Test
    void testEditAllFields() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        Assertions.assertEquals("Введите новое название или «-», чтобы оставить прежнее.",
                botLogic.handle(USER_ID, "/edit 1"));
        Assertions.assertEquals(
                "Укажите ингредиенты через запятую или «-», чтобы оставить прежние.",
                botLogic.handle(USER_ID, "Щи"));
        Assertions.assertEquals(
                "Введите инструкцию приготовления или «-», чтобы оставить прежнюю.",
                botLogic.handle(USER_ID, "Капуста, морковь"));
        Assertions.assertEquals("Рецепт «Щи» обновлён.",
                botLogic.handle(USER_ID, "Тушить"));
        Assertions.assertEquals("""
                                1. Щи
                                Ингредиенты: капуста, морковь
                                Инструкция: Тушить""", botLogic.handle(USER_ID, "/recipe 1"));
    }

    /**
     * Тестирование того, что «-» оставляет все поля прежними.
     */
    @Test
    void testEditWithDashes() {
        addRecipe(USER_ID, "Борщ", "свёкла, капуста", "Варить");
        botLogic.handle(USER_ID, "/edit 1");
        botLogic.handle(USER_ID, "-");
        botLogic.handle(USER_ID, "-");
        Assertions.assertEquals("Рецепт «Борщ» обновлён.",
                botLogic.handle(USER_ID, "-"));
        Assertions.assertEquals("""
                                1. Борщ
                                Ингредиенты: свёкла, капуста
                                Инструкция: Варить""", botLogic.handle(USER_ID, "/recipe 1"));
    }

    /**
     * Тестирование того, что меняется только выбранный рецепт.
     */
    @Test
    void testEditChangesOnlyChosenRecipe() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        addRecipe(USER_ID, "Щи", "капуста", "Тушить");
        botLogic.handle(USER_ID, "/edit 2");
        botLogic.handle(USER_ID, "Солянка");
        botLogic.handle(USER_ID, "-");
        botLogic.handle(USER_ID, "-");
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ
                                2. Солянка""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование редактирования несуществующего рецепта.
     */
    @Test
    void testEditErrors() {
        Assertions.assertEquals("Рецепт с ID 3 не найден.",
                botLogic.handle(USER_ID, "/edit 3"));
        Assertions.assertEquals(NOT_UNDERSTOOD, botLogic.handle(USER_ID, "Новое имя"));
    }

    /**
     * Тестирование нового названия, которое уже занято другим рецептом:
     * бот остаётся на шаге названия.
     */
    @Test
    void testEditDuplicateName() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        addRecipe(USER_ID, "Щи", "капуста", "Тушить");
        botLogic.handle(USER_ID, "/edit 2");
        Assertions.assertEquals("Рецепт «борщ» уже есть в вашей книге (ID: 1). "
                        + "Введите другое название или «-».",
                botLogic.handle(USER_ID, "борщ"));
        Assertions.assertEquals(
                "Укажите ингредиенты через запятую или «-», чтобы оставить прежние.",
                botLogic.handle(USER_ID, "Солянка"));
        botLogic.handle(USER_ID, "-");
        botLogic.handle(USER_ID, "-");
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Борщ
                                2. Солянка""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование изменения регистра в собственном названии рецепта.
     */
    @Test
    void testEditOwnNameCase() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        botLogic.handle(USER_ID, "/edit 1");
        Assertions.assertEquals(
                "Укажите ингредиенты через запятую или «-», чтобы оставить прежние.",
                botLogic.handle(USER_ID, "БОРЩ"));
        botLogic.handle(USER_ID, "-");
        botLogic.handle(USER_ID, "-");
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. БОРЩ""", botLogic.handle(USER_ID, "/list"));
    }

    /**
     * Тестирование того, что команда посреди редактирования отменяет его:
     * введённые ранее значения не сохраняются, а следующий текст не считается ответом.
     */
    @Test
    void testCommandCancelsEdit() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        botLogic.handle(USER_ID, "/edit 1");
        botLogic.handle(USER_ID, "Щи");
        botLogic.handle(USER_ID, "капуста");
        botLogic.handle(USER_ID, "/list");
        Assertions.assertEquals(NOT_UNDERSTOOD, botLogic.handle(USER_ID, "-"));
        Assertions.assertEquals("""
                                1. Борщ
                                Ингредиенты: свёкла
                                Инструкция: Варить""", botLogic.handle(USER_ID, "/recipe 1"));
    }


    /**
     * Тестирование того, что у разных пользователей могут быть рецепты с одинаковым названием.
     */
    @Test
    void testSameNameForDifferentUsers() {
        addRecipe(USER_ID, "Борщ", "свёкла", "Варить");
        Assertions.assertEquals("Укажите ингредиенты через запятую.",
                botLogic.handle(OTHER_USER_ID, "/add Борщ"));
    }

    /**
     * Тестирование того, что после перезапуска бота (новая логика на том же файле)
     * сохраняются рецепты, правки и удаления, а нумерация продолжается.
     */
    @Test
    void testRecipesSurviveRestart(@TempDir Path dir) {
        String file = dir.resolve("recipes.json").toString();
        BotLogic first = new BotLogic(new SessionStorage(),
                new RecipeRepository(file), new IngredientReader());
        addRecipe(first, USER_ID, "Борщ", "свёкла", "Варить");
        addRecipe(first, USER_ID, "Щи", "капуста", "Тушить");
        addRecipe(first, USER_ID, "Окрошка", "квас", "Смешать");
        first.handle(USER_ID, "/delete 2");
        first.handle(USER_ID, "/edit 1");
        first.handle(USER_ID, "Солянка");
        first.handle(USER_ID, "-");
        first.handle(USER_ID, "Варить два часа");

        BotLogic second = new BotLogic(new SessionStorage(),
                new RecipeRepository(file), new IngredientReader());

        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Солянка
                                2. Окрошка""", second.handle(USER_ID, "/list"));
        Assertions.assertEquals("""
                                1. Солянка
                                Ингредиенты: свёкла
                                Инструкция: Варить два часа""", second.handle(USER_ID, "/recipe 1"));
        addRecipe(second, USER_ID, "Рассольник", "огурцы", "Варить");
        Assertions.assertEquals("""
                                Ваши рецепты:
                                1. Солянка
                                2. Окрошка
                                3. Рассольник""", second.handle(USER_ID, "/list"));
    }
}