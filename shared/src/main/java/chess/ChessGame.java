package chess;

import chess.ChessMove;
import chess.ChessPosition;
import java.util.Collection;
import java.util.Objects;
import java.util.ArrayList;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor currentTurn;
    private ChessMove lastMove;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.currentTurn = TeamColor.WHITE;

    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return this.currentTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.currentTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        Collection<ChessMove> validMoves = new ArrayList<>();
        Collection<ChessMove> allMoves = piece.pieceMoves(board, startPosition);

        for (ChessMove move : allMoves) {
            ChessBoard copy = new ChessBoard(board);
            ChessPosition start = move.getStartPosition();
            ChessPosition end = move.getEndPosition();
            ChessPiece movingPiece = copy.getPiece(start);
            if (movingPiece == null) {
                continue;
            }
            copy.addPiece(end, movingPiece);
            copy.removePiece(start);

            ChessGame simulatedGame = new ChessGame();
            simulatedGame.setBoard(copy);

            if (!simulatedGame.isInCheck(piece.getTeamColor())) {
                validMoves.add(move);
            }
        }

        addCastlingMoves(piece, startPosition, validMoves);

        addEnPassantMoves(piece, startPosition, validMoves);

        return validMoves;
    }

    private void addCastlingMoves(ChessPiece piece, ChessPosition startPosition, Collection<ChessMove> validMoves) {
        if (piece.getPieceType() != ChessPiece.PieceType.KING || piece.hasMoved() || isInCheck(piece.getTeamColor())) {
            return;
        }
        int row = startPosition.getRow();
        // King-side castling
        ChessPiece kingSideRook = board.getPiece(new ChessPosition(row, 8));
        if (kingSideRook != null && kingSideRook.getPieceType() == ChessPiece.PieceType.ROOK &&
                kingSideRook.getTeamColor() == piece.getTeamColor() && !kingSideRook.hasMoved()) {
            if (board.getPiece(new ChessPosition(row, 6)) == null &&
                    board.getPiece(new ChessPosition(row, 7)) == null) {
                if (canCastleThrough(piece, startPosition, new int[]{6, 7})) {
                    validMoves.add(new ChessMove(startPosition, new ChessPosition(row, 7), null));
                }
            }
        }
        // Queen-side castling
        ChessPiece queenSideRook = board.getPiece(new ChessPosition(row, 1));
        if (queenSideRook != null && queenSideRook.getPieceType() == ChessPiece.PieceType.ROOK &&
                queenSideRook.getTeamColor() == piece.getTeamColor() && !queenSideRook.hasMoved()) {
            if (board.getPiece(new ChessPosition(row, 2)) == null &&
                    board.getPiece(new ChessPosition(row, 3)) == null &&
                    board.getPiece(new ChessPosition(row, 4)) == null) {
                if (canCastleThrough(piece, startPosition, new int[]{4, 3})) {
                    validMoves.add(new ChessMove(startPosition, new ChessPosition(row, 3), null));
                }
            }
        }
    }

    private boolean canCastleThrough(ChessPiece king, ChessPosition startPosition, int[] cols) {
        for (int col : cols) {
            ChessBoard copy = new ChessBoard(board);
            copy.addPiece(new ChessPosition(startPosition.getRow(), col), king);
            copy.removePiece(startPosition);
            ChessGame tempGame = new ChessGame();
            tempGame.setBoard(copy);
            if (tempGame.isInCheck(king.getTeamColor())) {
                return false;
            }
        }
        return true;
    }

    private void addEnPassantMoves(ChessPiece piece, ChessPosition startPosition, Collection<ChessMove> validMoves) {
        if (piece.getPieceType() != ChessPiece.PieceType.PAWN || lastMove == null) return;

        ChessPosition lastStart = lastMove.getStartPosition();
        ChessPosition lastEnd = lastMove.getEndPosition();
        ChessPiece lastPiece = board.getPiece(lastEnd);

        if (lastPiece != null &&
            lastPiece.getPieceType() == ChessPiece.PieceType.PAWN &&
            Math.abs(lastStart.getRow() - lastEnd.getRow()) == 2 &&
            lastEnd.getRow() == startPosition.getRow() &&
            Math.abs(lastEnd.getColumn() - startPosition.getColumn()) == 1) {

            int direction = (piece.getTeamColor() == TeamColor.WHITE) ? 1 : -1;
            ChessPosition enPassantTarget = new ChessPosition(lastEnd.getRow() + direction, lastEnd.getColumn());
            ChessMove enPassantMove = new ChessMove(startPosition, enPassantTarget, null);

            ChessBoard copy = new ChessBoard(board);
            copy.addPiece(enPassantTarget, piece);
            copy.removePiece(startPosition);
            copy.removePiece(lastEnd); // simulate capture

            ChessGame tempGame = new ChessGame();
            tempGame.setBoard(copy);

            if (!tempGame.isInCheck(piece.getTeamColor())) {
                validMoves.add(enPassantMove);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return Objects.equals(board, chessGame.board) && currentTurn == chessGame.currentTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, currentTurn);
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        ChessPiece movingPiece = board.getPiece(start);

        if (movingPiece == null) {
            throw new InvalidMoveException("No piece at start position.");
        }

        if (movingPiece.getTeamColor() != currentTurn) {
            throw new InvalidMoveException("It's not your turn.");
        }

        Collection<ChessMove> legalMoves = validMoves(start);

        if (legalMoves == null || !legalMoves.contains(move)) {
            throw new InvalidMoveException("Invalid move.");
        }

        // Handle en passant capture
        if (movingPiece.getPieceType() == ChessPiece.PieceType.PAWN && board.getPiece(end) == null && start.getColumn() != end.getColumn()) {
            int direction = (movingPiece.getTeamColor() == TeamColor.WHITE) ? -1 : 1;
            ChessPosition capturedPawnPos = new ChessPosition(end.getRow() + direction, end.getColumn());
            board.removePiece(capturedPawnPos);
        }

        if (move.getPromotionPiece() != null) {
            board.addPiece(end, new ChessPiece(movingPiece.getTeamColor(), move.getPromotionPiece()));
        } else {
            board.addPiece(end, movingPiece);
        }

        movingPiece.setHasMoved(true);

        if(movingPiece.getPieceType() == ChessPiece.PieceType.KING){
            int startCol = start.getColumn();
            int endCol = end.getColumn();
            int row = start.getRow();

            if(startCol == 5 && endCol == 7){
                ChessPiece rook = board.getPiece(new ChessPosition(row, 8));
                board.removePiece(new ChessPosition(row, 8));
                board.addPiece(new ChessPosition(row, 6), rook);
                if(rook != null) {
                    rook.setHasMoved(true);
                }
            }
            if(startCol == 5 && endCol == 3){
                ChessPiece rook = board.getPiece(new ChessPosition(row, 1));
                board.removePiece(new ChessPosition(row, 1));
                board.addPiece(new ChessPosition(row, 4), rook);
                if(rook != null) {
                    rook.setHasMoved(true);
                }
            }
        }

        board.removePiece(start);

        // Set lastMove before switching turn
        this.lastMove = move;

        currentTurn = (currentTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPos = null;

        for(int row = 1; row <= 8; row++){
            for(int col = 1; col <= 8; col++){
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                if(piece != null && piece.getPieceType() == ChessPiece.PieceType.KING && piece.getTeamColor() == teamColor){
                    kingPos = new ChessPosition(row, col);
                    break;
                }
            }
        }
        if(kingPos == null) {
            throw new IllegalStateException("No king found for team: " + teamColor);
        }

        for(int row = 1; row <= 8; row++){
            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                if (piece != null && piece.getTeamColor() != teamColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, new ChessPosition(row, col));
                    for (ChessMove move : moves) {
                        if (move.getEndPosition().equals(kingPos)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if(!isInCheck(teamColor)){
            return false;
        }
        for(int row = 1; row <= 8; row++){
            for(int col = 1; col <= 8; col++){
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);

                if (piece != null && piece.getTeamColor() == teamColor && hasValidMoves(piece, pos)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasValidMoves(ChessPiece piece, ChessPosition pos) {
        if (piece != null) {
            Collection<ChessMove> moves = validMoves(pos);
            return moves != null && !moves.isEmpty();
        }
        return false;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)){
            return false;
        }

        for(int row = 1; row <= 8; row++){
            for(int col = 1; col <= 8; col++){
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if(piece != null && piece.getTeamColor() == teamColor){
                    Collection<ChessMove> moves = validMoves(position);

                    if(moves!=null && !moves.isEmpty()){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return this.board;
    }
}
