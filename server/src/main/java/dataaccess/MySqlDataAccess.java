package dataaccess;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.List;
import com.google.gson.Gson;
import chess.ChessGame;
import java.util.ArrayList;

import com.google.gson.reflect.TypeToken;
import org.mindrot.jbcrypt.BCrypt;
import model.*;

public class MySqlDataAccess implements DataAccess {

    public MySqlDataAccess() throws DataAccessException {
        DatabaseManager.createDatabase();
        DatabaseManager.createTables();
    }

    private void createTables() throws SQLException {
        System.out.println("Creating tables...");

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
                CREATE TABLE games (
                    gameID INT AUTO_INCREMENT PRIMARY KEY,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255),
                    gameData TEXT NOT NULL,
                    observers TEXT
                );
            """);

        } catch (DataAccessException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.email());
            System.out.println("➕ Creating user: " + user.username());

            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Error creating user", ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, password, email FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)){
            System.out.println("🔍 Checking for user: " + username);
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()){
                if (rs.next()){
                    String user = rs.getString("username");
                    String pass = rs.getString("password");
                    String email = rs.getString("email");
                    return new UserData(user, pass, email);
                } else{
                    return null;
                }
            }
        }
       catch (SQLException ex){
            throw new DataAccessException("Error fetching user", ex);
       }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String sql = "INSERT INTO auth (authToken, username) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());

            stmt.executeUpdate();
        } catch (SQLException ex){
            throw new DataAccessException("Error creating auth token", ex);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT authToken, username FROM auth WHERE authToken = ?";

        try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, authToken);

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    String token = rs.getString("authToken");
                    String username = rs.getString("username");
                    return new AuthData(token, username);
                }else{
                    return null;
                }
            }
        }catch (SQLException ex) {
            throw new DataAccessException("Error retrieving auth token", ex);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auth WHERE authToken = ?";

        try(Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, authToken);
            int rowsAffected = stmt.executeUpdate();

            if(rowsAffected == 0) {
                throw new DataAccessException("Error: Auth token not found: " + authToken);
            }
        } catch(SQLException ex) {
            throw new DataAccessException("Error deleting auth token", ex);
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        if (game == null || game.gameName() == null || game.gameName().isBlank() || game.game() == null) {
            throw new DataAccessException("Error: Invalid game data");
        }

        String insertSql = "INSERT INTO games (whiteUsername, blackUsername, gameName, gameData) VALUES (?, ?, ?, ?)";
        String gameJson = new Gson().toJson(game.game());

        if (game.gameName() == null || game.gameName().isBlank()) {
            throw new DataAccessException("Error: Game name cannot be null or blank");
        }
        if (gameJson == null || gameJson.isBlank()) {
            throw new DataAccessException("Error: Game JSON serialization failed or empty");
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            if (game.whiteUsername() == null || game.whiteUsername().isBlank()) {
                stmt.setNull(1, Types.VARCHAR);
            } else {
                stmt.setString(1, game.whiteUsername());
            }

            if (game.blackUsername() == null || game.blackUsername().isBlank()) {
                stmt.setNull(2, Types.VARCHAR);
            } else {
                stmt.setString(2, game.blackUsername());
            }

            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);

            int affectedRows = 0;
            try {
                affectedRows = stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DataAccessException("Error: SQL Error during game insert: " + e.getSQLState() + " - " + e.getMessage(), e);
            }
            if (affectedRows == 0) {
                throw new DataAccessException("Error: Creating game failed, no rows affected.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DataAccessException("Creating game failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert game: " + game.gameName(), e);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT * FROM games WHERE gameID = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql2 = "SELECT gameID, whiteUsername, blackUsername, gameName, gameData, observers FROM games WHERE gameID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql2)) {
                stmt.setInt(1, gameID);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String whiteUsername = rs.getString("whiteUsername");
                        System.out.println("✔️ Reading from DB - whiteUsername: " + whiteUsername);
                        String blackUsername = rs.getString("blackUsername");
                        String gameName = rs.getString("gameName");
                        String gameJson = rs.getString("gameData");
                        ChessGame game = new Gson().fromJson(gameJson, ChessGame.class);
                        return new GameData(gameID, whiteUsername, blackUsername, gameName, game, new ArrayList<>());
                    } else {
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get game: " + e.getMessage());
        }
    }

    @Override
    public void updateGame(int gameID, ChessGame game) throws DataAccessException {
        String sql = "UPDATE games SET gameData = ? WHERE gameID = ?";
        String gameJson = new Gson().toJson(game);

        try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, gameJson);
            stmt.setInt(2, gameID);

            int rowsAffected = stmt.executeUpdate();
            if(rowsAffected == 0){
                throw new DataAccessException("No game found with ID: " + gameID);
            }
        }catch (SQLException ex){
            throw new DataAccessException("Error updating game in database", ex);
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        String sql = "SELECT gameID, whiteUsername, blackUsername, gameName, gameData FROM games";
        List<GameData> games = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()){
            while (rs.next()){
                int gameID = rs.getInt("gameID");
                String white = rs.getString("whiteUsername");
                String black = rs.getString("blackUsername");
                String name = rs.getString("gameName");
                String gameJson = rs.getString("gameData");

                ChessGame game = new Gson().fromJson(gameJson, ChessGame.class);
                games.add(new GameData(gameID, white, black, name, game, new ArrayList<>()));
            }
            return games;
        }catch(SQLException ex){
            throw new DataAccessException("Error listing games", ex);
        }

    }

    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM auth");
                stmt.executeUpdate("DELETE FROM games");
                stmt.executeUpdate("DELETE FROM users");
            }
            DatabaseManager.createTables();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to clear database", ex);
        }
    }
    @Override
    public void setWhiteUsername(int gameID, String username) throws DataAccessException {
        GameData game = getGame(gameID);
        GameData updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game(), game.observers());
        System.out.println("Saving whiteUsername: " + username);
        updateGameData(gameID, updated);
    }
    @Override
    public void setBlackUsername(int gameID, String username) throws DataAccessException {
        GameData game = getGame(gameID);
        GameData updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game(), game.observers());
        updateGameData(gameID, updated);
        System.out.println("Saving blackUsername: " + username);
    }

    @Override
    public void addObserver(int gameID, String username) throws DataAccessException {
        System.out.printf("Observer %s added to game %d%n", username, gameID);
    }

    @Override
    public String getUsernameFromAuth(String authToken) throws DataAccessException {
        AuthData auth = getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: Auth token not found: " + authToken);
        }
        return auth.username();
    }

    @Override
    public void updateGameData(int gameID, GameData game) throws DataAccessException {
        String sql = "UPDATE games SET whiteUsername = ?, blackUsername = ?, gameName = ?, gameData = ?, observers = ? WHERE gameID = ?";
        String gameJson = new Gson().toJson(game.game());
        String observersJson = new Gson().toJson(game.observers());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            System.out.println("updateGameData: whiteUsername = " + game.whiteUsername());
            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);
            stmt.setString(5, observersJson);
            stmt.setInt(6, gameID);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("No game found with ID: " + gameID);
            }

        } catch (SQLException ex) {
            throw new DataAccessException("Error updating full game data", ex);
        }
    }
}