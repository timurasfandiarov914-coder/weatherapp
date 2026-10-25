# SOLEN Weather

SOLEN is a premium, minimal Android weather app prototype with an atmospheric animated sky, live current conditions, hourly forecast, precipitation trends, weather details, a 7-day outlook, city search, and device location.

## Preview build

Every push to `main` triggers a GitHub Actions debug APK build. Open **Actions → Build SOLEN Android APK → latest successful run → Artifacts → SOLEN-Android-APK** to download the APK archive. Extract `SOLEN-preview.apk` and install it on an Android device.

## Weather data

Forecast and geocoding data are provided by [Open-Meteo](https://open-meteo.com/). No API key is required. An internet connection is needed to refresh weather and search for cities.

## Current prototype

- Native Android UI built with Kotlin and Jetpack Compose
- Animated atmospheric background and translucent weather panels
- Live conditions, apparent temperature, humidity, wind, sunrise and sunset
- Hourly forecast with precipitation probability and 7-day forecast
- City search and optional device location
- Android APK built automatically by GitHub Actions
