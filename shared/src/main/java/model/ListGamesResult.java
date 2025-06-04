package model;

import java.util.List;

public record ListGamesResult(List<GameSummary> games) {
    public record GameSummary(int gameID, String whiteUsername, String blackUsername, String gameName) {}
}
