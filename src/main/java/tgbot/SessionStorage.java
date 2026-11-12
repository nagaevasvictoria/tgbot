package tgbot;

import java.util.HashMap;
import java.util.Map;

/** Хранит состояния диалогов по идентификатору чата, у каждого пользователя своё. */
public class SessionStorage {
    /** Сессии пользователей. */
    private final Map<Long, DialogSession> sessions = new HashMap<>();

    /**
     * Возвращает сессию чата, при первом обращении создаёт новую.
     *
     * @param chatId идентификатор чата
     * @return состояние диалога этого чата
     */
    public DialogSession get(long chatId) {
        DialogSession session = sessions.get(chatId);
        if (session == null) {
            session = new DialogSession();
            sessions.put(chatId, session);
        }
        return session;
    }
}