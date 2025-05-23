package dataaccess;
import model.UserData;
import model.AuthData;
import model.GameData;
import java.util.List;

public interface DataAccess {
    void clear();
    UserData getUser(String username) throws DataAccessException;

    void createUser(UserData username) throws DataAccessException;
    void createAuth(AuthData auth) throws DataAccessException;

    AuthData getAuth(String authToken) throws DataAccessException;

    void deleteAuth(String authToken) throws DataAccessException;
    void createGame(GameData game) throws DataAccessException;
    List<GameData> listGames() throws DataAccessException;
}