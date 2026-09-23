package com.chess.pieces;

import com.chess.core.Board;
import com.chess.core.Coordinate;

public class Knight extends Piece {

    public Knight(Coordinate position, String color) {
        super("Knight", position, color);
    }

    @Override
    public java.util.List<Coordinate> calculateMoves(Board board) {
        java.util.List<Coordinate> moves = new java.util.ArrayList<>();
        Coordinate currentPos = getPosition();

        int[][] knightMoves = {
            {2, 1}, {1, 2}, {-1, 2}, {-2, 1},
            {-2, -1}, {-1, -2}, {1, -2}, {2, -1}
        };
        for (int[] move : knightMoves) {
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
        Knight k = new Knight(pos, getColor());
        k.setMoved(isMoved());
        return k;
    }
}
