package service.game;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.data.AuthData;
import model.data.GameData;
import model.JoinGameRequest;

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
            System.out.println("❌ Auth is null for token: " + authToken);
            throw new DataAccessException("unauthorized");
        }

        GameData game = db.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("bad request");
        }

        String username = auth.username();
        System.out.println("Comparing: game.whiteUsername=[" + game.whiteUsername() + "] to auth.username=[" + username + "]");
        System.out.println("Joining as: " + username);
        System.out.println("whiteUsername: " + game.whiteUsername());
        System.out.println("Requested color: " + request.playerColor());
        System.out.println("request.playerColor() = [" + request.playerColor() + "]");

        if (request.playerColor() == null || request.playerColor().isBlank()) {
            throw new DataAccessException("missing playerColor");
        }

        ChessGame.TeamColor color;
        try {
            color = ChessGame.TeamColor.valueOf(request.playerColor().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DataAccessException("Invalid team color: " + request.playerColor());
        }

        // System.out.println("Comparing: game.whiteUsername=[" + game.whiteUsername() + "] to auth.username=[" + username + "]");

        switch (color) {
            case WHITE -> {
                String existingWhite = game.whiteUsername();
                if (existingWhite != null && !existingWhite.equalsIgnoreCase(username)) {
                    throw new DataAccessException("Error: white player already taken");
                }
                db.setWhiteUsername(game.gameID(), username);
                game.setWhiteUsername(username);
            }
            case BLACK -> {
                String existingBlack = game.blackUsername();
                if (existingBlack != null && !existingBlack.equalsIgnoreCase(username)) {
                    throw new DataAccessException("Error: black player already taken");
                }
                db.setBlackUsername(game.gameID(), username);
                game.setBlackUsername(username);
            }
            default -> throw new DataAccessException("bad request");
        }
    }
}