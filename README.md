# Project Launcher 3.0 — Motorola G84 UI refresh

A personal, offline-first Android launcher for waking a free MongoDB server, storing project links locally, and opening projects with one tap.

## What changed in 3.0
- Fixed status-bar overlap/edge-to-edge spacing on modern Android phones.
- Rebuilt the bottom navigation with a safer, cleaner layout and a floating add button.
- Replaced the old gray Android dialogs with custom dark dialogs that resize/scroll correctly when the keyboard opens.
- Added real project-image previews on project cards.
- Added project search filtering.
- Improved typography, spacing, contrast, card hierarchy and touch targets for a 6.5-inch 120 Hz phone such as the Motorola G84.
- Kept the app dependency-free: Android SDK only, local storage, no paid API.

## Main features
- Futuristic dark/neon responsive UI using only Android SDK views (no paid API and no cloud backend required).
- Home: Start MongoDB Server, server status, and My Projects.
- Projects: searchable project cards with image, React/frontend, Node/backend and GitHub badges.
- Add/Edit/Delete projects locally.
- Manage: edit/delete/reorder projects and configure the server wake/status trigger.
- Server animation + success/failure screen based on the HTTP response.
- Settings: local accent color selection; no online appearance API.
- Project images are selected from the phone and persisted as URI permissions.

## Build
Open the `ProjectLauncher` folder in Android Studio. Let Gradle sync, then **Build > Build APK(s)**.

The final APK will normally appear under `app/build/outputs/apk/debug/app-debug.apk`.

## Important
The app itself does not require a backend. It only makes HTTP requests to the endpoint you configure for waking/checking your MongoDB/free server. Your project links and settings stay on the phone.
