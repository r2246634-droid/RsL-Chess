package com.chess.core;

import com.chess.pieces.Piece;

/**
 * Satranç tahtasını temsil eder.
 * 8x8 boyutundaki bir tahta oluşturur ve taşları alıp koymak için temel işlevleri sağlar.
 */
public class Board {
    public static final int ROWS = 8;
    public static final int COLS = 8;

    private Piece[][] boardMatrix = new Piece[ROWS][COLS];

    /**
     * Satırdaki ve sütundaki karedeki mevcut taşı alır.
     * 
     * @param coord Taş alınacak koordinat
     * @return Belirtilen karedeki taşı referans olarak döndürür veya null döndürür
     */
    public Piece getPiece(Coordinate coord) {
        if (isWithinBounds(coord)) {
            return boardMatrix[coord.row()][coord.col()];
        }
        return null;
    }

    /**
     * Koordinata bir taş yerleştirir.
     * 
     * @param coord Taşın konumu
     * @param piece Yerleştirilecek taş
     */
    public void setPiece(Coordinate coord, Piece piece) {
        if (isWithinBounds(coord)) {
            boardMatrix[coord.row()][coord.col()] = piece;
            if (piece != null) {
                piece.setPosition(coord);
            }
        }
    }

    /**
     * Koordinatın tahta sınırlarında olup olmadığını kontrol eder.
     * 
     * @param coord Kontrol edilecek koordinat
     * @return Koordinat sınırlar içinde ise true, değilse false döndürür
     */
    public boolean isWithinBounds(Coordinate coord) {
        return coord.row() >= 0 && coord.row() < ROWS &&
               coord.col() >= 0 && coord.col() < COLS;
    }

    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                Coordinate coord = new Coordinate(r, c);
                Piece p = getPiece(coord);
                if (p != null) b.setPiece(coord, p.copy(coord));
            }
        return b;
    }

    /**
     * Tahtanın tüm içeriğini temizler.
     */
    public void clear() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                boardMatrix[row][col] = null;
            }
        }
    }
}
