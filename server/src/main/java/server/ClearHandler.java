package server;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;
import spark.Route;
import service.ClearService;
import dataaccess.MemoryDataAccess;

public class ClearHandler implements Route {
    private final MemoryDataAccess db;

    public ClearHandler(MemoryDataAccess db) {
        this.db = db;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            new ClearService(db).clear();
            res.status(200);
            return "{}";
        } catch (Exception e) {
            res.status(500);
            return new Gson().toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
}