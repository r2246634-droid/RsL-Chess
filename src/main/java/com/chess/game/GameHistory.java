package com.chess.game;

import com.chess.i18n.I18n;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GameHistory {

    private static final Path HISTORY_DIR = Path.of("game_history");

    private final String gameMode;
    private final String startTime;

    public GameHistory(String gameMode) {
        this.gameMode = gameMode;
        this.startTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    /** sanMoves: GameController.getMoveLog() — beyaz/siyah sırayla SAN gösterimleri. */
    public void save(List<String> sanMoves, String result) {
        String filename = "rsl_chess_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".txt";
        try {
            Files.createDirectories(HISTORY_DIR);
        } catch (IOException e) {
            System.err.println("Geçmiş klasörü oluşturulamadı: " + e.getMessage());
        }
        String path = HISTORY_DIR.resolve(filename).toString();
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, false))) {
            pw.println("=========================================");
            pw.println("      " + I18n.t("history.title"));
            pw.println("=========================================");
            pw.println(I18n.t("history.date") + " : " + startTime);
            pw.println(I18n.t("history.mode") + " : " + gameMode);
            pw.println("-----------------------------------------");
            for (int i = 0; i < sanMoves.size(); i += 2) {
                String white = sanMoves.get(i);
                String black = (i + 1 < sanMoves.size()) ? sanMoves.get(i + 1) : "";
                pw.println(String.format("  %3d. %-10s %s", (i / 2) + 1, white, black));
            }
            pw.println("-----------------------------------------");
            pw.println(I18n.t("history.result") + " : " + result);
            pw.println("=========================================");
        } catch (IOException e) {
            System.err.println("Gecmis kaydedilemedi: " + e.getMessage());
        }
        System.out.println("Oyun gecmisi kaydedildi: " + filename);
    }

    public static String toAlgebraic(int row, int col) {
        return String.valueOf((char) ('a' + col)) + (8 - row);
    }
}
