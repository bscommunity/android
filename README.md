<picture>
  <source media="(prefers-color-scheme: dark)" srcset="/.github/cover.png">
  <source media="(prefers-color-scheme: light)" srcset="/.github/cover_light.png">
    <img alt="bscm Android" src="/.github/cover_light.png">
</picture>

[![Crowdin](https://badges.crowdin.net/bscm/localized.svg)](https://crowdin.com/project/bscm)

## 📱 About

This repository contains the official bscm Android app, allowing players to explore, download, and manage charts directly from their mobile device.

The app is built with Kotlin, Java, and C++, focused on performance, usability, and integration with the bscm ecosystem.

> [!WARNING]
> This project is a **_work in progress_**! We’re actively developing features and improving the experience. Check back often for updates.

## 🚀 Features

- **Explore and download charts**  
  Browse, view details, and download charts directly in the app.
- **Deep links and system integration**  
  Supports links like `bscm://chart/details/{id}` for quick navigation.
- **Update management**  
  Download and install app and chart updates.
- **Preferences and settings**  
  Customize themes, notifications, and app behavior.
- **Community sync**  
  Integration with API and website for always up-to-date data.

## 📦 Project Structure

- `app/src/main/java/com/meninocoiso/bscm/`
    - `data/` — Data layer (local, remote, repositories)
        - `local/` — Local database and DAOs
        - `manager/` — Chart and download managers
        - `remote/` — API clients and DTOs
        - `repository/` — Data repositories
    - `di/` — Dependency injection modules
    - `domain/` — Models, enums, and lists
    - `presentation/` — UI, navigation, screens, components, viewmodels
    - `service/` — Update and download services
    - `util/` — Utilities
- `BaseApplication.kt`, `MainActivity.kt` — App entrypoints
- `build.gradle` — Project configuration

## 🛠️ Running Locally

1. Install dependencies:
    ```bash
    ./gradlew assembleDebug
    ```
2. Run the app on an emulator or Android device:
    ```bash
    ./gradlew installDebug
    ```
3. To test deep links:
    ```bash
    ./adb shell am start -a android.intent.action.VIEW -d "bscm://chart/details/{id}" com.meninocoiso.bscm
    ```

> Requires [Android Studio](https://developer.android.com/studio) and [Java 17+](https://adoptium.net/) installed.

### Sensitive Variables and Keystore

- To sign the app, convert the keystore:
    ```bash
    base64 -w 0 app/keystore.jks > keystore.b64
    ```
- To generate the SHA-256 hash:
    ```bash
    keytool -list -v -keystore keystore.jks
    ```
- Configure variables and credentials in the `local.properties` file or via environment as needed.

## 🤝 Contributing

Contributions are welcome!

- Found a bug? Open an [issue](https://github.com/bscommunity/bscm/issues)
- Have a feature idea? Suggest or submit a PR
- Into mobile design? Help us improve the UI and experience!

## 📄 License

This project follows the bscm organization license. See the main repository for details.