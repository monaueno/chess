package server;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;
import spark.Route;
import service.game.ClearService;
import dataaccess.DataAccess;

public class ClearHandler implements Route {
    private final DataAccess dataAccess;

    public ClearHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            new ClearService(dataAccess).clear();
            res.status(200);
            return "{}";
        } catch (Exception e) {
            res.status(500);
            return new Gson().toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
}