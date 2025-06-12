package server;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import dataaccess.DataAccess;
import dataaccess.MySqlDataAccess;
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
import websocket.messages.ServerMessage;
import websocket.MessageSerializer;

@WebSocket
public class WebSocketHandler {

    private static final Map<Integer, GameSessionManager> gameSessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private final DataAccess db;
    private static DataAccess sharedDB;
    private static final Map<Integer, ChessGame> activeGames = new ConcurrentHashMap<>();



    public static void setSharedDB(DataAccess db) { sharedDB = db;}

    public WebSocketHandler() {
        this.db = sharedDB;
        System.out.println("🚀 WebSocketHandler created. sharedDB = " + sharedDB);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Client connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        System.out.println("📨 Message received: " + message);



        try {
            System.out.println("Calling deserialize()");
            UserGameCommand command = websocket.MessageSerializer.deserialize(message);



            if (command == null || command.getCommandType() == null) {
                System.out.println("❌ Command is null after deserialization.");
                session.getRemote().sendString(gson.toJson(Map.of("serverMessageType", "ERROR", "errorMessage", "Error: Invalid command")));
                return;
            }
            System.out.println("✅ Command successfully deserialized.");

            System.out.println("➡️ Command type received: " + command.getCommandType());

            switch (command.getCommandType()) {
                case CONNECT:
                    System.out.println("✅ Entered handleConnect");
                    handleConnect(session, command);
                    break;
                case MAKE_MOVE:
                    System.out.println("Entered makeMoveConnect");
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

        manager.getGame(command.getGameID()).setGameOver(true);

        String resignationMsg = String.format("%s resigned", username);
        manager.broadcast(gson.toJson(new NotificationMessage(resignationMsg)));

        manager.remove(session);
        System.out.println("Session removed after resignation from game ID " + gameID);
    }

    private void handleLeave(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        System.out.println("Handling LEAVE for gameID " + gameID);

        GameSessionManager manager = gameSessions.get(gameID);
        if (manager == null) {
            System.out.println("No session manager found for gameID " + gameID);
            return;
        }

        String username = manager.getUsername(session);
        if (username == null) {
            System.out.println("Could not identify user from session");
            return;
        }

        GameData gameData = manager.getGameData(command.getGameID());

        if (gameData == null) {
            System.out.println("GameData is null after loading. Aborting.");
            return;
        }
        if (gameData.whiteUsername() != null && gameData.whiteUsername().equals(username)) {
            gameData.setWhiteUsername(null);
        }
        if (gameData.blackUsername() != null && gameData.blackUsername().equals(username)) {
            gameData.setBlackUsername(null);
        }

        String msg = String.format("%s left the game", username);
        broadcastToOthers(gameID, new NotificationMessage(msg), session);

        manager.remove(session);
        System.out.println("Session removed from game ID " + gameID);
    }

    private void broadcastToOthers(int gameID, NotificationMessage notificationMessage, Session session) {
        GameSessionManager manager = gameSessions.get(gameID);
        if (manager == null) {
            System.out.println("No session manager found for gameID " + gameID);
            return;
        }
        String messageJson = gson.toJson(notificationMessage);
        manager.broadcastExcept(messageJson, session);
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
        chess.ChessGame game = manager.getGame(command.getGameID());
        if (game == null) {
            System.out.println("Game not found for gameID " + command.getGameID());
            return;
        }

        try {
            Collection<ChessMove> validMoves = game.validMoves(move.getStartPosition());

            System.out.println("Trying move: " + move);
            if (game.getBoard() == null) {
                System.out.println("Board is null before move validation.");
            } else {
                System.out.println("Board is present.");
                System.out.println("Piece at start: " + game.getBoard().getPiece(move.getStartPosition()));
            }
            System.out.println("Valid moves: " + validMoves);


            if (validMoves == null) {
                session.getRemote().sendString(gson.toJson(Map.of(
                        "serverMessageType", "ERROR",
                        "errorMessage", "Error: Cannot retrieve valid moves — board might be uninitialized"
                )));
                return;
            }

            if (!validMoves.contains(move)) {
                session.getRemote().sendString(gson.toJson(Map.of(
                        "serverMessageType", "ERROR",
                        "errorMessage", "Error: Illegal move"
                )));
                return;
            }
            GameData gameData = manager.getGameData(command.getGameID());

            System.out.println("🔎 Pre-move turn check:");
            System.out.println("Player username: " + username);
            System.out.println("White username: " + gameData.whiteUsername());
            System.out.println("Black username: " + gameData.blackUsername());
            System.out.println("Current game turn: " + game.getTeamTurn());
            System.out.println("Player attempting move as: " +
                    (username.equals(gameData.whiteUsername()) ? "WHITE" :
                            username.equals(gameData.blackUsername()) ? "BLACK" : "OBSERVER"));

            game.makeMove(move, username.equals(gameData.whiteUsername()) ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK);


            System.out.println("Player username: " + username);
            System.out.println("White username: " + gameData.whiteUsername());
            System.out.println("Black username: " + gameData.blackUsername());

            gameData.setGame(game);
            gameData.setBoard(game.getBoard());

            LoadGameMessage updatedGameMsg = new LoadGameMessage(gameData);
            String updatedGameJson = gson.toJson(updatedGameMsg);
            System.out.println("Broadcasted updated board: " + updatedGameJson);
            manager.broadcast(updatedGameJson);

            String moveNotation = move.toString(); // Assuming ChessMove has a proper toString override like e2-e4
            String moveMessage = String.format("%s moved %s", username, move.getStartPosition().toString(), move.getEndPosition().toString());
            NotificationMessage notification = new NotificationMessage(moveMessage);
            manager.broadcastExcept(gson.toJson(notification), session);

            // Check for check, checkmate, or stalemate
            if (game.isInCheckmate(game.getTeamTurn())) {
                NotificationMessage checkmateMsg = new NotificationMessage(game.getTeamTurn() + " is in checkmate");
                manager.broadcast(gson.toJson(checkmateMsg));
            } else if (game.isInCheck(game.getTeamTurn())) {
                NotificationMessage checkMsg = new NotificationMessage(game.getTeamTurn() + " is in check");
                manager.broadcast(gson.toJson(checkMsg));
            } else if (game.isInStalemate(game.getTeamTurn())) {
                NotificationMessage stalemateMsg = new NotificationMessage(game.getTeamTurn() + " is in stalemate");
                manager.broadcast(gson.toJson(stalemateMsg));
            }

        } catch (Exception e) {
            try {
                session.getRemote().sendString(gson.toJson(Map.of(
                    "serverMessageType", "ERROR",
                        "errorMessage", "Error: Failed to apply move - " + e.getMessage()
                )));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void handleConnect(Session session, UserGameCommand command) {
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();
        System.out.println("🚨 ENTERED handleConnect()");

        System.out.println("Handling CONNECT for gameID " + gameID + ", authToken: " + authToken);

        gameSessions.putIfAbsent(gameID, new GameSessionManager());
        GameSessionManager manager = gameSessions.get(gameID);

        GameData gameData;
        try {
            gameData = db.getGame(gameID);
            if (gameData == null) {
                try {
                    session.getRemote().sendString(gson.toJson(
                            Map.of("serverMessageType", "ERROR",
                                    "errorMessage", "Error: Game not found")));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                return;
            }

            ChessGame game = gameData.getGame();
            game.setBoard(gameData.getBoard());

            activeGames.put(gameID, game);

        } catch (dataaccess.DataAccessException e) {
            try {
                session.getRemote().sendString(gson.toJson(
                        Map.of("serverMessageType", "ERROR",
                                "errorMessage", "Error: Failed to access game data")));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
            return;
        }
        manager.setGameData(gameID, gameData);            // store in session manager   // wrap it
        manager.add(session, authToken);      // store it so it won't be null next time

        String username;

        try {
            username = db.getUsernameFromAuth(authToken);
            manager.add(session, username);
        } catch (dataaccess.DataAccessException e) {
            System.out.println("Failed to get username from authToken: " + e.getMessage());
            try {
                session.getRemote().sendString(gson.toJson(Map.of(
                    "serverMessageType", "ERROR",
                    "errorMessage", "Error: Failed to retrieve username"
                )));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return;
        }
        if (username == null) {
            System.out.println("Could not identify user from session.");
            return;
        }

        String role;
        if (username.equals(gameData.whiteUsername())) {
            role = "white";
        } else if (username.equals(gameData.blackUsername())) {
            role = "black";
        } else {
            role = "observer";
        }

        String msg = String.format("%s joined as %s", username, role);
        broadcastToOthers(gameID, new NotificationMessage(msg), session);

        LoadGameMessage loadGameMsg = new LoadGameMessage(gameData);
        try {
            session.getRemote().sendString(gson.toJson(loadGameMsg));
        } catch (IOException e) {
            e.printStackTrace();
        }
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