package com.chess.ui;

import com.chess.core.Board;
import com.chess.core.Coordinate;
import com.chess.game.GameController;
import com.chess.pieces.Piece;
import com.chess.sound.SoundEngine;
import com.chess.theme.BoardTheme;
import com.chess.theme.ThemeManager;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.util.Duration;
import javafx.animation.Interpolator;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kendi çerçevesi ve koordinat etiketleriyle tam bir satranç tahtası bileşeni.
 * GridPane'i Pane içine sararak mutlak konumlandırma kullanır.
 */
public class ChessBoardUI extends Pane {

    // ── Boyutlar ─────────────────────────────────────────────────────────────
    static final int CELL     = 84;   // kare genişliği/yüksekliği (px)
    static final int LABEL_W  = 24;   // rank etiket sütunu genişliği
    static final int LABEL_H  = 24;   // file etiket satırı yüksekliği
    static final int FRAME    = 14;   // dış çerçeve dolgusu
    static final int BOARD_X  = FRAME + LABEL_W;
    static final int BOARD_Y  = FRAME;
    static final int TOTAL_W  = FRAME * 2 + LABEL_W + CELL * 8;
    static final int TOTAL_H  = FRAME * 2 + LABEL_H + CELL * 8;

    private final GameController controller;
    private final GridPane        grid    = new GridPane();
    private final StackPane[][]   cells   = new StackPane[8][8];

    // Taşlar, hücre hiyerarşisinin dışında, tek bir üst katmanda mutlak konumlanır.
    // Bu sayede bir taş komşu hücrelerin üzerinden kayarken diğer karelerin arkasında kalmaz.
    private final Pane pieceLayer = new Pane();
    // Kimlik-tabanlı takip: Board.setPiece() bir hamlede aynı Piece referansını taşır
    // (yalnızca terfi/yakalama yeni/kaybolan referans üretir), bu yüzden hangi taşın
    // nereden nereye gittiğini nesne kimliğiyle belirleyebiliyoruz.
    private final Map<Piece, StackPane>   pieceNodes  = new IdentityHashMap<>();
    private final Map<Piece, Coordinate>  pieceCoords = new IdentityHashMap<>();

    private Coordinate       selected   = null;
    private List<Coordinate> legalMoves = new ArrayList<>();
    private Runnable         onMoveCompleted;

    // ── Constructor ──────────────────────────────────────────────────────────

    public ChessBoardUI(GameController controller) {
        this.controller = controller;
        grid.setHgap(0);
        grid.setVgap(0);
        buildCells();
        getChildren().add(grid);
        buildDecorations();

        pieceLayer.relocate(BOARD_X, BOARD_Y);
        pieceLayer.setPrefSize(CELL * 8, CELL * 8);
        pieceLayer.setMouseTransparent(true);
        getChildren().add(pieceLayer);

        controller.setUiExecutor(javafx.application.Platform::runLater);
        controller.setOnSoundEvent(ChessBoardUI::dispatchSound);
        controller.setOnBoardChanged(() -> {
            refreshPieces();
            if (onMoveCompleted != null) onMoveCompleted.run();
        });

        refreshPieces();
        setPrefSize(TOTAL_W, TOTAL_H);
    }

    public void setOnMoveCompleted(Runnable r) { onMoveCompleted = r; }

    // ── Tema yenileme (dışarıdan çağrılır) ───────────────────────────────────

