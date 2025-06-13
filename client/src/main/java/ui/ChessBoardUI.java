package ui;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.ChessGame.TeamColor;

import java.util.Set;

/**
 * Pretty, monospaced ASCII rendering of a chessboard.
 * Light squares use ANSI 47 (light grey); dark squares use ANSI 100 (dark grey).
 * Pieces are single‑letter symbols: white in uppercase, black in lowercase.
 * <p>
 * Call {@code drawBoard(board, true)}  to show the board from White / observer perspective
 * (a1 appears bottom‑left).  Call {@code drawBoard(board, false)} for Black perspective
 * (a1 appears top‑right).
 */
public class ChessBoardUI {

    private static final String BG_LIGHT = "\u001B[47m";   // light grey
    private static final String BG_DARK  = "\u001B[100m";  // dark grey
    private static final String RESET    = "\u001B[0m";

    /**
     * @param board
     * @param whitePerspective
     */
    public void drawBoard(ChessBoard board, boolean whitePerspective, ChessPosition highlightedFrom, Set<ChessPosition> highlightedTo) {

        // File labels (flip for Black view)
        System.out.print("    ");
        for (int i = 0; i < 8; i++) {
            char file = (char) ('a' + (whitePerspective ? i : 7 - i));
            System.out.print(" " + file + " ");
        }
        System.out.println();

        for (int row = 0; row < 8; row++) {
            int actualRow   = whitePerspective ? 7 - row : row;
            int displayRank = actualRow + 1;   // rank numbers flip automatically

            System.out.printf(" %2d ", displayRank);


            for (int col = 0; col < 8; col++) {
                int actualCol = whitePerspective ? col : 7 - col;

                ChessPosition pos = new ChessPosition(actualRow + 1, actualCol + 1);
                String bg;
                if (highlightedFrom != null && pos.equals(highlightedFrom)) {
                    bg = "\u001B[43m";
                } else if (highlightedTo != null && highlightedTo.contains(pos)) {
                    bg = "\u001B[42m";
                } else {
                    bg = ((row + col) % 2 == 0) ? BG_LIGHT : BG_DARK;
                }

                ChessPiece piece = board.getPiece(new ChessPosition(actualRow + 1, actualCol + 1));
                String symbol = getSymbol(piece);

                System.out.print(bg + " " + symbol + " " + RESET);
            }

            System.out.printf(" %2d ", displayRank);
            System.out.println();
        }


        System.out.print("    ");
        for (int i = 0; i < 8; i++) {
            char file = (char) ('a' + (whitePerspective ? i : 7 - i));
            System.out.print(" " + file + " ");
        }
        System.out.println();
    }

    private String getSymbol(ChessPiece piece) {
        if (piece == null) {
            return " ";
        }
        return switch (piece.getPieceType()) {
            case KING   -> piece.getTeamColor() == TeamColor.WHITE ? "K" : "k";
            case QUEEN  -> piece.getTeamColor() == TeamColor.WHITE ? "Q" : "q";
            case ROOK   -> piece.getTeamColor() == TeamColor.WHITE ? "R" : "r";
            case BISHOP -> piece.getTeamColor() == TeamColor.WHITE ? "B" : "b";
            case KNIGHT -> piece.getTeamColor() == TeamColor.WHITE ? "N" : "n";
            case PAWN   -> piece.getTeamColor() == TeamColor.WHITE ? "P" : "p";
        };
    }
}