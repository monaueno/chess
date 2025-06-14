package service.game;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;
import model.data.AuthData;
import model.data.GameData;

import java.util.ArrayList;
import java.util.List;

public class ListGamesService {
    private final DataAccess db;

    public ListGamesService(DataAccess db) {
        this.db = db;
    }

    public ListGamesResult listGames(String authToken) throws DataAccessException {
        AuthData auth = db.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        List<GameData> allGames = db.listGames();  // ← you'll need to implement this
        List<ListGamesResult.GameSummary> summaries = new ArrayList<>();

        for (GameData game : allGames) {
            summaries.add(new ListGamesResult.GameSummary(
                    game.gameID(),
                    game.whiteUsername(),
                    game.blackUsername(),
                    game.gameName()
            ));
        }

        return new ListGamesResult(summaries);
    }
}