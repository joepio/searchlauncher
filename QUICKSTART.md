# Quick Start Guide

Get SearchLauncher up and running in 5 minutes!

## Step 1: Build the App

### Option A: Using VSCode
1. Open VSCode
2. File → Open Folder → select `searchlauncher`
3. Open terminal in VSCode (Ctrl/Cmd + `)
4. Build and install:
```bash
./gradlew installDebug
```

### Option B: Using Android Studio
1. Open Android Studio
2. Click "Open" and select the `searchlauncher` folder
3. Wait for Gradle sync to complete
4. Click the green "Run" button (▶️)
5. Select your device or emulator

### Option C: Using Command Line
```bash
# Navigate to project directory
cd searchlauncher

# Build and install
./gradlew installDebug

# Or on Windows
gradlew.bat installDebug
```

## Step 2: Complete Onboarding

When you first launch the app, you'll see a 5-step onboarding process:

### Screen 1: Welcome
- Introduces the app
- Click "Get Started"

### Screen 2: Display Over Other Apps
- **Required** for the app to work
- Click "Grant Permission"
- Toggle the switch on in Settings
- Press back button
- Click "Continue"

### Screen 3: Accessibility Service
- **Required** for gesture detection
- Click "Grant Permission"
- Find "SearchLauncher" in the list
- Toggle it on
- Confirm the dialog
- Press back button
- Click "Continue"

### Screen 4: Usage Access
- **Optional** (helps sort apps by recent use)
- Click "Grant Permission" or "Skip"
- If granting: toggle on in Settings
- Click "Continue"

### Screen 5: Complete
- Click "Finish"
- The service will start automatically

## Step 3: Use the App

### Method 1: Gesture (Recommended)
1. Open any app on your phone
2. Swipe from the left edge of the screen (about 40px from edge)
3. Swipe back to the edge
4. The search bar appears!

### Method 2: From Home Screen
1. Open SearchLauncher app
2. Click "Start Service" if not already running
3. The notification will appear
4. Use the gesture method above

## Step 4: Search and Launch

1. **Search bar appears** as an overlay
2. **Type to search** - results appear instantly
3. **Tap any result** to launch the app
4. **Tap outside** the search bar to dismiss it

## Tips

- The service runs in the background (check notification)
- You can start/stop the service from the main app
- Search works on app names and package names
- Results are sorted by recent usage (if permission granted)
- The gesture works from any screen when service is running

## Troubleshooting

### Gesture not working?
- ✅ Check that overlay permission is granted
- ✅ Check that accessibility service is enabled
- ✅ Check that the service is running (notification visible)
- ✅ Try swiping slower and more deliberately
- ✅ Make sure you swipe from the very edge

### App not appearing in search?
- Only shows apps that can be launched (have launcher intent)
- System apps are filtered out (unless updated)
- Try searching for the exact app name

### Search bar won't appear?
- ✅ Verify overlay permission: Settings > Apps > SearchLauncher > Display over other apps
- ✅ Check service status in the app
- ✅ Restart the service from the main screen

### Service keeps stopping?
- Some Android manufacturers aggressively kill background services
- Try: Settings > Battery > SearchLauncher > Unrestricted
- Add SearchLauncher to "Protected apps" (varies by manufacturer)

## What's Next?

- Explore all installed apps by searching with empty query
- Try different search terms
- Adjust permissions if needed in Settings
- Check the main app for service status

## Need Help?

- Check [DEVELOPMENT.md](DEVELOPMENT.md) for technical details
- Review [README.md](README.md) for full documentation
- Check logcat for error messages: `adb logcat -s SearchLauncher`

---

**Enjoy your new search launcher!** 🚀
