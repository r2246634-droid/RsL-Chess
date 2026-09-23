package com.chess.pieces;

import com.chess.core.Board;
import com.chess.core.Coordinate;

public class King extends Piece {

    public King(Coordinate position, String color) {
        super("King", position, color);
    }

    @Override
    public java.util.List<Coordinate> calculateMoves(Board board) {
        java.util.List<Coordinate> moves = new java.util.ArrayList<>();
        Coordinate currentPos = getPosition();

        int[][] kingMoves = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (int[] move : kingMoves) {
            int newRow = currentPos.row() + move[0];
            int newCol = currentPos.col() + move[1];
            Coordinate newPos = new Coordinate(newRow, newCol);
            if (board.isWithinBounds(newPos)) {
                Piece target = board.getPiece(newPos);
                if (target == null || !target.getColor().equals(getColor())) {
                    moves.add(newPos);
                }
            }
        }
        return moves;
    }

    @Override
    public Piece copy(Coordinate pos) {
        King k = new King(pos, getColor());
        k.setMoved(isMoved());
        return k;
    }
}
