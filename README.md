# CineBox TV 0.2 — TMDB olmadan

TMDB API bağlantısı, TMDB anahtarı ve zorunlu katalog istemi kaldırıldı.
Uygulama, arama/favori/puanlama/öneri ekranlarını denemek için **6 örnek katalog kartıyla** açılır. Bunlar gerçek bir film veritabanı veya lisanslı yayın kaynağı değildir.

**Oynatıcı testi** butonu açık bir örnek MP4 video akışı açar; seçilen film dosyasını açmaz.

## GitHub'a yükleme

ZIP'i açın. İçerideki `app`, `.github`, `gradle.properties`, `settings.gradle.kts`, `build.gradle.kts` ve `README.md` dosyalarını depo köküne koyun (fazladan `CineBoxTV-no-TMDB/` klasörü içine değil). GitHub Desktop ile Commit → Push origin yapın. Actions → CineBox TV Android APK iş akışından derlemeyi çalıştırın.

## Sonraki aşama

Kaynak sistemi için izinli MP4/HLS akışları ve katalog verisi bağlanmalıdır. DRM veya erişim kontrolü aşılmamalıdır.
