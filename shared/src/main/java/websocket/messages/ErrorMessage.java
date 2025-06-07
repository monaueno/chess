package websocket.messages;

import model.GameData;

public class ErrorMessage extends ServerMessage {
    public GameData game;

    public ErrorMessage(GameData game) {
        this.serverMessageType = ServerMessageType.LOAD_GAME;
        this.game = game;
    }
}