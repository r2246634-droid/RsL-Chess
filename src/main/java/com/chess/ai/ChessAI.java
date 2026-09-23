package com.chess.ai;

import com.chess.core.Board;
import com.chess.core.ChessRules;
import com.chess.core.Coordinate;
import com.chess.core.Move;
import com.chess.pieces.Piece;
import com.chess.pieces.Queen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChessAI implements AIPlayer {

    // Değerlendirme puanlarından (en fazla birkaç bin) çok büyük olmalı; mat
    // tespit edildiğinde derinliğe göre ayarlanarak "en hızlı mat" tercih edilir.
    private static final int MATE_SCORE = 1_000_000;

    private final int maxDepth;

    public ChessAI(int depth) {
        this.maxDepth = depth;
    }

    @Override
    public Move chooseMove(Board board, String aiColor) {
        List<Move> moves = getAllMoves(board, aiColor);
        if (moves.isEmpty()) return null;

        if (maxDepth == 1) Collections.shuffle(moves);

        Move best = null;
        int bestScore = Integer.MIN_VALUE + 1;

        for (Move move : moves) {
            Board next = applyMove(board, move);
            int score = -negamax(next, maxDepth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE, opponent(aiColor));
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return best;
    }

    private int negamax(Board board, int depth, int alpha, int beta, String color) {
        if (depth == 0) return evaluate(board, color);

        List<Move> moves = getAllMoves(board, color);
        if (moves.isEmpty()) {
            // Mat: ağır ceza — kalan derinlik puana eklenerek DAHA HIZLI matlar
            // (kökten daha az hamlede ulaşılanlar) her zaman daha yavaş bir mata
            // tercih edilir. Pat: sıfır (berabere, ne iyi ne kötü).
            return ChessRules.isInCheck(board, color) ? -(MATE_SCORE + depth) : 0;
        }

        int best = Integer.MIN_VALUE + 1;
        for (Move move : moves) {
            Board next = applyMove(board, move);
            int score = -negamax(next, depth - 1, -beta, -alpha, opponent(color));
            best = Math.max(best, score);
            alpha = Math.max(alpha, best);
            if (alpha >= beta) break;
        }
        return best;
    }

    private int evaluate(Board board, String color) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p == null) continue;
                int val = pieceValue(p.getType())
                        + PieceSquareTables.valueAt(p.getType(), p.getColor(), r, c);
                score += p.getColor().equals(color) ? val : -val;
            }
        }
        return score;
    }

    private int pieceValue(String type) {
        switch (type) {
            case "Pawn":   return 100;
            case "Knight": return 320;
            case "Bishop": return 330;
            case "Rook":   return 500;
            case "Queen":  return 900;
            case "King":   return 20000;
            default:       return 0;
        }
    }

    // Şah çeki altında bırakacak hamleleri filtreler
    private List<Move> getAllMoves(Board board, String color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p == null || !p.getColor().equals(color)) continue;
                // AI arama ağacında geçerken alma hakkını izlemiyoruz (nadir ve maliyetli); rok desteklenir.
                for (Coordinate target : ChessRules.pseudoLegalMovesWithSpecials(board, p.getPosition(), null)) {
                    Board next = applyMove(board, new Move(p.getPosition(), target));
                    if (!ChessRules.isInCheck(next, color)) {
                        moves.add(new Move(p.getPosition(), target));
                    }
                }
            }
        }
        return moves;
    }

    private Board applyMove(Board board, Move move) {
        Board next = board.copy();
        Piece piece = next.getPiece(move.from());
        next.setPiece(move.from(), null);
        next.setPiece(move.to(), piece);
        ChessRules.finalizeMove(next, piece, move.from(), move.to());

        if ("Pawn".equals(piece.getType())) {
            boolean promotes = ("WHITE".equals(piece.getColor()) && move.to().row() == 0)
                    || ("BLACK".equals(piece.getColor()) && move.to().row() == 7);
            if (promotes) next.setPiece(move.to(), new Queen(move.to(), piece.getColor()));
        }
        return next;
    }

    private String opponent(String color) {
        return "WHITE".equals(color) ? "BLACK" : "WHITE";
    }
}
