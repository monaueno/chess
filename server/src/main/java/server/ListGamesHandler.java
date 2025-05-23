package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import service.ListGameResult;
import service.ListGameService;
import spark.Request;
import spark.Response;
import spark.Route;

public class ListGamesHandler implements Route {
    private final MemoryDataAccess db;

    public ListGamesHandler(MemoryDataAccess db) {
        this.db = db;
    }

    @Override
    public Object handle(Request req, Response res) {
        Gson gson = new Gson();

        try {
            String authToken = req.headers("authorization");

            ListGameService service = new ListGameService(db);
            ListGameResult result = service.listGames(authToken);

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