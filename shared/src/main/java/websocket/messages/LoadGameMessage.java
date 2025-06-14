package websocket.messages;

import model.data.GameData;

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
    public String getWhiteUsername() {
        return game.whiteUsername();
    }

    public String getBlackUsername() {
        return game.blackUsername();
    }
}