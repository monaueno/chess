package model;

public record JoinGameRequest(String playerColor, int gameID, boolean asObserver) {
    public JoinGameRequest(String playerColor, int gameID) {
        this(playerColor, gameID, "observe".equalsIgnoreCase(playerColor));
    }
}