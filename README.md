<div align="center">

# 🎧 AirStream Cast for Sonos & UPnP
**Stream live Android internal audio, YouTube, YouTube Music & SoundCloud directly to legacy Sonos Play:1 and UPnP/DLNA speakers—with zero root required.**

[![Download Latest APK](https://img.shields.io/badge/Download_APK-v7.0_(Latest)-22D3EE?style=for-the-badge&logo=android&logoColor=black)](https://github.com/yadavkrishna-sudo/sonos-airstream/releases/latest)
[![Android 10+](https://img.shields.io/badge/Android-10%2B%20(API%2029%2B)-A855F7?style=for-the-badge&logo=android&logoColor=white)](https://github.com/yadavkrishna-sudo/sonos-airstream/releases/latest)
[![Support on Ko-fi](https://img.shields.io/badge/Support_Development-Ko--fi_%2F_Donate-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](#☕-support--donate)

</div>

---

## ✨ Why AirStream Cast?

First-generation **Sonos Play:1** and legacy UPnP speakers lack Apple AirPlay, Bluetooth, and direct YouTube casting. **AirStream Cast** bridges that gap by capturing real-time 16-bit 44.1kHz stereo PCM audio from your Android device and serving an instant, low-latency HTTP WAV stream over your local Wi-Fi network.

### 🚀 Key Features
* **📡 Auto-Discovery (SSDP):** Automatically finds your Sonos Play:1 or UPnP speaker on your local Wi-Fi network.
* **🔊 Hardware Volume Sync:** Control your Sonos Play:1 hardware amplifier volume (`0%–100%`) using the on-screen slider **or your phone's physical Volume Up / Down buttons**.
* **🌐 Built-In Background Web Player:** Stream **YouTube**, **YouTube Music**, and **SoundCloud** directly inside the app—continues playing even when your phone screen is turned off.
* **🚗 Android Auto Support:** Includes a dedicated `MediaBrowserService` so you can switch streams and control playback right from your car dashboard and steering wheel buttons.
* **🔒 100% Local & Private:** Zero cloud servers, zero ads, and zero tracking. Audio streams directly from your phone to your speaker over local Wi-Fi.

---

## 📲 Download & Installation

1. **[Click here to download `AirStream-Cast-v7.apk` from GitHub Releases](https://github.com/yadavkrishna-sudo/sonos-airstream/releases/latest)**.
2. Open the downloaded `.apk` file on your Android phone (Android 10 or newer).
3. If prompted by Google Play Protect during installation, tap **More details** and select **Install anyway**.
4. Open **AirStream Cast**, tap **Scan** (or enter your Sonos Play:1 IP address), and tap **Stream**!
   * *Tip:* When the Android permission prompt appears, select **Entire screen** so the internal audio engine can capture all media playing on your phone.

### 🚗 Enabling Android Auto (Optional)
Because this app is installed outside the Google Play Store, enable **Unknown sources** once in Android Auto:
1. Open **Settings** on your phone and go to **Connected devices** -> **Connection preferences** -> **Android Auto**.
2. Scroll to the bottom and tap **Version** 10 times to unlock Developer Mode.
3. Tap the three-dot menu in the top-right corner, open **Developer settings**, and check **Unknown sources**.

---

## ☕ Support & Donate

**AirStream Cast** is free and built for the community. If it breathed new life into your Sonos Play:1 speaker, consider buying me a coffee to support future updates!

* 🌍 **International (Ko-fi / PayPal / Card):** **(https://ko-fi.com/krishnayadav47442)**
* 🇮🇳 **India (UPI — GPay / PhonePe / Paytm):** 'rocks.krishna-1@okhdfcbank` *(or tap the **☕ Donate** button inside the app!)*

---

## 🛠️ Built With
* **Kotlin & Android `AudioPlaybackCapture` API** (Real-time 44.1kHz 16-bit Stereo PCM capture)
* **Zero-Dependency Embedded HTTP WAV Server** (Low-latency chunked streaming)
* **UPnP / SOAP `AVTransport` & `RenderingControl`** (Direct Sonos Play:1 control on port `1400`)
* **AndroidX Media `MediaBrowserServiceCompat`** (Android Auto dashboard integration)
