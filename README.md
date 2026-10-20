# CineBox TV (Android TV başlangıç projesi)

**Durum:** Kaynak proje. Derlenmiş/test edilmiş APK değildir.

## Gereksinimler
- Android Studio (JDK 17; Android SDK 35)
- İnternet bağlantısı olan Android TV / Google TV (Android 6+)
- TMDB API Read Access Token (v4 bearer token) - https://www.themoviedb.org/settings/api

## Android Studio ile APK oluşturma
1. `CineBoxTV` klasörünü Android Studio'da **Open** ile açın.
2. Proje kökündeki `gradle.properties` dosyasının en altına şunu yazın:
   `TMDB_BEARER_TOKEN=BURAYA_TMDB_V4_READ_ACCESS_TOKEN`
3. Gradle Sync işlemini yapın. Gerekirse Android Studio'nun önerdiği SDK bileşenlerini yükleyin.
4. **Build > Build APK(s)** deyin.
5. Üretilen APK: `app/build/outputs/apk/debug/app-debug.apk`.
6. APK'yı Android TV'ye ADB ile aktarın: `adb install -r app-debug.apk` veya TV'nizde izin verilen APK yükleme yöntemiyle kurun.

## Mevcut yetenekler
- Gerçek TMDB haftalık film listesi, Türkçe film arama, açıklama ve afiş
- Favoriler (cihazda SharedPreferences), 1–10 puanlama, beğenilen türden öneri
- Media3 ExoPlayer ile örnek MP4 oynatma; TV uzaktan kumanda odaklanabilir Compose butonlar

## Bilinen eksikler / sonraki sürümler
- Favoriler yalnız mevcut oturumda yüklenen film kartlarından listeleniyor; sonraki sürümde favoriler TMDB ID ile yeniden çekilmeli.
- İzlemeye devam et/oynatma pozisyonu kaydı henüz yok.
- Her film için gerçek yayın akışı yok; **'Oynatıcıyı test et' yalnızca Big Buck Bunny demo videosunu açar**.
- Özel oynatma kaynakları, lisanslı servis entegrasyonları, altyazı seçimi, dizi/sezonlar, kullanıcı profilleri ve çok satırlı ana sayfa henüz yok.
- TMDB v4 Read Access Token uygulama içine gömülür. Herkese açık dağıtım için güvenli sunucu proxy/anahtar yönetimi gerekir.
- Uygulamanın ticari dağıtımında TMDB'nin yazılı ticari lisans gerekliliklerini kontrol edin.
- Kod Android SDK/Gradle bulunmayan ortamda hazırlanmıştır, cihazda test edilmemiştir.

## İçerik politikası
Kaynak sağlayıcıdan açık izinli MP4/HLS akışlarını veya kendi videolarınızı oynatın. TMDB film videolarını sağlamaz. DRM'yi veya erişim kurallarını atlatmak için kullanılmamalıdır.

## GitHub üzerinden APK derleme (Android Studio gerekmez)
1. ZIP dosyasını çıkarın ve `CineBoxTV` klasörünün **içindeki dosyaları** yeni bir GitHub repository'sine yükleyin. `.github/workflows/android-apk.yml` gizli klasörü mutlaka dahil olsun.
2. GitHub repository ekranında **Actions → CineBox TV Android APK → Run workflow** seçeneğini açın. `main` veya `master` dalını seçin.
3. Çalışma başarılı olursa ilgili **Actions** çalıştırmasının **Artifacts** kısmından `CineBoxTV-debug-APK` dosyasını indirin.
4. İçinden çıkan `app-debug.apk` dosyasını Android telefonunuza yükleyin (APK yükleme izni gerekebilir).
5. TMDB film bilgilerini etkinleştirmek için repository **Settings → Secrets and variables → Actions → New repository secret** yolundan `TMDB_BEARER_TOKEN` adlı secret ekleyin ve yeniden derleyin. Anahtar girmezseniz katalog boş kalır ama oynatıcı test ekranı için film seçmeniz gerektiğinden ilk test için anahtar önerilir.

**Not:** Bu otomatik derleme iş akışı hazırlanmıştır ama bu ortamda çalıştırılamadığı için başarılı APK çıkacağı henüz doğrulanmamıştır. Debug APK test amaçlıdır; Google Play sürümü değildir.
