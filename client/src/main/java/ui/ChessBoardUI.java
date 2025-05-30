

package ui;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPiece.PieceType;
import chess.ChessPosition;
import chess.ChessGame.TeamColor;

public class ChessBoardUI {

    public void drawBoard(ChessBoard board, boolean whitePerspective) {
        for (int row = 0; row < 8; row++) {
            int actualRow = whitePerspective ? 7 - row : row;

            // Print row number
            System.out.print((whitePerspective ? actualRow + 1 : 8 - actualRow) + " ");

            for (int col = 0; col < 8; col++) {
                int actualCol = whitePerspective ? col : 7 - col;
                String bgColor = ((row + col) % 2 == 0)
                        ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY
                        : EscapeSequences.SET_BG_COLOR_DARK_GREY;

                ChessPiece piece = board.getPiece(new ChessPosition(actualRow + 1, actualCol + 1));
                String symbol = getSymbol(piece);

                System.out.print(bgColor + symbol + EscapeSequences.RESET_BG_COLOR);
            }

            System.out.println();
        }

        // Print column letters
        System.out.print("  ");
        for (int col = 0; col < 8; col++) {
            char label = whitePerspective ? (char) ('a' + col) : (char) ('h' - col);
            System.out.print(" " + label + " ");
        }

        System.out.println();
    }

    private String getSymbol(ChessPiece piece) {
        if (piece == null) return EscapeSequences.EMPTY;

        return switch (piece.getPieceType()) {
            case KING ->
                    (piece.getTeamColor() == TeamColor.WHITE) ? EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING;
            case QUEEN ->
                    (piece.getTeamColor() == TeamColor.WHITE) ? EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN;
            case BISHOP ->
                    (piece.getTeamColor() == TeamColor.WHITE) ? EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP;
            case KNIGHT ->
                    (piece.getTeamColor() == TeamColor.WHITE) ? EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT;
            case ROOK ->
                    (piece.getTeamColor() == TeamColor.WHITE) ? EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK;
            case PAWN ->
                    (piece.getTeamColor() == TeamColor.WHITE) ? EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN;

        };
    }
}