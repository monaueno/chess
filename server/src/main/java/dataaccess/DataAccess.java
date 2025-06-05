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

    void addObserver(int gameID, String username) throws DataAccessException;

    String getUsernameFromAuth(String authToken) throws DataAccessException;

    void setWhiteUsername(int i, String username) throws DataAccessException;

    void setBlackUsername(int i, String username) throws DataAccessException;

    void updateGameData(int i, GameData game) throws DataAccessException;
}