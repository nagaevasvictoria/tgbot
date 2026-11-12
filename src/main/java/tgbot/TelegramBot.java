package tgbot;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Бот, который обрабатывает сообщения пользователей.
 */
public class TelegramBot extends TelegramLongPollingBot {

    /**
     * Имя файла с рецептами.
     */
    private static final String RECIPES_FILE = "recipes.json";

    /**
     * Настройки из файла .env, загружаются один раз.
     */
    private static final Dotenv DOTENV = Dotenv.load();

    /**
     * Логика диалога бота.
     */
    private final BotLogic botLogic;

    /**
     * Создаёт объект Telegram-бота и передаёт токен родительскому классу.
     */
    public TelegramBot() {
        super(DOTENV.get("BOT_TOKEN"));
        botLogic = new BotLogic(
                new SessionStorage(),
                new RecipeRepository(RECIPES_FILE),
                new IngredientReader());
    }

    /**
     * Обрабатывает команды и текстовые сообщения пользователей.
     *
     * @param update входящее обновление от Telegram
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        String answer = botLogic.handle(chatId, text);

        sendMessage(String.valueOf(chatId), answer);
    }

    /**
     * Отправляет сообщение в указанный чат.
     *
     * @param chatId идентификатор чата
     * @param text текст ответа
     */
    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException exception) {
            throw new IllegalStateException(
                    "Не удалось отправить сообщение в Telegram",
                    exception
            );
        }
    }

    /**
     * Возвращает имя бота из настроек.
     *
     * @return имя бота
     */
    @Override
    public String getBotUsername() {
        return DOTENV.get("BOT_USERNAME");
    }
}