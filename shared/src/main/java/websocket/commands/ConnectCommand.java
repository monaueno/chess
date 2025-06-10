package websocket.commands;

import chess.ChessMove;
import chess.ChessGame;

public class ConnectCommand extends UserGameCommand {
    private ChessMove move;
    private ChessGame.TeamColor playerColor;

    public ChessMove getMove() {
        return move;
    }

    public void seMove(ChessMove move) {
        this.move = move;
    }

    public ChessGame.TeamColor getPlayerColor(){
        return playerColor;
    }

    public void setPlayerColor(ChessGame.TeamColor playerColor) {
        this.playerColor = playerColor;
    }
}