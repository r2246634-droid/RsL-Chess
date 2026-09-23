package com.chess.pieces;

import com.chess.core.Board;
import com.chess.core.Coordinate;

public class Pawn extends Piece {

    public Pawn(Coordinate position, String color) {
        super("Pawn", position, color);
    }

    @Override
    public java.util.List<Coordinate> calculateMoves(Board board) {
        java.util.List<Coordinate> moves = new java.util.ArrayList<>();
        Coordinate currentPos = getPosition();
        int row = currentPos.row();
        int col = currentPos.col();

        boolean isWhite = "WHITE".equals(getColor());
        // Standart satranç: beyaz altta (satır 7), üste doğru hareket eder (satır azalır)
        int direction = isWhite ? -1 : 1;
        int startRow  = isWhite ? 6 : 1;

        // İleri hamle (kare boşsa)
        Coordinate oneForward = new Coordinate(row + direction, col);
        if (board.isWithinBounds(oneForward) && board.getPiece(oneForward) == null) {
            moves.add(oneForward);

            // Başlangıç konumundan çift hamle
            if (row == startRow) {
                Coordinate twoForward = new Coordinate(row + 2 * direction, col);
                if (board.isWithinBounds(twoForward) && board.getPiece(twoForward) == null) {
                    moves.add(twoForward);
                }
            }
        }

        // Çapraz yakalamalar
        int[] captureCols = {col - 1, col + 1};
        for (int captureCol : captureCols) {
            Coordinate capturePos = new Coordinate(row + direction, captureCol);
            if (board.isWithinBounds(capturePos)) {
                Piece target = board.getPiece(capturePos);
                if (target != null && !target.getColor().equals(getColor())) {
                    moves.add(capturePos);
                }
            }
        }

        return moves;
    }

    @Override
    public Piece copy(Coordinate pos) {
        Pawn p = new Pawn(pos, getColor());
        p.setMoved(isMoved());
        return p;
    }
}
