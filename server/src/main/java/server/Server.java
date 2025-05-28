package server;

import dataaccess.MemoryDataAccess;
import spark.*;
import dataaccess.MySqlDataAccess;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import dataaccess.DataAccess;

public class Server {

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        DataAccess db = null;
        try {
            DatabaseManager.createDatabase();
            DatabaseManager.createTables();
            db = new MySqlDataAccess();
        } catch (DataAccessException ex) {
            System.err.println("Failed to initialize database: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1); // fail fast if DB setup fails
        }

        Spark.post("/user", new RegisterHandler(db));
        Spark.post("/session", new LoginHandler(db));
        Spark.delete("/db", new ClearHandler(db));
        Spark.delete("/session", new LogoutHandler(db));
        Spark.post("/game", new CreateGameHandler(db));
        Spark.get("/game", new ListGamesHandler(db));
        Spark.put("/game", new JoinGameHandler(db));

        // Register your endpoints and handle exceptions here.

        //This line initializes the server and can be removed once you have a functioning endpoint 
        Spark.init();

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
