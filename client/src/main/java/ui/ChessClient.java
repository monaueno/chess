package ui;

import client.ServerFacade;
import java.util.List;

import java.util.Scanner;


public class ChessClient {

    private final Scanner scanner = new Scanner(System.in);
    private ServerFacade facade;
    private String authToken = null;
    private List<ListGamesResult.GameSummary> cachedGames;

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
                === Preloading Menu ===
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
                CreateGameResult result = facade.createGame(gameName, authToken);
                System.out.println("Created game with ID: " + result.gameID());
            }
            case "list games" -> {
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
            case "play game" -> {
                if (cachedGames == null || cachedGames.isEmpty()) {
                    System.out.println("No games available. Use 'list games' first.");
                    break;
                }

                try {
                    System.out.print("Enter game number to play: ");
                    int index = Integer.parseInt(scanner.nextLine()) -1;

                    if (index < 0 || index >= cachedGames.size()) {
                        System.out.println("Invalid game number.");
                        break;
                    }

                    ListGamesResult.GameSummary selectedGame = cachedGames.get(index);

                    System.out.print("Enter color (WHITE or BLACK): ");
                    String color = scanner.nextLine().trim().toUpperCase();

                    if (!color.equals("WHITE") && !color.equals("BLACK")) {
                        System.out.println("Invalid color. Please enter 'WHITE' or 'BLACK'.");
                        break;
                    }

                    facade.joinGame(selectedGame.gameID(), color, authToken);

                    System.out.printf("Successfully joined game '%s' as %s.%n", selectedGame.gameName(), color);
                    drawBoard(color.equals("WHITE"));
                } catch(NumberFormatException e){
                    System.out.println("Please enter a valid number.");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }

            }
                case "observe game" -> {
                    if (cachedGames == null || cachedGames.isEmpty()) {
                        System.out.println("No games available. Use 'list games' first.");
                        break;
                    }

                    try {
                        System.out.print("Enter game number to observe: ");
                        int index = Integer.parseInt(scanner.nextLine()) - 1;

                        if (index < 0 || index >= cachedGames.size()) {
                            System.out.println("Invalid game number.");
                            break;
                        }

                        ListGamesResult.GameSummary selectedGame = cachedGames.get(index);

                        facade.joinGame(selectedGame.gameID(), null, authToken);
                        System.out.printf("Now observing game '%s'.%n", selectedGame.gameName());
                        drawBoard(true);
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid number.");
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
            default -> System.out.println("Invalid command. Type 'help' to see options.");
        }
    }

    private void drawBoard(boolean isWhite) {
        System.out.println("Drawing board from " + (isWhite ? "White" : "Black") + "'s perspective...");

        char[] files = {'a','b','c','d','e','f','g','h'};
        int[] ranks = {1,2,3,4,5,6,7,8};

        if (!isWhite) {
            files = new char[] {'h','g','f','e','d','c','b','a'};
            ranks = new int[] {8,7,6,5,4,3,2,1};
        }

        for (int r = ranks.length - 1; r >= 0; r--) {
            System.out.print(ranks[r] + " ");
            for (int f = 0; f < files.length; f++) {
                boolean lightSquare = (r + f) % 2 == 0;
                System.out.print(lightSquare ? "[ ]" : "[##]");
            }
            System.out.println();
        }

        System.out.print(" ");
        for (char file : files) {
            System.out.print(" " + file + " ");
        }
        System.out.println("\n");
    }
}