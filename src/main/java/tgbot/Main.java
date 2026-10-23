package tgbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
/**
 * Главный класс приложения для запуска бота
 */
public class Main {
    /**
     * Запускает приложение и регистрирует бота
     *
     * @param args аргументы командной строки
     * @throws Exception если произошла ошибка при регистрации бота
     */
    public static void main(String[] args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new TelegramBot());
    }
}