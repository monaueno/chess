package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import model.JoinGameRequest;
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

        try {
            String authToken = req.headers("authorization");
            JoinGameRequest request = gson.fromJson(req.body(), JoinGameRequest.class);

            JoinGameService service = new JoinGameService(dataAccess);
            service.joinGame(request, authToken);

            res.status(200);
            return "{}";

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
}