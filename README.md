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

## 🏗 Mimari Yapı ve Mimari Tercihler

Proje, **Clean Architecture** prensipleri üzerine inşa edilmiştir. Kod tabanı üç ana katmana ayrılmıştır:

1.  **Data Layer:** Veri kaynakları (Local Room DB, Remote API) ve Repository implementasyonları.
2.  **Domain Layer:** İş mantığı (Business Logic), Use Case'ler ve Domain modelleri.
3.  **UI (Presentation) Layer:** ViewModel'ler ve Jetpack Compose ekranları.

### Neden Clean Architecture?

*   **Sürdürülebilirlik:** Katmanlar arasındaki düşük bağımlılık (Decoupling), projenin uzun vadede bakımını kolaylaştırır.
*   **Test Edilebilirlik:** İş mantığı (Domain) Android framework'ünden bağımsız olduğu için kolayca Unit Test yazılabilir.
*   **Esneklik:** Örneğin, veritabanı kütüphanesini değiştirmek veya yeni bir API eklemek diğer katmanları etkilemez.
*   **Ekip Çalışması:** Farklı geliştiriciler aynı anda UI ve Data katmanlarında çakışma yaşamadan çalışabilir.

### Neden MVVM?

Sunum katmanında **Model-View-ViewModel (MVVM)** mimarisi tercih edilmiştir:

*   **Yaşam Döngüsü Yönetimi:** `ViewModel` kullanımı sayesinde ekran döndürme gibi yapılandırma değişikliklerinde verilerin korunması sağlanır.
*   **Durum Yönetimi (State Management):** `StateFlow` ve `Flow` API'leri ile UI durumunun (UI State) merkezi ve reaktif bir noktadan yönetilmesi kolaylaşır.
*   **UI ve Mantık Ayrımı:** Jetpack Compose (View) sadece veriyi göstermekten sorumludur; iş mantığı ve durum dönüşümleri ViewModel'de tutulur.
*   **Tek Yönlü Veri Akışı (UDF):** Verinin ViewModel'den UI'ya akması, eventlerin ise UI'dan ViewModel'e iletilmesi sayesinde öngörülebilir ve kolay hata ayıklanabilir bir yapı kurulur.

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

---
*Bu proje, modern Android standartları gözetilerek geliştirilmiştir.*
