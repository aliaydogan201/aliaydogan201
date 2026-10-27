# CineBox TV v0.18 — Derleme ayarları

Gradle 8.11.1 sabit, Java 17, Ubuntu 24.04. Ayrıntılı build-log artifact eklendi. KURULUM.txt adımlarını uygulayın; .github klasörünü de güncellemek gerekir. APK burada derlenmedi.

# CineBox TV v0.17 — v11 tabanlı içerik ekleme
v11 esas alındı. Mevcut kayıtlar, favori kimlikleri, puanlama, arama, MP4 oynatıcı ve reklam filtreli WebPlayerActivity korundu.

56 film, 6 dizi; 0 tekrar ayıklandı. v11 kayıtları ve web oynatıcı byte karşılaştırmasıyla korundu.

İki kullanıcı HTML dosyasındaki kayıtlar birleştirildi. Filmler ve Diziler kategorilerinde aynı afiş kartları ve detay ekranı kullanılır. Türk Dizileri ayrıca korunur ve Diziler altında da görünür. Birincil düğme “Web oynatıcıda aç”: Guguk Kuşu ile aynı uygulama içi oynatıcıdır. Tarayıcıya çıkış ikincil seçenek olarak kalır. Doğrudan MP4 bağlantısı çıkarılmadı; sitedeki oynatma/bölüm seçimi gerekebilir. Filtre bütün reklamları engelleme garantisi vermez.

HTML dosyalarında bulunan afiş srcset adresleri kullanıldı; yerel bilgisayar yolları eklenmedi. Afiş indirme/oynatma cihazda denenmedi. Yeni katalog APK içindedir; açılışta site kataloğuna ağ isteği yapılmaz. Tüm site arşivi değildir.

GitHub projesindeki app klasörünü bu paketle değiştirin; .github dahil kök dosyaları aktarın. Actions ile APK oluşturun. Sürüm kodu 17; önceki sürümlerin üstüne aynı imzayla güncellenebilir. APK derlemesi ve cihaz testi burada yapılmadı.
