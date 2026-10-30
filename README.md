# FarhansSonicPlay — Android Offline Music Player

> **"Your Music. Your World."**  
> Developed by **Nasimudeen Muhammed Farhan**

---

## 🎵 Overview

**FarhansSonicPlay** is a modern, high-performance, completely offline Android music player application engineered with modern Android architecture. Inspired by the functional depth of reference applications like Musicolet, it delivers an independent, original, privacy-first listening experience with zero network tracking, AMOLED dark mode, Material You theming, and an advanced **20-Queue playback system**.

---

## ✨ Key Features & Architecture

### 1. 100% Offline & Absolute Privacy
- Requires **zero internet permissions** (`android.permission.INTERNET` is completely absent from the manifest).
- Your audio files, listening history, and personal preferences never leave your device.

### 2. Multi-Queue Engine (Up to 20 Independent Queues)
- Seamlessly switch between up to 20 independent playback queues (e.g., *Main*, *Workout*, *Chill*, *Late Night*).
- Each queue remembers its own track position, playback order, and progress independently.

### 3. Audio Effects & Equalizer
- Built-in multi-band Graphic Equalizer with system audio effects.
- Dynamic **Bass Boost** and **3D Surround Virtualizer** controls.
- Playback speed adjustment (0.25x – 2.5x) and pitch control via Android Media3 / ExoPlayer.

### 4. Media Scanning & Library Navigation
- Automatic scanning of device audio files using Android `MediaStore` ContentProvider.
- Excludes short ringtones, notifications, and voice notes (< 10 seconds).
- Tabs for **Songs**, **Folders**, **Albums**, **Artists**, **Playlists**, and **Queues**.
- Support for embedded lyrics and companion `.lrc` lyrics files.
- ID3 / Metadata Tag editing directly within the app.

### 5. Foreground Audio Service & MediaSession
- Android `MediaSessionService` foreground service ensures seamless background playback.
- Media notification with Previous, Play/Pause, and Next controls.
- Automatic audio focus management (lowers volume or pauses for incoming calls/alarms).
- `HeadsetReceiver` pauses playback immediately upon headphone unplugging or Bluetooth disconnection.

### 6. Design, Customization & Personal Developer Identity
- **AMOLED Pitch Black**, Dark, Light, and System Theme modes.
- Dynamic Material You accent colors with selectable vibrant presets (Sonic Cyan, Electric Violet, Cyber Neon, etc.).
- Animated MiniPlayer and smooth fullscreen Now Playing sheet.
- **About Developer section**: Tastefully features **Nasimudeen Muhammed Farhan**'s photograph (`Myphoto.png`) rendered as a circular developer badge with the label *"Developed by Farhan"*, keeping the player interface clean and modern.

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.0.20
- **UI Framework:** Jetpack Compose (Material3, Navigation Compose)
- **Audio Engine:** AndroidX Media3 1.4.1 (ExoPlayer + MediaSession)
- **Database:** Room 2.6.1 (SQLite local persistence)
- **Preferences:** Preferences DataStore 1.1.1
- **Image Loading:** Coil 2.7.0 (Album artwork & developer photo)
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)

---

## 📂 Project Structure

```
FarhansSonicPlay/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/farhan/sonicplay/
│       │   ├── SonicPlayApplication.kt
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── Daos.kt
│       │   │   │   ├── PreferencesManager.kt
│       │   │   │   ├── SonicPlayDatabase.kt
│       │   │   │   └── entities/Entities.kt
│       │   │   ├── model/Models.kt
│       │   │   └── repository/
│       │   │       ├── MusicRepository.kt
│       │   │       ├── MusicScanner.kt
│       │   │       └── TagEditorRepository.kt
│       │   ├── playback/
│       │   │   ├── AudioEffectsController.kt
│       │   │   ├── HeadsetReceiver.kt
│       │   │   ├── MediaNotificationManager.kt
│       │   │   ├── SleepTimerManager.kt
│       │   │   ├── SonicPlaybackManager.kt
│       │   │   └── SonicPlaybackService.kt
│       │   └── ui/
│       │       ├── MainActivity.kt
│       │       ├── MainViewModel.kt
│       │       ├── components/
│       │       │   ├── MiniPlayer.kt
│       │       │   ├── NowPlayingScreen.kt
│       │       │   └── UpNextQueueSheet.kt
│       │       ├── screens/
│       │       │   ├── equalizer/EqualizerScreen.kt
│       │       │   ├── library/LibraryHomeScreen.kt
│       │       │   ├── queue/MultiQueueScreen.kt
│       │       │   ├── search/SearchScreen.kt
│       │       │   ├── settings/SettingsScreen.kt
│       │       │   └── tageditor/TagEditorScreen.kt
│       │       └── theme/Theme.kt
│       └── res/
│           ├── drawable/
│           ├── drawable-nodpi/
│           │   ├── developer_avatar.png  <-- [Cropped Profile Badge]
│           │   └── developer_farhan.png  <-- [Full Image]
│           ├── mipmap-anydpi-v26/
│           └── values/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```

---

## 🚀 How to Build & Run

### In Android Studio:
1. Open **Android Studio** (Koala / Ladybug or newer).
2. Select **File > Open...** and select the `FarhansSonicPlay` folder.
3. Allow Gradle to synchronize dependencies.
4. Connect an Android device (or launch an emulator) running Android 8.0 or higher.
5. Click **Run 'app'** (`Shift + F10`) or select **Build > Build Bundle(s) / APK(s) > Build APK(s)** to generate the APK.
