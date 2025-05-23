package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import service.RegisterRequest;
import service.RegisterResult;
import service.RegisterService;
import spark.Request;
import spark.Response;
import spark.Route;

public class RegisterHandler implements Route {

    @Override
    public Object handle(Request req, Response res) {
        var gson = new Gson();
        try {
            // Parse JSON request body
            RegisterRequest request = gson.fromJson(req.body(), RegisterRequest.class);

            // Call the service
            RegisterService service = new RegisterService(new MemoryDataAccess());
            RegisterResult result = service.register(request);

            res.status(200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            // Custom error codes based on message
            switch (e.getMessage()) {
                case "bad request" -> res.status(400);
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