# 🌌 MiRearUltra (Xiaomi 17 Pro Max Secondary Rear Display Engine)

[![Release](https://img.shields.io/badge/Release-v1.1--Alpha-orange.svg)](https://github.com/)
[![HyperOS](https://img.shields.io/badge/HyperOS-3.0%2B-blue.svg)](https://miui.com/)
[![Android](https://img.shields.io/badge/Android-15%20%2F%2016-green.svg)](https://developer.android.com/)
[![Display](https://img.shields.io/badge/Display-976x596%20AMOLED%20120Hz%20LTPO-purple.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Languages](https://img.shields.io/badge/Languages-10%20Languages%20Supported-yellow.svg)]()

> ⚠️ **PUBLIC ALPHA NOTICE / AÇIK ALFA DUYURUSU**
> 
> **English:** This repository contains the **v1.1 Alpha (Public Preview)** release of MiRearUltra. The project is under **active development**. While core modules, display rendering, and Shizuku integrations are fully functional, you may encounter experimental behaviors or visual adjustments across different HyperOS builds. Feedback and pull requests are warmly welcomed!
> 
> **Türkçe:** Bu depo MiRearUltra'nın **v1.1 Alpha (Halka Açık Önizleme)** sürümünü içermektedir. Proje **aktif ve sürekli geliştirilme aşamasındadır**. Çekirdek modüller, ekran render motoru ve Shizuku entegrasyonu kararlı çalışmakla birlikte farklı HyperOS sürümlerinde deneysel davranışlarla karşılaşılabilir. Geri bildirimler ve katkılar memnuniyetle kabul edilir!

---

## 📖 Overview / Genel Bakış

**MiRearUltra**, Xiaomi 17 Pro Max modelinin arka tarafında yer alan ikincil **976x596 AMOLED 120Hz LTPO** ekranını tam potansiyeliyle çalıştırmak için sıfırdan tasarlanmış, açık kaynaklı, yüksek performanslı bir arka ekran yönetim motorudur.

Standart sistem kısıtlamalarını aşarak arka ekranda tam interaktif dokunmatik deneyim, düşük gecikmeli 120 FPS render motoru, gizlilik odaklı arama yönetimi ve hızlı araçlar sunar.

---

## 🌍 Multilingual Support / Çoklu Dil Desteği

MiRearUltra supports the **10 most spoken languages in the world** across both the front management dashboard and the rear display 120Hz canvas renderer:

| Code | Language | Dil / 语言 / لغة | Status |
|:---:|:---|:---|:---:|
| `en` | **English** | Global default | ✅ 100% |
| `zh` | **Mandarin Chinese (简体中文)** | 官方简体中文 | ✅ 100% |
| `es` | **Spanish (Español)** | Interfaz y pantalla trasera | ✅ 100% |
| `hi` | **Hindi (हिन्दी)** | पूर्ण समर्थन | ✅ 100% |
| `ar` | **Arabic (العربية)** | دعم كامل للواجهة | ✅ 100% |
| `bn` | **Bengali (বাংলা)** | সম্পূর্ণ সমর্থন | ✅ 100% |
| `pt` | **Portuguese (Português)** | Painel e tela traseira | ✅ 100% |
| `ru` | **Russian (Русский)** | Полный интерфейс | ✅ 100% |
| `ja` | **Japanese (日本語)** | 背面ディスプレイ対応 | ✅ 100% |
| `tr` | **Turkish (Türkçe)** | Tam arayüz ve arka ekran | ✅ 100% |

Language can be switched dynamically inside **Customization Studio (Özelleştirme Stüdyosu)** or automatically synchronized with the system locale.

---

## ✨ Key Features / Temel Özellikler

### 1. ⚡ 120Hz LTPO Ultra-Smooth Canvas Engine
- Düşük gecikmeli, pil dostu dinamik yenileme hızı yönetimi.
- Modern saat tasarımları, canlı pil göstergesi, hava durumu ve adım sayar.
- Yüzüstü koyulduğunda arka ekranı otomatik uyutan ve cepte istenmeyen dokunmaları engelleyen optimize edilmiş akıllı yakınlık/cep modu.

### 2. 📞 Privacy-Preserving Call Management (Akıllı Arama)
- **Gelen Arama:** Arka ekranda arayanın adı ve tek dokunuşla Cevapla / Reddet butonları.
- **Arama Gizliliği:** Görüşme cevaplandığı an arka ekrandaki arama kartı derhal temizlenir ve ana saat ekranına dönülür; telefon kulağa götürüldüğünde veya arka ekrana bakıldığında numara/görüşme ekranda kalmaz.

### 3. 👥 Quick Speed Dial (6 Hızlı Kişi)
- En çok görüşülen 6 kişiye doğrudan arka ekrandan tek dokunuşla arama yapabilme.
- Ön arayüz üzerinden rehberden kolayca kişi seçme veya manuel numara ekleme olanağı.

### 4. 📜 Fluid Notification Curtain (Bildirim Perdesi)
- Ekranın en üstünden aşağı doğru hafif kaydırma hareketiyle açılan bildirim perdesi.
- Akıcı kayarak açılan mesaj önizlemeleri ve tek tıkla temizleme.

### 5. 🛠️ Quick Tools & Utilities (Hızlı Araçlar)
- **Fener:** Hızlı aç/kapa fener denetimi.
- **Ses Kaydedici:** Arka ekrandan anında ses kaydı başlatma ve durdurma.
- **Retro Tuğla Oyunu:** Arka ekranda oynanabilen interaktif, fizik tabanlı mini tuğla kırma oyunu.

---

## 🏗️ Architecture & Technology Stack

```
+-------------------------------------------------------------+
|                     Android 15 / 16                         |
+-------------------------------------------------------------+
|                      HyperOS 3.0 Core                       |
+------------------------------+------------------------------+
|        Front Display         |        Rear Display          |
|  Jetpack Compose Material 3  |  Secondary Display (Id: 1)   |
|   - Theme & Color Studio     |  - Presentation API Engine   |
|   - 6 Quick Contacts Selector|  - Native Canvas 120Hz LTPO  |
|   - 10-Language Selector     |  - Shizuku Multi-Display ADB |
+------------------------------+------------------------------+
                               |
               +---------------+---------------+
               |   RearStateRepository (State) |
               +---------------+---------------+
                               |
            +------------------+------------------+
            |  RearEngineForegroundService        |
            |  RearNotificationService            |
            |  Proximity & Orientation Sensors    |
            +-------------------------------------+
```

- **UI Toolkit:** Jetpack Compose (Material 3)
- **Display Pipeline:** Android `Presentation`, `DisplayManager` (Display #1), Shizuku ADB privileged bridge
- **Rendering:** Zero-allocation double-buffered `android.graphics.Canvas`
- **I18n:** `RearI18n` zero-overhead runtime dictionary + standard Android `res/values-*/strings.xml`

---

## 🚀 Installation & Getting Started / Kurulum Rehberi

### Prerequisites / Gereksinimler
- **Cihaz:** Xiaomi 17 Pro Max (HyperOS 3+)
- **Shizuku:** [Shizuku](https://shizuku.rikka.app/) yüklü ve kablosuz hata ayıklama (Wireless Debugging) üzerinden başlatılmış olmalıdır.

### Adım Adım Kurulum:
1. `Releases` bölümünden en son `MiRearUltra-v1.1-Alpha-Xiaomi17ProMax.apk` dosyasını indirin.
2. Cihazınıza APK'yı yükleyin.
3. Uygulamayı açın ve aşağıdaki izinleri sırasıyla onaylayın:
   - **Shizuku İzni** (Arka ekrana dokunmatik enjeksiyonu ve sanal ekran yönetimi için)
   - **Bildirim Erişimi** (Gelen bildirimleri arka ekranda göstermek için)
   - **Arama / Telefon İzni** (Gelen aramaları arka ekrandan yönetmek için)
   - **Rehber İzni** (Hızlı arama 6 kişi listesini oluşturabilmek için)
4. "Arka Ekran Motorunu Başlat" butonuna dokunarak sistemi etkinleştirin.

---

## 🎮 Gestures & Rear Touch Guide / Arka Dokunmatik Rehberi

| Hareket / Gesture | İşlev / Action |
|:---|:---|
| **Sağa / Sola Kaydırma (Swipe Horizontal)** | Modüller arasında geçiş yapar (Saat ➔ Hızlı Arama ➔ Hızlı Araçlar ➔ Mini Oyun). |
| **Yukarıdan Aşağı Çekme (Swipe Down)** | Bildirim Perdesini açar. |
| **Aşağıdan Yukarı Kaydırma (Swipe Up)** | Bildirim Perdesini kapatır. |
| **Tek Dokunma (Tap)** | Seçilen kişiyi arar, feneri açar, oyunu başlatır veya aramayı yanıtlar. |

---

## 🗺️ Roadmap & Release Stages

Check out [ROADMAP.md](ROADMAP.md) for upcoming milestones including:
- Dynamic island styled live activities
- Camera rear preview & viewfinder integration
- Custom lock screen wallpaper pack engine

---

## 🤝 Contributing / Katkıda Bulunma

We welcome all contributions from the community! Please read our [CONTRIBUTING.md](CONTRIBUTING.md) to understand how you can submit bug reports, suggest improvements, or create pull requests.

---

## 📄 License / Lisans

Bu proje [Apache License 2.0](LICENSE) altında lisanslanmıştır.
Tüm hakları saklıdır. Xiaomi, HyperOS ve ilgili ticari markalar Xiaomi Inc.'e aittir.
