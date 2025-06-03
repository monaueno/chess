package ui;

import client.ServerFacade;

import java.util.Scanner;

public class ChessClient {

    private final Scanner scanner = new Scanner(System.in);
    private ServerFacade facade;
    private String authToken = null;

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
            System.out.println("Fatal error: " + ex.getMessage());
        }
    }

    private void displayPreloginMenu() {
        System.out.println("""
                === Prelogin Menu ===
                - Help
                - Register
                - Login
                - Quit
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

        var auth = facade.register(username, password, email);
        authToken = auth.authToken();
        System.out.println("Registered and logged in as " + username);
    }

    private void handleLogin() throws Exception {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        var auth = facade.login(username, password);
        authToken = auth.authToken();
        System.out.println("Logged in as " + username);
    }

    private void displayPostloginMenu() {
        System.out.println("""
                === Postlogin Menu ===
                - Help
                - Logout
                - Create Game
                - List Games
                - Play Game
                - Observe Game
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
                System.out.println("Logged out.");
            }
            case "create game" -> {
                System.out.print("Game name: ");
                String gameName = scanner.nextLine();
                var result = facade.createGame(gameName, authToken);
                System.out.println("Created game with ID: " + result.gameID());
            }
            case "list games" -> {
                var result = facade.listGames(authToken);
                int i = 1;
                for (var game : result.games()) {
                    System.out.printf("%d. %s | White: %s | Black: %s%n",
                            i++, game.gameName(),
                            game.whiteUsername() != null ? game.whiteUsername() : "-",
                            game.blackUsername() != null ? game.blackUsername() : "-");
                }
            }
            case "play game", "observe game" -> System.out.println("Not implemented yet. Will draw board.");
            default -> System.out.println("Invalid command. Type 'help' to see options.");
        }
    }
}