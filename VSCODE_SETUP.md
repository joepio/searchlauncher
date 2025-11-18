# VSCode Setup Guide for SearchLauncher

Yes, you can absolutely use VSCode to work on this Android project! Here's how to set it up.

## Prerequisites

1. **Java Development Kit (JDK) 17**
   ```bash
   # Check if installed
   java -version

   # macOS (using Homebrew)
   brew install openjdk@17

   # Linux
   sudo apt install openjdk-17-jdk

   # Windows
   # Download from https://adoptium.net/
   ```

2. **Android SDK**
   ```bash
   # macOS (using Homebrew)
   brew install --cask android-commandlinetools

   # Or download Android Studio to get SDK
   # Then set ANDROID_HOME environment variable
   export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
   export ANDROID_HOME=$HOME/Android/Sdk          # Linux
   ```

3. **ADB (Android Debug Bridge)**
   ```bash
   # Usually comes with Android SDK
   # Add to PATH
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

## VSCode Extensions

Install these recommended extensions (VSCode will prompt when you open the project):

1. **Kotlin** (fwcd.kotlin) - Kotlin language support
2. **Kotlin Language** (mathiasfrohlich.kotlin) - Additional Kotlin features
3. **Java Extension Pack** (vscjava.vscode-java-pack) - Java/Gradle support
4. **Gradle for Java** (vscjava.vscode-gradle) - Gradle tasks

### Install Extensions via Command Line
```bash
code --install-extension fwcd.kotlin
code --install-extension mathiasfrohlich.kotlin
code --install-extension vscjava.vscode-java-pack
code --install-extension vscjava.vscode-gradle
```

## Opening the Project

```bash
cd searchlauncher
code .
```

## Building from VSCode

### Method 1: Using VSCode Tasks
Press `Cmd/Ctrl + Shift + B` to run the default build task, or:

1. `Cmd/Ctrl + Shift + P` → "Tasks: Run Task"
2. Select:
   - **Build Debug** - Builds the APK
   - **Install Debug** - Builds and installs on device
   - **Clean Build** - Clean and rebuild
   - **View Logs** - Watch logcat output

### Method 2: Using Integrated Terminal
Open terminal in VSCode (`Ctrl/Cmd + `` `):

```bash
# Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean build

# Run with logs
./gradlew installDebug && adb logcat -s SearchLauncher
```

### Method 3: Using Gradle Sidebar
1. Look for "Gradle" in the sidebar (Java/Gradle extension)
2. Expand "searchlauncher > Tasks > build"
3. Click "assembleDebug" or "installDebug"

## Running the App

1. **Connect Android Device**
   ```bash
   # Check device is connected
   adb devices
   ```

2. **Build and Install**
   ```bash
   ./gradlew installDebug
   ```

3. **Launch App**
   ```bash
   # Open app
   adb shell am start -n com.searchlauncher.app/.ui.MainActivity
   ```

## Debugging

### View Logs
```bash
# All logs from app
adb logcat -s SearchLauncher

# Clear and watch
adb logcat -c && adb logcat -s SearchLauncher

# Save to file
adb logcat -s SearchLauncher > logs.txt
```

### Inspect App
```bash
# Check if service is running
adb shell dumpsys activity services | grep SearchLauncher

# Check app info
adb shell dumpsys package com.searchlauncher.app

# Clear app data
adb shell pm clear com.searchlauncher.app
```

### Uninstall
```bash
adb uninstall com.searchlauncher.app
```

## VSCode Workspace Tips

### 1. Code Navigation
- `Cmd/Ctrl + P` - Quick file open
- `Cmd/Ctrl + Shift + O` - Go to symbol
- `F12` - Go to definition
- `Shift + F12` - Find references

### 2. Kotlin Features
- Auto-completion works out of the box
- Error highlighting
- Code formatting (install Kotlin formatter)
- Rename refactoring

### 3. Gradle Integration
- Gradle tasks appear in sidebar
- Auto-sync on build.gradle changes
- Run tasks from command palette

## What Works in VSCode

✅ **Full support:**
- Code editing with syntax highlighting
- Auto-completion and IntelliSense
- Go to definition/references
- Building with Gradle
- Installing on device
- Viewing logs
- Git integration
- Terminal access

⚠️ **Limited support:**
- No visual layout editor (edit XML manually)
- No APK analyzer (use command line)
- No device emulator integration (use separate emulator)
- No visual debugger (use logcat)

## What's Better in Android Studio

If you need these features, consider using Android Studio:
- Visual layout editor for XML
- Compose preview
- Built-in emulator
- Visual debugger with breakpoints
- APK analyzer
- Profiler tools (CPU, memory, network)
- Lint integration with quick fixes

## Recommended Workflow

For **SearchLauncher**, VSCode is perfectly fine because:
- ✅ Pure Kotlin code (no complex layouts)
- ✅ Jetpack Compose (code-based UI, no XML)
- ✅ Simple Gradle build
- ✅ Can build and install from terminal
- ✅ Lightweight and fast

### Suggested Setup:
1. Use **VSCode** for coding
2. Use **terminal** for building and installing
3. Use **adb logcat** for debugging
4. Use **physical device** for testing (easier than emulator setup)

## Quick Commands Cheat Sheet

```bash
# Build and install
./gradlew installDebug

# Watch logs
adb logcat -s SearchLauncher

# Restart app
adb shell am force-stop com.searchlauncher.app
adb shell am start -n com.searchlauncher.app/.ui.MainActivity

# Clean install
./gradlew uninstallDebug installDebug

# Check device
adb devices

# Take screenshot
adb exec-out screencap -p > screenshot.png
```

## Troubleshooting

### "gradle: command not found"
Use `./gradlew` (Gradle wrapper) instead of `gradle`

### "ANDROID_HOME not set"
```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### "No connected devices"
```bash
# Check USB debugging is enabled on phone
adb devices

# If unauthorized, accept prompt on phone
```

### Kotlin extension not working
1. Reload VSCode
2. Check Java is installed: `java -version`
3. Try: `Cmd/Ctrl + Shift + P` → "Reload Window"

### Build fails
```bash
# Clean and rebuild
./gradlew clean build

# Check Java version
java -version  # Should be 17
```

## Conclusion

**TL;DR**: Yes, you can use VSCode!

Just open the folder, install the extensions, and use `./gradlew installDebug` to build and install. You'll have a great development experience for this Kotlin-based Android project.

---

**Happy coding in VSCode!** 🚀
