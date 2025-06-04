package model;

import chess.ChessGame;
import java.util.ArrayList;
import java.util.List;

public class GameData {
    private int gameID;
    private String whiteUsername;
    private String blackUsername;
    private String gameName;
    private ChessGame game;
    private List<String> observers = new ArrayList<>();

    public GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
        this.gameID = gameID;
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.gameName = gameName;
        this.game = game;
    }

    // Getters and setters
    public int getGameID() { return gameID; }
    public String getWhiteUsername() { return whiteUsername; }
    public String getBlackUsername() { return blackUsername; }
    public String getGameName() { return gameName; }
    public ChessGame getGame() { return game; }
    public List<String> getObservers() { return observers; }

    public void addObserver(String observer) {
        observers.add(observer);
    }
}