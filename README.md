# RsL Chess

## Hakkında

JavaFX ile yazılmış, yapay zeka destekli bir satranç oyunu. Tek bilgisayarda iki oyuncu, yapay zekaya karşı üç zorluk seviyesi, ya da yerel ağ üzerinden iki oyuncu (TCP) olarak oynanabilir.

## Özellikler

- Tam kurallı satranç: rok (kısa/uzun), geçerken alma (en passant), piyon terfisi (Vezir/Kale/Fil/At seçimi), şah/mat/pat tespiti
- Tüm beraberlik kuralları: 50 hamle kuralı, üçlü tekrar, yetersiz materyal
- İstifa, karşılıklı beraberlik teklifi (yapay zekaya karşı materyal durumuna göre kabul/red) ve hamle geri alma
- Gerçek satranç notasyonu (Nf3, O-O, exd5, Qxh7# gibi) — hem hamle listesinde hem kaydedilen dosyada
- Yapay zeka artık pozisyonel değerlendirme de yapıyor (merkez kontrolü, şah güvenliği, vb.) ve mümkün olan en az hamlede mat etmeyi tercih ediyor (kaybediyorsa oyalanır)
- 3 dil desteği: Türkçe, İngilizce, Rusça (ana menüden anında değiştirilebilir)
- 3 zorluk seviyesinde yapay zeka (negamax + alpha-beta budama)
- Yerel ağ üzerinden 2 oyunculu çevrimiçi mod (sohbet, istifa ve beraberlik teklifi senkronize edilir)
- Süreli oyun modları (Bullet, Blitz, Rapid) ve süresiz mod
- 5 farklı görsel tema (Klasik, Minimalist, Fantezi, Cam, Fütüristik)
- Hamle sırasında yalnızca oynanan taşın kaydığı, pürüzsüz animasyonlar
- Prosedürel olarak üretilen ses efektleri (hamle, yakalama, şah, mat)
- Her oyun sonunda `game_history/` klasörüne kaydedilen okunabilir hamle geçmişi

## Kurulum ve Çalıştırma

Bağımlılık yönetimi Maven/Gradle ile değil, doğrudan `javac`/`java` ile yapılır.

1. [JavaFX SDK](https://openjfx.io/)'yi indirin ve `run.bat` içindeki `MODULE_PATH` değişkenini kendi kurulum yolunuzla güncelleyin.
2. Oyunu derleyip başlatın:
   ```
   run.bat
   ```
3. Windows için tek başına çalışan bir `.exe` paketlemek isterseniz:
   ```
   package.bat
   ```

## Mimari Özeti

Ayrıntılı mimari notları için [`CLAUDE.md`](CLAUDE.md) dosyasına bakın.

| Paket | Sorumluluk |
|---|---|
| `com.chess.core` | Tahta, koordinat/hamle kayıtları, oyun kuralları (şah/rok/geçerken alma), zamanlayıcı |
| `com.chess.pieces` | Taş sınıfları |
| `com.chess.game` | Oyun akışı, hamle geçmişi |
| `com.chess.ai` | Yapay zeka (negamax) |
| `com.chess.i18n` | Dil desteği (TR/EN/RU) |
| `com.chess.network` | TCP tabanlı çevrimiçi oyun |
| `com.chess.sound` | Sentezlenmiş ses efektleri |
| `com.chess.theme` | Görsel temalar |
| `com.chess.ui` | JavaFX arayüzü |
