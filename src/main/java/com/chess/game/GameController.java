package com.chess.game;

import com.chess.ai.AIPlayer;
import com.chess.ai.ChessAI;
import com.chess.core.*;
import com.chess.i18n.I18n;
import com.chess.network.NetworkManager;
import com.chess.pieces.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class GameController {

    private final Board board;
    private final GameHistory history;
    private final GameConfig config;
    private final AIPlayer ai;
    private final GameTimer timer;

    private NetworkManager networkManager;

    private String currentTurn = "WHITE";
    private boolean gameOver   = false;
    private int moveCount      = 0;
    private Coordinate enPassantTarget = null;

    // 50 hamle kuralı: piyon oynanmadan/taş alınmadan geçen yarım hamle sayısı.
    // 100'e (50 tam hamle) ulaşınca oyun berabere biter.
    private int halfmoveClock = 0;

    private final List<String> moveLog = new ArrayList<>();

    private Runnable onBoardChanged;
    private Consumer<String> onGameOver;
    private Consumer<String> onSoundEvent;
    private Consumer<Runnable> uiExecutor = Runnable::run;

    public GameController(GameConfig config) {
        this.config  = config;
        this.board   = new Board();
        this.history = new GameHistory(config.modeLabel());
        this.ai      = createAI(config.mode());
        this.timer   = config.hasTimer()
                ? new GameTimer(config.initialMs(), config.incrementMs())
                : null;
        initBoard();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Board getBoard()          { return board; }
    public String getCurrentTurn()   { return currentTurn; }
    public boolean isGameOver()      { return gameOver; }
    public int getMoveCount()        { return moveCount; }
    public GameConfig getConfig()    { return config; }
    public GameTimer getTimer()      { return timer; }
    public List<String> getMoveLog() { return Collections.unmodifiableList(moveLog); }

    public boolean isAITurn() {
        return ai != null && "BLACK".equals(currentTurn) && !gameOver;
    }

    /** True when it is the network opponent's turn (blocks local clicks). */
    public boolean isRemoteTurn() {
        return networkManager != null
                && !config.localColor().equals(currentTurn)
                && !gameOver;
    }

    public void setOnBoardChanged(Runnable r)       { onBoardChanged = r; }
    public void setOnGameOver(Consumer<String> c)   { onGameOver = c; }
    public void setOnSoundEvent(Consumer<String> c) { onSoundEvent = c; }
    public void setUiExecutor(Consumer<Runnable> e) { uiExecutor = e; }

    // ── Network manager (set after constructor) ───────────────────────────────

    public void setNetworkManager(NetworkManager nm) {
        this.networkManager = nm;
        if (nm == null) return;
        nm.setOnMoveReceived(coords -> {
            String[] p = coords.split(",");
            Coordinate from = new Coordinate(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            Coordinate to   = new Coordinate(Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            executeMove(from, to);   // already on UI thread via uiExecutor set in NetworkManager
        });
        nm.setOnDisconnected(() -> {
            if (!gameOver && onGameOver != null)
                onGameOver.accept(I18n.t("result.disconnected"));
        });
    }

    // ── Timer ────────────────────────────────────────────────────────────────

    public void startTimer() {
        if (timer == null) return;
        timer.setOnWhiteTimeout(() -> uiExecutor.accept(() -> {
            if (!gameOver) endGame(I18n.win(I18n.t("color.black"),
                    I18n.t("result.timeout") + " (" + I18n.t("color.white") + ")"));
        }));
        timer.setOnBlackTimeout(() -> uiExecutor.accept(() -> {
            if (!gameOver) endGame(I18n.win(I18n.t("color.white"),
                    I18n.t("result.timeout") + " (" + I18n.t("color.black") + ")"));
        }));
        timer.start(true);
    }

    // ── Check detection ───────────────────────────────────────────────────────

    public boolean isInCheck(Board b, String color) {
        return ChessRules.isInCheck(b, color);
    }

    public boolean isCurrentPlayerInCheck() {
        return !gameOver && isInCheck(board, currentTurn);
    }

    // ── Legal moves ───────────────────────────────────────────────────────────

    public List<Coordinate> getLegalMoves(Coordinate from) {
        Piece piece = board.getPiece(from);
        if (piece == null) return new ArrayList<>();
        String color = piece.getColor();
        List<Coordinate> legal = new ArrayList<>();
        for (Coordinate to : ChessRules.pseudoLegalMovesWithSpecials(board, from, enPassantTarget)) {
            Board test = board.copy();
            Piece moving = test.getPiece(from);
            test.setPiece(from, null);
            test.setPiece(to, moving);
            if (ChessRules.isEnPassantCapture(moving, from, to, board.getPiece(to))) {
                test.setPiece(new Coordinate(from.row(), to.col()), null);
            }
            if (!isInCheck(test, color)) legal.add(to);
        }
        return legal;
    }

    // ── Execute move ──────────────────────────────────────────────────────────

    public void executeMove(Coordinate from, Coordinate to) {
        if (gameOver || from == null || to == null) return;
        Piece piece    = board.getPiece(from);
        Piece captured = board.getPiece(to);
        if (piece == null) return;

        // Is this move made by the local human player?
        boolean localMove = networkManager != null
                && config.localColor().equals(currentTurn);

        board.setPiece(from, null);
        board.setPiece(to, piece);

        Piece epCaptured = ChessRules.resolveEnPassant(board, piece, from, to, captured);
        ChessRules.finalizeMove(board, piece, from, to);
        boolean isCapture = captured != null || epCaptured != null;

        sound(isCapture ? "capture" : "move");
        moveCount++;

        boolean isPawnMove = "Pawn".equals(piece.getType());
        halfmoveClock = (isPawnMove || isCapture) ? 0 : halfmoveClock + 1;

        // Pawn promotion → Queen
        if (isPawnMove) {
            if (("WHITE".equals(piece.getColor()) && to.row() == 0)
                    || ("BLACK".equals(piece.getColor()) && to.row() == 7))
                board.setPiece(to, new Queen(to, piece.getColor()));
        }

        // Bir sonraki hamle için geçerken alma hakkı (yalnızca çift kare ileri atlayışta doğar)
        enPassantTarget = ("Pawn".equals(piece.getType()) && Math.abs(to.row() - from.row()) == 2)
                ? new Coordinate((from.row() + to.row()) / 2, from.col())
                : null;

        String fromA = GameHistory.toAlgebraic(from.row(), from.col());
        String toA   = GameHistory.toAlgebraic(to.row(), to.col());
        history.record(piece.getColor(), piece.getType(), fromA, toA, isCapture);
        moveLog.add(fromA + "–" + toA);

        if (timer != null) timer.switchTurn();

        // Send move to remote before switching turn
        if (localMove) networkManager.sendMove(from.row(), from.col(), to.row(), to.col());

        currentTurn = "WHITE".equals(currentTurn) ? "BLACK" : "WHITE";
        if (onBoardChanged != null) onBoardChanged.run();

        // Checkmate / stalemate
        if (isInCheck(board, currentTurn)) {
            if (getAllLegalMovesForColor(currentTurn).isEmpty()) {
                sound("checkmate");
                String winner = "WHITE".equals(currentTurn) ? I18n.t("color.black") : I18n.t("color.white");
                endGame(I18n.win(winner, I18n.t("result.checkmate")));
                return;
            }
            sound("check");
        } else if (getAllLegalMovesForColor(currentTurn).isEmpty()) {
            endGame(I18n.t("result.stalemate"));
            return;
        }

        if (halfmoveClock >= 100) {
            endGame(I18n.t("result.fiftyMove"));
            return;
        }

        if (isAITurn()) triggerAIMove();
    }

    // ── Board init ────────────────────────────────────────────────────────────

    private void initBoard() {
        // Black (top, rows 0–1)
        board.setPiece(new Coordinate(0, 0), new Rook(new Coordinate(0, 0),   "BLACK"));
        board.setPiece(new Coordinate(0, 1), new Knight(new Coordinate(0, 1), "BLACK"));
        board.setPiece(new Coordinate(0, 2), new Bishop(new Coordinate(0, 2), "BLACK"));
        board.setPiece(new Coordinate(0, 3), new Queen(new Coordinate(0, 3),  "BLACK"));
        board.setPiece(new Coordinate(0, 4), new King(new Coordinate(0, 4),   "BLACK"));
        board.setPiece(new Coordinate(0, 5), new Bishop(new Coordinate(0, 5), "BLACK"));
        board.setPiece(new Coordinate(0, 6), new Knight(new Coordinate(0, 6), "BLACK"));
        board.setPiece(new Coordinate(0, 7), new Rook(new Coordinate(0, 7),   "BLACK"));
        for (int c = 0; c < 8; c++)
            board.setPiece(new Coordinate(1, c), new Pawn(new Coordinate(1, c), "BLACK"));

        // White (bottom, rows 6–7)
        board.setPiece(new Coordinate(7, 0), new Rook(new Coordinate(7, 0),   "WHITE"));
        board.setPiece(new Coordinate(7, 1), new Knight(new Coordinate(7, 1), "WHITE"));
        board.setPiece(new Coordinate(7, 2), new Bishop(new Coordinate(7, 2), "WHITE"));
        board.setPiece(new Coordinate(7, 3), new Queen(new Coordinate(7, 3),  "WHITE"));
        board.setPiece(new Coordinate(7, 4), new King(new Coordinate(7, 4),   "WHITE"));
        board.setPiece(new Coordinate(7, 5), new Bishop(new Coordinate(7, 5), "WHITE"));
        board.setPiece(new Coordinate(7, 6), new Knight(new Coordinate(7, 6), "WHITE"));
        board.setPiece(new Coordinate(7, 7), new Rook(new Coordinate(7, 7),   "WHITE"));
        for (int c = 0; c < 8; c++)
            board.setPiece(new Coordinate(6, c), new Pawn(new Coordinate(6, c), "WHITE"));
    }

    // ── AI ───────────────────────────────────────────────────────────────────

    private void triggerAIMove() {
        Board snapshot = board.copy();
        Thread t = new Thread(() -> {
            try { Thread.sleep(350); } catch (InterruptedException ignored) {}
            Move move = ai.chooseMove(snapshot, "BLACK");
            if (move != null) uiExecutor.accept(() -> executeMove(move.from(), move.to()));
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<Coordinate> getAllLegalMovesForColor(String color) {
        List<Coordinate> all = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p != null && p.getColor().equals(color))
                    all.addAll(getLegalMoves(new Coordinate(r, c)));
            }
        return all;
    }

    private void endGame(String result) {
        gameOver = true;
        if (timer != null) timer.stop();
        history.save(result);
        if (onGameOver != null) onGameOver.accept(result);
    }

    private void sound(String event) { if (onSoundEvent != null) onSoundEvent.accept(event); }

    private AIPlayer createAI(String mode) {
        switch (mode) {
            case "AI_EASY":   return new ChessAI(1);
            case "AI_MEDIUM": return new ChessAI(3);
            case "AI_EXPERT": return new ChessAI(4);
            default:          return null;
        }
    }
}
