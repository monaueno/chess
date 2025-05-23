package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import java.util.ArrayList;
import java.util.List;

public class ListGameService {
    private final DataAccess db;

    public ListGameService(DataAccess db) {
        this.db = db;
    }

    public ListGameResult listGames(String authToken) throws DataAccessException {
        AuthData auth = db.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        List<GameData> allGames = db.listGames();  // ← you'll need to implement this
        List<ListGameResult.GameSummary> summaries = new ArrayList<>();

        for (GameData game : allGames) {
            summaries.add(new ListGameResult.GameSummary(
                    game.gameID(),
                    game.whiteUsername(),
                    game.blackUsername(),
                    game.gameName()
            ));
        }

        return new ListGameResult(summaries);
    }
}