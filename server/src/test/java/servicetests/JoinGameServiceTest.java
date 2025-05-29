package servicetests;

import chess.ChessGame;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.game.JoinGameRequest;
import service.game.JoinGameService;

import static org.junit.jupiter.api.Assertions.*;

class JoinGameServiceTest {
    private MySqlDataAccess db;
    private service.game.JoinGameService service;


    @BeforeEach
    void setUp() throws Exception {
        db = new MySqlDataAccess();
        db.clear();

        db.createUser(new UserData("player1", "pass", "email"));
        db.createAuth(new AuthData("token1", "player1"));

        db.createUser(new UserData("player2", "pass", "email2"));
        db.createAuth(new AuthData("token2", "player2"));

        GameData game = new GameData(0, null, null, "test game", new ChessGame());
        db.createGame(game);

        service = new JoinGameService(db);
    }

    @Test
    void joinGamePositive() throws Exception {
        int gameID = db.listGames().get(0).gameID();
        JoinGameRequest request = new JoinGameRequest("BLACK", gameID);

        service = new service.game.JoinGameService(db);
        assertDoesNotThrow(() -> service.joinGame(request, "token1"));

        GameData updated = db.getGame(gameID);
        assertEquals("player1", updated.blackUsername());

        service = new JoinGameService(db);
    }

    @Test
    void joinGameNegativeAlreadyTaken() throws Exception {
        int gameID = db.listGames().get(0).gameID();

        // First player takes BLACK
        JoinGameRequest first = new JoinGameRequest("BLACK", gameID);
        service = new service.game.JoinGameService(db);
        service.joinGame(first, "token1");

        // Second player tries to take BLACK
        JoinGameRequest second = new JoinGameRequest("BLACK", gameID);
        assertThrows(dataaccess.DataAccessException.class, () -> service.joinGame(second, "token2"));

        service = new JoinGameService(db);
    }

    @Test
    void joinGameNegativeInvalidToken() throws DataAccessException{
        int gameID = db.listGames().get(0).gameID();
        JoinGameRequest request = new JoinGameRequest("WHITE", gameID);
        assertThrows(DataAccessException.class, () -> service.joinGame(request, "invalid-token"));
        service = new JoinGameService(db);
    }

    @Test
    void joinGameNegativeInvalidColor() throws DataAccessException {
        int gameID = db.listGames().get(0).gameID();
        JoinGameRequest request = new JoinGameRequest("GREEN", gameID);
        assertThrows(DataAccessException.class, () -> service.joinGame(request, "token1"));

        service = new JoinGameService(db);
    }

    @Test
    void joinGameNegativeInvalidGameID() throws DataAccessException{
        JoinGameRequest request = new JoinGameRequest("WHITE", 9999); // game doesn't exist
        assertThrows(DataAccessException.class, () -> service.joinGame(request, "token1"));

        service = new JoinGameService(db);
    }
}