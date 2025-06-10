package server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import websocket.GameSessionManager;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.ServerMessage;
import websocket.MessageSerializer;

@WebSocket
public class WebSocketHandler {

    private static final Map<Integer, GameSessionManager> gameSessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Client connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        System.out.println("📨 Message received: " + message);

        try {
            UserGameCommand command = websocket.MessageSerializer.deserialize(message);

            if (command == null || command.getCommandType() == null) {
                session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Invalid command")));
                return;
            }

            switch (command.getCommandType()) {
                case CONNECT:
                    handleConnect(session, command);
                    break;
                case MAKE_MOVE:
                    handleMakeMove(session, command);
                    break;
                case LEAVE:
                    handleLeave(session, command);
                    break;
                case RESIGN:
                    handleResign(session, command);
                    break;
                default:
                    session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Unknown command")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Failed to parse command")));
        }
    }

    private void handleMakeMove(Session session, UserGameCommand command) {
        System.out.println("Handling Make Move for gameID " + command.getGameID() + ", authToken: " + command.getAuthToken());
        GameSessionManager manager = gameSessions.get(command.getGameID());
        if (manager == null) {
            System.out.println("No session manager found for gameID " + command.getGameID());
            return;
        }

        String username = manager.getUsername(session);
        if (username == null) {
            System.out.println("Could not identify user from session.");
            return;
        }

        String moveMessage = String.format("%s attempted a move", username);
        websocket.messages.NotificationMessage notification = new websocket.messages.NotificationMessage(moveMessage);

        manager.broadcastExcept(gson.toJson(notification), session);
    }

    private void handleConnect(Session session, UserGameCommand command) {
        System.out.println("Handling CONNECT for gamID " + command.getGameID() + ", authToken: " + command.getAuthToken());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Connection closed: " + session + " (" + reason + ")");
        for (GameSessionManager manager : gameSessions.values()) {
            manager.remove(session);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error for session " + session + ": " + error.getMessage());
        error.printStackTrace();
    }
}