package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import model.JoinGameRequest;
import model.data.GameData;
import service.game.JoinGameService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;

public class JoinGameHandler implements Route {
    private final DataAccess dataAccess;

    public JoinGameHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        Gson gson = new Gson();
        System.out.println("🔹 JoinGameHandler triggered");
        System.out.println("🔹 Request body: " + req.body());
        // String authToken = req.headers("authorization"); // Removed redundant line
        String authToken = req.headers("authorization");
        System.out.println("🔹 Authorization header: " + authToken);

        try {
            authToken = req.headers("authorization");
            JoinGameRequest request = gson.fromJson(req.body(), JoinGameRequest.class);
            JoinGameService service = new JoinGameService(dataAccess);
            String username = dataAccess.getUsernameFromAuth(authToken);

            String colorRaw = request.playerColor();

            if (colorRaw == null || colorRaw.isBlank()) {
                res.status(400);
                return gson.toJson(new ErrorMessage("Error: missing required playerColor."));
            }

            ChessGame.TeamColor color;
            try {
                color = ChessGame.TeamColor.valueOf(colorRaw.toUpperCase());
            } catch (IllegalArgumentException e) {
                res.status(400);
                return gson.toJson(new ErrorMessage("Error: Invalid playerColor."));
            }

            GameData game = dataAccess.getGame(request.gameID());
            if (game == null) {
                res.status(400);
                return gson.toJson(new ErrorMessage("Error: game not found"));
            }
            if (color == ChessGame.TeamColor.WHITE) {
                String existingWhite = game.whiteUsername();
                if (existingWhite != null && !existingWhite.equals(username)) {
                    throw new DataAccessException("Error: white player already taken");
                }
            }
            if (color == ChessGame.TeamColor.BLACK) {
                String existingBlack = game.blackUsername();
                if (existingBlack != null && !existingBlack.equals(username)) {
                    throw new DataAccessException("Error: black player already taken");
                }
            }

            GameData updatedGame = new GameData(
                    game.gameID(),
                    color == ChessGame.TeamColor.WHITE ? username : game.whiteUsername(),
                    color == ChessGame.TeamColor.BLACK ? username : game.blackUsername(),
                    game.gameName(),
                    game.game(),
                    game.observers()
            );
            dataAccess.updateGameData(request.gameID(), updatedGame);

            service.joinGame(request, authToken);

            res.status(200);
            res.type("application/json");
            return gson.toJson(new SuccessMessage("success"));

        } catch (DataAccessException e) {
            System.out.println("e.getMessage() = " + e.getMessage());
            if (e.getMessage().contains("Auth token not found")) {
                res.status(401);
            }else if (e.getMessage().contains("Invalid team color")) {
                res.status(400);
            }else if (e.getMessage().contains("bad request")){
                res.status(400);
            }else if (e.getMessage().contains("already taken")) {
                res.status(403);
            } else {
                res.status(500); // Default error
            }

            return gson.toJson(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(new ErrorMessage(e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
    private record SuccessMessage(String message){}
}