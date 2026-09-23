package com.chess.pieces;

import com.chess.core.Board;
import com.chess.core.Coordinate;

public class Bishop extends Piece {

    public Bishop(Coordinate position, String color) {
        super("Bishop", position, color);
    }

    @Override
    public java.util.List<Coordinate> calculateMoves(Board board) {
        java.util.List<Coordinate> moves = new java.util.ArrayList<>();
        Coordinate currentPos = getPosition();

        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
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
        Bishop b = new Bishop(pos, getColor());
        b.setMoved(isMoved());
        return b;
    }
}
