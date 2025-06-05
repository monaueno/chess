package client;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import model.*;


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

//    public ListGamesResult listGames(String authToken) throws IOException {
//        return this.<Object, ListGamesResult>makeRequest("/game/list", null, ListGamesResult.class, authToken);
//    }
public ListGamesResult listGames(String authToken) throws IOException {
    HttpURLConnection connection = makeConnection("GET", "/list", authToken);
    connection.connect();
    checkResponse(connection);

    try (InputStream is = connection.getInputStream()) {
        String raw = new String(is.readAllBytes());
        System.out.println("Raw response from server: " + raw);
        return gson.fromJson(raw, ListGamesResult.class);
    }
}

    public void joinGame(int gameID, String color, String authToken) throws IOException {
        JoinGameRequest request = new JoinGameRequest(color, gameID);
        System.out.println("Sending POST to: " + serverUrl + "/join");
        System.out.println("Payload: " + gson.toJson(request));
        System.out.println("JoinGame JSON: " + gson.toJson(request));
        this.<JoinGameRequest, SuccessResponse>makeRequest("/join", request, SuccessResponse.class, authToken);
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
            System.out.println("Raw response from server: " + raw);
            return gson.fromJson(raw, responseType);
//            Reader reader = new InputStreamReader(is);
//            return gson.fromJson(reader, responseType);
        }
    }

    private String sendPost(String endpoint, Object requestBody, String authToken) throws IOException {
        HttpURLConnection connection = makeConnection("POST", endpoint, authToken);

        if (requestBody != null) {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }
        }

        int status = connection.getResponseCode();
        InputStream is = (status >= 400) ? connection.getErrorStream() : connection.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            return response.toString();
        }
    }

    private void checkResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status >= 400) {
            try (InputStream errorStream = connection.getErrorStream()) {
                String raw = new String(errorStream.readAllBytes());
                System.out.println("Raw ERROR response from server: " + raw);
                ErrorResult error = gson.fromJson(raw, ErrorResult.class); // this may still fail
                throw new IOException(error.message());
            }
//            try (InputStream is = connection.getErrorStream()) {
//                Reader reader = new InputStreamReader(is);
//                ErrorResult error = gson.fromJson(reader, ErrorResult.class);
//                throw new IOException("Error: " + error.message());
//            }
        }
    }
}