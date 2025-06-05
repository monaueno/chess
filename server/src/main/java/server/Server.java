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

        Spark.post("/register", new RegisterHandler(db));
        Spark.post("/login", new LoginHandler(db));
        Spark.delete("/db", new ClearHandler(db));
        Spark.delete("/session", new LogoutHandler(db));
        Spark.post("/create", new CreateGameHandler(db));
        Spark.get("/list", new ListGamesHandler(db));
        System.out.println("🔍 Registering /game/join route");
        Spark.post("/join", new JoinGameHandler(db));
        Spark.post("/logout", new LogoutHandler(db));
        Spark.post("/user", new RegisterHandler(db));
        Spark.post("/session", new LoginHandler(db));
        Spark.post("/game", new CreateGameHandler(db));
        Spark.put("/game", new JoinGameHandler(db));
        Spark.get("/game", new ListGamesHandler(db));

        // Register your endpoints and handle exceptions here.

        //This line initializes the server and can be removed once you have a functioning endpoint 
        System.out.println("✅ All routes registered, starting Spark server...");
        Spark.init();

        Spark.awaitInitialization();
        System.out.println("✅ Server initialized on port " + Spark.port());
        Spark.routes().forEach(route -> System.out.println("📍 Registered: " + route));
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
