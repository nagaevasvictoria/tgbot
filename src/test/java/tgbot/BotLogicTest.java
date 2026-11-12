package tgbot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Проверяет логику диалога бота. */
class BotLogicTest {
    /** Создаёт чистую логику бота для каждого теста. */
    private BotLogic createLogic() {
        return new BotLogic(new SessionStorage(), new RecipeRepository(), new IngredientReader());
    }

    /** Добавляет рецепт с ингредиентами «макароны, сыр» и инструкцией «Варить». */
    private void addRecipe(BotLogic logic, long chatId, String name) {
        logic.handle(chatId, "/add " + name);
        logic.handle(chatId, "макароны, сыр");
        logic.handle(chatId, "Варить");
    }

    /** Рецепт добавляется за три шага и потом показывается. */
    @Test
    void shouldAddRecipeStepByStep() {
        BotLogic logic = createLogic();

        Assertions.assertEquals("Укажите ингредиенты через запятую.",
                logic.handle(1, "/add Паста с сыром"));
        Assertions.assertEquals("Введите инструкцию приготовления.",
                logic.handle(1, "Макароны, сыр, сливки"));
        Assertions.assertEquals("Рецепт «Паста с сыром» сохранён. ID: 1.",
                logic.handle(1, "Отварить макароны."));
        Assertions.assertEquals(
                "1. Паста с сыром\nИнгредиенты: макароны, сыр, сливки\nИнструкция: Отварить макароны.",
                logic.handle(1, "/recipe 1"));
    }

    /** Повторное добавление рецепта с тем же названием отклоняется. */
    @Test
    void shouldRejectDuplicateName() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста с сыром");

        Assertions.assertEquals(
                "Рецепт «Паста с сыром» уже есть в вашей книге (ID: 1). Чтобы создать новый, "
                        + "выберите другое название или сначала удалите старый командой /delete <id>.",
                logic.handle(1, "/add паста с сыром"));
        Assertions.assertEquals("Не понял. Список команд: /help", logic.handle(1, "Макароны"));
    }

    /** Два пользователя ведут диалоги одновременно, рецепты и состояния не смешиваются. */
    @Test
    void shouldKeepUsersSeparate() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста");

        Assertions.assertEquals("У вас пока нет рецептов. Добавьте первый командой /add <название>.",
                logic.handle(2, "/list"));
        Assertions.assertEquals("Рецепт с ID 1 не найден.", logic.handle(2, "/recipe 1"));

        logic.handle(1, "/add Омлет");
        Assertions.assertEquals("Не понял. Список команд: /help", logic.handle(2, "яйца"));

        Assertions.assertEquals("Рецепт «Борщ» сохранён. ID: 1.", saveAndGetAnswer(logic, 2, "Борщ"));
        Assertions.assertEquals("Ваши рецепты:\n1. Паста", logic.handle(1, "/list"));
    }

    /** Сохраняет рецепт и возвращает ответ на последний шаг. */
    private String saveAndGetAnswer(BotLogic logic, long chatId, String name) {
        logic.handle(chatId, "/add " + name);
        logic.handle(chatId, "свёкла");
        return logic.handle(chatId, "Варить");
    }

    /** После удаления рецепта номера следующих сдвигаются. */
    @Test
    void shouldRenumberAfterDelete() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Первый");
        addRecipe(logic, 1, "Второй");
        addRecipe(logic, 1, "Третий");

        Assertions.assertEquals("Рецепт «Второй» удалён.", logic.handle(1, "/delete 2"));
        Assertions.assertEquals("Ваши рецепты:\n1. Первый\n2. Третий", logic.handle(1, "/list"));
    }

    /** Рецепт редактируется за три шага. */
    @Test
    void shouldEditRecipe() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста");

        Assertions.assertEquals("Введите новое название или «-», чтобы оставить прежнее.",
                logic.handle(1, "/edit 1"));
        Assertions.assertEquals("Укажите ингредиенты через запятую или «-», чтобы оставить прежние.",
                logic.handle(1, "Борщ"));
        Assertions.assertEquals("Введите инструкцию приготовления или «-», чтобы оставить прежнюю.",
                logic.handle(1, "свёкла, мясо"));
        Assertions.assertEquals("Рецепт «Борщ» обновлён.", logic.handle(1, "Варить долго"));
        Assertions.assertEquals("1. Борщ\nИнгредиенты: свёкла, мясо\nИнструкция: Варить долго",
                logic.handle(1, "/recipe 1"));
    }

    /** Тире при редактировании оставляет прежние значения. */
    @Test
    void shouldKeepOldValuesWithDash() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста");

        logic.handle(1, "/edit 1");
        logic.handle(1, "-");
        logic.handle(1, "-");

        Assertions.assertEquals("Рецепт «Паста» обновлён.", logic.handle(1, "-"));
        Assertions.assertEquals("1. Паста\nИнгредиенты: макароны, сыр\nИнструкция: Варить",
                logic.handle(1, "/recipe 1"));
    }

    /** При редактировании нельзя взять название другого своего рецепта. */
    @Test
    void shouldRejectEditToExistingName() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста");
        addRecipe(logic, 1, "Борщ");

        logic.handle(1, "/edit 2");

        Assertions.assertEquals(
                "Рецепт «Паста» уже есть в вашей книге (ID: 1). Введите другое название или «-».",
                logic.handle(1, "Паста"));
    }

    /** Новая команда посреди диалога сбрасывает его. */
    @Test
    void shouldResetDialogOnCommand() {
        BotLogic logic = createLogic();

        logic.handle(1, "/add Паста");

        Assertions.assertEquals("У вас пока нет рецептов. Добавьте первый командой /add <название>.",
                logic.handle(1, "/list"));
        Assertions.assertEquals("Не понял. Список команд: /help", logic.handle(1, "макароны"));
    }

    /** Пустой список ингредиентов не принимается, бот просит ввести заново. */
    @Test
    void shouldAskAgainWhenIngredientsEmpty() {
        BotLogic logic = createLogic();

        logic.handle(1, "/add Паста");

        Assertions.assertEquals("Не вижу ингредиентов. Укажите их через запятую.", logic.handle(1, " , "));
        Assertions.assertEquals("Введите инструкцию приготовления.", logic.handle(1, "сыр"));
    }

    /** Неверные и пустые ID обрабатываются сообщением, а не ошибкой. */
    @Test
    void shouldHandleInvalidIds() {
        BotLogic logic = createLogic();

        Assertions.assertEquals("Рецепт с ID abc не найден.", logic.handle(1, "/recipe abc"));
        Assertions.assertEquals("Укажите ID рецепта: /recipe <id>", logic.handle(1, "/recipe"));
        Assertions.assertEquals("Рецепт с ID 5 не найден.", logic.handle(1, "/delete 5"));
        Assertions.assertEquals("Укажите ID рецепта: /edit <id>", logic.handle(1, "/edit"));
    }

    /** Приветствие, справка и неизвестные сообщения. */
    @Test
    void shouldAnswerStartHelpAndUnknown() {
        BotLogic logic = createLogic();

        Assertions.assertEquals("Это бот для хранения личных рецептов.\nСписок команд: /help.",
                logic.handle(1, "/start"));
        Assertions.assertTrue(logic.handle(1, "/help").contains("/add"));
        Assertions.assertEquals("Неизвестная команда. Список команд: /help", logic.handle(1, "/abc"));
        Assertions.assertEquals("Не понял. Список команд: /help", logic.handle(1, "привет"));
    }
}