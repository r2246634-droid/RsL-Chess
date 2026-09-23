package com.chess.game;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GameHistory {

    private static final Path HISTORY_DIR = Path.of("game_history");

    private final List<String> entries = new ArrayList<>();
    private final String gameMode;
    private final String startTime;
    private int moveNumber = 1;

    public GameHistory(String gameMode) {
        this.gameMode = gameMode;
        this.startTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    public void record(String color, String pieceType, String from, String to, boolean capture) {
        String side = color.equals("WHITE") ? "BEYAZ" : "SİYAH";
        String entry = String.format("  %3d. [%s] %s %s -> %s%s",
                moveNumber++, side, pieceType, from, to, capture ? " (yakalama)" : "");
        entries.add(entry);
    }

    public void save(String result) {
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
            pw.println("      RsL Chess - Oyun Gecmisi          ");
            pw.println("=========================================");
            pw.println("Tarih : " + startTime);
            pw.println("Mod   : " + gameMode);
            pw.println("-----------------------------------------");
            entries.forEach(pw::println);
            pw.println("-----------------------------------------");
            pw.println("Sonuc : " + result);
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
