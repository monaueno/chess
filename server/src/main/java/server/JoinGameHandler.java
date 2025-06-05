package server;

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
        String authToken = null;
        System.out.println("🔹 Authorization header: " + authToken);

        try {
            authToken = req.headers("authorization");
            JoinGameRequest request = gson.fromJson(req.body(), JoinGameRequest.class);

            JoinGameService service = new JoinGameService(dataAccess);
            service.joinGame(request, authToken);

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