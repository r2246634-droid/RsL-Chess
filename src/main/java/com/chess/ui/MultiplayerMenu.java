package com.chess.ui;

import com.chess.core.GameConfig;
import com.chess.i18n.I18n;
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

    public static void show(Stage stage,
                            BiConsumer<GameConfig, NetworkManager> onGame,
                            Runnable onBack) {

        String[] timerLabels = {
            I18n.t("timer.unlimited_full"), I18n.t("timer.bullet"), I18n.t("timer.blitz"), I18n.t("timer.rapid")
        };
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
        Text logo = new Text(I18n.t("online.title"));
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 40));
        logo.setFill(Color.web("#d4af37"));
        logo.setEffect(new DropShadow(18, Color.web("#d4af37", 0.35)));

        // ── Oyuncu adı ───────────────────────────────────────────────────────
        Label nameLbl = new Label(I18n.t("online.player_name"));
        nameLbl.setFont(Font.font("Segoe UI", 13));
        nameLbl.setTextFill(Color.web("#8b949e"));

        TextField nameField = new TextField(I18n.t("online.default_name"));
        nameField.setMaxWidth(300);
        nameField.setStyle(
            "-fx-background-color:#21262d;-fx-text-fill:#e6edf3;-fx-border-color:#30363d;" +
            "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:6 10 6 10;-fx-font-size:13;");

        HBox nameRow = new HBox(10, nameLbl, nameField);
        nameRow.setAlignment(Pos.CENTER);

        // ── Süre seçimi ──────────────────────────────────────────────────────
        Label timeLbl = new Label(I18n.t("online.time"));
        timeLbl.setFont(Font.font("Segoe UI", 13));
        timeLbl.setTextFill(Color.web("#8b949e"));

        Button[] timerBtns = new Button[timerLabels.length];
        HBox timerRow = new HBox(6);
        timerRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < timerLabels.length; i++) {
            final int idx = i;
            Button tb = new Button(timerLabels[i]);
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
        Label sep1 = separator("── " + I18n.t("online.host_section") + " ──────────────────────────");

        // ── IP göstergesi ────────────────────────────────────────────────────
        Label ipInfo = new Label("IP: " + NetworkManager.localAddress() + "  Port: " + NetworkManager.PORT);
        ipInfo.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        ipInfo.setTextFill(Color.web("#58a6ff"));
        ipInfo.setVisible(false);
        ipInfo.setManaged(false);

        Label waitingLabel = new Label(I18n.t("online.waiting"));
        waitingLabel.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 13));
        waitingLabel.setTextFill(Color.web("#f0883e"));
        waitingLabel.setVisible(false);
        waitingLabel.setManaged(false);

        Button hostBtn = bigBtn(I18n.t("online.start_server"), "#1a3a1a", "#3fb950");
        hostBtn.setOnAction(e -> {
            if (pending[0] != null) return; // already waiting
            NetworkManager nm = buildNM(nameField.getText());
            long[] tc = TIMERS[timerIdx[0]];
            nm.setTimeControl(tc[0], tc[1]);
            pending[0] = nm;

            nm.setOnConnected(() -> {
                pending[0] = null;
                GameConfig cfg = new GameConfig("NETWORK_HOST", nm.getTimeInitialMs(), nm.getTimeIncrementMs());
                onGame.accept(cfg, nm);
            });
            nm.startAsHost(() -> Platform.runLater(() -> {
                ipInfo.setVisible(true);  ipInfo.setManaged(true);
                waitingLabel.setVisible(true); waitingLabel.setManaged(true);
            }));
        });

        // ── Client ───────────────────────────────────────────────────────────
        Label sep2 = separator("── " + I18n.t("online.guest_section") + " ────────────────────────");

        TextField ipField = new TextField("192.168.1.");
        ipField.setMaxWidth(260);
        ipField.setStyle(
            "-fx-background-color:#21262d;-fx-text-fill:#e6edf3;-fx-border-color:#30363d;" +
            "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:6 10 6 10;-fx-font-size:13;");
        Label ipLbl = new Label(I18n.t("online.host_ip"));
        ipLbl.setFont(Font.font("Segoe UI", 13));
        ipLbl.setTextFill(Color.web("#8b949e"));
        HBox ipRow = new HBox(10, ipLbl, ipField);
        ipRow.setAlignment(Pos.CENTER);

        Label connectStatus = new Label("");
        connectStatus.setFont(Font.font("Segoe UI", 12));
        connectStatus.setTextFill(Color.web("#f85149"));

        Button joinBtn = bigBtn(I18n.t("online.connect"), "#1a1a3a", "#6e76e5");
        joinBtn.setOnAction(e -> {
            joinBtn.setDisable(true); // bağlanırken çift tıklama iki bağlantı açmasın
            connectStatus.setText(I18n.t("online.connecting"));
            connectStatus.setTextFill(Color.web("#f0883e"));
            String addr = ipField.getText().trim();
            NetworkManager nm = buildNM(nameField.getText());
            nm.setOnConnected(() -> {
                // Süreyi host belirler; eski sürüm bir host süre yollamadıysa kendi seçimimize düşeriz.
                GameConfig cfg = nm.getTimeInitialMs() > 0
                        ? new GameConfig("NETWORK_CLIENT", nm.getTimeInitialMs(), nm.getTimeIncrementMs())
                        : makeConfig("NETWORK_CLIENT", timerIdx[0]);
                onGame.accept(cfg, nm);
            });
            new Thread(() -> {
                try {
                    nm.connectToHost(addr);
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        joinBtn.setDisable(false);
                        connectStatus.setText(I18n.t("online.connect_failed") + ex.getMessage());
                        connectStatus.setTextFill(Color.web("#f85149"));
                    });
                }
            }, "connect-thread").start();
        });

        // ── Geri ─────────────────────────────────────────────────────────────
        Button backBtn = bigBtn(I18n.t("online.back"), "#21262d", "#8b949e");
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
        stage.setTitle("RsL Chess — " + I18n.t("online.title"));
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
