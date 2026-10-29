# ALICOTV v52 — Kalıcı imza ve yedekleme

## 1. Mevcut uygulamayı kaldırmadan yedek alın
Eski APK'nın özel imza anahtarı bu projede yok. Yeni sürümdeki yedek düğmesi eski sürümde kullanılamaz.
Bilgisayarda Python 3.9+ ve Android SDK Platform Tools (adb) gerekir.
Telefonda/TV'de geliştirici seçenekleri ve USB veya cihazın desteklediği ağ hata ayıklamasını açın.
Bilgisayara bağlantı iznini cihazdan kabul edin. `adb devices` cihazı `device` olarak göstermeli.
ZIP'i açtığınız klasörde çalıştırın:

```text
python tools/eski_surumu_yedekle.py
```

Birden fazla cihaz varsa `--serial CIHAZ_KIMLIGI` ekleyin.
Yardımcı uygulamayı kapatıp verilerini okur; kayıtları silmez. Önceki debug APK'ların run-as desteğine dayanır.
İşlem başarısızsa uygulamayı kaldırmayın. Cihaz erişimi sağlanmadan veya eski imza anahtarı bulunmadan güvenli geçiş yapılamaz.
Oluşan ALICOTV-eski-surum-yedek.json dosyasını bilgisayarda saklayın; USB/dosya aktarımıyla cihaza da kopyalayın.

## 2. İmza anahtarını yalnızca BİR KEZ oluşturun
Java JDK kurulu bilgisayarda:

```text
keytool -genkeypair -v -keystore alico-update.jks -alias alico -keyalg RSA -keysize 3072 -validity 10000 -storetype JKS
```

Parolayı keytool sorduğunda girin. Anahtar parolası için aynı parolayı kullanabilirsiniz.
Her sürümde anahtar oluşturmayın. JKS dosyasını ve parolayı güvenli yerde yedekleyin.
Eski APK'nın özel anahtarı varsa yeni üretmek yerine onu kullanın.
PowerShell'de Base64 metnini yerel dosyaya yazın:

```powershell
[IO.File]::WriteAllText("$PWD/alico-update.base64", [Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD/alico-update.jks")))
```

GitHub deposu → Settings → Secrets and variables → Actions → New repository secret:

- ALICO_KEYSTORE_BASE64: base64 dosyasının içeriği
- ALICO_KEYSTORE_PASSWORD: keystore parolası
- ALICO_KEY_ALIAS: alico (eski anahtar kullanılıyorsa onun alias değeri)
- ALICO_KEY_PASSWORD: anahtar parolası

JKS, Base64 ve parolaları GitHub dosyalarına yüklemeyin. .gitignore bu dosyaları dışlar.
ZIP içindeki .github/workflows/android-apk.yml dahil proje dosyalarını güncelleyin.
Workflow bu dört Secret olmadan bilerek durur; geçici anahtar üretmez.
Yerel derlemede JKS signing/alico-update.jks altında, parola/alias ortam değişkenleri tanımlı olmalı.

## 3. İlk geçiş
Eski imza anahtarı yoksa v52 eski APK üzerine kurulamaz.
Yalnızca ilk adımda yedek başarıyla alındıktan sonra eski sürümü kaldırıp v52'yi kurun.
Ana sayfa → Yedeği geri yükle → JSON dosyasını seç → Geri yükle.
Bu işlem mevcut ek arşiv ve kişisel kayıtları yedekle değiştirir; birleştirme yapmaz.
Kayıtları kontrol edin ve dış yedeği saklayın.
Sonraki APK'lar AYNI dört Secret ile derlenirse uygulamayı kaldırmadan Güncelle diyerek kurulabilir.

## 4. Sonraki yedekler
Ana sayfada Arşivi yedekle / Yedeği geri yükle düğmeleri bulunur.
Ek arşiv, güncellenen bölümler, favoriler, puanlar, geçmiş, takip ve izlendi işaretleri dahil edilir.
Videolar, APK'nın sabit kataloğu ve WebView oturum çerezleri dahil edilmez.
Dosyayı uygulama dışında, tercihen bilgisayarda veya USB'de saklayın.
JSON şifresizdir, izleme geçmişi içerir; herkese açık paylaşmayın.
Android sistem dosya seçicisi bulunmayan TV'lerde dosya seçme düğmeleri çalışmaz.
Hatalı yedek yazmadan önce denetlenir; yazma hatasında eski anlık görüntü geri uygulanır.
İşlem sırasında güç kesilirse çok dosyalı tam işlem garantisi yoktur; dış yedeği koruyun.

APK derlemesi, gerçek cihaz yedeklemesi ve TV testi bu ortamda yapılmadı.
