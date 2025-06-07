package websocket.messages;

import model.GameData;

public class NotificationMessage extends ServerMessage {
    public GameData game;

    public NotificationMessage(GameData game) {
        this.serverMessageType = ServerMessageType.LOAD_GAME;
        this.game = game;
    }
}