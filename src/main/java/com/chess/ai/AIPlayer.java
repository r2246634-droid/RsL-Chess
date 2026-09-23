package com.chess.ai;

import com.chess.core.Board;
import com.chess.core.Move;

public interface AIPlayer {
    Move chooseMove(Board board, String aiColor);
}
