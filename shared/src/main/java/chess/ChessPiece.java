package chess;

import java.util.Collection;
import java.util.ArrayList;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final ChessPiece.PieceType pieceType;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.pieceType = type;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return this.pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return this.pieceType;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        if (this.pieceType == PieceType.KING) {
            int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, -1}, {-1, 1}, {1, -1}};
            addMoves(directions, board, myPosition, moves);
        }
        else if (this.pieceType == PieceType.BISHOP){
            int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            addSlidingMoves(directions, board, myPosition, moves);
        }
        else if (this.pieceType == PieceType.KNIGHT) {
            int[][] knightMoves = {{2,1},{1,2},{2,-1},{1,-2},{-2,1},{-1,2},{-2,-1},{-1,-2}};
            addJumpMoves(knightMoves, board, myPosition, moves);
        }
        else if (this.pieceType == PieceType.PAWN) {
            addPawnMoves(board, myPosition, moves);
        }
        return moves;
    }

    private void addSlidingMoves(int[][] directions, ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves) {
        for (int[] dir : directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();
            while (true) {
                row += dir[0];
                col += dir[1];
                if (row < 1 || row > 8 || col < 1 || col > 8) break;
                ChessPosition newPos = new ChessPosition(row, col);
                ChessPiece target = board.getPiece(newPos);
                if (target == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (target.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
        }
    }
    private void addMoves(int[][] directions, ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves) {
        for (int[] dir : directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();
                row += dir[0];
                col += dir[1];
                if (row < 1 || row > 8 || col < 1 || col > 8) continue;
                ChessPosition newPos = new ChessPosition(row, col);
                ChessPiece target = board.getPiece(newPos);
                if (target == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (target.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }

            }
        }
    }
    private void addJumpMoves(int[][] knightMoves, ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves) {
        for (int[] move : knightMoves) {
            int row = myPosition.getRow() + move[0];
            int col = myPosition.getColumn() + move[1];

            if (row < 1 || row > 8 || col < 1 || col > 8) continue;
                ChessPosition newPos = new ChessPosition(row,col);
                ChessPiece target = board.getPiece(newPos);
                if (target == null || target.getTeamColor() != this.getTeamColor()) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                }
        }
    }

    private void addPawnMoves(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves) {
        int direction = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        ChessPosition oneAhead = new ChessPosition(row + direction, col);
        if (row + direction >= 1 && row + direction <= 8 && board.getPiece(oneAhead) == null) {
            moves.add(new ChessMove(myPosition, oneAhead, null));
        }

        if (((this.pieceColor == ChessGame.TeamColor.WHITE && row == 2) || (this.pieceColor == ChessGame.TeamColor.BLACK && row == 7)) && board.getPiece(oneAhead) == null) {
            ChessPosition twoAhead = new ChessPosition(row + 2 * direction, col);
            if (board.getPiece(twoAhead) == null) {
                moves.add(new ChessMove(myPosition, twoAhead, null));
            }
        }

        for (int dc = -1; dc <= 1; dc += 2) {
            int newCol = col + dc;
            int newRow = row + direction;
            if (newCol >= 1 && newCol <= 8 && newRow >= 1 && newCol <= 8) {
                ChessPosition diag = new ChessPosition(newRow, newCol);
                ChessPiece target = board.getPiece(diag);
                if (target != null && target.getTeamColor() != this.getTeamColor()) {
                    moves.add(new ChessMove(myPosition, diag, null));
                }
            }
        }
    }
}
