package dataaccess;

import java.util.HashMap;
import java.util.Map;
import model.*;
import java.util.ArrayList;
import java.util.List;
import chess.ChessGame;

public class MemoryDataAccess implements DataAccess{
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();

    public void clear(){
        users.clear();
        games.clear();
        authTokens.clear();
    }

    public UserData getUser(String username) {
        return users.get(username);
    }

    public void createUser(UserData user) {
        users.put(user.username(), user);
    }

    public void createAuth(AuthData auth) {
        authTokens.put(auth.authToken(), auth);
    }
    @Override
    public AuthData getAuth(String authToken) {
        return authTokens.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) {
        authTokens.remove(authToken);
    }
    @Override
    public int createGame(GameData game) {
        games.put(game.gameID(), game);
        return game.gameID();
    }

    @Override
    public List<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public void updateGame(int gameID, ChessGame game) throws DataAccessException {
        GameData old = games.get(gameID);
        if(old == null){
            throw new DataAccessException("Game ID not found: " + gameID);
        }
        games.put(gameID, new GameData(
                gameID,
                old.whiteUsername(),
                old.blackUsername(),
                old.gameName(),
                game
        ));
    }
    @Override
    public void setBlackPlayer(int gameID, String username) throws DataAccessException {
        GameData old = games.get(gameID);
        if (old == null) {
            throw new DataAccessException("Game ID not found: " + gameID);
        }
        games.put(gameID, new GameData(
                gameID,
                old.whiteUsername(),
                username,
                old.gameName(),
                old.game()
        ));
    }
    @Override
    public void setWhitePlayer(int gameID, String username) throws DataAccessException {
        GameData old = games.get(gameID);
        if (old == null) {
            throw new DataAccessException("Game ID not found: " + gameID);
        }
        games.put(gameID, new GameData(
                gameID,
                old.blackUsername(),
                username,
                old.gameName(),
                old.game()
        ));
    }

    @Override
    public void addObserver(int gameID, String username) throws DataAccessException {
        GameData game = games.get(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found");
        }
        game.observers().add(username);
    }


}