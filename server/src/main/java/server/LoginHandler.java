package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import service.LoginRequest;
import service.LoginResult;
import service.LoginService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;

public class LoginHandler implements Route {
    private final DataAccess dataAccess;
    public LoginHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        Gson gson = new Gson();

        try {
            LoginRequest request = gson.fromJson(req.body(), LoginRequest.class);
            LoginService service = new LoginService(dataAccess);
            LoginResult result = service.login(request);

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