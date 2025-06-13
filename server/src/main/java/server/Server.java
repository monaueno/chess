package server;

import dataaccess.MemoryDataAccess;
import spark.*;
import dataaccess.MySqlDataAccess;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import dataaccess.DataAccess;
import server.WebSocketHandler;

public class Server {

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        DataAccess db;
        try {
            DatabaseManager.createDatabase();
            DatabaseManager.createTables();
            db = new MySqlDataAccess();
        } catch (DataAccessException e) {
            System.err.println("DB init failed: " + e.getMessage());
            return -1;
        }

        /* ❷ Provide it to the WebSocket handler BEFORE registering */
        WebSocketHandler.setSharedDB(db);

        Spark.webSocket("/ws", WebSocketHandler.class);

        Spark.post("/register", new RegisterHandler(db));
        Spark.post("/login", new LoginHandler(db));
        Spark.delete("/db", new ClearHandler(db));
        Spark.delete("/session", new LogoutHandler(db));
        Spark.post("/create", new CreateGameHandler(db));
        Spark.get("/list", new ListGamesHandler(db));
        Spark.post("/join", new JoinGameHandler(db));
        Spark.post("/logout", new LogoutHandler(db));
        Spark.post("/user", new RegisterHandler(db));
        Spark.post("/session", new LoginHandler(db));
        Spark.post("/game", new CreateGameHandler(db));
        Spark.put("/game", new JoinGameHandler(db));
        Spark.get("/game", new ListGamesHandler(db));
        Spark.get("/observe", new ObserveGameHandler(db));


        System.out.println("✅ All routes registered, starting Spark server...");
        Spark.init();

        Spark.awaitInitialization();
        System.out.println("✅ Server initialized on port " + Spark.port());
        Spark.routes().forEach(route -> System.out.println("Registered: " + route));
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
