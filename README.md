# CineBox TV v0.5 — Büyük Tavşan HTTP hatası

## Değişiklikler
- APK içindeki eski Büyük Tavşan Google Storage bağlantısı yapılan kontrolde HTTP 403 döndürdü.
- Büyük Tavşan artık W3Schools kısa test klibini kullanır; HTTP hatasında test-videos.co.uk üzerindeki 10 saniyelik klibe bir kez geçer. Bunlar tam film değildir.
- İki yeni URL de 27 Ekim 2026 kontrolünde Range isteğine HTTP 206 ve MP4 başlığı döndürdü. Bu kontrol TV'de tam oynatma testi değildir.
- Hata ekranı gerçek HTTP kodunu, açıklamayı, sunucu adını ve Media3 hata kodunu gösterir. URL sorgu parametreleri ekrana yazılmaz.
- Yeniden dene ve Baştan dene düğmeleri eklendi.
- HTTP bağlantı ve okuma zaman aşımı ayarları ve CineBoxTV User-Agent eklendi.
- Oynatma konumu artık kaynak URL'sine göre ayrı tutulur. Eski sürümdeki devam etme konumları aktarılmaz; favoriler ve puanlama değişmedi.
- Diğer katalog kaynakları ve Wikimedia arama mantığı değiştirilmedi.

## APK oluşturma
Bu ZIP kaynak kodudur, kurulabilir APK değildir. Bu ortamda Android SDK ve Gradle bulunmadığından derleme ve Android TV cihaz testi yapılmadı.
1. ZIP'i açın; içeriğini mevcut GitHub projenizin köküne kopyalayın. `.github` klasörü de dahil olmalıdır.
2. GitHub Desktop üzerinden Commit ve Push yapın.
3. GitHub → Actions → CineBox TV Android APK çalışmasının tamamlanmasını bekleyin.
4. Artifacts → CineBoxTV-debug-APK paketini indirin; içindeki app-debug.apk dosyasını TV'ye yükleyin.
5. Büyük Tavşan'ı açın. Hata sürerse yeni ekrandaki HTTP kodunun fotoğrafını paylaşın.

Android Studio ile de proje açılıp Gradle eşitlemesinden sonra Build menüsünden APK oluşturulabilir.

## Test planı
- Büyük Tavşan kısa klibinin görüntü/ses oynatması.
- İlk kaynak HTTP hatası verirse yalnız bir kez yedek klibe geçmesi.
- İki kaynak da başarısızsa hata ekranında kalması, sonsuz döngü olmaması.
- Hatalı manuel URL'de gerçek HTTP kodunun görünmesi.
- Yeniden deneme, baştan deneme ve oynatıcıdan geri dönüş.

## Kaynak ve lisans
Big Buck Bunny: Blender Foundation, https://www.bigbuckbunny.org/ — CC BY 3.0.
Birincil test klibi: https://www.w3schools.com/html/mov_bbb.mp4
Yedek 10 saniyelik test klibi: https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4

TMDB/API anahtarı gerekmez. Arama Wikimedia Commons açık lisanslı videolarına yöneliktir; ticari film kataloğu değildir.
