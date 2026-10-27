# CineBox TV v0.7 — Türkçe dublaj kaynakları

Ana sayfadaki örnek kartlar üç tam film kaydıyla değiştirildi:
- Arizona Haydutları / Robbers’ Roost (1955): yaklaşık 80 dakika.
- 30 Winchester per El Diablo (1965): yaklaşık 85 dakika.
- Yalnız Kovboy / Ride Lonesome (1959): yaklaşık 73 dakika.

Üç kaydın başlığı Türkçe dublaj, dil alanı tur olarak işaretlidir. Ses dinlenerek ayrıca doğrulanmadı. Videolar paketin içinde değildir; Internet Archive MP4 bağlantıları üzerinden oynatılır. Her karta ayrı film kimliği, kaynak ve yerel kapak eklendi. Başka film veya test videosuna otomatik geçiş yoktur.

## Kurulum
ZIP içeriğini mevcut GitHub projesinin köküne kopyalayın (.github dahil). GitHub Desktop → Commit → Push. Actions → CineBox TV Android APK çalışması başarıyla bitince Artifacts → CineBoxTV-debug-APK paketini indirin. İçindeki app-debug.apk dosyasını TV'ye yükleyin.

## Korunan özellikler
Favoriler, kişisel puanlar, manuel video bağlantısı, VTT altyazı, ayrıntılı HTTP hata ekranı, yeniden/baştan deneme korunur. İnternet araması hâlâ Wikimedia Commons kullanır; genel Türkçe film arama motoruna dönüştürülmedi. Yeni filmler eski örnek kartların favori/puan kimliklerini kullanmaz.

## Doğrulama ve sınırlar
27 Ekim 2026: Her MP4 için ilk 1024 bayt Range isteğiyle alındı. Üçü de HTTP 206, video/mp4 ve geçerli MP4 ftyp başlığı döndürdü. Kapaklar JPEG olarak doğrulandı. Süre ve ses dili kaynak metadata alanlarından alındı. Tam oynatma, ses dinleme ve TV cihaz testi yapılmadı.
Android SDK/Gradle bu ortamda bulunmadığı için APK derlenmedi. Bu ZIP kaynak kodudur. Ağ erişimi cihaz/ülke bazında farklı olabilir; bağlantılar sonradan değişebilir. Lisans durumu kaynakta belirtilmemiştir; açık lisans iddiası yoktur. Kaynaklar SOURCES.md ve SOURCE_CHECKS.json içindedir.
