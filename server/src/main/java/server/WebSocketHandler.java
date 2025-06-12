package server;

import chess.ChessGame;
import chess.ChessMove;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import websocket.GameSessionManager;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

@WebSocket
public class WebSocketHandler {

    private static final Map<Integer, GameSessionManager> gameSessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static DataAccess sharedDB;
    private final DataAccess db;
    private static final Map<Integer, ChessGame> activeGames = new ConcurrentHashMap<>();

    public WebSocketHandler() {
        this.db = sharedDB;
        System.out.println("\uD83D\uDE80 WebSocketHandler created. sharedDB = " + sharedDB);
    }

    public static void setSharedDB(DataAccess db) {
        sharedDB = db;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Client connected: " + session);
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

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        try {
            UserGameCommand command = websocket.MessageSerializer.deserialize(message);

            if (command == null || command.getCommandType() == null) {
                session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Invalid command")));
                return;
            }

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(session, command);
                case MAKE_MOVE -> handleMakeMove(session, command);
                case LEAVE -> handleLeave(session, command);
                case RESIGN -> handleResign(session, command);
                default -> session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Unknown command")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Failed to parse command")));
        }
    }

    private void handleConnect(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();

        gameSessions.putIfAbsent(gameID, new GameSessionManager());
        GameSessionManager manager = gameSessions.get(gameID);

        GameData gameData;
        try {
            gameData = db.getGame(gameID);
            if (gameData == null) {
                session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Game not found")));
                return;
            }
            ChessGame game = gameData.getGame();
            game.setBoard(gameData.getBoard());
            activeGames.put(gameID, game);
        } catch (Exception e) {
            try {
                session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Failed to access game data")));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return;
        }

        manager.setGameData(gameID, gameData);
        manager.add(session, authToken);

        String username;
        try {
            username = db.getUsernameFromAuth(authToken);
            manager.add(session, username);
        } catch (Exception e) {
            try {
                session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Failed to retrieve username")));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return;
        }
        if (username.equals(gameData.whiteUsername()) || username.equals(gameData.blackUsername())) {
            // Already assigned; do nothing
        } else if (gameData.whiteUsername() == null) {
            gameData.setWhiteUsername(username);
        } else if (gameData.blackUsername() == null) {
            gameData.setBlackUsername(username);
        }

        String role = username.equals(gameData.whiteUsername()) ? "white" :
                username.equals(gameData.blackUsername()) ? "black" : "observer";
        String msg = String.format("%s joined as %s", username, role);
        broadcastToOthers(gameID, new NotificationMessage(msg), session);

        boolean yourTurn = (username.equals(gameData.whiteUsername()) && gameData.getGame().getTeamTurn() == ChessGame.TeamColor.WHITE)
                || (username.equals(gameData.blackUsername()) && gameData.getGame().getTeamTurn() == ChessGame.TeamColor.BLACK);

        LoadGameMessage loadGameMsg = new LoadGameMessage(gameData, yourTurn);
        try {
            session.getRemote().sendString(gson.toJson(loadGameMsg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMakeMove(Session session, UserGameCommand command) {
        MakeMoveCommand moveCommand = (MakeMoveCommand) command;
        ChessMove move = moveCommand.getMove();

        GameSessionManager manager = gameSessions.get(command.getGameID());
        if (manager == null) {
            System.out.println("manager is null");
            return;
        }

        String username = manager.getUsername(session);
        if (username == null) {
            System.out.println("username is null");
            return;
        }

        ChessGame game = manager.getGame(command.getGameID());
        if (game == null) {
            System.out.println("game is null");
            return;
        }

        GameData gameData = manager.getGameData(command.getGameID());
        if (gameData == null) {
            System.out.println("gamedata is null");
            return;
        }

        Collection<ChessMove> validMoves = game.validMoves(move.getStartPosition());
        if (validMoves == null || !validMoves.contains(move)) {
            sendError(session, "Illegal move");
            System.out.println("no valid moves");
            return;
        }

        boolean isWhite = username.equals(gameData.whiteUsername());
        boolean isBlack = username.equals(gameData.blackUsername());
        ChessGame.TeamColor playerColor = isWhite ? ChessGame.TeamColor.WHITE : isBlack ? ChessGame.TeamColor.BLACK : null;


        System.out.println("DEBUG — TeamTurn is " + game.getTeamTurn());
        System.out.println("DEBUG — playerColor is " + playerColor);
        System.out.println("DEBUG — game.whiteUsername = " + gameData.whiteUsername());
        System.out.println("DEBUG — username = " + username);
        if (playerColor == null || playerColor != game.getTeamTurn()) {
            sendError(session, "It's not your turn.");
            System.out.println("not your turn");
            return;
        }

        try {
            game.makeMove(move, playerColor);
            db.updateGame(command.getGameID(), game);
            db.updateBoard(command.getGameID(), game.getBoard());
        } catch (Exception e) {
            sendError(session, "Failed to apply move - " + e.getMessage());
            System.out.println("couldn't apply move");
            return;
        }

        try {
            db.updateGameData(command.getGameID(), gameData);
        } catch (DataAccessException e) {
            sendError(session, "Failed to persist gameData: " + e.getMessage());
            System.out.println("gamedata's not responding");
            return;
        }

        System.out.println("✅ Move applied: " + move.getStartPosition() + " -> " + move.getEndPosition());

        for (Session s : manager.getSessions()) {
            String otherUser = manager.getUsername(s);
            boolean theirTurn = (otherUser != null &&
                    ((game.getTeamTurn() == ChessGame.TeamColor.WHITE && otherUser.equals(gameData.whiteUsername())) ||
                            (game.getTeamTurn() == ChessGame.TeamColor.BLACK && otherUser.equals(gameData.blackUsername()))));
            LoadGameMessage updatedGameMsg = new LoadGameMessage(gameData, theirTurn);
            try {
                s.getRemote().sendString(gson.toJson(updatedGameMsg));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String moveMessage = String.format("%s moved %s", username, move.getStartPosition(), move.getEndPosition());
        manager.broadcastExcept(gson.toJson(new NotificationMessage(moveMessage)), session);
    }

    private void handleLeave(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        GameSessionManager manager = gameSessions.get(gameID);
        if (manager == null) return;

        String username = manager.getUsername(session);
        if (username == null) return;

        GameData gameData = manager.getGameData(gameID);
        if (gameData == null) return;

        if (username.equals(gameData.whiteUsername())) gameData.setWhiteUsername(null);
        if (username.equals(gameData.blackUsername())) gameData.setBlackUsername(null);

        broadcastToOthers(gameID, new NotificationMessage(username + " left the game"), session);
        manager.remove(session);
    }

    private void handleResign(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        GameSessionManager manager = gameSessions.get(gameID);
        if (manager == null) return;

        String username = manager.getUsername(session);
        if (username == null) return;

        ChessGame game = manager.getGame(gameID);
        game.setGameOver(true);

        NotificationMessage resignationMsg = new NotificationMessage(username + " has resigned.");
        manager.broadcast(gson.toJson(resignationMsg));
        manager.remove(session);
    }

    private void broadcastToOthers(int gameID, NotificationMessage notificationMessage, Session session) {
        GameSessionManager manager = gameSessions.get(gameID);
        if (manager != null) {
            manager.broadcastExcept(gson.toJson(notificationMessage), session);
        }
    }

    private void sendError(Session session, String message) {
        try {
            session.getRemote().sendString(gson.toJson(Map.of(
                    "serverMessageType", "ERROR",
                    "errorMessage", "Error: " + message
            )));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}