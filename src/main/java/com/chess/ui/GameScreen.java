package com.chess.ui;

import com.chess.core.GameConfig;
import com.chess.core.GameTimer;
import com.chess.game.GameController;
import com.chess.network.NetworkManager;
import com.chess.sound.SoundEngine;
import com.chess.theme.BoardTheme;
import com.chess.theme.ThemeManager;
import com.chess.theme.Themes;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.List;

public class GameScreen {

    public static void show(Stage stage, GameConfig config, NetworkManager nm, Runnable onBackToMenu) {

        GameController controller = new GameController(config);
        if (nm != null) {
            nm.setUiExecutor(Platform::runLater);
            controller.setNetworkManager(nm);
        }

        // ── Tahta ──────────────────────────────────────────────────────────
        ChessBoardUI boardUI = new ChessBoardUI(controller);

        // ChessBoardUI artık kendi çerçevesi ve koordinat etiketlerini içeriyor

        // ── Üst çubuk ──────────────────────────────────────────────────────
        String titleText = "RsL Chess  ·  " + config.modeLabel()
                + "  [" + config.timerLabel() + "]";
        Label titleLabel = new Label(titleText);
        titleLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#d4af37"));

        Button soundBtn = smallBtn("🔊", "#58a6ff");
        soundBtn.setOnAction(e -> {
            boolean on = !SoundEngine.isEnabled();
            SoundEngine.setEnabled(on);
            soundBtn.setText(on ? "🔊" : "🔇");
        });

        Button menuBtn = smallBtn("← Menü", "#8b949e");
        menuBtn.setOnAction(e -> {
            if (controller.getTimer() != null) controller.getTimer().stop();
            if (nm != null) nm.disconnect();
            onBackToMenu.run();
        });

        HBox topBar = new HBox(8, titleLabel, spacer(), soundBtn, menuBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(9, 14, 9, 14));
        topBar.setStyle("-fx-background-color:#161b22;-fx-border-color:#21262d;-fx-border-width:0 0 1 0;");

        // ── Sağ panel ──────────────────────────────────────────────────────

        // Tema seçici (küçük butonlar)
        HBox themeRow = buildThemeRow(boardUI);

        // Siyah zamanlayıcı
        Label blackTimeLabel = timerLabel("SİYAH ♟", config.hasTimer() ? "--:--" : "∞");
        VBox blackBox = timerBox(blackTimeLabel, "#1c1c2e", "#444466");

        // Hamle listesi
        ListView<String> moveList = buildMoveList();
        VBox.setVgrow(moveList, Priority.ALWAYS);
        Label moveHeader = sectionHeader("  HAMLELER");
        VBox moveSection = new VBox(0, moveHeader, moveList);
        VBox.setVgrow(moveSection, Priority.ALWAYS);

        // Beyaz zamanlayıcı
        Label whiteTimeLabel = timerLabel("BEYAZ ♙", config.hasTimer() ? "--:--" : "∞");
        VBox whiteBox = timerBox(whiteTimeLabel, "#1e2a1e", "#336633");

        // Durum çubuğu
        Circle turnDot  = new Circle(6);
        Label turnLabel = new Label("Sıra: BEYAZ");
        turnLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        turnLabel.setTextFill(Color.web("#e6edf3"));
        Label aiStatus = new Label("");
        aiStatus.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 11));
        aiStatus.setTextFill(Color.web("#f0883e"));
        HBox statusRow = new HBox(8, turnDot, turnLabel, spacer(), aiStatus);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(7, 10, 7, 10));
        statusRow.setStyle("-fx-background-color:#161b22;-fx-border-color:#21262d;-fx-border-width:1 0 0 0;");

        // Chat (sadece çevrimiçi modda)
        VBox chatSection = nm != null ? buildChatSection(nm) : null;

        VBox rightPanel;
        if (chatSection != null) {
            rightPanel = new VBox(0, themeRow, blackBox, moveSection, chatSection, whiteBox, statusRow);
        } else {
            rightPanel = new VBox(0, themeRow, blackBox, moveSection, whiteBox, statusRow);
        }
        rightPanel.setStyle("-fx-background-color:#0d1117;-fx-border-color:#21262d;-fx-border-width:0 0 0 1;");
        rightPanel.setPrefWidth(290);
        rightPanel.setMinWidth(210);

        // ── Orta alan ──────────────────────────────────────────────────────
        HBox center = new HBox(0, boardUI, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        center.setAlignment(Pos.CENTER_LEFT);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0d1117;");
        root.setTop(topBar);
        root.setCenter(center);
        BorderPane.setMargin(center, new Insets(10, 0, 0, 10));

        Scene scene = new Scene(root, 940, 620);
        stage.setScene(scene);
        stage.setTitle("RsL Chess  ·  " + config.modeLabel());
        stage.setResizable(true);
        stage.setMaximized(true);

        // ── Durum güncelleme ───────────────────────────────────────────────
        Runnable updateStatus = () -> {
            String turn    = controller.getCurrentTurn();
            boolean isWhite = "WHITE".equals(turn);
            boolean inCheck = controller.isCurrentPlayerInCheck();

            turnDot.setFill(isWhite ? Color.web("#58a6ff") : Color.web("#f85149"));
            if (inCheck) {
                turnLabel.setText("Sıra: " + (isWhite ? "BEYAZ" : "SİYAH") + "  ŞAH!");
                turnLabel.setTextFill(Color.web("#f85149"));
            } else {
                turnLabel.setText("Sıra: " + (isWhite ? "BEYAZ" : "SİYAH"));
                turnLabel.setTextFill(Color.web("#e6edf3"));
            }

            if (controller.isAITurn())        aiStatus.setText("AI düşünüyor...");
            else if (controller.isRemoteTurn()) aiStatus.setText("Rakip bekliyor...");
            else                               aiStatus.setText("");

            if (!config.hasTimer()) {
                setTimerText(whiteTimeLabel, "∞", false, false);
                setTimerText(blackTimeLabel, "∞", false, false);
            }
            updateMoveList(moveList, controller.getMoveLog());
        };

        boardUI.setOnMoveCompleted(updateStatus);
        updateStatus.run();

        // ── Zamanlayıcı ────────────────────────────────────────────────────
        if (config.hasTimer()) {
            GameTimer timer = controller.getTimer();
            timer.setOnTick(t -> Platform.runLater(() -> {
                boolean wTurn = "WHITE".equals(controller.getCurrentTurn());
                setTimerText(whiteTimeLabel, t.formatWhite(),
                        t.getWhiteMs() < 60_000, t.getWhiteMs() < 10_000);
                setTimerText(blackTimeLabel, t.formatBlack(),
                        t.getBlackMs() < 60_000, t.getBlackMs() < 10_000);
                whiteBox.setStyle(wTurn
                    ? "-fx-background-color:#253325;-fx-border-color:#3fb950;-fx-border-width:0 0 0 3;"
                    : "-fx-background-color:#1e2a1e;-fx-border-color:#336633;-fx-border-width:0 0 0 1;");
                blackBox.setStyle(!wTurn
                    ? "-fx-background-color:#252535;-fx-border-color:#6666cc;-fx-border-width:0 0 0 3;"
                    : "-fx-background-color:#1c1c2e;-fx-border-color:#444466;-fx-border-width:0 0 0 1;");
            }));
            controller.startTimer();
        }

        // ── Oyun sonu ──────────────────────────────────────────────────────
        controller.setOnGameOver(result -> Platform.runLater(() -> {
            updateStatus.run();
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Oyun Bitti");
            alert.setHeaderText(result);
            alert.setContentText("Oyun geçmişi kaydedildi.\nYeni oyun için menüye dönün.");
            alert.getDialogPane().setStyle("-fx-background-color:#161b22;-fx-border-color:#d4af37;");
            alert.showAndWait();
            if (controller.getTimer() != null) controller.getTimer().stop();
            if (nm != null) nm.disconnect();
            onBackToMenu.run();
        }));
    }

    // ── Tema satırı ─────────────────────────────────────────────────────────

    private static HBox buildThemeRow(ChessBoardUI boardUI) {
        String[] abbrev = {"Klas", "Mini", "Fan", "Cam", "Füt"};
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(5, 6, 5, 6));
        row.setStyle("-fx-background-color:#101820;");

        for (int i = 0; i < Themes.ALL.length; i++) {
            final BoardTheme t = Themes.ALL[i];
            final String label = abbrev[i];
            Button tb = new Button(label);
            tb.setFont(Font.font("Segoe UI", 11));
            tb.setPrefWidth(50);
            tb.setPrefHeight(24);
            String acc = toHex(t.accentColor());
            tb.setStyle("-fx-background-color:#1a1a2a;-fx-border-color:" + acc +
                        ";-fx-text-fill:" + acc + ";-fx-border-width:1;-fx-border-radius:4;" +
                        "-fx-background-radius:4;-fx-cursor:hand;");
            tb.setOnMouseEntered(e -> tb.setStyle(
                "-fx-background-color:" + acc + "33;-fx-border-color:" + acc +
                ";-fx-text-fill:" + acc + ";-fx-border-width:1;-fx-border-radius:4;" +
                "-fx-background-radius:4;-fx-cursor:hand;"));
            tb.setOnMouseExited(e -> tb.setStyle(
                "-fx-background-color:#1a1a2a;-fx-border-color:" + acc +
                ";-fx-text-fill:" + acc + ";-fx-border-width:1;-fx-border-radius:4;" +
                "-fx-background-radius:4;-fx-cursor:hand;"));
            tb.setOnAction(e -> {
                ThemeManager.get().set(t);
                boardUI.refreshTheme();
            });
            row.getChildren().add(tb);
        }
        return row;
    }

    // ── Chat bölümü (sadece ağ modu) ─────────────────────────────────────────

    private static VBox buildChatSection(NetworkManager nm) {
        Label header = sectionHeader("  SOHBET");

        ListView<String> chatList = new ListView<>();
        chatList.setPrefHeight(130);
        chatList.setMaxHeight(130);
        chatList.setStyle("-fx-background-color:#0d1117;-fx-border-color:#21262d;-fx-border-width:1;" +
                          "-fx-control-inner-background:#0d1117;");
        chatList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); }
                else {
                    setText(s);
                    setStyle("-fx-text-fill:#c9d1d9;-fx-font-family:Consolas;-fx-font-size:11;" +
                             "-fx-background-color:#0d1117;");
                }
            }
        });

        nm.setOnChatReceived(msg -> chatList.getItems().add(msg));

        TextField chatInput = new TextField();
        chatInput.setPromptText("Mesaj yaz...");
        chatInput.setStyle("-fx-background-color:#21262d;-fx-text-fill:#e6edf3;" +
                           "-fx-border-color:#30363d;-fx-border-radius:4;-fx-background-radius:4;" +
                           "-fx-padding:4 8 4 8;-fx-font-size:12;");

        Button sendBtn = new Button("→");
        sendBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        sendBtn.setStyle("-fx-background-color:#1f3a6e;-fx-text-fill:#58a6ff;-fx-border-color:#388bfd;" +
                         "-fx-border-radius:4;-fx-background-radius:4;-fx-cursor:hand;-fx-padding:4 10 4 10;");

        Runnable doSend = () -> {
            String txt = chatInput.getText().trim();
            if (!txt.isEmpty()) {
                nm.sendChat(txt);
                chatList.getItems().add(nm.getLocalPlayerName() + ": " + txt);
                chatInput.clear();
                if (!chatList.getItems().isEmpty())
                    chatList.scrollTo(chatList.getItems().size() - 1);
            }
        };
        sendBtn.setOnAction(e -> doSend.run());
        chatInput.setOnAction(e -> doSend.run());

        HBox.setHgrow(chatInput, Priority.ALWAYS);
        HBox inputRow = new HBox(4, chatInput, sendBtn);
        inputRow.setPadding(new Insets(4, 6, 4, 6));
        inputRow.setStyle("-fx-background-color:#101820;");

        return new VBox(0, header, chatList, inputRow);
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    private static ListView<String> buildMoveList() {
        ListView<String> lv = new ListView<>();
        lv.setStyle("-fx-background-color:#0d1117;-fx-border-color:#21262d;-fx-border-width:1;" +
                    "-fx-control-inner-background:#0d1117;");
        lv.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); }
                else {
                    setText(s);
                    setStyle("-fx-text-fill:#c9d1d9;-fx-font-family:Consolas;-fx-font-size:12;" +
                             "-fx-background-color:" + (getIndex() % 2 == 0 ? "#0d1117" : "#161b22") + ";");
                }
            }
        });
        return lv;
    }

    private static void updateMoveList(ListView<String> view, List<String> log) {
        view.getItems().clear();
        for (int i = 0; i < log.size(); i += 2) {
            String w = log.get(i);
            String b = (i + 1 < log.size()) ? log.get(i + 1) : "";
            view.getItems().add(String.format("  %2d.  %-8s  %s", (i / 2) + 1, w, b));
        }
        if (!view.getItems().isEmpty()) view.scrollTo(view.getItems().size() - 1);
    }

    private static Label timerLabel(String side, String time) {
        Label l = new Label(side + "    " + time);
        l.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        l.setTextFill(Color.web("#c9d1d9"));
        l.setPadding(new Insets(6, 12, 6, 12));
        return l;
    }

    private static void setTimerText(Label lbl, String time, boolean warn, boolean danger) {
        String cur = lbl.getText();
        int idx = cur.lastIndexOf("  ");
        if (idx >= 0) lbl.setText(cur.substring(0, idx + 2) + time);
        lbl.setTextFill(danger ? Color.web("#f85149") : warn ? Color.web("#e3b341") : Color.web("#c9d1d9"));
    }

    private static VBox timerBox(Label label, String bg, String border) {
        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefHeight(52);
        box.setMinHeight(52);
        box.setMaxHeight(52);
        box.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border + ";-fx-border-width:0 0 0 1;");
        return box;
    }

    private static Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#8b949e"));
        l.setPadding(new Insets(5, 0, 3, 0));
        return l;
    }

    private static Label coordLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Georgia", 11));
        l.setTextFill(Color.web("#8b949e"));
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private static Button smallBtn(String text, String color) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", 12));
        b.setTextFill(Color.web(color));
        String base = "-fx-background-color:#21262d;-fx-border-color:#30363d;" +
                      "-fx-border-radius:6;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 12 5 12;";
        String hover = "-fx-background-color:#30363d;-fx-border-color:#484f58;" +
                       "-fx-border-radius:6;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 12 5 12;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static String toHex(javafx.scene.paint.Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}
