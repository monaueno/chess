package server;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;
import spark.Route;
import service.ClearService;
import dataaccess.MemoryDataAccess;

public class ClearHandler implements Route{
    @Override
    public Object handle(Request req, Response res){
        try{
            new ClearService(new MemoryDataAccess()).clear();
            res.status(200);
            return"{}";
        } catch (Exception e) {
            res.status(500);
            return new Gson().toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }
    record ErrorMessage(String message) {}
}