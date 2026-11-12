package tgbot;

import java.util.List;
import java.util.Optional;

/** Логика диалога: принимает сообщение пользователя и формирует ответ бота. */
public class BotLogic {
    /** Текст приветствия. */
    private static final String START_TEXT = "Это бот для хранения личных рецептов.\nСписок команд: /help.";
    /** Текст справки. */
    private static final String HELP_TEXT = """
            Как пользоваться ботом:
            /add <название> - добавить рецепт
            /list - список ваших рецептов
            /recipe <id> - показать рецепт
            /edit <id> - изменить рецепт
            /delete <id> - удалить рецепт
            /help - показать эту инструкцию

            Ингредиенты пишите через запятую или с новой строки.
            Дробные числа можно писать и через запятую, и через точку.""";

    /** Состояния диалогов пользователей. */
    private final SessionStorage sessions;
    /** Хранилище рецептов. */
    private final RecipeRepository recipes;
    /** Чтение ингредиентов из текста. */
    private final IngredientReader reader;

    /**
     * Создаёт логику бота с заданными зависимостями.
     *
     * @param sessions хранилище состояний диалогов
     * @param recipes хранилище рецептов
     * @param reader чтение ингредиентов
     */
    public BotLogic(SessionStorage sessions, RecipeRepository recipes, IngredientReader reader) {
        this.sessions = sessions;
        this.recipes = recipes;
        this.reader = reader;
    }

    /**
     * Обрабатывает сообщение пользователя: команда начинает новое действие,
     * обычный текст воспринимается как ответ на текущий шаг диалога.
     *
     * @param chatId идентификатор чата
     * @param text текст сообщения
     * @return ответ бота
     */
    public String handle(long chatId, String text) {
        DialogSession session = sessions.get(chatId);
        String message = text.trim();
        if (message.startsWith("/")) {
            session.reset();
            return handleCommand(chatId, session, message);
        }
        return handleStep(chatId, session, message);
    }

    /** Выполняет команду. */
    private String handleCommand(long chatId, DialogSession session, String message) {
        String[] parts = message.split("\\s+", 2);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        return switch (parts[0]) {
            case "/start" -> START_TEXT;
            case "/help" -> HELP_TEXT;
            case "/add" -> startAdd(chatId, session, argument);
            case "/list" -> list(chatId);
            case "/recipe" -> show(chatId, argument);
            case "/edit" -> startEdit(chatId, session, argument);
            case "/delete" -> delete(chatId, argument);
            default -> "Неизвестная команда. Список команд: /help";
        };
    }

    /** Обрабатывает ответ пользователя на текущем шаге диалога. */
    private String handleStep(long chatId, DialogSession session, String text) {
        return switch (session.getStep()) {
            case ADD_INGREDIENTS -> receiveIngredients(session, text);
            case ADD_INSTRUCTION -> receiveInstruction(chatId, session, text);
            case EDIT_NAME -> receiveEditedName(chatId, session, text);
            case EDIT_INGREDIENTS -> receiveEditedIngredients(chatId, session, text);
            case EDIT_INSTRUCTION -> receiveEditedInstruction(chatId, session, text);
            case IDLE -> "Не понял. Список команд: /help";
        };
    }

    /** Начинает добавление рецепта. */
    private String startAdd(long chatId, DialogSession session, String name) {
        if (name.isEmpty()) {
            return "Укажите название: /add <название>";
        }
        Optional<Recipe> existing = recipes.findByName(chatId, name);
        if (existing.isPresent()) {
            return "Рецепт «" + existing.get().getName() + "» уже есть в вашей книге (ID: "
                    + existing.get().getId() + "). Чтобы создать новый, выберите другое название "
                    + "или сначала удалите старый командой /delete <id>.";
        }
        session.startAdd(name);
        return "Укажите ингредиенты через запятую.";
    }

    /** Принимает ингредиенты нового рецепта. */
    private String receiveIngredients(DialogSession session, String text) {
        List<String> ingredients = reader.read(text);
        if (ingredients.isEmpty()) {
            return "Не вижу ингредиентов. Укажите их через запятую.";
        }
        session.setIngredients(ingredients);
        return "Введите инструкцию приготовления.";
    }

    /** Принимает инструкцию и сохраняет новый рецепт. */
    private String receiveInstruction(long chatId, DialogSession session, String text) {
        Recipe recipe = recipes.add(chatId, session.getDraftName(), session.getDraftIngredients(), text);
        session.reset();
        return "Рецепт «" + recipe.getName() + "» сохранён. ID: " + recipe.getId() + ".";
    }

