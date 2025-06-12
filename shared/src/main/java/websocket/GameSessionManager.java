package websocket;

import chess.ChessGame;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionManager {

    private final Map<Session, String> sessionToUsername = new ConcurrentHashMap<>();

    public void add(Session session, String username) {
        sessionToUsername.put(session, username);
    }

    public void remove(Session session) {
        sessionToUsername.remove(session);
    }

    public String getUsername(Session session) {
        return sessionToUsername.get(session);
    }

    public Set<Session> getAllSessions() {
        return sessionToUsername.keySet();
    }

    private final Map<Integer, GameData> gameDataMap = new ConcurrentHashMap<>();

    public void broadcast(String message) {
        for (Session s : getAllSessions()) {
            try {
                s.getRemote().sendString(message);
            } catch (IOException e) {
                System.err.println("Failed to send message: " + e.getMessage());
            }
        }
    }

    public void broadcastExcept(String message, Session excludedSession) {
        for (Session s : getAllSessions()) {
            if (!s.equals(excludedSession)) {
                try {
                    s.getRemote().sendString(message);
                } catch (IOException e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                }
            }
        }
    }

    public GameData getGameData(int gameID) {
        return gameDataMap.get(gameID);
    }

    public void setGameData(int gameID, GameData gameData) {
        gameDataMap.put(gameID, gameData);
    }

    public ChessGame getGame(int gameID) {
        GameData data = getGameData(gameID);
        return data != null ? data.game() : null;
    }

    public Session[] getSessions() {
        Set<Session> sessions = sessionToUsername.keySet();
        return sessions.toArray(new Session[0]);
    }
}