package websocket.messages;

import model.GameData;

public class ErrorMessage extends ServerMessage {
    private String errorMessage;

    public ErrorMessage(GameData game) {
        super(ServerMessageType.ERROR);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return  errorMessage;
    }
}