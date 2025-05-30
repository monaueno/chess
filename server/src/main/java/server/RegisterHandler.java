package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.auth.RegisterRequest;
import service.auth.RegisterResult;
import service.auth.RegisterService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;

public class RegisterHandler implements Route {
    private final DataAccess dataAccess;
    public RegisterHandler(DataAccess dataAccess){
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        var gson = new Gson();
        try {
            // Parse JSON request body
            RegisterRequest request = gson.fromJson(req.body(), RegisterRequest.class);

            // Call the service
            RegisterService service = new RegisterService(dataAccess);
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