package servicetests;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import model.ListGamesResult;
import model.ListGamesResult.GameSummary;
import chess.ChessGame;
import service.game.ListGamesService;
import model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListGamesServiceTest {
    private DataAccess db;
    private ListGamesService service;


    @BeforeEach
    void setUp() throws Exception {
        db = new MySqlDataAccess();
        db.clear();
        service = new ListGamesService(db);

        db.createUser(new model.UserData("testuser", "password", "test@example.com"));
        db.createAuth(new AuthData("valid-token", "testuser"));

        ChessGame game1 = new ChessGame();
        ChessGame game2 = new ChessGame();

        db.createGame(new GameData(0, "testuser", null, "Game One", game1));
        db.createGame(new GameData(0, "testuser", null, "Game Two", game2));
    }

    @Test
    void listGamesPositive() throws Exception {
        ListGamesResult result = service.listGames("valid-token");
        List<GameSummary> games = result.games();

        assertEquals(2, games.size());
        assertTrue(games.stream().anyMatch(g -> g.gameName().equals("Game One")));
        assertTrue(games.stream().anyMatch(g -> g.gameName().equals("Game Two")));
    }

    @Test
    void listGamesNegativeInvalidToken() {
        assertThrows(DataAccessException.class, () -> service.listGames("invalid-token"));
    }
}