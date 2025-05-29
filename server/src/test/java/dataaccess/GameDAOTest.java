import dataaccess.DataAccess;
import dataaccess.MySqlDataAccess;
import model.GameData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import chess.ChessGame;
import dataaccess.DataAccessException;

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
    void getGame_Positive() throws Exception{
        GameData game = new GameData(0, "white", "black", "testGame", new ChessGame());
        int gameID = db.createGame(game);
        assertTrue(gameID > 0);
    }

    @Test
    void createGame_Positive() throws Exception {
        GameData game = new GameData(0, "white", "black", "testGame", new ChessGame());
        int gameID = db.createGame(game);
        GameData fetched = db.getGame(gameID);
        assertNotNull(fetched);
        assertEquals("testGame", fetched.gameName());
    }

    @Test
    void getGame_Negative_NotFound() throws Exception {
        assertNull(db.getGame(99999));
    }

    @Test
    void updateGame_Positive() throws Exception {
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
    void updateGame_Negative_NotFound() {
        ChessGame game = new ChessGame();
        assertThrows(DataAccessException.class, () -> db.updateGame(99999, game));
    }

    @Test
    void listGames_Positive() throws Exception{
        db.createGame(new GameData(0, "white", "black", "game1", new ChessGame()));
        db.createGame(new GameData(0, "white", "black", "game2", new ChessGame()));
        assertEquals(2, db.listGames().size());
    }

    @Test
    void clear_Positive() throws Exception {
        db.createGame(new GameData(0, "white", "black", "game1", new ChessGame()));
        db.clear();
        assertEquals(0, db.listGames().size());
    }
}