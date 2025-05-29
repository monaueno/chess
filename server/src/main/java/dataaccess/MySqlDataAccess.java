package dataaccess;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import model.UserData;
import model.AuthData;
import model.GameData;
import com.google.gson.Gson;
import chess.ChessGame;
import java.util.ArrayList;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Properties;
import java.io.InputStream;

public class MySqlDataAccess implements DataAccess {

    public MySqlDataAccess() throws DataAccessException {
        try {
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                if (input == null) {
                    throw new RuntimeException("Unable to find db.properties");
                }
                props.load(input);
            }

            String dbHost = props.getProperty("db.host");
            String dbPort = props.getProperty("db.port");
            String dbName = props.getProperty("db.name");
            String dbUser = props.getProperty("db.user");
            String dbPassword = props.getProperty("db.password");

            // Create database if it doesn't exist
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://" + dbHost + ":" + dbPort + "/?user=" + dbUser + "&password=" + dbPassword);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            }

            createTables();
        } catch (IOException | SQLException e) {
            throw new DataAccessException("Failed to initialize MySQL DAO", e);
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
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.email());

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
                throw new DataAccessException("Auth token not found: " + authToken);
            }
        } catch(SQLException ex) {
            throw new DataAccessException("Error deleting auth token", ex);
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        // Input validation at the beginning
        if (game == null || game.gameName() == null || game.gameName().isBlank() || game.game() == null) {
            throw new DataAccessException("Invalid game data");
        }
        String insertSql = "INSERT INTO games (whiteUsername, blackUsername, gameName, gameData) VALUES (?, ?, ?, ?)";
        String gameJson = new Gson().toJson(game.game());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int gameID = rs.getInt(1);
                    return gameID;
                } else {
                    throw new DataAccessException("Failed to retrieve generated game ID");
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert game: " + game.gameName(), e);
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
                games.add(new GameData(gameID, white, black, name, game));
            }
            return games;
        }catch(SQLException ex){
            throw new DataAccessException("Error listing games", ex);
        }

    }

    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
        Statement stmt = conn.createStatement()){
            stmt.executeUpdate("DELETE FROM auth");
            stmt.executeUpdate("DELETE FROM games");
            stmt.executeUpdate("DELETE FROM users");
        }catch (SQLException ex){
            throw new DataAccessException("Failed to clear database", ex);
        }
    }
    @Override
    public void setWhitePlayer(int gameID, String username) throws DataAccessException {
        String sql = "UPDATE games SET whiteUsername = ? WHERE gameID = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, gameID);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to set white player", ex);
        }
    }
    @Override
    public void setBlackPlayer(int gameID, String username) throws DataAccessException {
        String sql = "UPDATE games SET blackUsername = ? WHERE gameID = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, gameID);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to set black player", ex);
        }
    }
}