    public void refreshTheme() {
        // Çerçeve + etiketleri kaldır (grid ve taş katmanı kalır)
        getChildren().removeIf(n -> n != grid && n != pieceLayer);
        buildDecorations();

        // Hücre arkaplanlarını güncelle
        BoardTheme t = ThemeManager.get().current();
        boolean isNeon = t.pieceStyle() == BoardTheme.PieceStyle.NEON;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                StackPane cell = cells[r][c];
                cell.getChildren().removeIf(n ->
                    "sel-overlay".equals(n.getId()) ||
                    "hint".equals(n.getId()) ||
                    "check-ring".equals(n.getId()) ||
                    "neon-border".equals(n.getId()));
                setBg(cell, baseFill(r, c));
                if (isNeon) addNeonBorder(cell);
            }
        }
        selected   = null;
        legalMoves = new ArrayList<>();

        // Taşları yeni temanın renk/efektleriyle sıfırdan çiz
        pieceLayer.getChildren().clear();
        pieceNodes.clear();
        pieceCoords.clear();
        refreshPieces();
    }

    // ── Dekorasyon katmanı ───────────────────────────────────────────────────

    private void buildDecorations() {
        BoardTheme t = ThemeManager.get().current();

        // Arka plan çerçevesi
        Rectangle frame = new Rectangle(TOTAL_W, TOTAL_H);
        frame.setFill(t.panelColor());
        frame.setId("frame-bg");

        // İnce iç kenarlık
        Rectangle innerBorder = new Rectangle(CELL * 8 + 6, CELL * 8 + 6);
        innerBorder.setFill(Color.TRANSPARENT);
        innerBorder.setStroke(t.panelBorderColor().deriveColor(0, 1, 1, 0.55));
        innerBorder.setStrokeWidth(1.5);
        innerBorder.setId("frame-bg");
        innerBorder.relocate(BOARD_X - 3, BOARD_Y - 3);

        // Grid konumu
        grid.relocate(BOARD_X, BOARD_Y);

        // Rank etiketleri (8..1) sol taraf
        for (int r = 0; r < 8; r++) {
            Label l = coordLabel(String.valueOf(8 - r), t);
            l.setPrefWidth(LABEL_W - 4);
            l.setAlignment(Pos.CENTER_RIGHT);
            l.relocate(FRAME, BOARD_Y + r * CELL + (CELL - 16) / 2.0);
            l.setId("coord-label");
            getChildren().add(l);
        }

        // File etiketleri (a..h) alt kısım
        for (int c = 0; c < 8; c++) {
            Label l = coordLabel(String.valueOf((char) ('a' + c)), t);
            l.setPrefWidth(CELL);
            l.setAlignment(Pos.CENTER);
            l.relocate(BOARD_X + c * CELL, BOARD_Y + CELL * 8 + 5);
            l.setId("coord-label");
            getChildren().add(l);
        }

        // Rank etiketleri (8..1) sağ taraf (opsiyonel, geleneksel)
        for (int r = 0; r < 8; r++) {
            Label l = coordLabel(String.valueOf(8 - r), t);
            l.setPrefWidth(LABEL_W - 4);
            l.setAlignment(Pos.CENTER_LEFT);
            l.relocate(BOARD_X + CELL * 8 + 4, BOARD_Y + r * CELL + (CELL - 16) / 2.0);
            l.setId("coord-label");
            getChildren().add(l);
        }

        // Z-sıralama: çerçeve en altta, sonra grid, sonra etiketler
        getChildren().add(0, innerBorder);
        getChildren().add(0, frame);

        // Gölge efekti
        setEffect(new DropShadow(32, 0, 10, Color.web("#000000", 0.75)));
    }

    // ── Hücre oluşturma ─────────────────────────────────────────────────────

    private void buildCells() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                cells[r][c] = createCell(r, c);
                grid.add(cells[r][c], c, r);
            }
    }

    private StackPane createCell(int row, int col) {
        StackPane cell = new StackPane();
        cell.setPrefSize(CELL, CELL);

        Rectangle bg = new Rectangle(CELL, CELL);
        bg.setFill(baseFill(row, col));
        cell.getChildren().add(bg);

        final int r = row, c = col;
        cell.setOnMouseClicked(e -> handleClick(new Coordinate(r, c)));
        cell.setOnMouseEntered(e -> {
            Coordinate coord = new Coordinate(r, c);
            if (!coord.equals(selected) && !legalMoves.contains(coord))
                bg.setFill(baseFill(r, c).deriveColor(0, 1, 0.84, 1));
        });
        cell.setOnMouseExited(e -> {
            Coordinate coord = new Coordinate(r, c);
            if (!coord.equals(selected) && !legalMoves.contains(coord))
                bg.setFill(baseFill(r, c));
        });
        return cell;
    }

    // Neon tema için ince ızgara kenarlığı
    private void addNeonBorder(StackPane cell) {
        Rectangle nb = new Rectangle(CELL, CELL);
        nb.setFill(Color.TRANSPARENT);
        nb.setStroke(Color.web("#1e3060", 0.55));
        nb.setStrokeWidth(0.75);
        nb.setId("neon-border");
        nb.setMouseTransparent(true);
        cell.getChildren().add(1, nb);
    }

    // ── Taşları yenile ──────────────────────────────────────────────────────

    /**
     * Tahtayı mevcut Board durumuyla senkronize eder. Yalnızca gerçekten değişen
     * taşlar animasyonlanır: aynı Piece nesnesi hâlâ tahtadaysa ama karesi
     * değiştiyse kayar (TranslateTransition); tahtadan tamamen kaybolduysa
     * (yakalandı / terfi ile değiştirildi) solarak kaybolur; yeni beliren bir
     * taş (ilk kurulum, terfi sonucu) belirme animasyonuyla eklenir.
     */
    void refreshPieces() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                cells[r][c].getChildren().removeIf(n -> "check-ring".equals(n.getId()));

        Board board = controller.getBoard();
        Map<Piece, Coordinate> newPos = new IdentityHashMap<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p != null) newPos.put(p, new Coordinate(r, c));
            }
        }

        // Artık tahtada olmayan taşları sil (yakalandı veya terfiyle değiştirildi)
        List<Piece> vanished = new ArrayList<>();
        for (Piece p : pieceNodes.keySet()) {
            if (!newPos.containsKey(p)) vanished.add(p);
        }
        for (Piece p : vanished) {
            fadeOutAndRemove(pieceNodes.remove(p));
            pieceCoords.remove(p);
        }

        // Mevcut taşları kaydır, yeni taşları belirt
        for (Map.Entry<Piece, Coordinate> e : newPos.entrySet()) {
            Piece piece = e.getKey();
            Coordinate to = e.getValue();
            StackPane node = pieceNodes.get(piece);
            if (node == null) {
                node = createPieceNode(piece);
                node.relocate(to.col() * CELL, to.row() * CELL);
                pieceLayer.getChildren().add(node);
                pieceNodes.put(piece, node);
                pieceCoords.put(piece, to);
                popIn(node);
            } else {
                Coordinate from = pieceCoords.get(piece);
                if (!to.equals(from)) animateMove(node, piece, from, to);
            }
        }

        updateCheckHighlight();
    }

    private StackPane createPieceNode(Piece piece) {
        StackPane node = new StackPane(makePieceText(piece));
        node.setPrefSize(CELL, CELL);
        node.setMouseTransparent(true);
        node.setScaleX(0.0);
        node.setScaleY(0.0);
        return node;
    }

    private void popIn(StackPane node) {
        ScaleTransition appear = new ScaleTransition(Duration.millis(200), node);
        appear.setToX(1.0);
        appear.setToY(1.0);
        appear.setInterpolator(Interpolator.SPLINE(0.34, 1.56, 0.64, 1.0));
        appear.play();
    }

    private void animateMove(StackPane node, Piece piece, Coordinate from, Coordinate to) {
        double dx = (to.col() - from.col()) * CELL;
        double dy = (to.row() - from.row()) * CELL;
        node.toFront();

        TranslateTransition slide = new TranslateTransition(Duration.millis(230), node);
        slide.setByX(dx);
        slide.setByY(dy);
        slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
        slide.setOnFinished(e -> {
            node.relocate(to.col() * CELL, to.row() * CELL);
            node.setTranslateX(0);
            node.setTranslateY(0);
        });
        slide.play();
        pieceCoords.put(piece, to);
    }

    private void fadeOutAndRemove(StackPane node) {
        if (node == null) return;
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setToValue(0);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), node);
        shrink.setToX(0.55);
        shrink.setToY(0.55);
        ParallelTransition vanish = new ParallelTransition(fade, shrink);
        vanish.setOnFinished(e -> pieceLayer.getChildren().remove(node));
        vanish.play();
    }

    // ── Şah vurgusu ─────────────────────────────────────────────────────────

    private void updateCheckHighlight() {
        if (!controller.isCurrentPlayerInCheck()) return;
        Board board = controller.getBoard();
        String turn = controller.getCurrentTurn();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(new Coordinate(r, c));
                if (p != null && "King".equals(p.getType()) && p.getColor().equals(turn)) {
                    Color ringColor = ThemeManager.get().current().checkRingColor();

                    // Dış büyük halka
                    Circle outerRing = new Circle(CELL / 2.0 - 3);
                    outerRing.setFill(ringColor.deriveColor(0, 1, 1, 0.12));
                    outerRing.setStroke(ringColor);
                    outerRing.setStrokeWidth(5.0);
                    outerRing.setId("check-ring");
                    outerRing.setMouseTransparent(true);
                    outerRing.setEffect(new DropShadow(12, ringColor));

                    FadeTransition fade = new FadeTransition(Duration.millis(420), outerRing);
                    fade.setFromValue(1.0);
                    fade.setToValue(0.12);
                    fade.setCycleCount(Animation.INDEFINITE);
                    fade.setAutoReverse(true);
                    fade.play();

                    cells[r][c].getChildren().add(outerRing);
                    return;
                }
            }
        }
    }

    // ── Taş sembolü ve efekti ────────────────────────────────────────────────

    private Text makePieceText(Piece piece) {
        boolean white = "WHITE".equals(piece.getColor());
        String sym;
        switch (piece.getType()) {
            case "King":   sym = white ? "♔" : "♚"; break;
            case "Queen":  sym = white ? "♕" : "♛"; break;
            case "Rook":   sym = white ? "♖" : "♜"; break;
            case "Bishop": sym = white ? "♗" : "♝"; break;
            case "Knight": sym = white ? "♘" : "♞"; break;
            case "Pawn":   sym = white ? "♙" : "♟"; break;
            default:       sym = "?";
        }

        BoardTheme theme = ThemeManager.get().current();
        Text t = new Text(sym);
        t.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 52));
        t.setFill(white ? theme.whitePieceColor() : theme.blackPieceColor());
        t.setMouseTransparent(true);
        t.setEffect(buildPieceEffect(theme, white));
        return t;
    }

    private Effect buildPieceEffect(BoardTheme theme, boolean white) {
        switch (theme.pieceStyle()) {
            case CLEAN: {
                // Temiz, minimal gölge
                DropShadow ds = new DropShadow(5, 2, 2, Color.web("#000000", white ? 0.45 : 0.70));
                return ds;
            }
            case GLOWING: {
                // Altın/gümüş parıltı
                Color glow = white
                    ? Color.web("#ffd700")
                    : Color.web("#b0c4de");
                DropShadow ds = new DropShadow(20, 0, 0, glow);
                ds.setSpread(0.35);
                // Ek derin gölge katmanı (zincirleme)
                DropShadow base = new DropShadow(6, 2, 3, Color.web("#000000", 0.75));
                base.setInput(ds);
                return base;
            }
            case NEON: {
                // Güçlü neon parıltı + bloom
                Color neon = white
                    ? Color.web("#00e5ff")
                    : Color.web("#ff1493");
                DropShadow outer = new DropShadow(28, 0, 0, neon);
                outer.setSpread(0.55);
                Bloom bloom = new Bloom(0.0);
                outer.setInput(bloom);
                return outer;
            }
            default: // SHADOWED — klasik 3D efekt
                if (white) {
                    // Fildişi taş: sıcak derin gölge
                    DropShadow ds = new DropShadow(8, 2, 4, Color.web("#2a1000", 0.92));
                    ds.setSpread(0.08);
                    return ds;
                } else {
                    // Espresso taş: sert siyah gölge + hafif kenar
                    DropShadow ds = new DropShadow(6, 1, 3, Color.web("#000000", 0.98));
                    ds.setSpread(0.12);
                    return ds;
                }
        }
    }

    // ── Tıklama ve hamle ────────────────────────────────────────────────────

    private void handleClick(Coordinate coord) {
        if (controller.isGameOver())   return;
        if (controller.isAITurn())     return;
        if (controller.isRemoteTurn()) return;

        Board board   = controller.getBoard();
        Piece clicked = board.getPiece(coord);
        String turn   = controller.getCurrentTurn();

        if (selected == null) {
            if (clicked != null && clicked.getColor().equals(turn))
                selectPiece(coord, board);
        } else {
            if (clicked != null && clicked.getColor().equals(turn)) {
                Coordinate prev = selected;
                clearHighlight();
                if (!coord.equals(prev)) selectPiece(coord, board);
            } else if (legalMoves.contains(coord)) {
                doMove(coord);
            } else {
                clearHighlight();
            }
        }
    }

    private void selectPiece(Coordinate coord, Board board) {
        selected   = coord;
        legalMoves = controller.getLegalMoves(coord);

        // Animasyonlu seçim katmanı
        addSelectionOverlay(cells[coord.row()][coord.col()]);

        // Hamle ipuçları
        for (Coordinate mv : legalMoves)
            addMoveHint(cells[mv.row()][mv.col()], board.getPiece(mv) != null);
    }

    private void addSelectionOverlay(StackPane cell) {
        Rectangle overlay = new Rectangle(CELL, CELL);
        overlay.setFill(ThemeManager.get().current().selectionColor());
        overlay.setId("sel-overlay");
        overlay.setMouseTransparent(true);

        // Nabız efekti
        FadeTransition ft = new FadeTransition(Duration.millis(560), overlay);
        ft.setFromValue(0.82);
        ft.setToValue(0.30);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        // bg'den sonra, taştan önce
        cell.getChildren().add(1, overlay);
    }

    private void addMoveHint(StackPane cell, boolean isCapture) {
        BoardTheme t = ThemeManager.get().current();
        if (isCapture) {
            // Pulsing yakalama halkası
            Circle ring = new Circle(CELL / 2.0 - 8);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(t.captureRingColor());
            ring.setStrokeWidth(6.5);
            ring.setId("hint");
            ring.setMouseTransparent(true);
            ring.setEffect(new DropShadow(8, t.captureRingColor().deriveColor(0,1,1,0.6)));

            FadeTransition ft = new FadeTransition(Duration.millis(620), ring);
            ft.setFromValue(0.95);
            ft.setToValue(0.35);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();
            cell.getChildren().add(ring);
        } else {
            // Nefes alan nokta
            Circle dot = new Circle(13);
            dot.setFill(t.moveHintColor());
            dot.setId("hint");
            dot.setMouseTransparent(true);
            dot.setEffect(new DropShadow(6, t.moveHintColor().deriveColor(0,1,1,0.5)));

            ScaleTransition st = new ScaleTransition(Duration.millis(780), dot);
            st.setFromX(1.0); st.setToX(0.62);
            st.setFromY(1.0); st.setToY(0.62);
            st.setCycleCount(Animation.INDEFINITE);
            st.setAutoReverse(true);
            st.play();
            cell.getChildren().add(dot);
        }
    }

    private void doMove(Coordinate to) {
        Coordinate from = selected;
        clearHighlight();
        controller.executeMove(from, to);
    }

    private void clearHighlight() {
        if (selected != null)
            cells[selected.row()][selected.col()].getChildren()
                .removeIf(n -> "sel-overlay".equals(n.getId()));
        for (Coordinate mv : legalMoves)
            cells[mv.row()][mv.col()].getChildren()
                .removeIf(n -> "hint".equals(n.getId()));
        selected   = null;
        legalMoves = new ArrayList<>();
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    private void setBg(StackPane cell, Color color) {
        cell.getChildren().stream()
            .filter(n -> n instanceof Rectangle && n.getId() == null)
            .findFirst()
            .ifPresent(n -> ((Rectangle) n).setFill(color));
    }

    private Color baseFill(int row, int col) {
        BoardTheme t = ThemeManager.get().current();
        return (row + col) % 2 == 0 ? t.lightSquare() : t.darkSquare();
    }

    private Label coordLabel(String text, BoardTheme t) {
        Label l = new Label(text);
        l.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        l.setTextFill(t.textSecondary());
        return l;
    }

    private static void dispatchSound(String event) {
        switch (event) {
            case "move":      SoundEngine.playMove();      break;
            case "capture":   SoundEngine.playCapture();   break;
            case "check":     SoundEngine.playCheck();     break;
            case "checkmate": SoundEngine.playCheckmate(); break;
        }
    }
}
