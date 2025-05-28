package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import service.CreateGameRequest;
import service.CreateGameResult;
import service.CreateGameService;
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
        Gson gson = new Gson();

        try {
            String authToken = req.headers("authorization");
            CreateGameRequest request = gson.fromJson(req.body(), CreateGameRequest.class);

            CreateGameService service = new CreateGameService(dataAccess);
            CreateGameResult result = service.createGame(request, authToken);

            res.status(200);
            return gson.toJson(result);

        } catch (DataAccessException e) {
            switch (e.getMessage()) {
                case "bad request" -> res.status(400);
                case "unauthorized" -> res.status(401);
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