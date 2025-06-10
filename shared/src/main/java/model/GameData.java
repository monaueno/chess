package model;

import chess.ChessBoard;
import chess.ChessGame;
import java.util.ArrayList;
import java.util.List;

public class GameData {
    private int gameID;
    private String whiteUsername;
    private String blackUsername;
    private String gameName;
    private ChessGame game;
    private List<String> observers;
    private ChessBoard board;

    public GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
        this.gameID = gameID;
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.gameName = gameName;
        this.game = game;
        this.observers = new ArrayList<>();
    }

    public GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game, List<String> observers) {
        this.gameID = gameID;
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.gameName = gameName;
        this.game = game;
        this.observers = observers != null ? observers : new ArrayList<>();
    }

    // Getters and setters
    public int gameID() {
        return gameID;
    }

    public String whiteUsername() {
        return whiteUsername;
    }

    public String blackUsername() {
        return blackUsername;
    }

    public String gameName() {
        return gameName;
    }

    public ChessGame game() {
        return game;
    }

    public List<String> observers() {
        return observers;
    }

    public void addObserver(String observer) {
        observers.add(observer);
    }

    public void setWhiteUsername(String whiteUsername) {
        this.whiteUsername = whiteUsername;
    }

    public void setBlackUsername(String blackUsername) {
        this.blackUsername = blackUsername;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public ChessBoard getBoard() {
        return board;
    }

    public GameData(ChessGame game) {
        this.game = game;
        this.gameID = -1;
        this.whiteUsername = null;
        this.blackUsername = null;
        this.gameName = null;
        this.observers = new ArrayList<>();
    }
}