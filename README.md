ALICOTV v23

- Uygulama adı ve TV banner: ALICOTV. Paket kimliği korunur; mevcut favoriler silinmez.
- Turuncu arka plan. Kumandayla odaklanan buton: sarı zemin, siyah yazı, beyaz çerçeve. Kartlarda sarı çerçeve.
- Film detayında ilk odak oynat düğmesidir.
- Web ve doğrudan video oynatıcıda Tam ekran düğmesi. Geri ile tam ekrandan çıkılır.
- Web oynatıcıda kumandanın Menü tuşu üst çubuğa geri döndürür.
- Web tam ekran: uygulama çubuklarını gizler. Üst sayfada video varsa sitenin tam ekran API'sini dener. Farklı alan adındaki gömülü oynatıcılarda video içindeki tam ekran düğmesi gerekebilir; sayfa büyütme her videonun kadraja sığmasını garanti etmez.
- 10 HTML dosyasından 180 kayıt okundu, 18 yinelenen kayıt atlandı: 162 yeni film.
- Önceki kataloglar ve Türkçe alfabetik sıralama korundu. Maç özetlerinde yalnız v22'deki 8 doğrudan 2025 GS özeti vardır.

KURULUM
ZIP içindeki projenin tamamını, .github klasörü dahil, GitHub deposuna aktarın. Actions > ALICOTV Android APK > Run workflow. Başarılı olunca ALICOTV-debug-APK paketini indirin.
Eski CatalogParser.java ve CatalogActivity.java dosyaları build.gradle.kts ile derlemeden dışlanmaya devam eder.
APK güncellemesi için aynı imza gerekir. Bu sürümde paket kimliği com.cinebox.tv olarak korunmuştur.

DOĞRULAMA
Katalog tekrarı/ID, eski katalogların korunması, kaynak yapılandırmaları ve Java reklam filtresi testleri kontrol edildi. Bu ortamda Android SDK/Gradle bulunmadığından APK derleme ve Android TV üzerinde odak/oynatma testi yapılamadı. Site erişimi ve video oynatma cihazda denenmelidir.
