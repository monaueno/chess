package ui;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import client.ServerFacade;
import client.websocket.GameplayWebSocketHandler;
import model.*;
import model.ListGamesResult;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import java.util.stream.Collectors;

import chess.ChessBoard;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import ui.ChessBoardUI;

public class ChessClient {

    private final Scanner scanner = new Scanner(System.in);
    private ServerFacade facade;
    private String authToken = null;
    private String currentUsername = null;
    private List<ListGamesResult.GameSummary> cachedGames;
    private ChessGame game;
    private ChessPosition highlightedFrom;
    private Set<ChessPosition> highlightedTo;
    private ChessBoard board;
    private boolean whitePerspective = true;

    public static void main(String[] args) {
        new ChessClient().run();
    }

    private void run() {
        System.out.println("Welcome to CS 240 Chess!");
        try {
            int port = 8080; // or read from args
            facade = new ServerFacade(port);

            while (true) {
                if (authToken == null) {
                    displayPreloginMenu();
                    handlePreloginInput();
                } else {
                    displayPostloginMenu();
                    handlePostloginInput();
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void displayPreloginMenu() {
        System.out.println();
        System.out.println("""
                - Register
                - Login
                - Quit
                - Help
                """);
    }

    private void handlePreloginInput() throws Exception {
        System.out.print("> ");
        String command = scanner.nextLine().trim().toLowerCase();

        switch (command) {
            case "help" -> System.out.println("Commands: register, login, quit");
            case "register" -> handleRegister();
            case "login" -> handleLogin();
            case "quit" -> System.exit(0);
            default -> System.out.println("Invalid command. Type 'help' to see options.");
        }
    }

    private void handleRegister() throws Exception {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();

        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            System.out.println();
            System.out.println("Registration failed: All fields are required.");
            return;
        }

        try {
            var auth = facade.register(username, password, email);
            authToken = auth.authToken();
            currentUsername = username;
            System.out.println();
            System.out.println("Registered and logged in as " + username);
        } catch (Exception e) {
            System.out.println();
            System.out.println("USER EXISTS: Please log in.");
            System.out.println(e.getMessage());
        }
    }

    private void handleLogin() throws Exception {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        try {
            var auth = facade.login(username, password);
            authToken = auth.authToken();
            currentUsername = username;
            System.out.println();
            System.out.println("Logged in as " + username);
        } catch (Exception e) {
            System.out.println();
            System.out.println("USER DOESN'T EXIST: Please register first");
            System.out.println(e.getMessage());
        }
    }

    private void displayPostloginMenu() {
        System.out.println();
        System.out.println("""
                === MENU ===
                - Create Game
                - List Games
                - Play Game
                - Observe Game
                - Help
                - Logout
                """);
    }

    private void handlePostloginInput() throws Exception {
        System.out.print("> ");
        String command = scanner.nextLine().trim().toLowerCase();

        switch (command) {
            case "help" -> System.out.println("Commands: logout, create game, list games, play game, observe game");
            case "logout" -> {
                facade.logout(authToken);
                authToken = null;
                currentUsername = null;
                System.out.println();
                System.out.println("Logged out");
            }
            case "create game" -> handleCreateGame();
            case "list games" -> handleListGames();
            case "play game" -> {
                handlePlayGame();
                return;
            }
            case "observe game" -> handleObserveGame();
            case String s when s.startsWith("h ") -> handleHighlight(s.substring(2).trim());
            default -> System.out.println("Invalid command. Type 'help' to see options.");
        }
    }

    private void handleCreateGame() {
        System.out.print("Game name: ");
        String gameName = scanner.nextLine();

        try {
            if (gameName.isBlank()) {
                System.out.println("Game creation failed: name cannot be blank.");
                return;
            }
            CreateGameResult result = facade.createGame(gameName, authToken);
            System.out.println("Created game: " + gameName);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleListGames() throws IOException {
        var result = facade.listGames(authToken);
        cachedGames = result.games();
        int i = 1;
        for (var game : cachedGames) {
            System.out.printf("%d. %s | White: %s | Black: %s%n",
                    i++, game.gameName(),
                    game.whiteUsername() != null ? game.whiteUsername() : "-",
                    game.blackUsername() != null ? game.blackUsername() : "-");
        }
    }

    private void handlePlayGame() {
        if (cachedGames == null || cachedGames.isEmpty()) {
            System.out.println("No games available. Use 'list games' first.");
            return;
        }

        try {
            System.out.print("Enter game number to play: ");
            int index = Integer.parseInt(scanner.nextLine()) - 1;

            if (index < 0 || index >= cachedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            ListGamesResult.GameSummary selectedGame = cachedGames.get(index);

            System.out.print("Enter color (WHITE or BLACK): ");
            String color = scanner.nextLine().trim().toUpperCase();
            whitePerspective = color.equals("WHITE");

            if (!color.equals("WHITE") && !color.equals("BLACK")) {
                System.out.println("Invalid color. Please enter 'WHITE' or 'BLACK'.");
                return;
            }

            try {
                facade.joinGame(selectedGame.gameID(), color, authToken);
                System.out.println();
                System.out.printf("Joined game '%s' as %s.%n", selectedGame.gameName(), color);

                WebSocketClient client = new WebSocketClient();
                client.start();
                URI uri = new URI("ws://localhost:8080/ws");

                GameplayWebSocketHandler handler = new GameplayWebSocketHandler(authToken, selectedGame.gameID(), currentUsername, this::promptForMove);
                client.connect(handler, uri).get();

                while(!handler.hasExited()) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }


        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void promptForMove() {
    }

    private void handleObserveGame() {
        if (cachedGames == null || cachedGames.isEmpty()) {
            System.out.println("No games available. Use 'list games' first.");
            return;
        }

        try {
            System.out.print("Enter game number to observe: ");
            int index = Integer.parseInt(scanner.nextLine()) - 1;

            if (index < 0 || index >= cachedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            observeGame(index);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    private void observeGame(int index) {
        ListGamesResult.GameSummary selectedGame = cachedGames.get(index);
        try {
            facade.observeGame(selectedGame.gameID(), authToken);
            System.out.printf("Now observing game '%s'.%n", selectedGame.gameName());
            ChessBoard board = new ChessBoard();
            board.resetBoard();
            new ChessBoardUI().drawBoard(board, true, highlightedFrom, highlightedTo);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    private void handleHighlight(String input) {
        try {
            if (input.length() != 2 || input.charAt(0) < 'a' || input.charAt(0) > 'h' || !Character.isDigit(input.charAt(1))) {
                System.out.println("Invalid input. Use format like 'h d2'.");
                return;
            }

            int file = input.charAt(0) - 'a' + 1;
            int rank = Character.getNumericValue(input.charAt(1));

            if (rank < 1 || rank > 8) {
                System.out.println("Invalid rank. Use numbers 1 through 8.");
                return;
            }

            int row = rank;
            int col = file;

            ChessPosition start = new ChessPosition(row, col);

            if (game == null) {
                System.out.println("Game not loaded yet.");
                return;
            }

            Collection<ChessMove> valid = game.validMoves(start);
            if (valid == null) {
                System.out.println("No piece at that position.");
                return;
            } else if (valid.isEmpty()) {
                System.out.println("No legal moves.");
                return;
            }

            highlightedFrom = start;
            highlightedTo = valid.stream().map(ChessMove::getEndPosition).collect(Collectors.toSet());

            new ChessBoardUI().drawBoard(game.getBoard(), whitePerspective, highlightedFrom, highlightedTo);
        } catch (Exception e) {
            System.out.println("Invalid input. Use format like 'h d2'.");
        }
    }
}
