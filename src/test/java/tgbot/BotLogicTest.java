package tgbot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Проверяет логику бота. */
class BotLogicTest {
    /** Создаёт логику бота с хранилищем в памяти. */
    private BotLogic createLogic() {
        return new BotLogic(new SessionStorage(), new RecipeRepository(), new IngredientParser());
    }

    /** Добавляет рецепт полным диалогом. */
    private void addRecipe(BotLogic logic, long chatId, String name) {
        logic.handle(chatId, "/add " + name);
        logic.handle(chatId, "Макароны, сыр, сливки");
        logic.handle(chatId, "Отварить макароны.");
    }

    /** Рецепт добавляется пошагово, его видно в списке и в подробностях. */
    @Test
    void shouldAddRecipeStepByStep() {
        BotLogic logic = createLogic();

        Assertions.assertEquals("Укажите ингредиенты через запятую.", logic.handle(1, "/add Паста с сыром"));
        Assertions.assertEquals("Введите инструкцию приготовления.", logic.handle(1, "Макароны, сыр, сливки"));
        Assertions.assertEquals("Рецепт «Паста с сыром» сохранён. ID: 1.", logic.handle(1, "Отварить макароны."));
        Assertions.assertEquals("Ваши рецепты:\n1. Паста с сыром", logic.handle(1, "/list"));
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

        logic.handle(1, "/add Паста");
        logic.handle(2, "/add Омлет");
        logic.handle(1, "Макароны");
        logic.handle(2, "Яйца");
        logic.handle(1, "Отварить");
        logic.handle(2, "Взбить");

        Assertions.assertEquals("Ваши рецепты:\n1. Паста", logic.handle(1, "/list"));
        Assertions.assertEquals("Ваши рецепты:\n2. Омлет", logic.handle(2, "/list"));
    }

    /** Чужой рецепт нельзя посмотреть, изменить или удалить. */
    @Test
    void shouldNotAllowForeignRecipes() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста с сыром");

        Assertions.assertEquals("У вас пока нет рецептов. Добавьте первый командой /add <название>.",
                logic.handle(2, "/list"));
        Assertions.assertEquals("Рецепт с ID 1 не найден.", logic.handle(2, "/recipe 1"));
        Assertions.assertEquals("Рецепт с ID 1 не найден.", logic.handle(2, "/edit 1"));
        Assertions.assertEquals("Рецепт с ID 1 не найден.", logic.handle(2, "/delete 1"));
        Assertions.assertEquals("Ваши рецепты:\n1. Паста с сыром", logic.handle(1, "/list"));
    }

    /** Редактирование проходит те же шаги, «-» оставляет прежнее значение, ID сохраняется. */
    @Test
    void shouldEditRecipeStepByStep() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста с сыром");

        Assertions.assertEquals("Введите новое название или «-», чтобы оставить прежнее.", logic.handle(1, "/edit 1"));
        Assertions.assertEquals("Укажите ингредиенты через запятую или «-», чтобы оставить прежние.",
                logic.handle(1, "-"));
        Assertions.assertEquals("Введите инструкцию приготовления или «-», чтобы оставить прежнюю.",
                logic.handle(1, "Макароны, сыр, сливки, чеснок"));
        Assertions.assertEquals("Рецепт «Паста с сыром» обновлён.", logic.handle(1, "-"));
        Assertions.assertEquals(
                "1. Паста с сыром\nИнгредиенты: макароны, сыр, сливки, чеснок\nИнструкция: Отварить макароны.",
                logic.handle(1, "/recipe 1"));
    }

    /** Рецепт удаляется, после этого список пуст. */
    @Test
    void shouldDeleteRecipe() {
        BotLogic logic = createLogic();
        addRecipe(logic, 1, "Паста с сыром");

        Assertions.assertEquals("Рецепт «Паста с сыром» удалён.", logic.handle(1, "/delete 1"));
        Assertions.assertEquals("У вас пока нет рецептов. Добавьте первый командой /add <название>.",
                logic.handle(1, "/list"));
    }

    /** Команда посреди диалога сбрасывает диалог. */
    @Test
    void shouldResetDialogOnCommand() {
        BotLogic logic = createLogic();

        logic.handle(1, "/add Паста");
        logic.handle(1, "/list");

        Assertions.assertEquals("Не понял. Список команд: /help", logic.handle(1, "Макароны"));
    }
}