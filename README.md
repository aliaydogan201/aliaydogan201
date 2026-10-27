# CineBox TV v0.9 — Web oynatıcı ve video kategorileri

## Yeni içerikler
- Guguk Kuşu (1975): kullanıcının verdiği fullhdfilmizlesene.now sayfası. Web oynatma denenmek üzere eklendi; site bu ortamda HTTP 403 verdi. TV’de erişim garanti edilmez.
- Belgesel: TRT Belgesel — Su Savaşları bölüm seçim sayfası. Sayfa açılınca bölüm seçilir.
- Maç özeti: TRT Spor — Türkiye–Gürcistan, EURO 2024, 18 Haziran 2024.
- Müzik: Tarkan’ın resmî sitesinden bağlantıları doğrulanan Şımarık ve Kuzu Kuzu klipleri.
- Önceki beş MP4 film korunur.

## Nasıl kullanılır?
Ana sayfada Tümü / Film / Belgesel / Maç Özeti / Müzik filtrelerinden seçin. İçerik kartını açıp “Web oynatıcıda aç” düğmesine basın. İçeriğin yayıncı sayfası uygulama içinde açılır; sayfadaki oynat düğmesini seçin. Kumandayla web alanına geçmek için “Oynatıcıya geç” düğmesi vardır. Web sayfasının kumanda uyumu yayıncıya bağlıdır; bazı sayfalarda fare gerekebilir.

Üst çubuk: Kapat, Geri, Yenile, Oynatıcıya geç, Tarayıcıda / YouTube’da aç. Yayıncı tam ekran isterse desteklenir; Geri tuşu önce tam ekrandan çıkar, sonra sayfa geçmişine döner. Harici uygulama düğmesi varsa YouTube veya tarayıcıya yönlendirir. Uygulama bulunmazsa mesaj gösterilir.

Web ekranı kaynak sitenin kendi oynatıcısını kullanır. Site engeli, 403, giriş zorunluluğu, coğrafi sınır veya DRM kısıtını kaldırmaz. YouTube videolarından MP4 çıkarılmaz. Web içeriklerinin ilerlemesi CineBox MP4 kayıt sistemiyle tutulmaz; yayıncıya bağlıdır. Video ve müzik dosyaları ZIP’e indirilmedi.

## APK oluşturma
ZIP içeriğini mevcut GitHub projenizin köküne kopyalayın (.github klasörü dahil). Commit → Push. Actions → CineBox TV Android APK → başarılı çalışmanın Artifacts bölümünden CineBoxTV-debug-APK paketini alın. İçindeki app-debug.apk dosyasını TV’ye yükleyin.

## Kontroller ve sınırlar
Android manifest, 10 benzersiz katalog kimliği (5 MP4 + 5 web kaydı), oynatıcı yönlendirmeleri ve ZIP bütünlüğü statik kontrol edildi. Android SDK/Gradle bu ortamda yok: APK derleme ve TV çalıştırma testi yapılmadı. WebView ve üçüncü taraf oynatıcıların çalışması cihazdaki Android System WebView sürümüne ve yayıncıya bağlıdır. Önceki SOURCE_CHECKS.json yalnız önceki MP4 kontrolleridir; web videoları için oynatma testi değildir.

WebView HTTPS başlangıç adresleri kabul eder; yerel dosya erişimi/JavaScript köprüsü yoktur. SSL hataları yok sayılmaz. Kamera/mikrofon izinleri otomatik reddedilir. Açılır yeni pencereler açılmaz; bazı yayıncı sayfaları bu nedenle harici uygulama gerektirebilir.
