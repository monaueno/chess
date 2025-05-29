package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.game.CreateGameRequest;
import service.game.CreateGameResult;
import service.game.CreateGameService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;

public class CreateGameHandler implements Route {
    private final DataAccess dataAccess;

    public CreateGameHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            CreateGameRequest request = parseJson(req, CreateGameRequest.class);

            CreateGameService service = new CreateGameService(dataAccess);
            CreateGameResult result = service.createGame(request, authToken);

            res.status(200);
            return toJson(result);
        } catch (Exception e) {
            return handleException(res, e);
        }
    }

    private <T> T parseJson(Request req, Class<T> clazz) {
        return new Gson().fromJson(req.body(), clazz);
    }

    private String toJson(Object obj) {
        return new Gson().toJson(obj);
    }

    private String handleException(Response res, Exception e) {
        if (e instanceof DataAccessException dae) {
            switch (dae.getMessage()) {
                case "bad request" -> res.status(400);
                case "unauthorized" -> res.status(401);
                default -> res.status(500);
            }
            return toJson(new ErrorMessage("Error: " + dae.getMessage()));
        } else {
            res.status(500);
            return toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
}