package com.chess.theme;

import javafx.scene.paint.Color;

public final class Themes {

    // 1 ── KLASIK: Sıcak ceviz ahşap
    public static final BoardTheme CLASSIC = new BoardTheme(
        "classic", "Klasik", BoardTheme.PieceStyle.SHADOWED,
        Color.web("#f0d9b5"),          // light kare — krem
        Color.web("#b58863"),          // dark kare  — ceviz kahvesi
        Color.web("#f6f669", 0.62),    // seçim
        Color.web("#20a020", 0.72),    // hamle ipucu
        Color.web("#e03030", 0.95),    // yakalama halkası
        Color.web("#ff1818"),          // şah halkası
        Color.web("#fffcf0"),          // beyaz taş — fildişi
        Color.web("#180800"),          // siyah taş — espresso
        Color.web("#2e1b0e"),          // arka plan
        Color.web("#1a0f06"),          // panel
        Color.web("#8b6914"),          // panel kenarlık
        Color.web("#e8d5b0"),          // metin birincil
        Color.web("#6a5030"),          // metin ikincil
        Color.web("#d4af37")           // vurgu — altın
    );

    // 2 ── MİNİMALİST: Çağdaş yeşil-gri
    public static final BoardTheme MINIMALIST = new BoardTheme(
        "minimalist", "Minimalist", BoardTheme.PieceStyle.CLEAN,
        Color.web("#eeeed2"),          // light — sıcak beyaz
        Color.web("#769656"),          // dark  — orman yeşili
        Color.web("#baca44", 0.68),    // seçim
        Color.web("#baca44", 0.78),    // ipucu
        Color.web("#e03030", 0.90),    // yakalama halkası
        Color.web("#ff0000"),          // şah halkası
        Color.web("#ffffff"),          // beyaz taş
        Color.web("#1c1c1c"),          // siyah taş
        Color.web("#302e2b"),          // arka plan
        Color.web("#262421"),          // panel
        Color.web("#4a4840"),          // panel kenarlık
        Color.web("#ddd9d0"),          // metin birincil
        Color.web("#807870"),          // metin ikincil
        Color.web("#769656")           // vurgu
    );

    // 3 ── FANTEZİ: Gizemli mor-altın
    public static final BoardTheme FANTASY = new BoardTheme(
        "fantasy", "Fantezi", BoardTheme.PieceStyle.GLOWING,
        Color.web("#d8c8f0"),          // light — lavanta
        Color.web("#6a1b9a"),          // dark  — derin mor
        Color.web("#ffd700", 0.58),    // seçim
        Color.web("#ffd700", 0.80),    // ipucu
        Color.web("#ff6d00", 0.95),    // yakalama halkası
        Color.web("#ff1744"),          // şah halkası
        Color.web("#ffd700"),          // beyaz taş — saf altın
        Color.web("#b0c4de"),          // siyah taş — gümüş-çelik
        Color.web("#1a0030"),          // arka plan
        Color.web("#110022"),          // panel
        Color.web("#7b1fa2"),          // panel kenarlık
        Color.web("#e1bee7"),          // metin birincil
        Color.web("#9c27b0"),          // metin ikincil
        Color.web("#ab47bc")           // vurgu
    );

    // 4 ── CAM: Derin okyanus mavisi
    public static final BoardTheme GLASS = new BoardTheme(
        "glass", "Cam", BoardTheme.PieceStyle.CLEAN,
        Color.web("#b3d4f0"),          // light — gökyüzü mavisi
        Color.web("#1055a0"),          // dark  — safir
        Color.web("#00e5ff", 0.50),    // seçim
        Color.web("#00e5ff", 0.78),    // ipucu
        Color.web("#ff5722", 0.95),    // yakalama halkası
        Color.web("#ff1744"),          // şah halkası
        Color.web("#e3f2fd"),          // beyaz taş — buz beyazı
        Color.web("#0a1628"),          // siyah taş — lacivert
        Color.web("#071220"),          // arka plan
        Color.web("#040c18"),          // panel
        Color.web("#0d47a1"),          // panel kenarlık
        Color.web("#e3f2fd"),          // metin birincil
        Color.web("#5599cc"),          // metin ikincil
        Color.web("#039be5")           // vurgu
    );

    // 5 ── FÜTÜRİSTİK: Neon siber alem
    public static final BoardTheme FUTURISTIC = new BoardTheme(
        "futuristic", "Fütüristik", BoardTheme.PieceStyle.NEON,
        Color.web("#070d1a"),          // light — zifiri karanlık 1
        Color.web("#0e1830"),          // dark  — zifiri karanlık 2
        Color.web("#bf5fff", 0.60),    // seçim
        Color.web("#00e5ff", 0.88),    // ipucu
        Color.web("#ff1493", 0.95),    // yakalama halkası
        Color.web("#ff0000"),          // şah halkası
        Color.web("#00e5ff"),          // beyaz taş — elektrik mavisi
        Color.web("#ff1493"),          // siyah taş — neon pembe
        Color.web("#020610"),          // arka plan
        Color.web("#010408"),          // panel
        Color.web("#2a0060"),          // panel kenarlık
        Color.web("#00e5ff"),          // metin birincil
        Color.web("#6050a0"),          // metin ikincil
        Color.web("#bf5fff")           // vurgu
    );

    public static final BoardTheme[] ALL = {CLASSIC, MINIMALIST, FANTASY, GLASS, FUTURISTIC};

    private Themes() {}
}
