package com.chess.game;

import com.chess.ai.AIPlayer;
import com.chess.ai.ChessAI;
import com.chess.core.*;
import com.chess.i18n.I18n;
import com.chess.network.NetworkManager;
import com.chess.pieces.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // Üçlü tekrar kuralı: pozisyon anahtarı -> kaç kez görüldüğü.
    private final Map<String, Integer> positionCounts = new HashMap<>();

    // Geri alma (undo) için hamle öncesi durum yığını.
    private final Deque<Snapshot> undoStack = new ArrayDeque<>();

    private final List<String> moveLog = new ArrayList<>(); // SAN gösterimi (Nf3, O-O, Qxh7# ...)

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
        positionCounts.put(positionKey(), 1);
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

    public boolean isVsAI()      { return ai != null; }
    public boolean isNetworkGame() { return networkManager != null; }
    public boolean canUndo()     { return !undoStack.isEmpty() && !gameOver; }

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
            String[] p = coords.split(",", -1);
            Coordinate from = new Coordinate(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            Coordinate to   = new Coordinate(Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            String promo = (p.length >= 5 && !p[4].isEmpty()) ? p[4] : null;
            executeMove(from, to, promo);   // already on UI thread via uiExecutor set in NetworkManager
        });
        nm.setOnDisconnected(() -> {
            if (!gameOver && onGameOver != null)
                onGameOver.accept(I18n.t("result.disconnected"));
        });
        nm.setOnResignReceived(() -> {
            if (gameOver) return;
            String winnerName = "WHITE".equals(config.localColor())
                    ? I18n.t("color.white") : I18n.t("color.black");
            endGame(I18n.win(winnerName, I18n.t("result.resignation")));
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
        executeMove(from, to, null);
    }

    /** promotionType: "Queen"|"Rook"|"Bishop"|"Knight", ya da terfi yoksa/otomatikse null (→ Vezir). */
    public void executeMove(Coordinate from, Coordinate to, String promotionType) {
        if (gameOver || from == null || to == null) return;
        Piece piece    = board.getPiece(from);
        Piece captured = board.getPiece(to);
        if (piece == null) return;

        // Is this move made by the local human player?
        boolean localMove = networkManager != null
                && config.localColor().equals(currentTurn);

        boolean isPawnMove = "Pawn".equals(piece.getType());
        boolean isEnPassantMove = ChessRules.isEnPassantCapture(piece, from, to, captured);
        boolean isCapture = captured != null || isEnPassantMove;
        boolean promotes = isPawnMove && (to.row() == 0 || to.row() == 7);
        String resolvedPromotion = promotes ? (promotionType != null ? promotionType : "Queen") : null;

        undoStack.push(new Snapshot(board.copy(), currentTurn, enPassantTarget, halfmoveClock, moveLog, positionCounts));

        // SAN'ın taş türü/belirsizlik-giderme kısmı, tahta değişmeden ÖNCE hesaplanmalı.
        String sanBody = SanNotation.toSan(board, from, to, isCapture, resolvedPromotion);

        board.setPiece(from, null);
        board.setPiece(to, piece);

        Piece epCaptured = ChessRules.resolveEnPassant(board, piece, from, to, captured);
        ChessRules.finalizeMove(board, piece, from, to);

        sound(isCapture ? "capture" : "move");
        moveCount++;
        halfmoveClock = (isPawnMove || isCapture) ? 0 : halfmoveClock + 1;

        if (promotes) {
            board.setPiece(to, createPromotedPiece(resolvedPromotion, to, piece.getColor()));
        }

        // Bir sonraki hamle için geçerken alma hakkı (yalnızca çift kare ileri atlayışta doğar)
        enPassantTarget = (isPawnMove && Math.abs(to.row() - from.row()) == 2)
                ? new Coordinate((from.row() + to.row()) / 2, from.col())
                : null;

        if (timer != null) timer.switchTurn();

        // Send move to remote before switching turn
        if (localMove) networkManager.sendMove(from.row(), from.col(), to.row(), to.col(), resolvedPromotion);

        currentTurn = "WHITE".equals(currentTurn) ? "BLACK" : "WHITE";

        boolean inCheck = isInCheck(board, currentTurn);
        boolean noMoves = getAllLegalMovesForColor(currentTurn).isEmpty();
        String san = sanBody + ((inCheck && noMoves) ? "#" : (inCheck ? "+" : ""));
        moveLog.add(san);

        if (onBoardChanged != null) onBoardChanged.run();

        // Checkmate / stalemate
        if (inCheck) {
            if (noMoves) {
                sound("checkmate");
                String winner = "WHITE".equals(currentTurn) ? I18n.t("color.black") : I18n.t("color.white");
                endGame(I18n.win(winner, I18n.t("result.checkmate")));
                return;
            }
            sound("check");
        } else if (noMoves) {
            endGame(I18n.t("result.stalemate"));
            return;
        }

        // Diğer berabere kuralları (yalnızca oyun devam ediyorsa kontrol edilir)
        int repCount = positionCounts.merge(positionKey(), 1, Integer::sum);
        if (repCount >= 3) {
            endGame(I18n.t("result.repetition"));
            return;
        }
        if (isInsufficientMaterial(board)) {
            endGame(I18n.t("result.insufficientMaterial"));
            return;
        }
        if (halfmoveClock >= 100) {
            endGame(I18n.t("result.fiftyMove"));
            return;
        }

        if (isAITurn()) triggerAIMove();
    }

    // ── Geri alma (undo) ─────────────────────────────────────────────────────

    public void undo() {
        if (!canUndo()) return;
        Snapshot s = undoStack.pop();
        board.copyFrom(s.board);
        currentTurn = s.turn;
        enPassantTarget = s.enPassant;
        halfmoveClock = s.halfmoveClock;
        moveLog.clear();
        moveLog.addAll(s.moveLog);
        positionCounts.clear();
        positionCounts.putAll(s.positionCounts);
        moveCount = Math.max(0, moveCount - 1);
        if (onBoardChanged != null) onBoardChanged.run();
    }

    /** İnsan oyuncunun kendi hamlesini geri alması: AI'a karşı, AI'ın cevabını da birlikte geri alır. */
    public void undoLastMove() {
        if (!canUndo()) return;
        undo();
        if (ai != null && "BLACK".equals(currentTurn) && canUndo()) undo();
    }

    // ── İstifa / beraberlik ──────────────────────────────────────────────────

    public void resign(String resigningColor) {
        if (gameOver) return;
        if (networkManager != null && config.localColor().equals(resigningColor)) {
            networkManager.sendResign();
        }
        String winner = "WHITE".equals(resigningColor) ? "BLACK" : "WHITE";
        String winnerName = "WHITE".equals(winner) ? I18n.t("color.white") : I18n.t("color.black");
        endGame(I18n.win(winnerName, I18n.t("result.resignation")));
    }

    /** Oyunu karşılıklı anlaşmayla berabere biter. Ağ/UI tarafındaki onay akışından SONRA çağrılır. */
    public void agreeDraw() {
        if (gameOver) return;
        endGame(I18n.t("result.drawAgreed"));
    }

    /** Basit materyal karşılaştırması: AI maddi olarak öndeyse teklifi reddeder. */
    public boolean aiAcceptsDraw() {
        if (ai == null) return false;
        return materialValue("BLACK") <= materialValue("WHITE");
    }

    private int materialValue(String color) {
        int total = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p != null && color.equals(p.getColor())) total += simpleValue(p.getType());
            }
        return total;
    }

    private int simpleValue(String type) {
        switch (type) {
            case "Pawn":   return 100;
            case "Knight": return 320;
            case "Bishop": return 330;
            case "Rook":   return 500;
            case "Queen":  return 900;
            default:       return 0;
        }
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

    private Piece createPromotedPiece(String type, Coordinate pos, String color) {
        switch (type) {
            case "Rook":   return new Rook(pos, color);
            case "Bishop": return new Bishop(pos, color);
            case "Knight": return new Knight(pos, color);
            default:       return new Queen(pos, color);
        }
    }

    /** Rok hakkı: Şah/Kale hiç hareket etmemiş VE hâlâ köşesinde duruyor olmalı. */
    private boolean canCastleRight(String color, boolean kingSide) {
        int row = "WHITE".equals(color) ? 7 : 0;
        Piece king = board.getPiece(new Coordinate(row, 4));
        if (!(king instanceof King) || king.isMoved()) return false;
        int rookCol = kingSide ? 7 : 0;
        Piece rook = board.getPiece(new Coordinate(row, rookCol));
        return rook instanceof Rook && !rook.isMoved() && color.equals(rook.getColor());
    }

    /**
     * Üçlü tekrar kuralı için pozisyon anahtarı: taş dizilimi + sıra + rok
     * hakları + geçerken alma imkânı. FIDE'nin "aynı pozisyon" tanımına
     * karşılık gelir (yalnızca kareler değil, haklar da eşleşmeli).
     */
    private String positionKey() {
        StringBuilder sb = new StringBuilder(90);
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p == null) { sb.append('.'); continue; }
                sb.append("WHITE".equals(p.getColor()) ? 'w' : 'b').append(pieceLetter(p.getType()));
            }
        sb.append(currentTurn.charAt(0));
        sb.append(canCastleRight("WHITE", true)  ? 'K' : '-');
        sb.append(canCastleRight("WHITE", false) ? 'Q' : '-');
        sb.append(canCastleRight("BLACK", true)  ? 'k' : '-');
        sb.append(canCastleRight("BLACK", false) ? 'q' : '-');
        sb.append(enPassantTarget != null ? (enPassantTarget.row() + "," + enPassantTarget.col()) : "-");
        return sb.toString();
    }

    private char pieceLetter(String type) {
        switch (type) {
            case "Pawn":   return 'P';
            case "Knight": return 'N';
            case "Bishop": return 'B';
            case "Rook":   return 'R';
            case "Queen":  return 'Q';
            case "King":   return 'K';
            default:       return '?';
        }
    }

    /**
     * Yaygın "ölü pozisyon" (dead position) sadeleştirmesi: K vs K, K+hafif taş
     * vs K, ve aynı renk karede fillerle K+F vs K+F. Piyon/Kale/Vezir varsa ya
     * da iki at gibi teorik-mat-mümkün durumlarda false döner (çoğu satranç
     * platformunun kullandığı standart, tam olmayan ama pratik sadeleştirme).
     */
    private boolean isInsufficientMaterial(Board b) {
        List<Piece> white = new ArrayList<>();
        List<Piece> black = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = b.getPiece(new Coordinate(r, c));
                if (p == null || "King".equals(p.getType())) continue;
                String t = p.getType();
                if ("Pawn".equals(t) || "Rook".equals(t) || "Queen".equals(t)) return false;
                ("WHITE".equals(p.getColor()) ? white : black).add(p);
            }
        }
        if (white.isEmpty() && black.isEmpty()) return true;
        if (white.size() + black.size() == 1) return true;
        if (white.size() == 1 && black.size() == 1
                && "Bishop".equals(white.get(0).getType()) && "Bishop".equals(black.get(0).getType())) {
            Coordinate wp = white.get(0).getPosition(), bp = black.get(0).getPosition();
            return ((wp.row() + wp.col()) % 2) == ((bp.row() + bp.col()) % 2);
        }
        return false;
    }

    private void endGame(String result) {
        gameOver = true;
        if (timer != null) timer.stop();
        history.save(moveLog, result);
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

    /** undo() için hamle-öncesi durum anlık görüntüsü. */
    private static final class Snapshot {
        final Board board;
        final String turn;
        final Coordinate enPassant;
        final int halfmoveClock;
        final List<String> moveLog;
        final Map<String, Integer> positionCounts;

        Snapshot(Board board, String turn, Coordinate enPassant, int halfmoveClock,
                 List<String> moveLog, Map<String, Integer> positionCounts) {
            this.board = board;
            this.turn = turn;
            this.enPassant = enPassant;
            this.halfmoveClock = halfmoveClock;
            this.moveLog = new ArrayList<>(moveLog);
            this.positionCounts = new HashMap<>(positionCounts);
        }
    }
}
