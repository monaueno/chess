package server;

import model.LoginRequest;
import model.LoginResult;
import service.auth.LoginService;
import spark.Request;
import spark.Response;
import spark.Route;
import dataaccess.DataAccess;

import com.google.gson.Gson;

public class LoginHandler implements Route {
    private final DataAccess dataAccess;
    public LoginHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            LoginRequest request = parseJson(req, LoginRequest.class);
            LoginService service = new LoginService(dataAccess);
            LoginResult result = service.login(request);

            res.status(200);
            return toJson(result);
        } catch (Exception e) {
            return handleException(res, e);
        }
    }

    private <T> T parseJson(Request req, Class<T> clazz) {
        return new Gson().fromJson(req.body(), clazz);
    }

    private String toJson(Object obj) {
        return new Gson().toJson(obj);
    }

    private String handleException(Response res, Exception e) {
        return ErrorHandlingUtil.handleException(res, e);
    }
}
class ErrorHandlingUtil {
    static String handleException(Response res, Exception e) {
        if (e instanceof dataaccess.DataAccessException dae) {
            switch (dae.getMessage()) {
                case "bad request" -> res.status(400);
                case "unauthorized" -> res.status(401);
                default -> res.status(500);
            }
            return new com.google.gson.Gson().toJson(new ErrorMessage("Error: " + dae.getMessage()));
        } else {
            res.status(500);
            return new com.google.gson.Gson().toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
}