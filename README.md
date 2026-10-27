ALICOTV v25 Fix1 (0.25.1)

Gönderilen derleme kaydındaki 9 adet Unresolved reference adFilterEnabled hatası düzeltildi. WebPlayerActivity içinde eksik olan @Volatile Boolean alanı geri eklendi. Ayar varsayılan olarak açık kalır ve kayıtlı tercih onCreate içinde okunur.
Arama, sayfalama, tam ekran ve içerikler değiştirilmedi.
ZIP içindeki proje dosyalarını mevcut GitHub deposunun köküne aktarın. Özellikle app/src/main/java/com/cinebox/tv/WebPlayerActivity.kt dosyasını değiştirin. Sonra Actions derlemesini yeniden çalıştırın. ZIP'i sadece depoya yüklemek kaynak dosyalarını güncellemez.
Kaynak karşılaştırmasıyla değişiklik kontrol edildi. Bu ortamda Android SDK/Gradle bulunmadığından APK derlemesi yapılmadı.

ALICOTV v25

İsim araması: Yazdıkça seçili kategoride başlığın herhangi bir yerinde geçen metni bulur. Türkçe büyük/küçük harf kuralları kullanılır. Tümü tüm içerikleri, Diziler yerli/yabancı dizileri, Filmler yalnız filmleri arar. Favoriler ve Öneriler sekmeleri kendi listeleri içinde arar. Temizle aramayı sıfırlar.
Sayfalama: Sonuçlar Türkçe alfabetik sıralanır, sayfa başına 20 kayıt gösterilir. Önceki/Sonraki ve sayfa sayacı bulunur. Arama, kategori veya sekme değişince ilk sayfaya dönülür.
Tam ekran: Web sayfasını büyütme kaldırıldı. Görünür videolar arasında oynayan video tercih edilir; bilinen oynatıcıların kendi tam ekran düğmesi veya videonun fullscreen API'si tetiklenir. Aynı kökenden erişilebilir gömülü oynatıcılar da kontrol edilir. Başka alan adındaki iframe veya kullanıcı dokunuşu zorunluluğu otomatik geçişi engelleyebilir. Bu durumda sayfa büyütülmez; videonun kendi tam ekran düğmesine basmanız istenir. Geri tuşu gerçek video tam ekranından çıkar.
Önceki 365 içerik ve v24 özellikleri korunur.

Kurulum: Projenin tamamını .github dahil GitHub deposuna aktarın; Actions üzerinden APK oluşturun.
Kontrol: 5 JavaScript tam ekran davranış testi geçti; kataloglar byte karşılaştırmasıyla korundu. Android SDK/Gradle olmadığı için APK derlemesi ve cihazda oynatma/odak testi yapılamadı.
