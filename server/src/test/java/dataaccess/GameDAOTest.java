package dataaccess;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import chess.ChessGame;

public class GameDAOTest{
    private DataAccess db;

    @BeforeEach
    public void setup() throws Exception{
        db = new MySqlDataAccess();
        db.clear();
    }

    @AfterEach
    public void cleanup() throws Exception{
        db.clear();
    }

    @Test
    void getGamePositive() throws Exception{
        GameData game = new GameData(0, "white", "black", "testGame", new ChessGame());
        int gameID = db.createGame(game);
        assertTrue(gameID > 0);
    }

    @Test
    void createGamePositive() throws Exception {
        GameData game = new GameData(0, "white", "black", "testGame", new ChessGame());
        int gameID = db.createGame(game);
        GameData fetched = db.getGame(gameID);
        assertNotNull(fetched);
        assertEquals("testGame", fetched.gameName());
    }

    @Test
    void createGameNegativeInvalidGame(){
        GameData invalidGame = new GameData(0, "", "", "", new ChessGame());
        assertThrows(DataAccessException.class, () -> db.createGame(invalidGame));
    }

    @Test
    void getGameNegativeNotFound() throws Exception {
        assertNull(db.getGame(99999));
    }

    @Test
    void updateGamePositive() throws Exception {
        GameData game = new GameData(0, "white", "black", "testGame", new ChessGame());
        int gameID = db.createGame(game);
        ChessGame newGame = new ChessGame();
        newGame.makeMove(new chess.ChessMove(new chess.ChessPosition(2, 2), new chess.ChessPosition(3, 2), null));
        db.updateGame(gameID, newGame);
        GameData updated = db.getGame(gameID);
        assertNotNull(updated);
        assertEquals(newGame.getBoard(), updated.game().getBoard());
    }

    @Test
    void updateGameNegativeNotFound() {
        ChessGame game = new ChessGame();
        assertThrows(DataAccessException.class, () -> db.updateGame(99999, game));
    }

    @Test
    void listGamesPositive() throws Exception{
        db.createGame(new GameData(0, "white", "black", "game1", new ChessGame()));
        db.createGame(new GameData(0, "white", "black", "game2", new ChessGame()));
        assertEquals(2, db.listGames().size());
    }

    @Test
    void listGamesNegativeEmpty() throws Exception {
        assertEquals(0, db.listGames().size());
    }

    @Test
    void clearPositive() throws Exception {
        db.createGame(new GameData(0, "white", "black", "game1", new ChessGame()));
        db.clear();
        assertEquals(0, db.listGames().size());
    }
}