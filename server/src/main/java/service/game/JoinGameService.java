package service.game;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import model.JoinGameRequest;
import model.*;

public class JoinGameService {
    private final DataAccess db;

    public JoinGameService(DataAccess db) {
        this.db = db;
    }

    public void joinGame(JoinGameRequest request, String authToken) throws DataAccessException {
        if (request.gameID() == 0) {
            throw new DataAccessException("bad request");
        }

        AuthData auth = db.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        GameData game = db.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("bad request");
        }

        String username = auth.username();

        if (request.playerColor() == null) {
            db.addObserver(game.gameID(), username);
            return;
        }

        ChessGame.TeamColor color;
        try {
            color = ChessGame.TeamColor.valueOf(request.playerColor().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DataAccessException("Invalid team color: " + request.playerColor());
        }

        switch (color) {
            case WHITE -> {
                if (game.whiteUsername() != null) {
                    throw new DataAccessException("already taken");
                }
                db.setWhitePlayer(game.gameID(), username);
            }
            case BLACK -> {
                if (game.blackUsername() != null) {
                    throw new DataAccessException("already taken");
                }
                db.setBlackPlayer(game.gameID(), username);
            }
            default -> throw new DataAccessException("bad request");
        }

        db.updateGame(game.gameID(), game.game());
    }
}