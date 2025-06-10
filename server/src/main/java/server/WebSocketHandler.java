package server;

import chess.ChessMove;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import websocket.GameSessionManager;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.NotificationMessage;
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

    private void handleResign(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        System.out.println("Handling RESIGN for gameID " + gameID);

        GameSessionManager manager = gameSessions.get(gameID);
        if (manager == null) {
            System.out.println("No session manager found for gameID " + gameID);
            return;
        }

        String username = manager.getUsername(session);
        if (username == null) {
            System.out.println("Could not identify user from session.");
            return;
        }

        String message = username + " has resigned.";
        websocket.messages.NotificationMessage notification = new websocket.messages.NotificationMessage(message);
        manager.broadcast(gson.toJson(notification));

        String resignationMsg = String.format("%s resigned", username);
        manager.broadcast(gson.toJson(new NotificationMessage(resignationMsg)));

        manager.remove(session);
        System.out.println("Session removed after resignation from game ID " + gameID);
    }

    private void handleLeave(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        System.out.println("Handling LEAVE for gameID " + gameID);

        String msg = String.format("%s left the game", username);
        broadcastToOthers(gameID, new NotificationMessage(msg), session);

        GameSessionManager manager = gameSessions.get(gameID);
        if (manager != null) {
            manager.remove(session);
            System.out.println("Session removed from game ID " + gameID);
        } else {
            System.out.println("No session manager found for gameID " + gameID);
        }
    }

    private void handleMakeMove(Session session, UserGameCommand command) {
        System.out.println("Handling Make Move for gameID " + command.getGameID() + ", authToken: " + command.getAuthToken());
        MakeMoveCommand moveCommand = (MakeMoveCommand) command;
        ChessMove move = moveCommand.getMove();

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

        // Get the game from the manager
        chess.ChessGame game = manager.getGame();
        if (game == null) {
            System.out.println("Game not found for gameID " + command.getGameID());
            return;
        }

        try {
            if (!game.validMoves(move.getStartPosition()).contains(move)) {
                session.getRemote().sendString(gson.toJson(Map.of(
                    "serverMessageType", "ERROR",
                    "errorMessage", "Error: Illegal move"
                )));
                return;
            }

            game.makeMove(move);

            // Broadcast updated game state (placeholder for now)
            websocket.messages.ServerMessage loadGameMsg = new websocket.messages.ServerMessage(websocket.messages.ServerMessage.ServerMessageType.LOAD_GAME);
            String loadGameJson = gson.toJson(loadGameMsg);
            manager.broadcast(loadGameJson);

            // Notify others of the move
            String moveMessage = String.format("%s moved from %s to %s", username,
                    move.getStartPosition(), move.getEndPosition());
            websocket.messages.NotificationMessage notification = new websocket.messages.NotificationMessage(moveMessage);
            manager.broadcastExcept(gson.toJson(notification), session);

            // Check for check or checkmate
            if (game.isInCheck(game.getTeamTurn())) {
                websocket.messages.NotificationMessage checkMsg =
                        new websocket.messages.NotificationMessage(username + " is in check");
                manager.broadcast(gson.toJson(checkMsg));
            }
            // TODO: Detect checkmate/stalemate (if you have methods for it)

        } catch (Exception e) {
            try {
                session.getRemote().sendString(gson.toJson(Map.of(
                    "serverMessageType", "ERROR",
                    "errorMessage", "Error: Failed to apply move"
                )));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void handleConnect(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        String role = (isObserver ? "observer" : color.toString().toLowerCase());
        String msg = String.format("%s joined as %s", username, role);
        broadcastToOthers(gameID, new NotificationMessage(msg), session);
        String msg = String.format("%s joined as an observer", username);
        String authToken = command.getAuthToken();

        System.out.println("Handling CONNECT for gamID " + command.getGameID() + ", authToken: " + command.getAuthToken());

        gameSessions.putIfAbsent(gameID, new GameSessionManager());
        GameSessionManager manager = gameSessions.get(gameID);

        manager.add(session, authToken);

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