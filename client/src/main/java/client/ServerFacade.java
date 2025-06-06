package client;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import model.*;
import model.JoinGameRequest;
import model.SuccessResponse;


public class ServerFacade {

    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public AuthResult register(String username, String password, String email) throws IOException {
        RegisterRequest request = new RegisterRequest(username, password, email);
        return this.<RegisterRequest, AuthResult>makeRequest("/register", request, AuthResult.class);
    }

    public AuthResult login(String username, String password) throws IOException {
        LoginRequest request = new LoginRequest(username, password);
        return this.<LoginRequest, AuthResult>makeRequest("/login", request, AuthResult.class);
    }

    public void logout(String authToken) throws IOException {
        HttpURLConnection connection = makeConnection("POST", "/logout", authToken);
        connection.connect();
        checkResponse(connection);
    }

    public CreateGameResult createGame(String gameName, String authToken) throws Exception {
        CreateGameRequest request = new CreateGameRequest(gameName);
        return this.<CreateGameRequest, CreateGameResult>makeRequest("/create", request, CreateGameResult.class, authToken);
    }

public ListGamesResult listGames(String authToken) throws IOException {
    HttpURLConnection connection = makeConnection("GET", "/list", authToken);
    connection.connect();
    checkResponse(connection);

    try (InputStream is = connection.getInputStream()) {
        String raw = new String(is.readAllBytes());
        return gson.fromJson(raw, ListGamesResult.class);
    }
}

    public SuccessResponse joinGame(int gameID, String playerColor, String authToken) throws IOException {
        // Build a request that sets asObserver to true when playerColor is "observe"
        JoinGameRequest request = new JoinGameRequest(playerColor, gameID);
        return this.<JoinGameRequest, SuccessResponse>makeRequest("/join", request, SuccessResponse.class, authToken);
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
            String raw = new String(is.readAllBytes());
            return gson.fromJson(raw, responseType);
        }
    }


    private void checkResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status >= 400) {
            try (InputStream errorStream = connection.getErrorStream()) {
                String raw = new String(errorStream.readAllBytes());
                ErrorResult error = gson.fromJson(raw, ErrorResult.class); // this may still fail
                throw new IOException(error.message());
            }
        }
    }

    public void clear() throws Exception {
        var url = new URL(serverUrl + "/db");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.connect();

        if (connection.getResponseCode() != 200) {
            throw new Exception("Failed to clear database: " + connection.getResponseMessage());
        }
    }

    private <T, R> R sendPost(String endpoint, T requestBody, String authToken, Class<R> responseType) throws IOException {
        HttpURLConnection connection = makeConnection("POST", endpoint, authToken);
        if (requestBody != null) {
            try (OutputStream os = connection.getOutputStream()) {
                os.write(gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8));
            }
        }

        checkResponse(connection);

        try (InputStream is = connection.getInputStream()) {
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return gson.fromJson(raw, responseType);
        }
    }
}