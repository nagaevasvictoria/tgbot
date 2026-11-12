package tgbot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Хранит состояния диалогов по идентификатору чата, у каждого пользователя своё. */
public class SessionStorage {
    /** Сессии пользователей. */
    private final Map<Long, DialogSession> sessions = new ConcurrentHashMap<>();

    /**
     * Возвращает сессию чата, при первом обращении создаёт новую.
     *
     * @param chatId идентификатор чата
     * @return состояние диалога этого чата
     */
    public DialogSession get(long chatId) {
        return sessions.computeIfAbsent(chatId, key -> new DialogSession());
    }
}