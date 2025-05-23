package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import chess.ChessGame;

import java.util.concurrent.atomic.AtomicInteger;

public class CreateGameService {
    private final DataAccess db;
    private static final AtomicInteger nextGameID = new AtomicInteger(1);

    public CreateGameService(DataAccess db) {
        this.db = db;
    }

    public CreateGameResult createGame(CreateGameRequest request, String authToken) throws DataAccessException {
        if (request.gameName() == null || request.gameName().isBlank()) {
            throw new DataAccessException("bad request");
        }

        AuthData auth = db.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        int gameID = nextGameID.getAndIncrement();
        ChessGame newGame = new ChessGame();
        GameData gameData = new GameData(gameID, null, null, request.gameName(), newGame);
        db.createGame(gameData);

        return new CreateGameResult(gameID);
    }
}