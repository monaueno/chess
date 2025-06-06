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


        System.out.println("DEBUG  ► asObserver = " + request.asObserver()
                + " | playerColor = [" + request.playerColor() + "]");
        if (request.asObserver() || "observe".equalsIgnoreCase(request.playerColor())) {
            db.addObserver(game.gameID(), username);
            game.addObserver(username); // Update in-memory state
            db.updateGame(game.gameID(), game.game()); // Persist the updated state
            return;
        } else {
            ChessGame.TeamColor color;
            try {
                color = ChessGame.TeamColor.valueOf(request.playerColor().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new DataAccessException("Invalid team color: " + request.playerColor());
            }

            // System.out.println("Comparing: game.whiteUsername=[" + game.whiteUsername() + "] to auth.username=[" + username + "]");

            if (color == ChessGame.TeamColor.WHITE && game.whiteUsername() != null && !game.whiteUsername().equals(username)) {
                throw new DataAccessException("White is already taken");
            }
            if (color == ChessGame.TeamColor.BLACK && game.blackUsername() != null && !game.blackUsername().equals(username)) {
                throw new DataAccessException("Black color already taken");
            }

            switch (color) {
                case WHITE -> {
                    db.setWhiteUsername(game.gameID(), username);
                }
                case BLACK -> {
                    db.setBlackUsername(game.gameID(), username);
                }
                default -> throw new DataAccessException("bad request");
            }
        }
    }
}