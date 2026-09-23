package com.chess.ui;

import com.chess.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** Piyon terfisinde taş seçimi için küçük modal pencere. */
final class PromotionDialog {

    private PromotionDialog() {}

    private static final String[] TYPES        = {"Queen", "Rook", "Bishop", "Knight"};
    private static final String[] LABEL_KEYS    = {"piece.queen", "piece.rook", "piece.bishop", "piece.knight"};
    private static final String[] SYMBOLS_WHITE = {"♕", "♖", "♗", "♘"};
    private static final String[] SYMBOLS_BLACK = {"♛", "♜", "♝", "♞"};

    /** Kullanıcı seçim yapana kadar bloklar. "Queen"|"Rook"|"Bishop"|"Knight" döndürür. */
    static String show(Stage owner, boolean white) {
        String[] result = {"Queen"};

        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label title = new Label(I18n.t("promotion.title"));
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#d4af37"));

        Label desc = new Label(I18n.t("promotion.desc"));
        desc.setFont(Font.font("Segoe UI", 12));
        desc.setTextFill(Color.web("#8b949e"));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);

        for (int i = 0; i < TYPES.length; i++) {
            final String type = TYPES[i];
            String symbol = white ? SYMBOLS_WHITE[i] : SYMBOLS_BLACK[i];

            Label sym = new Label(symbol);
            sym.setFont(Font.font("Segoe UI Symbol", 34));
            sym.setTextFill(Color.web("#e6edf3"));
            Label lbl = new Label(I18n.t(LABEL_KEYS[i]));
            lbl.setFont(Font.font("Segoe UI", 10));
            lbl.setTextFill(Color.web("#8b949e"));
            VBox inner = new VBox(4, sym, lbl);
            inner.setAlignment(Pos.CENTER);

            Button b = new Button();
            b.setGraphic(inner);
            b.setPrefSize(72, 72);
            String base = "-fx-background-color:#21262d;-fx-border-color:#30363d;-fx-border-width:1.5;" +
                          "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;";
            String hover = "-fx-background-color:#30363d;-fx-border-color:#d4af37;-fx-border-width:1.5;" +
                           "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;";
            b.setStyle(base);
            b.setOnMouseEntered(e -> b.setStyle(hover));
            b.setOnMouseExited(e -> b.setStyle(base));
            b.setOnAction(e -> { result[0] = type; dialog.close(); });
            row.getChildren().add(b);
        }

        VBox root = new VBox(14, title, desc, row);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:#161b22;-fx-border-color:#d4af37;-fx-border-width:1.5;");

        dialog.setScene(new Scene(root));
        dialog.showAndWait();
        return result[0];
    }
}
