package client.websocket;

import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.ServerMessage;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class GameplayWebSocketHandler {

    private final String authToken;
    private final int gameID;
    private Session session;
    private static final Gson gson = new Gson();

    public GameplayWebSocketHandler(String authToken, int gameID) {
        this.authToken = authToken;
        this.gameID = gameID;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("✅ WebSocket connected.");

        UserGameCommand connectCommand = new UserGameCommand(CommandType.CONNECT, authToken, gameID);
        try {
            session.getRemote().sendString(gson.toJson(connectCommand));
        } catch (Exception e) {
            System.err.println("Failed to send CONNECT command: " + e.getMessage());
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println("📨 Received message: " + message);

        ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME:
                // TODO: handle game state update
                break;
            case NOTIFICATION:
                // TODO: show notification to user
                break;
            case ERROR:
                // TODO: display error
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