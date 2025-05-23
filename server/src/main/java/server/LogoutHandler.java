package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import service.LogoutRequest;
import service.LogoutService;
import spark.Request;
import spark.Response;
import spark.Route;

public class LogoutHandler implements Route {
    private final MemoryDataAccess db;

    public LogoutHandler(MemoryDataAccess db) {
        this.db = db;
    }

    @Override
    public Object handle(Request req, Response res) {
        Gson gson = new Gson();

        try {
            // Get the authToken from the header
            String authToken = req.headers("authorization");

            LogoutRequest request = new LogoutRequest(authToken);
            LogoutService service = new LogoutService(db);
            service.logout(request);

            res.status(200);
            return "{}";

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