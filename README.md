ALICOTV v29 — Oynatıcıya geç düzeltmesi

v28 düğmesi yalnız WebView'e odak veriyordu. Yeni düğme sade görünümü etkinleştirir, oynatıcıyı yeniden arar ve bulunan video/iframe/oynatıcı kontrolüne odak verir. Dizi bölüm listesi açıkken bu düğme mevcut sayfadaki oynatıcıyı arar; normal dizi açılışındaki bölüm listesi davranışı devam eder.
Oynat düğmesine kullanıcı basar. Başka alan adındaki iframe'in iç düğmesine erişim garanti değildir. Kaynak sayfada video yoksa Siteyi göster ile önce bölüm veya kaynak seçilmelidir.
671 içerik, arama ve sayfalama korunur.

Kurulum: Proje dosyalarını .github dahil depo köküne aktarın ve Actions derlemesini çalıştırın.
Doğrulama: JavaScript sözdizimi, XML ve katalogların korunması kontrol edildi. Android SDK/Gradle ve cihaz testi yapılamadı; TV'deki odak davranışı cihazda doğrulanmalıdır.
