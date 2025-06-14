package client.websocket;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import model.GameData;
import ui.ChessBoardUI;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.ErrorMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;
import com.google.gson.Gson;
import websocket.messages.LoadGameMessage;

import java.util.Collection;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import chess.ChessPosition;


@ServerEndpoint(value="/ws")
public class GameplayWebSocketHandler {

    private final String authToken;
    private final int gameID;
    private final Runnable onGameLoadedCallback;
    private Session session;
    private static final Gson gson = new Gson();
    private ChessBoard board;
    private ChessGame game = new ChessGame();
    private boolean playerIsWhite;
    private ChessPosition highlightedFrom;
    private Set<ChessPosition> highlightedTo;
    private final Scanner scanner = new Scanner(System.in);
    private volatile boolean exited = false;
    private ChessGame.TeamColor playerColor;
    private String username;
    private final boolean isObserver;

    public GameplayWebSocketHandler(String authToken, int gameID, String username, Runnable promptForMove, boolean isObserver) {
        this.authToken = authToken;
        this.gameID = gameID;
        this.username = username;
        this.onGameLoadedCallback = promptForMove;
        this.isObserver = isObserver;
    }


    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        UserGameCommand connectCommand = new UserGameCommand(CommandType.CONNECT, authToken, gameID);

        try {
            session.getBasicRemote().sendText(gson.toJson(connectCommand));
        } catch (Exception e) {
            System.err.println("Failed to send CONNECT command: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        ServerMessage serverMessage = MessageSerializer.deserializeServerMessage(message);

        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME:
                try {
                    LoadGameMessage loadGameMessage = (LoadGameMessage) serverMessage;
                    GameData data = loadGameMessage.getGame();
                    this.game = data.game();
                    this.board = game.getBoard();

                    if (username.equals(loadGameMessage.getWhiteUsername())) {
                        this.playerColor = ChessGame.TeamColor.WHITE;
                    } else if (username.equals(loadGameMessage.getBlackUsername())) {
                        this.playerColor = ChessGame.TeamColor.BLACK;
                    } else if (!isObserver) {
                        System.err.println("⚠️ Username does not match either white or black player.");
                        return;
                    }

                    this.playerIsWhite = (this.playerColor == ChessGame.TeamColor.WHITE);

                    if (board == null) {
                        System.err.println("Error: board is null in LOAD_GAME");
                        return;
                    }

                    game.setBoard(board);

                    new ChessBoardUI().drawBoard(board, playerColor == ChessGame.TeamColor.WHITE, highlightedFrom, highlightedTo);

                    if (game.isGameOver()) {
                        System.out.println("Game is over.");
                        System.out.println();

                        if (game.isInCheckmate(ChessGame.TeamColor.WHITE)) {
                            System.out.println("Checkmate! Black wins.");
                        } else if (game.isInCheckmate(ChessGame.TeamColor.BLACK)) {
                            System.out.println("Checkmate! White wins.");
                        } else if (game.getResignedPlayer() != null) {
                            String resigned = game.getResignedPlayer();
                            String winner = resigned.equals(loadGameMessage.getWhiteUsername()) ? loadGameMessage.getBlackUsername() : loadGameMessage.getWhiteUsername();
                            System.out.println(resigned + " has resigned. " + winner + " wins.");
                        } else {
                            System.out.println("It's a draw.");
                        }

                        System.out.println("Only 'exit' is allowed.");
                        new Thread(() -> {
                            while (true) {
                                System.out.print("Command: ");
                                String input = scanner.nextLine().trim().toLowerCase();
                                if (input.equals("exit")) {
                                    sendLeaveCommand();
                                    break;
                                } else {
                                    System.out.println("Game is over. Only 'exit' is allowed.");
                                }
                            }
                        }).start();
                        return;
                    }
                    if (!game.isGameOver()) {
                        if (game.getMoveHistory() != null && !game.getMoveHistory().isEmpty()) {
                            ChessMove lastMove = game.getMoveHistory().get(game.getMoveHistory().size() - 1);
                            String from = (char)('a' + lastMove.getStartPosition().getColumn() - 1) + String.valueOf(lastMove.getStartPosition().getRow());
                            String to = (char)('a' + lastMove.getEndPosition().getColumn() - 1) + String.valueOf(lastMove.getEndPosition().getRow());
                            String moveText = String.format("%s made the move %s to %s", game.getTeamTurn() == ChessGame.TeamColor.WHITE ? loadGameMessage.getBlackUsername() : loadGameMessage.getWhiteUsername(), from, to);
                            System.out.println(moveText);
                            System.out.println("Current turn: " + game.getTeamTurn());
                            System.out.println();
                        }

                        if (!isObserver && game.getTeamTurn().equals(playerColor)) {
                            onGameLoadedCallback.run();
                        }

                        if (playerColor == game.getTeamTurn()) {
                            System.out.println("It's your turn!");
                            new Thread(() -> {
                                while (true) {
                                    System.out.print("Enter move: ");
                                    String input = scanner.nextLine().trim();

                                    if (input.equalsIgnoreCase("resign")) {
                                        System.out.print("Are you sure you want to resign? (y/n): ");
                                        String confirm = scanner.nextLine().trim().toLowerCase();
                                        if (confirm.equals("y")) {
                                            sendResignCommand();
                                            break;
                                        } else {
                                            continue;
                                        }
                                    } else if (input.equalsIgnoreCase("exit")) {
                                        sendLeaveCommand();
                                        break;
                                    } else if (input.matches("^[a-h][1-8]\\s+[a-h][1-8]$")) {
                                        String[] parts = input.split("\\s+");
                                        sendMove(parts[0], parts[1]);
                                        break;
                                    } else if (input.matches("[a-h][1-8]")) {
                                        handleHighlight(input);
                                        return;
                                    } else {
                                        System.out.println("Invalid input. Try again:");
                                    }
                                }
                            }).start();
                        } else {
                            if (!isObserver) {
                                System.out.println("Waiting for opponent to move. You may still type 'resign' or 'exit':");
                                new Thread(() -> {
                                    while (true) {
                                        System.out.print("Command: ");
                                        String input = scanner.nextLine().trim();

                                        if (input.equalsIgnoreCase("resign")) {
                                            sendResignCommand();
                                            break;
                                        } else if (input.equalsIgnoreCase("exit")) {
                                            sendLeaveCommand();
                                            break;
                                        } else {
                                            System.out.println("Not your turn. Only 'resign' or 'exit' allowed.");
                                        }
                                    }
                                }).start();
                            } else {
                                System.out.println("⏳ You are observing the game. Type a square like 'e2' to highlight moves, or 'exit' to leave:");
                                new Thread(() -> {
                                    boolean running = true;
                                    while (running) {
                                        System.out.print("Command: ");
                                        String input = scanner.nextLine().trim();
                                        if (input.equalsIgnoreCase("exit")) {
                                            sendLeaveCommand();
                                            running = false;
                                            break;
                                        } else if (input.matches("^[a-h][1-8]$")) {
                                            handleHighlight(input);
                                        } else {
                                            System.out.println("Invalid command. Type a square like 'e2' to highlight moves, or 'exit' to leave.");
                                        }
                                    }
                                }).start();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load game: " + e.getMessage());
                    return;
                }

                break;

            case NOTIFICATION:
                NotificationMessage noteMsg = (NotificationMessage) serverMessage;
                System.out.println(noteMsg.getMessage());
                break;

            case ERROR:
                ErrorMessage errorMsg = (ErrorMessage) serverMessage;
                System.out.println("Error: " + errorMsg.getErrorMessage());

                if (!game.isGameOver()) {
                    promptForMove();
                }
                break;
        }
    }


    private void handleHighlight(String input) {
        try {
            if (input.length() != 2 || input.charAt(0) < 'a' || input.charAt(0) > 'h' || !Character.isDigit(input.charAt(1))) {
                System.out.println("Invalid input. Use format like 'd2'.");
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

            // Always draw observer board from white's perspective
            new ChessBoardUI().drawBoard(game.getBoard(), isObserver || playerIsWhite, highlightedFrom, highlightedTo);
            promptForMove();
        } catch (Exception e) {
            System.out.println("Invalid input. Use format like 'd2'.");
        }
    }


    private ChessPosition parsePosition(String pos) {
        char fileChar = pos.charAt(0);
        char rankChar = pos.charAt(1);

        int row = Character.getNumericValue(rankChar); // e.g., '2' → 6
        int col = (fileChar - 'a') + 1; // e.g., 'e' → 5
        return new ChessPosition(row, col);
    }

    public void sendMove(String from, String to) {
        ChessPosition fromPos = parsePosition(from);
        ChessPosition toPos = parsePosition(to);

        MakeMoveCommand moveCommand = new MakeMoveCommand(authToken, gameID, fromPos, toPos);

        try {
            session.getBasicRemote().sendText(gson.toJson(moveCommand));
            // Do not draw board here; wait for LOAD_GAME message which will trigger UI update.
        } catch (Exception e) {
            System.err.println("Failed to send move: " + e.getMessage());
        }
    }

    private void promptForMove() {
        if (isObserver) {
            System.out.println("You are an observer. Only 'exit' is allowed.");
            return;
        }
        while (true) {
            System.out.print("\nEnter move (e.g., e2 e4), or a square (e.g., e2) to preview, or 'resign'/'exit': ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("resign")) {
                System.out.println("Resigning game.");
                sendResignCommand();
                return;
            } else if (input.equals("exit")) {
                System.out.println("Exiting game.");
                sendLeaveCommand();
                return;
            } else if (input.matches("^[a-h][1-8]\\s+[a-h][1-8]$")) {
                String[] parts = input.split("\\s+");
                sendMove(parts[0], parts[1]);
                return;
            } else if (input.matches("^[a-h][1-8]$")) {
                ChessPosition from = parsePosition(input);
                Collection<ChessMove> moves = game.validMoves(from);
                if (moves != null && !moves.isEmpty()) {
                    Set<ChessPosition> destinations = moves.stream()
                            .map(ChessMove::getEndPosition)
                            .collect(Collectors.toSet());

                    highlightedFrom = from;
                    highlightedTo = destinations;
                    new ChessBoardUI().drawBoard(board, isObserver || playerColor == ChessGame.TeamColor.WHITE, from, destinations);
                } else {
                    System.out.println("❌ No valid moves from " + input);
                }
            } else {
                System.out.println("Invalid input. Try again.");
            }
        }
    }


    public boolean hasExited() {
        return exited;
    }

    private void handleResignOrLeave(){
        exited = true;
    }

    private void sendResignCommand() {
        UserGameCommand resign = new UserGameCommand(CommandType.RESIGN, authToken, gameID);
        try {
            session.getBasicRemote().sendText(gson.toJson(resign));
            exited = true;
            if (session != null && session.isOpen()) session.close();
        } catch (Exception e) {
            System.err.println("Failed to send RESIGN command: " + e.getMessage());
        }
    }

    private void sendLeaveCommand() {
        UserGameCommand leave = new UserGameCommand(CommandType.LEAVE, authToken, gameID);
        try {
            session.getBasicRemote().sendText(gson.toJson(leave));
            exited = true;
            if (session != null && session.isOpen()) session.close();
        } catch (Exception e) {
            System.err.println("Failed to send LEAVE command: " + e.getMessage());
        }
    }


    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
    }
}
