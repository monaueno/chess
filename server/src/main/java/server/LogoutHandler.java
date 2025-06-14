package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import model.LogoutRequest;
import service.auth.LogoutService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;

public class LogoutHandler implements Route {
    private final DataAccess dataAccess;

    public LogoutHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        Gson gson = new Gson();

        try {
            // Get the authToken from the header
            String authToken = req.headers("authorization");

            if (authToken == null) {
                res.status(401);
                return gson.toJson(new ErrorMessage("Missing auth token"));
            }

            LogoutRequest request = new LogoutRequest(authToken);
            LogoutService service = new LogoutService(dataAccess);
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