package dataaccess;
import model.UserData;
import model.AuthData;
import model.GameData;
import java.util.List;
import chess.ChessGame;

public interface DataAccess {
    void clear() throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;

    void createUser(UserData username) throws DataAccessException;
    void createAuth(AuthData auth) throws DataAccessException;

    AuthData getAuth(String authToken) throws DataAccessException;

    void deleteAuth(String authToken) throws DataAccessException;
    int createGame(GameData game) throws DataAccessException;
    List<GameData> listGames() throws DataAccessException;

    GameData getGame(int gameID) throws DataAccessException;
    void updateGame(int gameID, ChessGame game) throws DataAccessException;

    void setWhitePlayer(int gameID, String username) throws DataAccessException;
    void setBlackPlayer(int gameID, String username) throws DataAccessException;
}