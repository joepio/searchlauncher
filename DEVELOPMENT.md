# Development Guide

## Project Overview

SearchLauncher is an Android application that provides system-wide search functionality through gesture activation. The app is built using modern Android development practices with Kotlin and Jetpack Compose.

## Technology Stack

- **Language**: Kotlin 1.9.20
- **UI Framework**: Jetpack Compose with Material 3
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: API 29 (Android 10)
- **Target SDK**: API 34 (Android 14)

## Key Components

### 1. Application Layer
- `SearchLauncherApp`: Main application class, handles notification channel creation

### 2. Data Layer
- `SearchRepository`: Manages app search and content search functionality
- `AppInfo`: Data model for installed applications
- `SearchResult`: Sealed class representing search results (Apps and Content)

### 3. Service Layer
- `OverlayService`: Foreground service that manages the overlay window and edge detection
- `GestureAccessibilityService`: Accessibility service for detecting system-wide gestures
- `SearchWindowManager`: Manages the floating search window using WindowManager

### 4. UI Layer
- `MainActivity`: Main entry point with permission checks and service controls
- `OnboardingScreen`: Multi-step onboarding flow for permission requests
- Compose UI for search interface with Material 3 design

## Architecture Decisions

### Overlay System
The app uses Android's `SYSTEM_ALERT_WINDOW` permission to display a floating search interface. This is implemented as:
1. A thin edge detector view that captures swipe gestures
2. A full search window that appears on gesture completion

### Search Implementation
Search is performed in two stages:
1. **App Search**: Queries `PackageManager` for installed launcher activities
2. **Content Search**: Uses Android's AppSearch API (currently limited by app support)

### Gesture Detection
Two methods are used:
1. **Edge Detection**: A thin overlay view at screen edge detects swipe gestures
2. **Accessibility Service**: Provides additional context and alternative activation methods

## Development Setup

### Prerequisites
```bash
# Install Android Studio
# Configure Android SDK with API 34
# Install JDK 17
```

### First Time Setup
```bash
# Clone the repository
git clone https://github.com/joepio/searchlauncher.git
cd searchlauncher

# Open in Android Studio
# Let Gradle sync complete
# Connect Android device or start emulator

# Build and run
./gradlew installDebug
```

### Testing on Device
For best testing experience:
1. Use a physical Android device (API 29+)
2. Enable Developer Options and USB Debugging
3. Grant all permissions during onboarding
4. Test gesture from different apps

## Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add comments for complex logic
- Use Compose best practices

## Common Development Tasks

### Adding a New Permission
1. Add permission to `AndroidManifest.xml`
2. Create permission check function in `MainActivity.kt`
3. Add permission step to `OnboardingScreen.kt`
4. Update UI to show permission status

### Modifying Search Logic
Edit `SearchRepository.kt`:
- `searchApps()`: Modify app search behavior
- `searchContent()`: Modify content search with AppSearch
- `sortAppsByUsage()`: Change app ranking logic

### Customizing UI
Edit Compose files:
- `SearchWindowManager.kt`: Search overlay UI
- `OnboardingScreen.kt`: Onboarding flow
- `MainActivity.kt`: Home screen
- `theme/Theme.kt`: App theming

## Debugging Tips

### Service Issues
```bash
# Check if service is running
adb shell dumpsys activity services | grep SearchLauncher

# View logs
adb logcat -s SearchLauncher
```

### Permission Issues
- Check Settings > Apps > SearchLauncher > Permissions
- Verify overlay permission: Settings > Apps > Special app access > Display over other apps
- Check accessibility: Settings > Accessibility > SearchLauncher

### Gesture Not Working
- Ensure edge detector view is added (check WindowManager)
- Verify touch events are being received
- Check if another app is blocking touch events

## Performance Considerations

- **Memory**: Overlay service runs continuously - keep memory footprint minimal
- **Battery**: Use efficient event listeners, avoid polling
- **Search Speed**: Implement debouncing (300ms) for search queries
- **UI Responsiveness**: Perform search operations on background threads

## Security Considerations

- Only request necessary permissions
- Clear explanation for each permission
- Don't access or store sensitive data
- Respect user privacy in search indexing

## Building for Release

```bash
# Create release build
./gradlew assembleRelease

# Sign the APK
# (Configure signing in app/build.gradle.kts)

# Test release build thoroughly
adb install app/build/outputs/apk/release/app-release.apk
```

## Troubleshooting

### Build Failures
- Clean and rebuild: `./gradlew clean build`
- Invalidate Android Studio caches
- Update Gradle dependencies

### Runtime Crashes
- Check logcat for stack traces
- Verify all permissions are granted
- Test on different Android versions

### UI Issues
- Test on different screen sizes
- Verify Compose preview renders
- Check Material 3 theme application

## Resources

- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://m3.material.io/)
- [AppSearch](https://developer.android.com/guide/topics/search/appsearch)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
