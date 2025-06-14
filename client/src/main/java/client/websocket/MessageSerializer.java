package client.websocket;

import com.google.gson.Gson;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

public class MessageSerializer {
    private static final Gson GSON = new Gson();

    public static ServerMessage deserializeServerMessage(String json) {
        ServerMessage baseMessage = GSON.fromJson(json, ServerMessage.class);

        switch (baseMessage.getServerMessageType()) {
            case LOAD_GAME:
                return GSON.fromJson(json, LoadGameMessage.class);
            case NOTIFICATION:
                return GSON.fromJson(json, NotificationMessage.class);
            case ERROR:
                return GSON.fromJson(json, ErrorMessage.class);
            default:
                throw new IllegalArgumentException("Unknown server message type");
        }
    }
}
