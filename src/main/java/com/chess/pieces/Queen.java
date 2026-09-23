package com.chess.pieces;

import com.chess.core.Board;
import com.chess.core.Coordinate;

public class Queen extends Piece {

    public Queen(Coordinate position, String color) {
        super("Queen", position, color);
    }

    @Override
    public java.util.List<Coordinate> calculateMoves(Board board) {
        java.util.List<Coordinate> moves = new java.util.ArrayList<>();
        Coordinate currentPos = getPosition();

        // Kale + Fil yönleri birleşik
        int[][] directions = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (int[] dir : directions) {
            int row = currentPos.row() + dir[0];
            int col = currentPos.col() + dir[1];
            while (true) {
                Coordinate pos = new Coordinate(row, col);
                if (!board.isWithinBounds(pos)) break;
                Piece target = board.getPiece(pos);
                if (target != null) {
                    if (!target.getColor().equals(getColor())) moves.add(pos);
                    break;
                }
                moves.add(pos);
                row += dir[0];
                col += dir[1];
            }
        }
        return moves;
    }

    @Override
    public Piece copy(Coordinate pos) {
        Queen q = new Queen(pos, getColor());
        q.setMoved(isMoved());
        return q;
    }
}
