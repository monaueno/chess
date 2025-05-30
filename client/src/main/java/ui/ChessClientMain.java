package ui;

import java.util.Scanner;

public class ChessClientMain{
    private static final Scanner scanner = new Scanner (System.in);
    private static boolean isLoggedIn = false;

    public static void main(String[] args) {
        System.out.println("Welcome to Chess!");
        String input = scanner.nextLine().trim().toLowerCase();

        switch(input) {
            case "help" -> printPreloginHelp();
            case "register" -> handleRegister();
            case "login" -> handleLogin();
            case "quit" -> {
                System.out.println("Goodbye!");
                return;
            }
            default -> System.out.println("Unknown command. Type 'help' to see options.");
        }
        if(isLoggedIn) {
            postloginLoop();
        }
    }
}

private static void postloginLoop(){
    white(true) {
        System.out.print("[Postlogin] > ");
        String input = scanner.nextLine().trim().toLowerCase();

        switch (input) {
            case "help" -> printPostloginHelp();
            case "logout" -> {
                TODO:
                isLoggedIn = false;
                System.out.println("Logged out.");
                return;
            }
            case "create game" -> handleCreateGame();
            case "list games" -> handleListGames();
            case "play game" -> handlePlayGame();
            case "observe game" -> handleObservationGame();
            default -> System.out.println("Unknown command. Type 'help' to see options.");
        }
    }
}

private static void printPreloginHelp() {
    System.out.println("Commands: help, register, login, quite");
}

private static void printPostloginHelp() {
    System.out.println("Commands: help, logout, create game, list games, play game, observe game");
}

private static void handleRegister() {
    TODO:
    System.out.println("[Stub] Registered user.");
    isLoggedIn = true;
}

private static void handleLogin() {
    TODO:
    System.out.println("[Stub] Logged in.");
    is LoggedIn = true;
}

private static void handleCreateGame() {
    // TODO: prompt and call ServerFacade.createGame()
    System.out.println("[Stub] Created game.");
}

private static void handleListGames() {
    // TODO: call ServerFacade.listGames() and display
    System.out.println("[Stub] Listed games.");
}

private static void handlePlayGame() {
    // TODO: prompt for game number and color, join and draw board
    System.out.println("[Stub] Playing game.");
}

private static void handleObserveGame() {
    // TODO: prompt for game number and draw board
    System.out.println("[Stub] Observing game.");
}
}