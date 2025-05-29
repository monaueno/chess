package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.game.ListGamesResult;
import service.game.ListGamesService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;

public class ListGamesHandler implements Route {
    private final DataAccess dataAccess;

    public ListGamesHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        Gson gson = new Gson();

        try {
            String authToken = req.headers("authorization");

            ListGamesService service = new ListGamesService(dataAccess);
            ListGamesResult result = service.listGames(authToken);

            res.status(200);
            return gson.toJson(result);

        } catch (DataAccessException e) {
            res.status("unauthorized".equals(e.getMessage()) ? 401 : 500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
}