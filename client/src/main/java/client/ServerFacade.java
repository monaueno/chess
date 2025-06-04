package client;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import model.*;


public class ServerFacade {

    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public AuthResult register(String username, String password, String email) throws IOException {
        RegisterRequest request = new RegisterRequest(username, password, email);
        return this.<RegisterRequest, AuthResult>makeRequest("/user/register", request, AuthResult.class);
    }

    public AuthResult login(String username, String password) throws IOException {
        LoginRequest request = new LoginRequest(username, password);
        return this.<LoginRequest, AuthResult>makeRequest("/user/login", request, AuthResult.class);
    }

    public void logout(String authToken) throws IOException {
        HttpURLConnection connection = makeConnection("POST", "/user/logout", authToken);
        connection.connect();
        checkResponse(connection);
    }

    public CreateGameResult createGame(String gameName, String authToken) throws Exception {
        CreateGameRequest request = new CreateGameRequest(gameName);
        return this.<CreateGameRequest, CreateGameResult>makeRequest("/game/create", request, CreateGameResult.class, authToken);
    }

    public ListGamesResult listGames(String authToken) throws IOException {
        return this.<Object, ListGamesResult>makeRequest("/game/list", null, ListGamesResult.class, authToken);
    }

    public void joinGame(int gameID, String color, String authToken) throws IOException {
        JoinGameRequest request = new JoinGameRequest(color, gameID);
        this.<JoinGameRequest, Void>makeRequest("/game/join", request, Void.class, authToken);
    }

    private HttpURLConnection makeConnection(String method, String endpoint, String authToken) throws IOException {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", authToken);
        connection.setRequestProperty("Content-Type", "application/json");
        return connection;
    }

    private <T, R> R makeRequest(String endpoint, T requestBody, Class<R> responseType) throws IOException {
        return makeRequest(endpoint, requestBody, responseType, null);
    }

    private <T, R> R makeRequest(String endpoint, T requestBody, Class<R> responseType, String authToken) throws IOException {
        HttpURLConnection connection = makeConnection("POST", endpoint, authToken);
        if (requestBody != null) {
            try (OutputStream os = connection.getOutputStream()) {
                os.write(gson.toJson(requestBody).getBytes());
            }
        }

        checkResponse(connection);

        try (InputStream is = connection.getInputStream()) {
            Reader reader = new InputStreamReader(is);
            return gson.fromJson(reader, responseType);
        }
    }

    private void checkResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status >= 400) {
            try (InputStream is = connection.getErrorStream()) {
                Reader reader = new InputStreamReader(is);
                ErrorResult error = gson.fromJson(reader, ErrorResult.class);
                throw new IOException("Error: " + error.message());
            }
        }
    }
}