    /** Показывает список рецептов пользователя. */
    private String list(long chatId) {
        List<Recipe> all = recipes.findAll(chatId);
        if (all.isEmpty()) {
            return "У вас пока нет рецептов. Добавьте первый командой /add <название>.";
        }
        StringBuilder builder = new StringBuilder("Ваши рецепты:");
        for (Recipe recipe : all) {
            builder.append("\n").append(recipe.getId()).append(". ").append(recipe.getName());
        }
        return builder.toString();
    }

    /** Показывает подробную информацию о рецепте. */
    private String show(long chatId, String idText) {
        if (idText.isEmpty()) {
            return "Укажите ID рецепта: /recipe <id>";
        }
        Optional<Recipe> found = findOwn(chatId, idText);
        if (found.isEmpty()) {
            return notFound(idText);
        }
        Recipe recipe = found.get();
        return recipe.getId() + ". " + recipe.getName()
                + "\nИнгредиенты: " + String.join(", ", recipe.getIngredients())
                + "\nИнструкция: " + recipe.getInstruction();
    }

    /** Удаляет рецепт пользователя. */
    private String delete(long chatId, String idText) {
        if (idText.isEmpty()) {
            return "Укажите ID рецепта: /delete <id>";
        }
        Optional<Recipe> found = findOwn(chatId, idText);
        if (found.isEmpty()) {
            return notFound(idText);
        }
        recipes.remove(chatId, found.get().getId());
        return "Рецепт «" + found.get().getName() + "» удалён.";
    }

    /** Начинает редактирование рецепта. */
    private String startEdit(long chatId, DialogSession session, String idText) {
        if (idText.isEmpty()) {
            return "Укажите ID рецепта: /edit <id>";
        }
        Optional<Recipe> found = findOwn(chatId, idText);
        if (found.isEmpty()) {
            return notFound(idText);
        }
        session.startEdit(found.get().getId());
        return "Введите новое название или «-», чтобы оставить прежнее.";
    }

    /** Принимает новое название рецепта. */
    private String receiveEditedName(long chatId, DialogSession session, String text) {
        Optional<Recipe> recipe = recipes.find(chatId, session.getEditedId());
        if (recipe.isEmpty()) {
            session.reset();
            return "Рецепт больше не существует. Редактирование отменено.";
        }
        String name = text.equals("-") ? recipe.get().getName() : text;
        Optional<Recipe> sameName = recipes.findByName(chatId, name);
        if (sameName.isPresent() && sameName.get().getId() != recipe.get().getId()) {
            return "Рецепт «" + name + "» уже есть в вашей книге (ID: " + sameName.get().getId()
                    + "). Введите другое название или «-».";
        }
        session.setEditedName(name);
        return "Укажите ингредиенты через запятую или «-», чтобы оставить прежние.";
    }

    /** Принимает новые ингредиенты рецепта. */
    private String receiveEditedIngredients(long chatId, DialogSession session, String text) {
        Optional<Recipe> recipe = recipes.find(chatId, session.getEditedId());
        if (recipe.isEmpty()) {
            session.reset();
            return "Рецепт больше не существует. Редактирование отменено.";
        }
        List<String> ingredients = text.equals("-") ? recipe.get().getIngredients() : reader.read(text);
        if (ingredients.isEmpty()) {
            return "Не вижу ингредиентов. Укажите их через запятую или «-».";
        }
        session.setEditedIngredients(ingredients);
        return "Введите инструкцию приготовления или «-», чтобы оставить прежнюю.";
    }

    /** Принимает новую инструкцию и сохраняет изменения. */
    private String receiveEditedInstruction(long chatId, DialogSession session, String text) {
        Optional<Recipe> recipe = recipes.find(chatId, session.getEditedId());
        if (recipe.isEmpty()) {
            session.reset();
            return "Рецепт больше не существует. Редактирование отменено.";
        }
        String instruction = text.equals("-") ? recipe.get().getInstruction() : text;
        String name = session.getDraftName();
        recipes.update(chatId, recipe.get().getId(), name, session.getDraftIngredients(), instruction);
        session.reset();
        return "Рецепт «" + name + "» обновлён.";
    }

    /** Ищет рецепт только среди рецептов этого пользователя. */
    private Optional<Recipe> findOwn(long chatId, String idText) {
        try {
            return recipes.find(chatId, Integer.parseInt(idText));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Формирует сообщение о ненайденном рецепте. */
    private String notFound(String idText) {
        return "Рецепт с ID " + idText + " не найден.";
    }
}