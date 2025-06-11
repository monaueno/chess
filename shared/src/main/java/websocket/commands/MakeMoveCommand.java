package websocket.commands;

import chess.ChessMove;
import chess.ChessPosition;

public class MakeMoveCommand extends UserGameCommand {
    private ChessMove move;

    public MakeMoveCommand(String authToken, int gameID, ChessPosition from, ChessPosition to) {
        super(CommandType.MAKE_MOVE, authToken, gameID);
        this.move = new ChessMove(from, to, null);
    }

    public ChessMove getMove() {
        return move;
    }

    public void setMove(ChessMove move) {
        this.move = move;
    }
}