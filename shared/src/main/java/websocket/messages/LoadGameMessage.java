package websocket.messages;

import model.GameData;
import websocket.messages.ServerMessage.ServerMessageType;

public class LoadGameMessage extends ServerMessage {
    private final GameData game;

    public LoadGameMessage(GameData game) {
        this.serverMessageType = ServerMessageType.LOAD_GAME;
        this.game = game;
    }

    public GameData getGame() {
        return game;
    }
}