package com.chess.game;

import com.chess.core.Board;
import com.chess.core.Coordinate;
import com.chess.pieces.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Standart Cebirsel Notasyon (SAN) üretir — Nf3, exd5, O-O, Qxh7# gibi.
 * `boardBeforeMove` hamle uygulanmadan ÖNCEKİ tahtadır (taş tipi ve
 * belirsizlik-giderme için); şah/mat bayrakları hamle uygulandıktan sonra
 * hesaplanıp çağıran tarafından verilir.
 */
public final class SanNotation {

    private SanNotation() {}

    /**
     * Şah/mat son eki HARİÇ SAN gövdesini döndürür — bunlar hamle uygulanıp
     * karşı tarafın durumu belli olana kadar bilinemez; çağıran taraf
     * (GameController) "+"/"#" ekini kendisi sona ekler.
     */
    public static String toSan(Board boardBeforeMove, Coordinate from, Coordinate to,
                                boolean isCapture, String promotionType) {
        Piece piece = boardBeforeMove.getPiece(from);

        if ("King".equals(piece.getType()) && Math.abs(to.col() - from.col()) == 2) {
            return to.col() > from.col() ? "O-O" : "O-O-O";
        }

        StringBuilder sb = new StringBuilder();
        if ("Pawn".equals(piece.getType())) {
            if (isCapture) sb.append((char) ('a' + from.col())).append('x');
            sb.append(GameHistory.toAlgebraic(to.row(), to.col()));
            if (promotionType != null) sb.append('=').append(pieceLetter(promotionType));
        } else {
            sb.append(pieceLetter(piece.getType()));
            sb.append(disambiguate(boardBeforeMove, piece, from, to));
            if (isCapture) sb.append('x');
            sb.append(GameHistory.toAlgebraic(to.row(), to.col()));
        }
        return sb.toString();
    }

    /**
     * Aynı tip/renkte, aynı hedef kareye ulaşabilen başka bir taş varsa
     * dosya/sıra harfiyle ayırt eder. Basit (rok/geçerken alma içermeyen)
     * hamleler üzerinden çalışır — At/Fil/Kale/Vezir için yeterlidir
     * (Şah zaten tek, Piyon ayrı ele alınır).
     */
    private static String disambiguate(Board board, Piece movingPiece, Coordinate from, Coordinate to) {
        List<Coordinate> others = new ArrayList<>();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Coordinate pos = new Coordinate(r, c);
                if (pos.equals(from)) continue;
                Piece p = board.getPiece(pos);
                if (p == null || !p.getType().equals(movingPiece.getType())
                        || !p.getColor().equals(movingPiece.getColor())) continue;
                for (Coordinate m : p.calculateMoves(board)) {
                    if (m.equals(to)) { others.add(pos); break; }
                }
            }
        }
        if (others.isEmpty()) return "";

        boolean sameFile = others.stream().anyMatch(p -> p.col() == from.col());
        boolean sameRank = others.stream().anyMatch(p -> p.row() == from.row());
        if (!sameFile) return String.valueOf((char) ('a' + from.col()));
        if (!sameRank) return String.valueOf(8 - from.row());
        return String.valueOf((char) ('a' + from.col())) + (8 - from.row());
    }

    private static char pieceLetter(String type) {
        switch (type) {
            case "Knight": return 'N';
            case "Bishop": return 'B';
            case "Rook":   return 'R';
            case "Queen":  return 'Q';
            case "King":   return 'K';
            default:       return '?';
        }
    }
}
