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
    private Session session;
    private static final Gson gson = new Gson();
    private ChessBoard board;
    private ChessGame game = new ChessGame();
    private final boolean playerIsWhite = true;
    private ChessPosition highlightedFrom;
    private Set<ChessPosition> highlightedTo;
    private final java.util.Scanner scanner = new java.util.Scanner(System.in);

    public GameplayWebSocketHandler(String authToken, int gameID) {
        this.authToken = authToken;
        this.gameID = gameID;
    }


    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("WebSocket connected.");

        UserGameCommand connectCommand = new UserGameCommand(CommandType.CONNECT, authToken, gameID);
        try {
            session.getRemote().sendString(gson.toJson(connectCommand));
            promptForMove(); // Start user input loop once
        } catch (Exception e) {
            System.err.println("Failed to send CONNECT command: " + e.getMessage());
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println("Received message: " + message);

        ServerMessage serverMessage = MessageSerializer.deserializeServerMessage(message);

        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME:
                LoadGameMessage lgMsg = (LoadGameMessage) serverMessage;
                GameData data = lgMsg.getGame();
                this.game = data.game();
                this.board = data.getBoard();
                game.setBoard(board);
                System.out.println("\nUpdated board after move:");
                new ChessBoardUI().drawBoard(board, playerIsWhite, highlightedFrom, highlightedTo);
                break;

            case NOTIFICATION:
                NotificationMessage noteMsg = (NotificationMessage) serverMessage;
                System.out.println("Notification: " + noteMsg.getMessage());
                break;

            case ERROR:
                ErrorMessage errorMsg = (ErrorMessage) serverMessage;
                System.err.println("Error: " + errorMsg.getErrorMessage());
                break;
        }
    }

    private ChessPosition parsePosition(String pos) {
        int row = 8 - Character.getNumericValue(pos.charAt(1)); // e.g., '2' → 6
        int col = pos.charAt(0) - 'a' + 1; // e.g., 'e' → 5
        return new ChessPosition(row, col);
    }

    public void sendMove(String from, String to) {
        ChessPosition fromPos = parsePosition(from);
        ChessPosition toPos = parsePosition(to);
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
        new Thread(() -> {
            while (true) {
                System.out.print("Enter move (e.g., e2 e4): ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("quit")) break;

                if (!input.matches("^[a-h][1-8]\\s+[a-h][1-8]$")) {
                    System.out.println("Invalid move format. Use: e2 e4");
                    continue;
                }

                String[] parts = input.split("\\s+");
                sendMove(parts[0], parts[1]);
            }
        }).start();
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
