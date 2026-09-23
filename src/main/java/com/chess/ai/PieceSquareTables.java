package com.chess.ai;

/**
 * Konumsal değerlendirme tabloları ("piece-square tables"). Salt materyal
 * puanına ek olarak taşların hangi karede daha değerli olduğunu (merkez
 * kontrolü, at/fil için kenar cezası, kale için açık sütun/7. sıra, şah için
 * orta oyunda köşede kalma) ekler — AI'ı yalnızca "taş say" seviyesinden
 * çıkarıp gerçek pozisyonel tercihler yapar hale getirir.
 *
 * Tablolar satır 0 = 8. sıra (kendi başlangıç sırasından uzak taraf) olacak
 * şekilde yazılmıştır — Board'un kendi koordinat sistemiyle birebir aynı
 * (bkz. CLAUDE.md). BEYAZ taşlar için doğrudan `table[row][col]` kullanılır;
 * SİYAH taşlar için satır aynalanır (`table[7-row][col]`), böylece aynı tablo
 * her iki renk için de "kendi arka sırasına yakın/uzak" anlamını korur.
 */
final class PieceSquareTables {

    private PieceSquareTables() {}

    static int valueAt(String pieceType, String color, int row, int col) {
        int[][] table = tableFor(pieceType);
        if (table == null) return 0;
        int r = "WHITE".equals(color) ? row : 7 - row;
        return table[r][col];
    }

    private static int[][] tableFor(String type) {
        switch (type) {
            case "Pawn":   return PAWN;
            case "Knight": return KNIGHT;
            case "Bishop": return BISHOP;
            case "Rook":   return ROOK;
            case "Queen":  return QUEEN;
            case "King":   return KING;
            default:       return null;
        }
    }

    private static final int[][] PAWN = {
        {  0,  0,  0,  0,  0,  0,  0,  0 },
        { 50, 50, 50, 50, 50, 50, 50, 50 },
        { 10, 10, 20, 30, 30, 20, 10, 10 },
        {  5,  5, 10, 25, 25, 10,  5,  5 },
        {  0,  0,  0, 20, 20,  0,  0,  0 },
        {  5, -5,-10,  0,  0,-10, -5,  5 },
        {  5, 10, 10,-20,-20, 10, 10,  5 },
        {  0,  0,  0,  0,  0,  0,  0,  0 },
    };

    private static final int[][] KNIGHT = {
        { -50,-40,-30,-30,-30,-30,-40,-50 },
        { -40,-20,  0,  0,  0,  0,-20,-40 },
        { -30,  0, 10, 15, 15, 10,  0,-30 },
        { -30,  5, 15, 20, 20, 15,  5,-30 },
        { -30,  0, 15, 20, 20, 15,  0,-30 },
        { -30,  5, 10, 15, 15, 10,  5,-30 },
        { -40,-20,  0,  5,  5,  0,-20,-40 },
        { -50,-40,-30,-30,-30,-30,-40,-50 },
    };

    private static final int[][] BISHOP = {
        { -20,-10,-10,-10,-10,-10,-10,-20 },
        { -10,  0,  0,  0,  0,  0,  0,-10 },
        { -10,  0,  5, 10, 10,  5,  0,-10 },
        { -10,  5,  5, 10, 10,  5,  5,-10 },
        { -10,  0, 10, 10, 10, 10,  0,-10 },
        { -10, 10, 10, 10, 10, 10, 10,-10 },
        { -10,  5,  0,  0,  0,  0,  5,-10 },
        { -20,-10,-10,-10,-10,-10,-10,-20 },
    };

    private static final int[][] ROOK = {
        {  0,  0,  0,  0,  0,  0,  0,  0 },
        {  5, 10, 10, 10, 10, 10, 10,  5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        {  0,  0,  0,  5,  5,  0,  0,  0 },
    };

    private static final int[][] QUEEN = {
        { -20,-10,-10, -5, -5,-10,-10,-20 },
        { -10,  0,  0,  0,  0,  0,  0,-10 },
        { -10,  0,  5,  5,  5,  5,  0,-10 },
        {  -5,  0,  5,  5,  5,  5,  0, -5 },
        {   0,  0,  5,  5,  5,  5,  0, -5 },
        { -10,  5,  5,  5,  5,  5,  0,-10 },
        { -10,  0,  5,  0,  0,  0,  0,-10 },
        { -20,-10,-10, -5, -5,-10,-10,-20 },
    };

    // Orta oyun için: köşede/arka sırada kalma teşvik edilir (rok yapılmış şah).
    private static final int[][] KING = {
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -20,-30,-30,-40,-40,-30,-30,-20 },
        { -10,-20,-20,-20,-20,-20,-20,-10 },
        {  20, 20,  0,  0,  0,  0, 20, 20 },
        {  20, 30, 10,  0,  0, 10, 30, 20 },
    };
}
