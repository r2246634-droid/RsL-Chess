package com.chess.ai;

import com.chess.core.Board;
import com.chess.core.Coordinate;
import com.chess.core.Move;

public interface AIPlayer {
    Move chooseMove(Board board, String aiColor);

    /** enPassantTarget: yalnızca kök hamlelerde kullanılır (arama ağacında izlenmez). */
    default Move chooseMove(Board board, String aiColor, Coordinate enPassantTarget) {
        return chooseMove(board, aiColor);
    }
}
