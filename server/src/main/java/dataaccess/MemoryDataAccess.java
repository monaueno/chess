package dataaccess;

import java.util.HashMap;
import java.util.Map;
import model.*;
import java.util.ArrayList;
import java.util.List;

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
    public void updateGame(GameData game) {
        games.put(game.gameID(), game);
    }

}