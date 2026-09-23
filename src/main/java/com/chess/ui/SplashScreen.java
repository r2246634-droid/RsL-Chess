package com.chess.ui;

import com.chess.i18n.I18n;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;

public class SplashScreen {

    public static void show(Stage stage, Runnable onFinished) {

        // ── Arka plan ────────────────────────────────────────────────────────
        StackPane root = new StackPane();
        Rectangle bg = new Rectangle(680, 820);
        bg.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0a0e18")),
                new Stop(1, Color.web("#141824"))));
        root.getChildren().add(bg);

        // ── İkon görseli ────────────────────────────────────────────────────
        ImageView logoView = loadLogo();
        logoView.setFitWidth(320);
        logoView.setFitHeight(320);
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);
        logoView.setOpacity(0);
        logoView.setEffect(new DropShadow(40, Color.web("#000000", 0.7)));

        // ── Başlık metni ─────────────────────────────────────────────────────
        Text titleText = new Text("RsL Chess");
        titleText.setFont(Font.font("Georgia", FontWeight.BOLD, 54));
        titleText.setFill(Color.web("#d4af37"));
        DropShadow glow = new DropShadow(28, Color.web("#d4af37", 0.55));
        titleText.setEffect(glow);
        titleText.setOpacity(0);

        Text sub = new Text(I18n.t("splash.subtitle"));
        sub.setFont(Font.font("Georgia", FontPosture.ITALIC, 18));
        sub.setFill(Color.web("#8b949e"));
        sub.setOpacity(0);

        Text version = new Text("v3.0  ·  JavaFX");
        version.setFont(Font.font("Consolas", 12));
        version.setFill(Color.web("#484f58"));
        version.setOpacity(0);

        VBox textBox = new VBox(10, titleText, sub, version);
        textBox.setAlignment(Pos.CENTER);
        VBox.setMargin(version, new Insets(6, 0, 0, 0));

        // İnce altın çizgi
        Rectangle line = new Rectangle(260, 1.5);
        line.setFill(Color.web("#d4af37", 0.4));
        line.setOpacity(0);

        VBox content = new VBox(28, logoView, line, textBox);
        content.setAlignment(Pos.CENTER);
        root.getChildren().add(content);

        Scene scene = new Scene(root, 680, 820);
        stage.setScene(scene);
        stage.setTitle("RsL Chess");
        stage.setResizable(false);
        stage.show();

        // ── Glow nabız ───────────────────────────────────────────────────────
        Timeline glowPulse = new Timeline(
            new KeyFrame(Duration.ZERO,          new KeyValue(glow.radiusProperty(), 22)),
            new KeyFrame(Duration.seconds(1.2),  new KeyValue(glow.radiusProperty(), 42)),
            new KeyFrame(Duration.seconds(2.4),  new KeyValue(glow.radiusProperty(), 22))
        );
        glowPulse.setCycleCount(Timeline.INDEFINITE);

        // ── Logo: scale yay + fade ─────────────────────────────────────────
        logoView.setScaleX(0.6); logoView.setScaleY(0.6);
        ScaleTransition scLogo = new ScaleTransition(Duration.seconds(1.1), logoView);
        scLogo.setToX(1); scLogo.setToY(1);
        scLogo.setInterpolator(Interpolator.SPLINE(0.22, 1.4, 0.60, 1.0));
        FadeTransition fdLogo = new FadeTransition(Duration.seconds(0.9), logoView);
        fdLogo.setToValue(1);
        ParallelTransition logoAnim = new ParallelTransition(scLogo, fdLogo);

        // ── Çizgi + metinler ─────────────────────────────────────────────────
        FadeTransition fdLine = new FadeTransition(Duration.seconds(0.6), line);
        fdLine.setToValue(1);

        titleText.setTranslateY(15);
        FadeTransition fdTitle = new FadeTransition(Duration.seconds(0.8), titleText);
        fdTitle.setToValue(1);
        TranslateTransition slTitle = new TranslateTransition(Duration.seconds(0.8), titleText);
        slTitle.setToY(0); slTitle.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fdSub = new FadeTransition(Duration.seconds(0.7), sub);
        fdSub.setToValue(1); fdSub.setDelay(Duration.seconds(0.15));
        FadeTransition fdVer = new FadeTransition(Duration.seconds(0.6), version);
        fdVer.setToValue(1); fdVer.setDelay(Duration.seconds(0.3));

        ParallelTransition textAnim = new ParallelTransition(
            fdLine, fdTitle, slTitle, fdSub, fdVer);

        // ── Çıkış: tüm sahne aşağı kayar + solar ─────────────────────────
        TranslateTransition slideOut = new TranslateTransition(Duration.seconds(0.55), content);
        slideOut.setToY(60);
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.55), root);
        fadeOut.setToValue(0);
        ParallelTransition exitAnim = new ParallelTransition(slideOut, fadeOut);
        exitAnim.setOnFinished(e -> onFinished.run());

        // ── Sekans ───────────────────────────────────────────────────────────
        SequentialTransition intro = new SequentialTransition(
            logoAnim,
            new PauseTransition(Duration.seconds(0.1)),
            textAnim
        );
        intro.setOnFinished(e -> {
            glowPulse.play();
            PauseTransition hold = new PauseTransition(Duration.seconds(1.4));
            hold.setOnFinished(ev -> exitAnim.play());
            hold.play();
        });
        intro.play();
    }

    // ── Görseli yükle, yoksa fallback text ikonunu döndür ───────────────────

    private static ImageView loadLogo() {
        // Önce resources/icon.png dene
        File f = new File("resources/icon.png");
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                return new ImageView(new Image(fis));
            } catch (Exception ignored) {}
        }
        // Fallback: büyük satranç sembolü içeren ImageView (placeholder)
        return fallbackIcon();
    }

    private static ImageView fallbackIcon() {
        // Canvas ile sembol çiz → Image'e dönüştür
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(320, 320);
        javafx.scene.canvas.GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.web("#d4af37", 0.15));
        gc.fillRoundRect(0, 0, 320, 320, 60, 60);
        gc.setFill(javafx.scene.paint.Color.web("#d4af37"));
        gc.setFont(Font.font("Segoe UI Symbol", 220));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("♛", 160, 250);

        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return new ImageView(c.snapshot(sp, null));
    }
}
