package service.game;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;
import chess.ChessGame;

import java.util.concurrent.atomic.AtomicInteger;

public class CreateGameService {
    private final DataAccess db;
    private static final AtomicInteger NEXT_GAME_ID = new AtomicInteger(1);

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

        ChessGame newGame = new ChessGame();
        GameData gameData = new GameData(0, null, null, request.gameName(), newGame);
        int gameID = db.createGame(gameData);
        return new CreateGameResult(gameID);
    }
}