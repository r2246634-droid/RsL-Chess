package com.chess.i18n;

import java.util.HashMap;
import java.util.Map;

/**
 * Basit, tek dosyalık çeviri tablosu. Proje harici kaynak dosyası (ResourceBundle vb.)
 * kullanmaz; derleme scriptleri yalnızca *.java dosyalarını derlediği için (bkz. run.bat)
 * bu, ek bir yapı-betiği değişikliği gerektirmeyen en basit çözümdür.
 *
 * Kullanım: I18n.t("menu.select_mode")  → mevcut dile göre çevrilmiş metni döndürür.
 * Dil değişimi anlıktır: I18n.set(Lang.EN) çağrıldıktan sonra oluşturulan her ekran
 * (MainMenu.show(), GameScreen.show() ...) yeni dille inşa edilir. Ekranlar statik
 * olduğundan ve her navigasyonda sıfırdan kurulduğundan, ayrıca bir "dinleyici" mekanizmasına
 * gerek yoktur — dil değiştiren ekran kendini yeniden çizer (bkz. MainMenu).
 */
public final class I18n {

    private static volatile Lang current = Lang.TR;

    private I18n() {}

    public static void set(Lang lang) { current = lang; }
    public static Lang current()      { return current; }

    /** Verilen anahtarın mevcut dildeki karşılığını döndürür; bulunamazsa anahtarın kendisini. */
    public static String t(String key) {
        String[] row = TABLE.get(key);
        if (row == null) return key;
        String value = row[current.ordinal()];
        return value != null ? value : key;
    }

    /** "{RENK} kazandı! — {sebep}" kalıbını kurar (üç dilde de sözcük sırası aynıdır). */
    public static String win(String winnerColor, String reason) {
        return winnerColor + " " + t("result.wins") + " — " + reason;
    }

    private static final Map<String, String[]> TABLE = new HashMap<>();

    // sıra: TR, EN, RU (Lang enum sırasıyla aynı)
    private static void put(String key, String tr, String en, String ru) {
        TABLE.put(key, new String[]{tr, en, ru});
    }

