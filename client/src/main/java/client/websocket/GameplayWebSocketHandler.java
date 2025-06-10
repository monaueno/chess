package client.websocket;

import chess.ChessBoard;
import chess.ChessGame;
import ui.ChessBoardUI;
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
                this.board = lgMsg.getGame().getBoard();
                game.setBoard(this.board);
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

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("WebSocket closed: " + reason);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.err.println("WebSocket error: " + t.getMessage());
    }
}

