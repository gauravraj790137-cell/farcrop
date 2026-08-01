# FarCrop Android App

This folder contains the Flutter app for the FarCrop project.

## Setup

1. Install Flutter SDK.
2. From this folder, run:

```bash
flutter pub get
flutter run
```

## Notes

- The app uses the backend base URL from Settings and sends requests to `/predict`.
- The backend must be running and reachable from the device/emulator.
- For local testing, set the base URL to your computer's LAN IP followed by `:8000`.
