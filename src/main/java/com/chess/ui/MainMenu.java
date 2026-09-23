package com.chess.ui;

import com.chess.core.GameConfig;
import com.chess.i18n.I18n;
import com.chess.i18n.Lang;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class MainMenu {

    private static final String BG_DARK  = "#0d1117";
    private static final String BG_CARD  = "#161b22";
    private static final String GOLD     = "#d4af37";
    private static final String BTN_BG   = "#21262d";
    private static final String BTN_HOV  = "#30363d";

    private static final long[][] TIMER_OPTIONS = {
        {GameConfig.UNLIMITED,    0},          // Süresiz
        {1  * 60_000L, 1_000L},               // Bullet  1+1
        {5  * 60_000L, 3_000L},               // Blitz   5+3
        {10 * 60_000L, 5_000L},               // Rapid  10+5
    };

    public static void show(Stage stage, Consumer<GameConfig> onConfig, Runnable onMultiplayer) {
        buildScene(stage, onConfig, onMultiplayer, 0);
    }

    // Dil değişince, seçili süre aralığını koruyarak tüm ekranı yeniden çizer.
    private static void buildScene(Stage stage, Consumer<GameConfig> onConfig, Runnable onMultiplayer,
                                    int initialTimerIdx) {
        int[] selectedTimer = {initialTimerIdx};
        String[] timerLabels = {
            I18n.t("timer.unlimited_full"), I18n.t("timer.bullet"), I18n.t("timer.blitz"), I18n.t("timer.rapid")
        };

        StackPane root = new StackPane();
        Rectangle bg = new Rectangle(760, 820);
        bg.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(BG_DARK)), new Stop(1, Color.web(BG_CARD))));
        root.getChildren().add(bg);

        VBox content = new VBox(0);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 70, 40, 70));

        // Logo
        Text logo = new Text("♛  RsL Chess");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 52));
        logo.setFill(Color.web(GOLD));
        logo.setEffect(new DropShadow(20, Color.web(GOLD, 0.4)));

        Text sub = new Text(I18n.t("menu.select_mode"));
        sub.setFont(Font.font("Georgia", FontPosture.ITALIC, 16));
        sub.setFill(Color.web("#8b949e"));

        // Dil seçici
        Region gapLang = new Region(); gapLang.setPrefHeight(16);
        Label langTitle = new Label(I18n.t("menu.language"));
        langTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        langTitle.setTextFill(Color.web("#8b949e"));

        HBox langRow = new HBox(6);
        langRow.setAlignment(Pos.CENTER);
        Button[] langBtns = new Button[Lang.values().length];
        for (Lang lang : Lang.values()) {
            Button lb = new Button(lang.code());
            lb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            lb.setPrefWidth(52);
            lb.setPrefHeight(26);
            langBtns[lang.ordinal()] = lb;
            lb.setOnAction(e -> {
                I18n.set(lang);
                buildScene(stage, onConfig, onMultiplayer, selectedTimer[0]);
            });
            langRow.getChildren().add(lb);
        }
        styleLangButtons(langBtns);

        Region gap1 = new Region(); gap1.setPrefHeight(28);

        // Mod butonları
        Button btn2P     = makeButton(I18n.t("menu.two_player.title"), I18n.t("menu.two_player.desc"));
        Button btnEasy   = makeButton(I18n.t("menu.ai_easy.title"),    I18n.t("menu.ai_easy.desc"));
        Button btnMedium = makeButton(I18n.t("menu.ai_medium.title"), I18n.t("menu.ai_medium.desc"));
        Button btnExpert = makeButton(I18n.t("menu.ai_expert.title"), I18n.t("menu.ai_expert.desc"));
        Button btnOnline = makeButton(I18n.t("menu.online.title"),    I18n.t("menu.online.desc"));

        btn2P.setOnAction(e -> onConfig.accept(makeConfig("TWO_PLAYER", selectedTimer[0])));
        btnEasy.setOnAction(e -> onConfig.accept(makeConfig("AI_EASY",   selectedTimer[0])));
        btnMedium.setOnAction(e -> onConfig.accept(makeConfig("AI_MEDIUM", selectedTimer[0])));
        btnExpert.setOnAction(e -> onConfig.accept(makeConfig("AI_EXPERT", selectedTimer[0])));
        btnOnline.setOnAction(e -> onMultiplayer.run());

        VBox modeBtns = new VBox(10, btn2P, btnEasy, btnMedium, btnExpert, btnOnline);
        modeBtns.setAlignment(Pos.CENTER);

        // Süre seçim başlığı
        Region gap2 = new Region(); gap2.setPrefHeight(24);
        Label timerTitle = new Label(I18n.t("menu.timer_control"));
        timerTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        timerTitle.setTextFill(Color.web("#8b949e"));

        // Süre toggle butonları
        Button[] timerBtns = new Button[timerLabels.length];
        HBox timerRow = new HBox(8);
        timerRow.setAlignment(Pos.CENTER);

        for (int i = 0; i < timerLabels.length; i++) {
            final int idx = i;
            Button tb = new Button(timerLabels[i]);
            tb.setFont(Font.font("Segoe UI", 12));
            tb.setPrefWidth(148);
            tb.setPrefHeight(36);
            timerBtns[i] = tb;

            tb.setOnAction(e -> {
                selectedTimer[0] = idx;
                updateTimerButtons(timerBtns, idx);
            });
            timerRow.getChildren().add(tb);
        }
        updateTimerButtons(timerBtns, selectedTimer[0]);

        // Çıkış butonu
        Region gap3 = new Region(); gap3.setPrefHeight(20);
        Button btnExit = makeExitButton(I18n.t("menu.exit"));
        btnExit.setOnAction(e -> System.exit(0));

        // Versiyon
        Region gap4 = new Region(); gap4.setPrefHeight(20);
        Label version = new Label(I18n.t("menu.version"));
        version.setTextFill(Color.web("#484f58"));
        version.setFont(Font.font("Arial", 12));

        content.getChildren().addAll(
            logo, sub, gapLang, langTitle, langRow, gap1,
            modeBtns, gap2,
            timerTitle, timerRow,
            gap3, btnExit, gap4, version
        );
        root.getChildren().add(content);

        Scene scene = new Scene(root, 760, 820);
        stage.setScene(scene);
        stage.setTitle("RsL Chess");
        stage.setMaximized(false);
        stage.setResizable(true);
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    private static GameConfig makeConfig(String mode, int timerIdx) {
        long[] t = TIMER_OPTIONS[timerIdx];
        return new GameConfig(mode, t[0], t[1]);
    }

    private static void updateTimerButtons(Button[] btns, int selectedIdx) {
        for (int i = 0; i < btns.length; i++) {
            boolean sel = (i == selectedIdx);
            btns[i].setStyle(
                "-fx-background-color: " + (sel ? "#1f3a1f" : "#21262d") + ";" +
                "-fx-border-color: "     + (sel ? "#3fb950" : "#30363d") + ";" +
                "-fx-text-fill: "        + (sel ? "#3fb950" : "#8b949e") + ";" +
                "-fx-border-width: 1.5; -fx-border-radius: 6;" +
                "-fx-background-radius: 6; -fx-cursor: hand;"
            );
        }
    }

    private static void styleLangButtons(Button[] btns) {
        for (Lang lang : Lang.values()) {
            boolean sel = lang == I18n.current();
            Button b = btns[lang.ordinal()];
            b.setStyle(
                "-fx-background-color: " + (sel ? "#1a2a4a" : "#21262d") + ";" +
                "-fx-border-color: "     + (sel ? "#58a6ff" : "#30363d") + ";" +
                "-fx-text-fill: "        + (sel ? "#58a6ff" : "#8b949e") + ";" +
                "-fx-border-width: 1.5; -fx-border-radius: 6;" +
                "-fx-background-radius: 6; -fx-cursor: hand;"
            );
        }
    }

    private static Button makeButton(String title, String desc) {
        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        titleLbl.setTextFill(Color.web("#e6edf3"));

        Label descLbl = new Label(desc);
        descLbl.setFont(Font.font("Segoe UI", 11));
        descLbl.setTextFill(Color.web("#8b949e"));

        VBox inner = new VBox(2, titleLbl, descLbl);
        inner.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();
        btn.setGraphic(inner);
        btn.setPrefWidth(380);
        btn.setPrefHeight(62);
        applyStyle(btn, BTN_BG, GOLD);
        btn.setOnMouseEntered(e -> applyStyle(btn, BTN_HOV, GOLD));
        btn.setOnMouseExited(e -> applyStyle(btn, BTN_BG, GOLD));
        return btn;
    }

    private static Button makeExitButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setTextFill(Color.web("#f85149"));
        btn.setPrefWidth(380);
        btn.setPrefHeight(48);
        applyStyle(btn, BTN_BG, "#f85149");
        btn.setOnMouseEntered(e -> applyStyle(btn, "#2d1a1a", "#f85149"));
        btn.setOnMouseExited(e -> applyStyle(btn, BTN_BG, "#f85149"));
        return btn;
    }

    private static void applyStyle(Button btn, String bg, String border) {
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1.5; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20 10 20;"
        );
    }
}
