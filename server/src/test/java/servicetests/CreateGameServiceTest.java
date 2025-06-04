package servicetests;

import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import model.CreateGameRequest;
import model.CreateGameResult;
import service.game.CreateGameService;

import static org.junit.jupiter.api.Assertions.*;

class CreateGameServiceTest {
    private MySqlDataAccess db;
    private CreateGameService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new MySqlDataAccess();
        db.clear();

        db.createUser(new UserData("creator", "password", "email@example.com"));
        db.createAuth(new AuthData("valid-token", "creator"));

        service = new CreateGameService(db);
    }

    @Test
    void createGamePositive() throws Exception {
        CreateGameRequest request = new CreateGameRequest("Test Game");
        CreateGameResult result = service.createGame(request, "valid-token");

        assertTrue(result.gameID() > 0);
        assertEquals("Test Game", db.getGame(result.gameID()).gameName());
    }

    @Test
    void createGameNegativeInvalidToken() {
        CreateGameRequest request = new CreateGameRequest("Test Game");
        assertThrows(DataAccessException.class, () -> service.createGame(request, "invalid-token"));
    }

    @Test
    void createGameNegativeBadName() {
        CreateGameRequest request = new CreateGameRequest("   ");
        assertThrows(DataAccessException.class, () -> service.createGame(request, "valid-token"));
    }
}