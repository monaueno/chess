package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;
import spark.Request;
import spark.Response;
import spark.Route;

public class ObserveGameHandler implements Route {

    private final DataAccess db;
    private final Gson gson = new Gson();

    public ObserveGameHandler(DataAccess db) {
        this.db = db;
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        String authToken = req.headers("Authorization");
        if (authToken == null || authToken.isBlank()) {
            res.status(401);
            return gson.toJson(new ErrorMessage("Error: missing auth token"));
        }

        String username;
        try {
            username = db.getUsernameFromAuth(authToken);
        } catch (DataAccessException e) {
            res.status(401);
            return gson.toJson(new ErrorMessage("Error: unauthorized"));
        }

        String idRaw = req.queryParams("gameID");
        if (idRaw == null) {
            res.status(400);
            return gson.toJson(new ErrorMessage("Error: missing gameID"));
        }

        int gameID;
        try {
            gameID = Integer.parseInt(idRaw);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(new ErrorMessage("Error: invalid gameID"));
        }

        GameData game = db.getGame(gameID);
        if (game == null) {
            res.status(400);
            return gson.toJson(new ErrorMessage("Error: game not found"));
        }

        db.addObserver(gameID, username);
        res.status(200);
        return gson.toJson(new SuccessResponse());
    }

    private record ErrorMessage(String message) {}
}