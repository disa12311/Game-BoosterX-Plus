# Game Booster+ (Android)

App tương tự Samsung Game Booster Plus, chạy trên **mọi máy Android 12+** có Shizuku.

## Tính năng

| Tính năng | Chi tiết |
|---|---|
| **GameMode API** | Set Performance / Standard / Battery per-game |
| **Render downscale** | `device_config` intervention – giảm GPU load |
| **FPS cap** | Giới hạn FPS ở Battery mode |
| **Auto scan** | Tự tìm game từ PackageManager |
| **Overlay HUD** | FPS, RAM, CPU hiển thị khi chơi |

## Yêu cầu

- Android 12+ (API 31)
- [Shizuku](https://shizuku.rikka.app/) đã cài và đang chạy
- Wireless ADB hoặc ADB pairing để khởi động Shizuku

## Build

```bash
# Clone và mở trong Android Studio Hedgehog trở lên
./gradlew assembleDebug
```

## Cấu trúc project

```
app/src/main/java/com/gamebooster/
├── data/
│   ├── model/GameProfile.kt      ← Entity Room
│   ├── db/                       ← DAO + Database
│   └── GameRepository.kt         ← Single source of truth
├── shizuku/
│   └── ShizukuManager.kt         ← Wrapper Shizuku API
├── gamemode/
│   └── GameModeManager.kt        ← Áp dụng GameMode + interventions
├── service/
│   └── OverlayService.kt         ← Floating HUD
├── ui/
│   ├── MainActivity.kt
│   ├── MainViewModel.kt
│   ├── screens/HomeScreen.kt
│   ├── components/GameModeChip.kt
│   └── theme/Theme.kt
└── di/AppModule.kt               ← Hilt DI
```

## Cách hoạt động

```
User chọn mode
    → MainViewModel.saveProfile()
        → GameRepository.saveAndApply()
            → Room DB upsert
            → GameModeManager.applyProfile()
                → ShizukuManager.exec("cmd game mode set 2 <pkg>")
                → ShizukuManager.exec("device_config put game_overlay <pkg> mode=2,downscaleFactor=0.8")
```

## Hạn chế

- `downscaleFactor` cần OEM hỗ trợ (Pixel, Samsung, một số Xiaomi)
- FPS override cần game không opt-out khỏi interventions
- Vulkan texture filter **không có** trong GameMode API chuẩn
