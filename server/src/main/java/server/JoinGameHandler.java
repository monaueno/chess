package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import model.JoinGameRequest;
import service.game.JoinGameService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;
import model.*;

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
            service.joinGame(request, authToken);

            String colorRaw = request.color();
            if (colorRaw == null) {
                dataAccess.addObserver(request.gameID(), username);
            } else {
                ChessGame.TeamColor color = ChessGame.TeamColor.valueOf(colorRaw.toUpperCase());
                if (color == ChessGame.TeamColor.WHITE) {
                    dataAccess.setWhiteUsername(request.gameID(), username);
                } else if (color == ChessGame.TeamColor.BLACK) {
                    dataAccess.setBlackUsername(request.gameID(), username);
                }
            }

            res.status(200);
            res.type("application/json");
            return gson.toJson(new SuccessMessage("success"));

        } catch (DataAccessException e) {
            switch (e.getMessage()) {
                case "bad request" -> res.status(400);
                case "unauthorized" -> res.status(401);
                case "already taken" -> res.status(403);
                default -> res.status(500);
            }
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
    private record SuccessMessage(String message){}
}