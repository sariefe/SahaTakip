# SahaTakip - Profesyonel Saha Personel Takip Sistemi

SahaTakip; saha teknisyenleri ve personelinin konumlarını, cihaz durumlarını ve görev bölgelerini (Geofence) gerçek zamanlı olarak izleyen, gizlilik odaklı, modern ve kurumsal bir Android uygulamasıdır.

## 🚀 Öne Çıkan Özellikler

### 📍 Takip ve İzleme
- **Gerçek Zamanlı MQTT Yayını:** Konum verilerinin anlık olarak JSON formatında MQTT (`test.mosquitto.org`) üzerinden yayınlanması ve izlenebilmesi.
- **Kesintisiz Arka Plan Konumu:** Cihaz uykuda veya uygulama kapalıyken bile düşük güç tüketimiyle hassas konum kaydı.
- **Watchdog (Bekçi) Sistemi:** `WorkManager` kullanılarak, takip servisinin sistem tarafından öldürülmesi durumunda otomatik olarak tekrar başlatılması (15 dk periyotlu kontrol).
- **Akıllı Rota Oynatma:** Geçmiş konum verilerini harita üzerinde farklı hızlarda (1x, 2x, 4x) görselleştirme.
- **Dinamik Geofencing:** Harita üzerinden güvenli bölgeler tanımlama; bölge ihlali durumunda anlık sistem bildirimi ve olay günlüğü oluşturma.

### 🛡️ Güvenlik ve Gizlilik
- **Hibrit Veri Şifreleme:** Yerel Room veritabanının **SQLCipher (AES-256)** ile şifrelenmesi ve hassas ayarların **Android Keystore** destekli **AES/GCM** ile korunması.
- **Just-In-Time Kamera & OCR:** Personel aktivasyonu için MLKit destekli OCR teknolojisi; minimum izin politikasıyla sadece tarama anında kamera izni istemi.
- **Biyometrik Giriş:** Parmak izi veya yüz tanıma desteği ile verilerin güvenliğini sağlayan ekran kilidi.
- **Gelişmiş Cihaz Güvenliği:** Kapsamlı root tespiti (Magisk, sistem bütünlüğü vb.) ve profesyonel Material 3 uyarı sistemi.
- **Ekran Güvenliği:** `FLAG_SECURE` ile ekran görüntüsü alınmasının ve App Switcher üzerinden veri sızıntısının engellenmesi.
- **Timber Güvenli Log:** Gelişmiş loglama politikası; logların sadece `debug` modda görünmesi, `release` sürümde otomatik gizlenmesi.

### 📊 Telemetri ve Senkronizasyon
- **Dinamik Cihaz Durumu:** Pil yüzdesi, şarj durumu, internet bağlantısı ve GPS aktifliğinin anlık izlenmesi.
- **Çevrimdışı Çalışma (Offline-First):** İnternet yokken verileri Room DB'de saklama; bağlantı (Wi-Fi/Cellular) geldiğinde otomatik senkronizasyon.
- **Olay Günlüğü (Event Logs):** Düşük pil, GPS kapanması, güç modu değişiklikleri ve senkronizasyon durumlarının tam zamanlı kaydı.

### 📱 Modern Kullanıcı Deneyimi
- **Adaptif Tasarım:** Tablet ve telefonlar için optimize edilmiş, Material 3 "Dynamic Color" destekli esnek arayüz.
- **Tam Lokalizasyon:** Tüm uygulama içi metinler, servis bildirimleri ve arka plan günlükleri için dinamik Türkçe ve İngilizce dil desteği.

---

## 📸 Ekran Görüntüleri

Uygulamanın arayüzünü ve temel özelliklerini aşağıda görebilirsiniz:

|    **Ana Panel (Dashboard)**    | **Canlı Harita & Takip** |   **Olay Günlüğü**    |
|:-------------------------------:|:------------------------:|:---------------------:|
| ![Dashboard](art/dashboard.png) |   ![Map](art/map.png)    | ![Logs](art/logs.png) |

