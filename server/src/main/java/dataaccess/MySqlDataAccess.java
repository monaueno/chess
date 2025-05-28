package dataaccess;

import java.sql.*;
import java.util.List;
import model.UserData;
import model.AuthData;
import model.GameData;
import com.google.gson.Gson;
import chess.ChessGame;

public class MySqlDataAccess implements DataAccess {

    public MySqlDataAccess() throws DataAccessException {
        try {
            createTables();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create database tables", e);
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(255) PRIMARY KEY,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth (
                    authToken VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(255),
                    FOREIGN KEY (username) REFERENCES users(username)
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT AUTO_INCREMENT PRIMARY KEY,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255),
                    gameData TEXT NOT NULL
                );
            """);

        } catch (DataAccessException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        String insertSql = "INSERT INTO games (whiteUsername, blackUsername, gameName, gameData) VALUES (?, ?, ?, ?)";
        String gameJson = new Gson().toJson(game.game()); // serialize the ChessGame object

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DataAccessException("Failed to retrieve generated game ID");
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert game", e);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT whiteUsername, blackUsername, gameName, gameData FROM games WHERE gameID = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gameID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String whiteUsername = rs.getString("whiteUsername");
                    String blackUsername = rs.getString("blackUsername");
                    String gameName = rs.getString("gameName");
                    String gameJson = rs.getString("gameData");

                    ChessGame chessGame = new Gson().fromJson(gameJson, ChessGame.class);
                    return new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame);
                } else {
                    return null;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve game", e);
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void clear() throws DataAccessException {
        // Not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }
}