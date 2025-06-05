package client;
import model.*;

import org.junit.jupiter.api.*;
import server.Server;


public class ServerFacadeTests {

    private static Server server;
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    public void registerPositive() throws Exception {
        ServerFacade facade = new ServerFacade(port);
        var result = facade.register("passoff", "123", "test@example.com");
        Assertions.assertNotNull(result.authToken());
        Assertions.assertEquals("passoff", result.username());
    }

    @Test
    public void registerNegative() {
        ServerFacade facade = new ServerFacade(port);
        Assertions.assertThrows(Exception.class, () -> {
            facade.register("", "", "");
        });
    }

    @Test
    public void loginPositive() throws Exception {
        ServerFacade facade = new ServerFacade(port);
        var result = facade.login("mona", "mona");
        Assertions.assertNotNull(result.authToken());
        Assertions.assertEquals("mona", result.username());
    }

    @Test
    public void loginNegative() {
        ServerFacade facade = new ServerFacade(port);
        Assertions.assertThrows(Exception.class, () -> {
            facade.login("null", "null");
        });
    }

    @Test
    public void logoutPositive() throws Exception {
        ServerFacade facade = new ServerFacade(port);
        var loginResult = facade.login("mona", "mona");
        facade.logout(loginResult.authToken());
        Assertions.assertTrue(true);
    }

    @Test
    public void logoutNegative() {
        ServerFacade facade = new ServerFacade(port);
        Assertions.assertThrows(Exception.class, () -> {
            facade.logout("");
        });
    }

    @Test
    public void createGamePositive() throws Exception {
        ServerFacade facade = new ServerFacade(port);
        var loginResult = facade.login("mona", "mona");
        var result = facade.createGame("TestGame", loginResult.authToken());
        Assertions.assertNotNull(result.gameID());
    }

    @Test
    public void createGameNegative() {
        ServerFacade facade = new ServerFacade(port);
        Assertions.assertThrows(Exception.class, () -> {
            facade.createGame("", "");
        });
    }

    @Test
    public void listGamesPositive() throws Exception {
        ServerFacade facade = new ServerFacade(port);
        var loginResult = facade.login("mona", "mona");
        var games = facade.listGames(loginResult.authToken());
        Assertions.assertNotNull(games);
    }

    @Test
    public void listGamesNegative() {
        ServerFacade facade = new ServerFacade(port);
        Assertions.assertThrows(Exception.class, () -> {
            facade.listGames("");
        });
    }

    @Test
    public void joinGamePositive() throws Exception {
        ServerFacade facade = new ServerFacade(port);
        AuthResult auth = facade.login("mona", "mona");
        CreateGameResult game = facade.createGame("Test Game", auth.authToken());
        facade.joinGame(game.gameID(), "WHITE", auth.authToken());
    }

    @Test
    public void joinGameNegative() {
        ServerFacade facade = new ServerFacade(port);
        Assertions.assertThrows(Exception.class, () -> {
            facade.joinGame(-1, "WHITE", "");
        });
    }

}
