# CineBox TV v0.11 — Eski diziler ve reklam filtresi

## Ayrılsak da Beraberiz
Türk Dizileri → Ayrılsak da Beraberiz (1999).
Janset ve Hakan Yılmaz’lı eski serinin üçüncü taraf arşiv sayfası eklendi. Sayfa 40 bölüm listeler; tüm seri veya bu bölümlerin hepsinin 1999 yılında yayınlandığı iddia edilmez. Ayrıca Dizi Arşivi kanalındaki 1. bölüm için YouTube kısayolu vardır. 2015 yeniden çevrimi eklenmedi. Bölümler tek tek oynatılarak test edilmedi.

## Reklam filtresi
Uygulama içindeki web oynatıcıda varsayılan olarak açık:
- 11 bilinen reklam alan adına ve alt alanlarına yapılan kaynak isteklerini boş yanıtla engeller.
- Aynı alan adlarına yapılan sayfa yönlendirmelerini engeller.
- Yeni/açılır pencereleri reddeder.
- Üst araç çubuğunda Reklam filtresi: Açık/Kapalı düğmesi vardır. Tercih saklanır ve değişince sayfa yenilenir. Video açılmazsa filtreyi kapatıp deneyin. Açılır pencereler önceki sürümlerde olduğu gibi kapalı kalır.

Sınırlar: Bu küçük bir yerel alan adı filtresidir, kapsamlı veya otomatik güncellenen bir reklam listesi değildir. Tüm reklamları engellemez. Video akışına gömülen, yayıncıyla aynı kaynaktan gelen veya YouTube’un kendi reklamlarını kaldırdığı iddia edilmez. HTTPS/DRM/erişim engelleri aşılmaz. Tarayıcıda veya YouTube’da aç ile uygulamadan çıkıldığında filtre etkili değildir. Mevcut MP4 oynatıcıya ayrıca reklam eklenmez.

## Doğrulama
Reklam alan adı eşleştirme kodu Java derleyicisiyle derlendi; 13 test geçti (tam alan, alt alan, büyük/küçük harf, sondaki nokta, benzer ama farklı alan, video CDN’lerinin korunması, boş/null). Android entegrasyonu, manifest, katalog ve ZIP statik kontrol edildi. Android SDK/Gradle bulunmadığı için APK derlenmedi; TV’de reklam engelleme ve video testi yapılmadı.

## APK
ZIP içeriğini mevcut GitHub proje köküne kopyalayın (.github dahil). Commit → Push. Actions → CineBox TV Android APK → Artifacts → CineBoxTV-debug-APK içindeki APK’yı alın. Sürüm kodu 11. Önceki içerikler ve Yaprak Dökümü sezon bağlantıları korunur.
