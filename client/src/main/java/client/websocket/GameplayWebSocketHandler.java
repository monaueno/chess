package client.websocket;

import chess.ChessBoard;
import chess.ChessGame;
import model.GameData;
import ui.ChessBoardUI;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.ErrorMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import websocket.messages.LoadGameMessage;
import java.util.Set;
import chess.ChessPosition;


@WebSocket
public class GameplayWebSocketHandler {

    private final String authToken;
    private final int gameID;
    private final Runnable onGameLoadedCallback;
    private Session session;
    private static final Gson gson = new Gson();
    private ChessBoard board;
    private ChessGame game = new ChessGame();
    private boolean playerIsWhite;
    private ChessPosition highlightedFrom;
    private Set<ChessPosition> highlightedTo;
    private final java.util.Scanner scanner = new java.util.Scanner(System.in);
    private volatile boolean exited = false;
    private ChessGame.TeamColor playerColor;
    private String username;

    public GameplayWebSocketHandler(String authToken, int gameID, String username, Runnable promptForMove) {
        this.authToken = authToken;
        this.gameID = gameID;
        this.username = username;
        this.onGameLoadedCallback = promptForMove;
    }


    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;

        UserGameCommand connectCommand = new UserGameCommand(CommandType.CONNECT, authToken, gameID);

        try {
            session.getRemote().sendString(gson.toJson(connectCommand));
        } catch (Exception e) {
            System.err.println("Failed to send CONNECT command: " + e.getMessage());
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {

        ServerMessage serverMessage = MessageSerializer.deserializeServerMessage(message);

        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME:
                LoadGameMessage lgMsg = (LoadGameMessage) serverMessage;
                GameData data = lgMsg.getGame();
                this.game = data.game();
                this.board = data.getBoard();

                if (username.equals(data.whiteUsername())) {
                    this.playerColor = ChessGame.TeamColor.WHITE;
                } else if (username.equals(data.blackUsername())) {
                    this.playerColor = ChessGame.TeamColor.BLACK;
                } else {
                    System.err.println("⚠️ Username does not match either white or black player.");
                    return;
                }

                this.playerIsWhite = (this.playerColor == ChessGame.TeamColor.WHITE);

                if (board == null) {
                    System.err.println("Error: board is null in LOAD_GAME");
                    return;
                }

                game.setBoard(board);

                System.out.println("🧩 Re-drawing board after LOAD_GAME...");
                new ChessBoardUI().drawBoard(board, playerColor == ChessGame.TeamColor.WHITE, highlightedFrom, highlightedTo);
                System.out.println("✅ Done drawing board.");
                System.out.println("Current turn: " + game.getTeamTurn());
                System.out.println("Move attempted by: " + username + " playing as " + playerColor);

                if (playerColor == game.getTeamTurn()) {
                    System.out.println("✅ It's your turn! Enter your move (e.g., e2 e4), or type 'resign' or 'exit':");
                    new Thread(() -> {
                        while (true) {
                            System.out.print("Enter move: ");
                            String input = scanner.nextLine().trim();

                            if (input.equalsIgnoreCase("resign")) {
                                sendResignCommand();
                                break;
                            } else if (input.equalsIgnoreCase("exit")) {
                                sendLeaveCommand();
                                break;
                            } else if (input.matches("^[a-h][1-8]\\s+[a-h][1-8]$")) {
                                String[] parts = input.split("\\s+");
                                sendMove(parts[0], parts[1]);
                                break;
                            } else {
                                System.out.println("Invalid input. Try again:");
                            }
                        }
                    }).start();
                } else {
                    System.out.println("⏳ Waiting for opponent to move. You may still type 'resign' or 'exit':");
                    new Thread(() -> {
                        while (true) {
                            System.out.print("Command: ");
                            String input = scanner.nextLine().trim();

                            if (input.equalsIgnoreCase("resign")) {
                                sendResignCommand();
                                break;
                            } else if (input.equalsIgnoreCase("exit")) {
                                sendLeaveCommand();
                                break;
                            } else {
                                System.out.println("Not your turn. Only 'resign' or 'exit' allowed.");
                            }
                        }
                    }).start();
                }

                break;

            case NOTIFICATION:
                NotificationMessage noteMsg = (NotificationMessage) serverMessage;
                System.out.println("Notification: " + noteMsg.getMessage());
                break;

            case ERROR:
                ErrorMessage errorMsg = (ErrorMessage) serverMessage;
                System.err.println(errorMsg.getErrorMessage());

                break;
        }
    }


    private ChessPosition parsePosition(String pos) {
        char fileChar = pos.charAt(0);
        char rankChar = pos.charAt(1);

        int row = Character.getNumericValue(rankChar); // e.g., '2' → 6
        int col = (fileChar - 'a') + 1; // e.g., 'e' → 5
        return new ChessPosition(row, col);
    }

    public void sendMove(String from, String to) {
        ChessPosition fromPos = parsePosition(from);
        ChessPosition toPos = parsePosition(to);
        System.out.println("DEBUG (client) — Parsed move:");
        System.out.println("  From: " + from + " -> " + fromPos);
        System.out.println("  To  : " + to + " -> " + toPos);

        MakeMoveCommand moveCommand = new MakeMoveCommand(authToken, gameID, fromPos, toPos);
        System.out.println("Sending move JSON: " + gson.toJson(moveCommand));

        try {
            session.getRemote().sendString(gson.toJson(moveCommand));
            // Do not draw board here; wait for LOAD_GAME message which will trigger UI update.
        } catch (Exception e) {
            System.err.println("Failed to send move: " + e.getMessage());
        }
    }

    private void promptForMove() {
        System.out.print("\nEnter move: ");
        String input = scanner.nextLine().trim().toLowerCase();
        if (input.equalsIgnoreCase("quit")) {
            System.out.println("Exiting game.");
            return;
        }

        if (input.equalsIgnoreCase("quit")) {
            System.out.println("Exiting game.");
            handleResignOrLeave();
            try {
                session.close();
            } catch (Exception e) {
                System.err.println("Error closing session: " + e.getMessage());
            }
            return;
        }

        if (!input.matches("^[a-h][1-8]\\s+[a-h][1-8]$")) {
            System.out.println("Invalid format. Use: e2 e4");
            promptForMove(); // retry
            return;
        }

        String[] parts = input.split("\\s+");
        sendMove(parts[0], parts[1]);
    }

    public boolean hasExited() {
        return exited;
    }

    private void handleResignOrLeave(){
        exited = true;
    }

    private void sendResignCommand() {
        UserGameCommand resign = new UserGameCommand(CommandType.RESIGN, authToken, gameID);
        try {
            session.getRemote().sendString(gson.toJson(resign));
            exited = true;
            if (session != null && session.isOpen()) session.close();
        } catch (Exception e) {
            System.err.println("Failed to send RESIGN command: " + e.getMessage());
        }
    }

    private void sendLeaveCommand() {
        UserGameCommand leave = new UserGameCommand(CommandType.LEAVE, authToken, gameID);
        try {
            session.getRemote().sendString(gson.toJson(leave));
            exited = true;
            if (session != null && session.isOpen()) session.close();
        } catch (Exception e) {
            System.err.println("Failed to send LEAVE command: " + e.getMessage());
        }
    }


    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("WebSocket closed: " + reason);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.err.println("WebSocket error: " + t.getMessage());
    }
}