    static {
        // ── Genel / renkler ─────────────────────────────────────────────────
        put("color.white", "BEYAZ", "WHITE", "БЕЛЫЕ");
        put("color.black", "SİYAH", "BLACK", "ЧЁРНЫЕ");

        // ── Oyun sonucu mesajları (GameController) ──────────────────────────
        put("result.wins",       "kazandı!",              "wins!",                   "победили!");
        put("result.timeout",    "Süre bitti",             "Time's up",               "Время истекло");
        put("result.checkmate",  "ŞAH MAT",                "CHECKMATE",               "МАТ");
        put("result.stalemate",  "PAT — Beraberlik!",      "STALEMATE — Draw!",       "ПАТ — Ничья!");
        put("result.fiftyMove",  "50 HAMLE KURALI — Beraberlik!", "FIFTY-MOVE RULE — Draw!", "ПРАВИЛО 50 ХОДОВ — Ничья!");
        put("result.repetition", "ÜÇLÜ TEKRAR — Beraberlik!", "THREEFOLD REPETITION — Draw!", "ТРОЙНОЕ ПОВТОРЕНИЕ — Ничья!");
        put("result.insufficientMaterial", "YETERSİZ MATERYAL — Beraberlik!", "INSUFFICIENT MATERIAL — Draw!", "НЕДОСТАТОЧНО МАТЕРИАЛА — Ничья!");
        put("result.timeoutVsInsufficient", "SÜRE BİTTİ, rakibin mat materyali yok — Beraberlik!", "TIME'S UP vs insufficient material — Draw!", "ВРЕМЯ ИСТЕКЛО при недостатке материала — Ничья!");
        put("result.resignation", "İSTİFA", "RESIGNATION", "СДАЧА");
        put("result.drawAgreed", "Karşılıklı anlaşma — Beraberlik!", "Draw by agreement!", "Ничья по соглашению!");
        put("result.disconnected", "Bağlantı kesildi",     "Disconnected",            "Соединение разорвано");

        // ── Oyun modu / süre etiketleri (GameConfig) ────────────────────────
        put("mode.two_player",     "2 Kişilik",           "2 Player",           "2 игрока");
        put("mode.ai_easy",        "Standart AI",         "Standard AI",        "Стандартный ИИ");
        put("mode.ai_medium",      "Orta Zorluk AI",      "Medium AI",          "ИИ (средний)");
        put("mode.ai_expert",      "Uzman AI",            "Expert AI",          "ИИ (эксперт)");
        put("mode.network_host",   "Çevrimiçi (Host)",    "Online (Host)",      "Онлайн (Хост)");
        put("mode.network_client", "Çevrimiçi (Misafir)", "Online (Guest)",     "Онлайн (Гость)");
        put("timer.unlimited",     "Süresiz",             "Unlimited",          "Без ограничений");

        // ── Splash ekranı ────────────────────────────────────────────────────
        put("splash.subtitle", "Java Satranç Motoru", "Java Chess Engine", "Шахматный движок на Java");

        // ── Ana menü ─────────────────────────────────────────────────────────
        put("menu.select_mode", "Mod Seçin", "Select Mode", "Выберите режим");

        put("menu.two_player.title", "♟  2 Kişilik", "♟  2 Player", "♟  2 игрока");
        put("menu.two_player.desc",
                "Aynı bilgisayarda iki oyuncu",
                "Two players on the same computer",
                "Два игрока на одном компьютере");

        put("menu.ai_easy.title", "🤖  Standart AI", "🤖  Standard AI", "🤖  Стандартный ИИ");
        put("menu.ai_easy.desc",
                "Kolay — Yeni başlayanlar",
                "Easy — For beginners",
                "Легко — для новичков");

        put("menu.ai_medium.title", "⚙  Orta Zorluk AI", "⚙  Medium AI", "⚙  ИИ (средний)");
        put("menu.ai_medium.desc",
                "Dengeli — Orta seviye",
                "Balanced — Intermediate level",
                "Сбалансированно — средний уровень");

        put("menu.ai_expert.title", "🏆  Uzman AI", "🏆  Expert AI", "🏆  ИИ (эксперт)");
        put("menu.ai_expert.desc",
                "En zor — Deneyimli oyuncular",
                "Hardest — Experienced players",
                "Сложно — для опытных игроков");

        put("menu.online.title", "🌐  Çevrimiçi", "🌐  Online", "🌐  Онлайн");
        put("menu.online.desc",
                "TCP üzerinden 2 oyuncu",
                "2 players over TCP",
                "2 игрока через TCP");

        put("menu.timer_control", "⏱  Süre Kontrolü", "⏱  Time Control", "⏱  Контроль времени");
        put("timer.unlimited_full", "∞  Süresiz", "∞  Unlimited", "∞  Без ограничений");
        put("timer.bullet", "⚡ Bullet  1+1", "⚡ Bullet  1+1", "⚡ Буллит  1+1");
        put("timer.blitz",  "🔥 Blitz  5+3",  "🔥 Blitz  5+3",  "🔥 Блиц  5+3");
        put("timer.rapid",  "🐢 Rapid  10+5", "🐢 Rapid  10+5", "🐢 Рапид  10+5");

        put("menu.exit", "✕  Çıkış", "✕  Exit", "✕  Выход");
        put("menu.language", "🌍 Dil", "🌍 Language", "🌍 Язык");
        put("menu.version", "v3.0  •  Java + JavaFX", "v3.0  •  Java + JavaFX", "v3.0  •  Java + JavaFX");

        put("theme.abbr.classic",    "Klas", "Clas", "Клас");
        put("theme.abbr.minimalist", "Mini", "Mini", "Мини");
        put("theme.abbr.fantasy",    "Fan",  "Fant", "Фэнт");
        put("theme.abbr.glass",      "Cam",  "Glas", "Стек");
        put("theme.abbr.futuristic", "Füt",  "Futr", "Футр");

        // ── Çevrimiçi menü ───────────────────────────────────────────────────
        put("online.title", "🌐  Çevrimiçi Oyun", "🌐  Online Game", "🌐  Онлайн-игра");
        put("online.player_name", "Oyuncu Adı:", "Player Name:", "Имя игрока:");
        put("online.default_name", "Oyuncu", "Player", "Игрок");
        put("online.time", "Süre:", "Time:", "Время:");
        put("online.host_section", "Host (BEYAZ)", "Host (WHITE)", "Хост (БЕЛЫЕ)");
        put("online.guest_section", "Misafir (SİYAH)", "Guest (BLACK)", "Гость (ЧЁРНЫЕ)");
        put("online.waiting", "Bekleniyor...", "Waiting...", "Ожидание...");
        put("online.start_server", "🖥  Sunucu Başlat (BEYAZ)", "🖥  Start Server (WHITE)", "🖥  Запустить сервер (БЕЛЫЕ)");
        put("online.host_ip", "Host IP:", "Host IP:", "IP хоста:");
        put("online.connect", "🔌  Sunucuya Bağlan (SİYAH)", "🔌  Connect to Server (BLACK)", "🔌  Подключиться (ЧЁРНЫЕ)");
        put("online.connecting", "Bağlanıyor...", "Connecting...", "Подключение...");
        put("online.connect_failed", "Bağlantı başarısız: ", "Connection failed: ", "Ошибка подключения: ");
        put("online.back", "← Geri", "← Back", "← Назад");

        // ── Oyun ekranı ──────────────────────────────────────────────────────
        put("game.back_to_menu", "← Menü", "← Menu", "← Меню");
        put("game.black_label", "SİYAH ♟", "BLACK ♟", "ЧЁРНЫЕ ♟");
        put("game.white_label", "BEYAZ ♙", "WHITE ♙", "БЕЛЫЕ ♙");
        put("game.moves_header", "  HAMLELER", "  MOVES", "  ХОДЫ");
        put("game.turn_prefix", "Sıra: ", "Turn: ", "Ход: ");
        put("game.check_suffix", "  ŞAH!", "  CHECK!", "  ШАХ!");
        put("game.ai_thinking", "AI düşünüyor...", "AI is thinking...", "ИИ думает...");
        put("game.opponent_waiting", "Rakip bekliyor...", "Waiting for opponent...", "Ожидание соперника...");
        put("game.chat_header", "  SOHBET", "  CHAT", "  ЧАТ");
        put("game.chat_prompt", "Mesaj yaz...", "Type a message...", "Введите сообщение...");
        put("game.over_title", "Oyun Bitti", "Game Over", "Игра окончена");
        put("game.over_content",
                "Oyun geçmişi kaydedildi.\nYeni oyun için menüye dönün.",
                "Game history saved.\nReturn to the menu for a new game.",
                "История игры сохранена.\nВернитесь в меню для новой игры.");

        put("game.resign", "🏳 İstifa", "🏳 Resign", "🏳 Сдаться");
        put("game.offer_draw", "🤝 Beraberlik Teklif Et", "🤝 Offer Draw", "🤝 Предложить ничью");
        put("game.undo", "↶ Geri Al", "↶ Undo", "↶ Отменить");
        put("game.resign_confirm_title", "İstifa", "Resign", "Сдаться");
        put("game.resign_confirm_content", "İstifa etmek istediğinizden emin misiniz?",
                "Are you sure you want to resign?", "Вы уверены, что хотите сдаться?");
        put("game.draw_confirm_title", "Beraberlik Teklifi", "Draw Offer", "Предложение ничьей");
        put("game.draw_confirm_content", "Rakip beraberlik teklif etti. Kabul ediyor musunuz?",
                "Your opponent offers a draw. Do you accept?", "Соперник предлагает ничью. Принимаете?");
        put("game.draw_declined", "Beraberlik teklifi reddedildi.", "Draw offer declined.", "Предложение ничьей отклонено.");
        put("game.draw_declined_by_ai", "Yapay zeka beraberlik teklifini reddetti.",
                "The AI declined the draw offer.", "ИИ отклонил предложение ничьей.");

        put("piece.queen", "Vezir", "Queen", "Ферзь");
        put("piece.rook", "Kale", "Rook", "Ладья");
        put("piece.bishop", "Fil", "Bishop", "Слон");
        put("piece.knight", "At", "Knight", "Конь");
        put("promotion.title", "Terfi", "Promotion", "Превращение");
        put("promotion.desc", "Piyonunuz hangi taşa dönüşsün?", "Choose a piece for your pawn",
                "Выберите фигуру для пешки");

        // ── Oyun geçmişi dosyası (GameHistory) ───────────────────────────────
        put("history.title",  "RsL Chess - Oyun Gecmisi", "RsL Chess - Game History", "RsL Chess - История игры");
        put("history.date",   "Tarih", "Date", "Дата");
        put("history.mode",   "Mod", "Mode", "Режим");
        put("history.result", "Sonuc", "Result", "Результат");
        put("history.capture", " (yakalama)", " (capture)", " (взятие)");
    }
}
