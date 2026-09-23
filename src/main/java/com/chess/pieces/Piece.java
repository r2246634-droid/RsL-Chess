package com.chess.pieces;

import com.chess.core.Board;
import com.chess.core.Coordinate;

/**
 * Tüm satranç taşlarını temsil eden abstract bir sınıftır.
 * Her taş, hareket kuralları sağlayan calculateMoves method'unu uygular.
 */
public abstract class Piece {
    private Coordinate position;
    private final String type;
    private boolean moved = false;

    /**
     * Taşı belirli bir koordinata yerleştirir.
     * 
     * @param position Taşın yeni konumu
     */
    public void setPosition(Coordinate position) {
        this.position = position;
    }

    /**
     * Taşın mevcut konumunu alır.
     * 
     * @return Taşın koordinat konumu
     */
    public Coordinate getPosition() {
        return position;
    }

    /**
     * Taşın mevcut tipini döndürür.
     * 
     * @return Taşın tip damgası
     */
    public String getType() {
        return type;
    }

    /**
     * Taşın belirtilen tahta üzerindeki geçerli hareketlerini hesaplar.
     * Her taş türünün hareket kuralları burada uygulanır.
     * 
     * @param board Taşın hareketlerini hesaplanması gereken tahta
     * @return Taşın hareketlerinin bir listesi
     */
    public abstract java.util.List<Coordinate> calculateMoves(Board board);

    public abstract Piece copy(Coordinate newPosition);

    /**
     * Taşın en az bir kez hareket edip etmediğini döndürür.
     * Rok hakkı kontrolü için gereklidir (Kral/Kale hiç hareket etmemiş olmalı).
     */
    public boolean isMoved() { return moved; }
    public void setMoved(boolean moved) { this.moved = moved; }

    private final String color;

    /**
     * Taşın rengini döndürür.
     *
     * @return Taşın rengi (örneğin, "WHITE", "BLACK")
     */
    public String getColor() {
        return color;
    }

    /**
     * Taş sınıfları için türetilmiş bir constructor'dır.
     *
     * @param type Taş tipi (örneğin, "Knight", "Rook")
     * @param position Taşın başlangıç konumu
     * @param color Taşın rengi
     */
    protected Piece(String type, Coordinate position, String color) {
        this.type = type;
        this.position = position;
        this.color = color;
    }
}
