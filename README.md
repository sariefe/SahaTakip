# SahaTakip - Akıllı Saha Operasyon Yönetimi

SahaTakip, saha personelinin verimliliğini artırmak, güvenlik süreçlerini optimize etmek ve gerçek zamanlı konum takibi sağlamak amacıyla geliştirilmiş modern bir Android uygulamasıdır. Uygulama, arka plan servisleri, biyometrik güvenlik ve yapay zeka entegrasyonu ile kurumsal düzeyde bir çözüm sunar.

## 🚀 Temel Özellikler

*   **Gerçek Zamanlı Konum Takibi:** `LocationTrackingService` ile arka planda düşük pil tüketimiyle hassas konum kaydı.
*   **Biyometrik Güvenlik:** Hassas verilere erişim ve uygulama kilidi için parmak izi/yüz tanıma entegrasyonu.
*   **AI Destekli İşlemler:** Firebase AI ve Google ML Kit (Text Recognition) ile akıllı veri işleme ve analiz.
*   **Çevrimdışı Çalışma Desteği:** Room veritabanı ile verilerin yerel olarak saklanması ve senkronizasyonu.
*   **Modern UI/UX:** Jetpack Compose ve Material 3 ile tamamen deklaratif ve kullanıcı dostu arayüz.
*   **Olay Günlüğü (Event Logging):** Personel hareketlerinin ve sistem uyarılarının detaylı takibi.

## 🛠 Kullanılan Teknolojiler

Uygulama, modern Android geliştirme ekosisteminin en güncel ve performanslı araçlarını kullanır:

*   **Dil:** Kotlin & Coroutines / Flow
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Mimari:** Clean Architecture + MVVM (Model-View-ViewModel)
*   **Dependency Injection:** Hilt / Koin (Manuel DI adaptasyonu)
*   **Local Database:** Room Persistence Library
*   **Network:** Retrofit & OkHttp & Moshi
*   **Image Loading:** Coil
*   **Service & Security:** Foreground Services, BiometricPrompt
*   **AI & Vision:** Firebase AI, ML Kit Text Recognition
*   **Test:** JUnit, Robolectric, Roborazzi (Screenshot Testing)

## 🏗 Mimari Yapı ve Gerekçesi

Proje, **Clean Architecture** prensipleri üzerine inşa edilmiştir. Kod tabanı üç ana katmana ayrılmıştır:

1.  **Data Layer:** Veri kaynakları (Local Room DB, Remote API) ve Repository implementasyonları.
2.  **Domain Layer:** İş mantığı (Business Logic), Use Case'ler ve Domain modelleri.
3.  **UI (Presentation) Layer:** ViewModel'ler ve Jetpack Compose ekranları.

### Neden Bu Mimari?

*   **Sürdürülebilirlik:** Katmanlar arasındaki düşük bağımlılık (Decoupling), projenin uzun vadede bakımını kolaylaştırır.
*   **Test Edilebilirlik:** İş mantığı (Domain) Android framework'ünden bağımsız olduğu için kolayca Unit Test yazılabilir.
*   **Esneklik:** Örneğin, veritabanı kütüphanesini değiştirmek veya yeni bir API eklemek diğer katmanları etkilemez.
*   **Ekip Çalışması:** Farklı geliştiriciler aynı anda UI ve Data katmanlarında çakışma yaşamadan çalışabilir.

## 📁 Proje Yapısı

```text
app/src/main/java/com/example/
├── data/           # Veri erişim katmanı (Local, Remote, Repository)
├── domain/         # İş mantığı ve servisler (LocationTrackingService, Modeller)
├── ui/             # Arayüz katmanı
│   ├── screens/    # Compose Ekranları (Auth, Map, Settings vb.)
│   ├── viewmodel/  # UI Durum Yönetimi
│   ├── theme/      # Tasarım sistemi ve Renkler
│   └── components/ # Yeniden kullanılabilir UI bileşenleri
└── MainActivity.kt # Uygulama giriş noktası
```

## ⚙️ Kurulum ve Çalıştırma

1.  Projeyi klonlayın: `git clone <repo-url>`
2.  Android Studio'da projeyi açın.
3.  `google-services.json` dosyasını `app/` dizinine ekleyin (Firebase özellikleri için).
4.  Projeyi Build edin ve bir cihazda/emülatörde çalıştırın.
<img width="200" height="500" alt="Screenshot_20260729_150832" src="https://github.com/user-attachments/assets/2b061095-494d-4964-92f7-50b33d3c4b20" />
<img width="200" height="500" alt="Screenshot_20260729_150808" src="https://github.com/user-attachments/assets/1dbeec91-7bb7-4105-bc9f-fe4950370320" />
<img width="200" height="500" alt="Screenshot_20260729_150815" src="https://github.com/user-attachments/assets/8aa1b0ca-13db-4c67-9fa0-4b3bc77d019c" />
<img width="200" height="500" alt="Screenshot_20260729_150821" src="https://github.com/user-attachments/assets/04f64d43-2355-4b33-8947-35768ceac736" />
<img width="200" height="500" alt="Screenshot_20260729_150826" src="https://github.com/user-attachments/assets/a6135da0-5ecf-42ef-84a4-53db81ea8c97" />

## Video Önizleme


---
*Bu proje, modern Android standartları gözetilerek geliştirilmiştir.*