|      **Biyometrik Giriş**       |    **Ayarlar & Güvenlik**     |   **İzin Talepleri**    |
|:-------------------------------:|:-----------------------------:|:-----------------------:|
| ![Biometric](art/biometric.png) | ![Settings](art/settings.png) | ![Leave](art/leave.png) |

### 🎥 Uygulama Tanıtım Videosu

SahaTakip uygulamasının gerçek zamanlı performansını ve kullanım senaryolarını izlemek için **GitHub Releases** sayfasındaki tanıtım videosuna göz atabilirsiniz:

[![Tanıtım Videosu](https://img.shields.io/badge/GitHub-Release_Video-blue?style=for-the-badge&logo=github)](https://github.com/sariefe/SahaTakip/releases/tag/video)

---

## 🛠 Kullanılan Teknolojiler (Tech Stack)

- **UI:** Jetpack Compose & Material 3
- **Mimari:** Clean Architecture (MVVM) & SOLID Prensipleri
- **Bağımlılık Enjeksiyonu:** Hilt (Dagger) - **KSP** entegrasyonu ile derleme hızı optimizasyonu
- **Ağ & Veri:** MQTT (Paho), Retrofit, Room (**SQLCipher Encrypted**), DataStore, Moshi
- **Analiz:** MLKit Text Recognition (OCR)
- **Güvenlik:** Android Keystore, AES-GCM, Secrets Gradle Plugin
- **Asenkron:** Kotlin Coroutines & Flow
- **Arka Plan:** Foreground Services, WorkManager
- **Test:** JUnit 4, MockK, Robolectric (85 test case - %100 Başarı)

## 🏗 Proje Yapısı

```text
com.sahatakip
├── di/             # Hilt Modülleri (App, Database, Network, Repository, Service)
├── data/           # Veri Katmanı (Room, Preferences, Remote API, Repository Impl)
├── domain/         # Alan Katmanı (Modeller, Repository Arayüzleri, Servisler, Workerlar)
├── ui/             # Sunum Katmanı (Compose Ekranları, Bileşenler, ViewModeller)
├── util/           # Yardımcı Araçlar (MQTT, OCR, Biyometri, İzinler, Lokalizasyon)
└── MainActivity.kt # Giriş Noktası ve İzin Yönetimi
```

## 📋 Kurulum ve Çalıştırma

1. **Projeyi Klonlayın:**
   ```bash
   git clone https://github.com/efe-sari/staj-testing.git
   ```
2. **API Anahtarı Ayarı:** Projenin kök dizinindeki `local.properties` dosyasına Google Maps API anahtarınızı ekleyin:
   ```properties
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```
3. **MQTT Gözlemleme (Firefox):**
   - [HiveMQ Web Client](http://www.hivemq.com/demos/websocket-client/) açın.
   - **Host:** `test.mosquitto.org`, **Port:** `8000` ile bağlanın.
   - **Topic:** `saha/takip/live` başlığına abone olun.
4. **İzinler:** Uygulama açılışında konum ve bildirim izinlerini onaylayın. Arka plan takibi için "Her zaman izin ver" ve "Pil Optimizasyonunu Kapat" seçeneklerini seçin.

## 🧪 Kalite ve Test

Proje, 85 farklı test senaryosu ile %100 başarı oranına sahiptir:
- **ViewModel Tests:** State yönetimi ve iş mantığı doğrulaması.
- **Robolectric UI Tests:** Lokalizasyon ve navigasyon akışları.
- **Integration Tests:** Veritabanı ve Preferences etkileşimleri.

Testleri çalıştırmak için:
```bash
./gradlew testDebugUnitTest
```

## 📜 Lisans

Bu proje, staj ve profesyonel gelişim kapsamında geliştirilmiştir. Tüm hakları saklıdır.
