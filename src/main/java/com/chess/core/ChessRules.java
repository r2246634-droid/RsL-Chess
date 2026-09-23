package com.chess.core;

import com.chess.pieces.King;
import com.chess.pieces.Pawn;
import com.chess.pieces.Piece;
import com.chess.pieces.Rook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Şah çeki, rok ve geçerken alma (en passant) kurallarını tek bir yerde toplar.
 * GameController (insan/ağ hamleleri) ve ChessAI (arama ağacı) aynı kuralları kullanır,
 * böylece iki taraf da birbirinden farklı davranmaz.
 */
public final class ChessRules {

    private ChessRules() {}

    public static String opponent(String color) {
        return "WHITE".equals(color) ? "BLACK" : "WHITE";
    }

    public static Coordinate findKing(Board board, String color) {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Coordinate pos = new Coordinate(r, c);
                Piece p = board.getPiece(pos);
                if (p instanceof King && color.equals(p.getColor())) return pos;
            }
        }
        return null;
    }

    public static boolean isSquareAttacked(Board board, Coordinate square, String attackerColor) {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p != null && attackerColor.equals(p.getColor())) {
                    for (Coordinate mv : p.calculateMoves(board)) {
                        if (mv.equals(square)) return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInCheck(Board board, String color) {
        Coordinate kingPos = findKing(board, color);
        return kingPos != null && isSquareAttacked(board, kingPos, opponent(color));
    }

    /**
     * Bir taşın normal hareketlerine (Piece.calculateMoves) rok ve geçerken alma
     * hamlelerini ekler. Şah güvenliği için ayrıca filtrelenmesi gerekir
     * (bkz. GameController.getLegalMoves).
     */
    public static List<Coordinate> pseudoLegalMovesWithSpecials(Board board, Coordinate from, Coordinate enPassantTarget) {
        Piece piece = board.getPiece(from);
        if (piece == null) return Collections.emptyList();

        List<Coordinate> moves = new ArrayList<>(piece.calculateMoves(board));
        if (piece instanceof King) addCastling(board, from, piece.getColor(), moves);
        if (piece instanceof Pawn) addEnPassant(from, piece.getColor(), enPassantTarget, moves);
        return moves;
    }

    private static void addCastling(Board board, Coordinate kingPos, String color, List<Coordinate> moves) {
        Piece king = board.getPiece(kingPos);
        if (king == null || king.isMoved()) return;
        if (isInCheck(board, color)) return; // şah çekindeyken rok yapılamaz

        tryCastle(board, kingPos, color, moves, 7, new int[]{5, 6}, 6); // kısa rok
        tryCastle(board, kingPos, color, moves, 0, new int[]{1, 2, 3}, 2); // uzun rok
    }

    private static void tryCastle(Board board, Coordinate kingPos, String color, List<Coordinate> moves,
                                   int rookCol, int[] emptyCols, int kingDestCol) {
        int row = kingPos.row();
        Piece rook = board.getPiece(new Coordinate(row, rookCol));
        if (!(rook instanceof Rook) || rook.isMoved() || !rook.getColor().equals(color)) return;

        for (int c : emptyCols) {
            if (board.getPiece(new Coordinate(row, c)) != null) return;
        }

        String opp = opponent(color);
        int step = Integer.compare(kingDestCol, kingPos.col());
        for (int c = kingPos.col(); ; c += step) {
            if (isSquareAttacked(board, new Coordinate(row, c), opp)) return;
            if (c == kingDestCol) break;
        }
        moves.add(new Coordinate(row, kingDestCol));
    }

    private static void addEnPassant(Coordinate from, String color, Coordinate enPassantTarget, List<Coordinate> moves) {
        if (enPassantTarget == null) return;
        int dir = "WHITE".equals(color) ? -1 : 1;
        if (from.row() + dir != enPassantTarget.row()) return;
        if (Math.abs(from.col() - enPassantTarget.col()) != 1) return;
        moves.add(enPassantTarget);
    }

    public static boolean isEnPassantCapture(Piece piece, Coordinate from, Coordinate to, Piece capturedAtTo) {
        return piece instanceof Pawn && from.col() != to.col() && capturedAtTo == null;
    }

    /** Hamle bir "geçerken alma" ise, alınan piyonu tahtadan kaldırır ve onu döndürür. */
    public static Piece resolveEnPassant(Board board, Piece piece, Coordinate from, Coordinate to, Piece capturedAtTo) {
        if (!isEnPassantCapture(piece, from, to, capturedAtTo)) return null;
        Coordinate epSquare = new Coordinate(from.row(), to.col());
        Piece captured = board.getPiece(epSquare);
        if (captured != null) board.setPiece(epSquare, null);
        return captured;
    }

    /**
     * Bir hamle uygulandıktan sonra çağrılır: taşı "hareket etti" olarak işaretler
     * ve rok hamlesiyse kaleyi de karşı tarafa taşır.
     */
    public static void finalizeMove(Board board, Piece piece, Coordinate from, Coordinate to) {
        piece.setMoved(true);
        if (piece instanceof King && Math.abs(to.col() - from.col()) == 2) {
            int row = from.row();
            boolean kingSide = to.col() > from.col();
            Coordinate rookFrom = new Coordinate(row, kingSide ? 7 : 0);
            Coordinate rookTo   = new Coordinate(row, kingSide ? 5 : 3);
            Piece rook = board.getPiece(rookFrom);
            if (rook != null) {
                board.setPiece(rookFrom, null);
                board.setPiece(rookTo, rook);
                rook.setMoved(true);
            }
        }
    }
}
