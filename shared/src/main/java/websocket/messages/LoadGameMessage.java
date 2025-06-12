package websocket.messages;

import model.GameData;

public class LoadGameMessage extends ServerMessage {
    private final GameData game;
    private boolean yourTurn;

    public LoadGameMessage(GameData game, boolean isYourTurn) {
        this.serverMessageType = ServerMessageType.LOAD_GAME;
        this.game = game;
        this.yourTurn = isYourTurn;
    }

    public GameData getGame() {
        return game;
    }
    public boolean isYourTurn() { return yourTurn; }
}