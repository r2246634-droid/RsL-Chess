package com.chess.ui;

import com.chess.core.GameConfig;
import com.chess.network.NetworkManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Pencere ikonu
        setStageIcon(stage);
        SplashScreen.show(stage, () -> showMenu(stage));
    }

    private void showMenu(Stage stage) {
        MainMenu.show(stage,
            (GameConfig config) ->
                GameScreen.show(stage, config, null, () -> showMenu(stage)),
            () ->
                MultiplayerMenu.show(stage,
                    (GameConfig cfg, NetworkManager nm) ->
                        GameScreen.show(stage, cfg, nm, () -> showMenu(stage)),
                    () -> showMenu(stage))
        );
    }

    private void setStageIcon(Stage stage) {
        File f = new File("resources/icon.png");
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                stage.getIcons().add(new Image(fis));
            } catch (Exception ignored) {}
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
