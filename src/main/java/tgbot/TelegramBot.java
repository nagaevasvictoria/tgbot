package tgbot;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
/**
 * Бот, который обрабатывает сообщения пользователей
 */
public class TelegramBot extends TelegramLongPollingBot {
    /**
     * Обрабатывает команды и текстовые сообщения пользователей
     */
    private final BotLogic botLogic = new BotLogic();
    /**
     * Создаёт объект Telegram-бота и передаёт токен родительскому классу
     */
    public TelegramBot() {
        super(Dotenv.load().get("BOT_TOKEN"));
    }
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String text = update.getMessage().getText();
        String answer = botLogic.botMessage(text);
        String chatId = update.getMessage().getChatId().toString();

        sendMessage(chatId,answer);
    }
    /**
     * Отправляет сообщение в указанный чат
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
    @Override
    public String getBotUsername() {return Dotenv.load().get("BOT_USERNAME");}
}
