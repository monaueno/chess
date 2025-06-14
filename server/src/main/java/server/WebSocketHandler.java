package server;

import chess.ChessGame;
import chess.ChessMove;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.data.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

@WebSocket
public class WebSocketHandler {

    private static final Map<Integer, GameSessionManager> GAME_SESSIONS = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    private static DataAccess sharedDB;
    private final DataAccess db;
    private static final Map<Integer, ChessGame> ACTIVE_GAMES = new ConcurrentHashMap<>();

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
        for (GameSessionManager manager : GAME_SESSIONS.values()) {
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
                session.getRemote().sendString(GSON.toJson(
                    Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Invalid command")));
                return;
            }

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(session, command);
                case MAKE_MOVE -> handleMakeMove(session, command);
                case LEAVE -> handleLeave(session, command);
                case RESIGN -> handleResign(session, command);
                default -> session.getRemote().sendString(GSON.toJson(Map.of(
                        "serverMessageType",
                        "ERROR",
                        "errorMessage",
                        "Error: Unknown command")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.getRemote().sendString(GSON.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Failed to parse command")));
        }
    }

    private void handleConnect(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();

        GAME_SESSIONS.putIfAbsent(gameID, new GameSessionManager());
        GameSessionManager manager = GAME_SESSIONS.get(gameID);

        GameData gameData;
        try {
            gameData = db.getGame(gameID);
            if (gameData == null) {
                session.getRemote().sendString(GSON.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Game not found")));
                return;
            }
            ChessGame game = gameData.getGame();
            game.setBoard(gameData.getBoard());
            ACTIVE_GAMES.put(gameID, game);
        } catch (Exception e) {
            try {
                session.getRemote().sendString(GSON.toJson(
                    Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Failed to access game data")));
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
                session.getRemote().sendString(GSON.toJson(
                    Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Failed to retrieve username")));
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
            session.getRemote().sendString(GSON.toJson(loadGameMsg));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (gameData.getGame().isGameOver()) {
            return;
        }
    }

    private void handleMakeMove(Session session, UserGameCommand command) {
        GameSessionManager manager = GAME_SESSIONS.get(command.getGameID());
        if (manager == null) {
            System.out.println("manager is null");
            return;
        }

        MakeMoveCommand moveCommand = (MakeMoveCommand) command;
        ChessMove move = moveCommand.getMove();

        String username = manager.getUsername(session);
        if (username == null) {
            System.out.println("username is null");
            return;
        }

        String token = command.getAuthToken();
        String userFromToken;
        try {
            userFromToken = db.getUsernameFromAuth(token);
        } catch (Exception e) {
            sendError(session, "Invalid authentication token.");
            return;
        }
        if (!username.equals(userFromToken)) {
            sendError(session, "Invalid authentication token.");
            return;
        }

        ChessGame game = manager.getGame(command.getGameID());
        if (game == null) {
            System.out.println("game is null");
            return;
        }

        // Prevent moves if the game is over
        if (game.isGameOver()) {
            sendError(session, "Game is over. No moves can be made.");
            return;
        }

        GameData gameData = manager.getGameData(command.getGameID());
        if (gameData == null) {
            System.out.println("gamedata is null");
            return;
        }

        if (!validateMove(session, command, manager, move, game, gameData, username)) {
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

        ChessGame.TeamColor opponent = (username.equals(gameData.whiteUsername())) ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;

        if (game.isInCheckmate(opponent)) {
            game.setGameOver(true);
            String winner = (username.equals(gameData.whiteUsername())) ? "White" : "Black";
            manager.broadcast(GSON.toJson(new NotificationMessage("Checkmate! Game over. " + winner + " wins.")));
        } else if (game.isInStalemate(opponent)) {
            game.setGameOver(true);
            manager.broadcast(GSON.toJson(new NotificationMessage("Stalemate! Game over. It's a draw.")));
        }  else if (game.isInCheck(opponent)) {
            System.out.println("check");
            String checkedPlayer = (opponent == ChessGame.TeamColor.WHITE) ? "white" : "black";
            String checkingPlayer = (opponent == ChessGame.TeamColor.WHITE) ? "black" : "white";
            manager.broadcast(GSON.toJson(new NotificationMessage("Check by " + checkingPlayer + " against " + checkedPlayer + "!")));
        }

        // Notify all other clients of the move
        String moveSummary = username + " moved from "
            + move.getStartPosition() + " to " + move.getEndPosition();
        manager.broadcastExcept(
            GSON.toJson(new NotificationMessage(moveSummary)),
            session
        );

        sendLoadGameToPlayers(manager, gameData, game);
    }

    private void handleLeave(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        GameSessionManager manager = GAME_SESSIONS.get(gameID);
        if (manager == null) {
            return;
        }

        String username = manager.getUsername(session);
        if (username == null) {
            return;
        }

        GameData gameData = manager.getGameData(gameID);
        if (gameData == null) {
            return;
        }

        if (username.equals(gameData.whiteUsername())) {
            gameData.setWhiteUsername(null);
        }
        if (username.equals(gameData.blackUsername())) {
            gameData.setBlackUsername(null);
        }

        // Persist the cleared slot to the database
        try {
            db.updateGameData(gameID, gameData);
        } catch (DataAccessException e) {
            try {
                session.getRemote().sendString(GSON.toJson(
                    Map.of("serverMessageType", "ERROR",
                           "errorMessage", "Error: Failed to persist gameData after leave: " + e.getMessage())));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return;
        }

        broadcastToOthers(gameID, new NotificationMessage(username + " left the game"), session);
        manager.remove(session);
    }

    private void handleResign(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        GameSessionManager manager = GAME_SESSIONS.get(gameID);
        String username = manager.getUsername(session);
        GameData gameData = manager.getGameData(gameID);

        if (!username.equals(gameData.whiteUsername()) && !username.equals(gameData.blackUsername())) {
            sendError(session, "Error: Only players can resign.");
            return;
        }

        if (manager == null) {
            return;
        }

        if (username == null) {
            return;
        }

        ChessGame game = manager.getGame(gameID);
        if (game.isGameOver()) {
            sendError(session, "Game is already over. Cannot resign.");
            return;
        }
        game.setResignedPlayer(username);
        game.setGameOver(true);

        if (gameData != null) {
            try {
                db.updateGameData(gameID, gameData);
            } catch (DataAccessException e) {
                sendError(session, "Failed to update game data: " + e.getMessage());
            }
        }

        try {
            db.updateGame(gameID, game);
        } catch (DataAccessException e) {
            sendError(session, "Failed to update game: " + e.getMessage());
            return;
        }

        String winner = "Opponent";
        if (gameData != null) {
            if (username.equals(gameData.whiteUsername())) {
                winner = gameData.blackUsername();
            } else if (username.equals(gameData.blackUsername())) {
                winner = gameData.whiteUsername();
            }
        }
        NotificationMessage resignationMsg = new NotificationMessage(username + " has resigned. " + winner + " wins!");
        manager.broadcast(GSON.toJson(resignationMsg));
        manager.remove(session);
    }

    private void broadcastToOthers(int gameID, NotificationMessage notificationMessage, Session session) {
        GameSessionManager manager = GAME_SESSIONS.get(gameID);
        if (manager != null) {
            manager.broadcastExcept(GSON.toJson(notificationMessage), session);
        }
    }

    private void sendError(Session session, String message) {
        try {
            session.getRemote().sendString(GSON.toJson(Map.of(
                    "serverMessageType", "ERROR",
                    "errorMessage", "Error: " + message
            )));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validateMove(
            Session session,
            UserGameCommand command,
            GameSessionManager manager,
            ChessMove move,
            ChessGame game,
            GameData gameData,
            String username) {
        Collection<ChessMove> validMoves = game.validMoves(move.getStartPosition());
        if (validMoves == null || !validMoves.contains(move)) {
            sendError(session, "Illegal move");
            System.out.println("no valid moves");
            return false;
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
            return false;
        }

        try {
            game.makeMove(move, playerColor);
            game.getMoveHistory().add(move);
            db.updateGame(command.getGameID(), game);
            db.updateBoard(command.getGameID(), game.getBoard());
        } catch (Exception e) {
            sendError(session, "Failed to apply move - " + e.getMessage());
            System.out.println("couldn't apply move");
            return false;
        }

        return true;
    }

    private void sendLoadGameToPlayers(GameSessionManager manager, GameData gameData, ChessGame game) {
        for (Session s : manager.getSessions()) {
            String otherUser = manager.getUsername(s);
            boolean theirTurn = (otherUser != null &&
                    ((game.getTeamTurn() == ChessGame.TeamColor.WHITE && otherUser.equals(gameData.whiteUsername())) ||
                     (game.getTeamTurn() == ChessGame.TeamColor.BLACK && otherUser.equals(gameData.blackUsername()))));
            LoadGameMessage updatedGameMsg = new LoadGameMessage(gameData, theirTurn);
            try {
                s.getRemote().sendString(GSON.toJson(updatedGameMsg));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}