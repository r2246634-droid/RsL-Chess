package com.chess.ui;

import com.chess.core.GameConfig;
import com.chess.network.NetworkManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.BiConsumer;

public class MultiplayerMenu {

    private static final long[][] TIMERS = {
        {GameConfig.UNLIMITED, 0},
        {1 * 60_000L, 1_000L},
        {5 * 60_000L, 3_000L},
        {10 * 60_000L, 5_000L}
    };
    private static final String[] TIMER_LABELS = {"∞ Süresiz", "⚡1+1", "🔥5+3", "🐢10+5"};

    public static void show(Stage stage,
                            BiConsumer<GameConfig, NetworkManager> onGame,
                            Runnable onBack) {

        int[] timerIdx = {0};
        NetworkManager[] pending = {null}; // server waiting for client

        // ── Kök ──────────────────────────────────────────────────────────────
        StackPane root = new StackPane();
        Rectangle bg = new Rectangle(760, 820);
        bg.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0d1117")), new Stop(1, Color.web("#161b22"))));
        root.getChildren().add(bg);

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 80, 40, 80));
        root.getChildren().add(content);

        // ── Başlık ───────────────────────────────────────────────────────────
        Text logo = new Text("🌐  Çevrimiçi Oyun");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 40));
        logo.setFill(Color.web("#d4af37"));
        logo.setEffect(new DropShadow(18, Color.web("#d4af37", 0.35)));

        // ── Oyuncu adı ───────────────────────────────────────────────────────
        Label nameLbl = new Label("Oyuncu Adı:");
        nameLbl.setFont(Font.font("Segoe UI", 13));
        nameLbl.setTextFill(Color.web("#8b949e"));

        TextField nameField = new TextField("Oyuncu");
        nameField.setMaxWidth(300);
        nameField.setStyle(
            "-fx-background-color:#21262d;-fx-text-fill:#e6edf3;-fx-border-color:#30363d;" +
            "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:6 10 6 10;-fx-font-size:13;");

        HBox nameRow = new HBox(10, nameLbl, nameField);
        nameRow.setAlignment(Pos.CENTER);

        // ── Süre seçimi ──────────────────────────────────────────────────────
        Label timeLbl = new Label("Süre:");
        timeLbl.setFont(Font.font("Segoe UI", 13));
        timeLbl.setTextFill(Color.web("#8b949e"));

        Button[] timerBtns = new Button[TIMER_LABELS.length];
        HBox timerRow = new HBox(6);
        timerRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < TIMER_LABELS.length; i++) {
            final int idx = i;
            Button tb = new Button(TIMER_LABELS[i]);
            tb.setFont(Font.font("Segoe UI", 12));
            tb.setPrefWidth(110);
            tb.setPrefHeight(34);
            timerBtns[i] = tb;
            tb.setOnAction(e -> { timerIdx[0] = idx; styleTimerBtns(timerBtns, idx); });
            timerRow.getChildren().add(tb);
        }
        styleTimerBtns(timerBtns, 0);
        HBox timerSection = new HBox(10, timeLbl, timerRow);
        timerSection.setAlignment(Pos.CENTER);

        // ── Ayraç ────────────────────────────────────────────────────────────
        Label sep1 = separator("── Host (BEYAZ) ──────────────────────────");

        // ── IP göstergesi ────────────────────────────────────────────────────
        Label ipInfo = new Label("IP: " + NetworkManager.localAddress() + "  Port: " + NetworkManager.PORT);
        ipInfo.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        ipInfo.setTextFill(Color.web("#58a6ff"));
        ipInfo.setVisible(false);
        ipInfo.setManaged(false);

        Label waitingLabel = new Label("Bekleniyor...");
        waitingLabel.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 13));
        waitingLabel.setTextFill(Color.web("#f0883e"));
        waitingLabel.setVisible(false);
        waitingLabel.setManaged(false);

        Button hostBtn = bigBtn("🖥  Sunucu Başlat (BEYAZ)", "#1a3a1a", "#3fb950");
        hostBtn.setOnAction(e -> {
            if (pending[0] != null) return; // already waiting
            NetworkManager nm = buildNM(nameField.getText());
            pending[0] = nm;

            nm.setOnConnected(() -> {
                pending[0] = null;
                GameConfig cfg = makeConfig("NETWORK_HOST", timerIdx[0]);
                onGame.accept(cfg, nm);
            });
            nm.startAsHost(() -> Platform.runLater(() -> {
                ipInfo.setVisible(true);  ipInfo.setManaged(true);
                waitingLabel.setVisible(true); waitingLabel.setManaged(true);
            }));
        });

        // ── Client ───────────────────────────────────────────────────────────
        Label sep2 = separator("── Misafir (SİYAH) ────────────────────────");

        TextField ipField = new TextField("192.168.1.");
        ipField.setMaxWidth(260);
        ipField.setStyle(
            "-fx-background-color:#21262d;-fx-text-fill:#e6edf3;-fx-border-color:#30363d;" +
            "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:6 10 6 10;-fx-font-size:13;");
        Label ipLbl = new Label("Host IP:");
        ipLbl.setFont(Font.font("Segoe UI", 13));
        ipLbl.setTextFill(Color.web("#8b949e"));
        HBox ipRow = new HBox(10, ipLbl, ipField);
        ipRow.setAlignment(Pos.CENTER);

        Label connectStatus = new Label("");
        connectStatus.setFont(Font.font("Segoe UI", 12));
        connectStatus.setTextFill(Color.web("#f85149"));

        Button joinBtn = bigBtn("🔌  Sunucuya Bağlan (SİYAH)", "#1a1a3a", "#6e76e5");
        joinBtn.setOnAction(e -> {
            connectStatus.setText("Bağlanıyor...");
            connectStatus.setTextFill(Color.web("#f0883e"));
            String addr = ipField.getText().trim();
            NetworkManager nm = buildNM(nameField.getText());
            nm.setOnConnected(() -> {
                GameConfig cfg = makeConfig("NETWORK_CLIENT", timerIdx[0]);
                onGame.accept(cfg, nm);
            });
            new Thread(() -> {
                try {
                    nm.connectToHost(addr);
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        connectStatus.setText("Bağlantı başarısız: " + ex.getMessage());
                        connectStatus.setTextFill(Color.web("#f85149"));
                    });
                }
            }, "connect-thread").start();
        });

        // ── Geri ─────────────────────────────────────────────────────────────
        Button backBtn = bigBtn("← Geri", "#21262d", "#8b949e");
        backBtn.setOnAction(e -> {
            if (pending[0] != null) { pending[0].disconnect(); pending[0] = null; }
            onBack.run();
        });

        content.getChildren().addAll(
            logo, nameRow, timerSection,
            sep1, ipInfo, waitingLabel, hostBtn,
            sep2, ipRow, connectStatus, joinBtn,
            backBtn
        );

        Scene scene = new Scene(root, 760, 820);
        stage.setScene(scene);
        stage.setTitle("RsL Chess — Çevrimiçi");
        stage.setMaximized(false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static NetworkManager buildNM(String name) {
        NetworkManager nm = new NetworkManager();
        nm.setLocalPlayerName(name.isBlank() ? "Oyuncu" : name.trim());
        nm.setUiExecutor(javafx.application.Platform::runLater);
        return nm;
    }

    private static GameConfig makeConfig(String mode, int tIdx) {
        long[] t = TIMERS[tIdx];
        return new GameConfig(mode, t[0], t[1]);
    }

    private static void styleTimerBtns(Button[] btns, int sel) {
        for (int i = 0; i < btns.length; i++) {
            boolean s = (i == sel);
            btns[i].setStyle(
                "-fx-background-color:" + (s ? "#1f3a1f" : "#21262d") + ";" +
                "-fx-border-color:"     + (s ? "#3fb950" : "#30363d") + ";" +
                "-fx-text-fill:"        + (s ? "#3fb950" : "#8b949e") + ";" +
                "-fx-border-width:1.5;-fx-border-radius:6;-fx-background-radius:6;-fx-cursor:hand;");
        }
    }

    private static Button bigBtn(String text, String bg, String border) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        b.setTextFill(Color.web(border));
        b.setPrefWidth(380);
        b.setPrefHeight(52);
        b.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border +
                   ";-fx-border-width:1.5;-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color:" + border + "22;-fx-border-color:" + border +
                ";-fx-border-width:1.5;-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;"));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color:" + bg + ";-fx-border-color:" + border +
                ";-fx-border-width:1.5;-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;"));
        return b;
    }

    private static Label separator(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web("#484f58"));
        return l;
    }
}
