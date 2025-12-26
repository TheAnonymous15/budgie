# Running Budgie App on a Physical Device

## 📱 Project Overview

**Budgie** is a comprehensive Android budget tracking and financial management application built with modern Android development practices.

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Database (local persistence)
- **Navigation**: Navigation Compose
- **Async Operations**: Kotlin Coroutines
- **Data Storage**: DataStore Preferences
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

### Features
Based on the screens found in the project:
- 💰 **Dashboard** - Overview of financial status
- 💸 **Expense Tracking** - Record and manage expenses
- 💵 **Income Tracking** - Track income sources
- 📊 **Budget Management** - Set and monitor budgets
- 📅 **Bills Management** - Track recurring bills
- 📈 **Insights** - Financial analytics and trends
- 💎 **Investments** - Investment tracking
- 🔮 **Wealth Projection** - Future financial projections

---

## ✅ Prerequisites

Before running the app, ensure you have:

1. **Android Studio** (latest version recommended - Hedgehog or newer)
2. **JDK 11** or higher
3. **Android SDK** with minimum API 24
4. **A Physical Android Device** running Android 7.0 or higher

---

## 🔧 Setting Up Your Physical Device

### Step 1: Enable Developer Options

1. Open **Settings** on your Android device
2. Scroll to **About Phone** (or **About Device**)
3. Find **Build Number** (may be under Software Information)
4. Tap **Build Number** 7 times
5. You'll see a message: "You are now a developer!"

### Step 2: Enable USB Debugging

1. Go back to main **Settings**
2. Find **Developer Options** (usually under System or Advanced)
3. Enable **Developer Options** toggle
4. Find and enable **USB Debugging**
5. (Optional but recommended) Enable **Install via USB**
6. (Optional) Enable **USB Debugging (Security Settings)** if available

### Step 3: Connect Your Device

1. Connect your Android device to your Mac via USB cable
2. On your device, you'll see a prompt: "Allow USB debugging?"
3. Check "Always allow from this computer"
4. Tap **OK** or **Allow**

### Step 4: Verify Connection

Open Terminal and run:
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb devices
```

You should see output like:
```
List of devices attached
ABC123XYZ    device
```

If you see "unauthorized", unlock your phone and check for the USB debugging prompt.

---

## 🚀 Running the App

### Method 1: Using Android Studio (Recommended)

1. **Open the Project**
   - Launch Android Studio
   - Select "Open" and navigate to `/Users/danielkinyua/Downloads/projects/budgie`
   - Wait for Gradle sync to complete

2. **Select Your Device**
   - Look at the top toolbar in Android Studio
   - Click the device dropdown (next to the Run button)
   - Your physical device should appear in the list
   - Select your device

3. **Run the App**
   - Click the green **Run** button (▶️) or press `Ctrl + R`
   - The app will build and install on your device
   - It will launch automatically

### Method 2: Using Command Line

1. **Navigate to Project Directory**
   ```bash
   cd /Users/danielkinyua/Downloads/projects/budgie
   ```

2. **Build and Install Debug APK**
   ```bash
   ./gradlew installDebug
   ```

3. **Launch the App Manually**
   ```bash
   /Users/danielkinyua/Library/Android/sdk/platform-tools/adb shell am start -n com.example.budgie/.MainActivity
   ```

### Method 3: Build APK and Install Manually

1. **Build the APK**
   ```bash
   cd /Users/danielkinyua/Downloads/projects/budgie
   ./gradlew assembleDebug
   ```

2. **Locate the APK**
   The APK will be at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Install on Device**
   ```bash
   /Users/danielkinyua/Library/Android/sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🐛 Troubleshooting

### Device Not Showing Up

**Check USB Connection**
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb devices
```

If empty or shows "unauthorized":
- Unlock your device
- Check for USB debugging prompt
- Revoke previous authorizations in Developer Options and reconnect
- Try a different USB cable (some cables are charge-only)

**Restart ADB Server**
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb kill-server
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb start-server
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb devices
```

### Build Failures

**Clean and Rebuild**
```bash
./gradlew clean build
```

**Invalidate Caches in Android Studio**
- File → Invalidate Caches → Invalidate and Restart

### Installation Failed

**Uninstall Previous Version**
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb uninstall com.example.budgie
```

Then try installing again.

### App Crashes on Launch

**Check Logcat**
In Android Studio: View → Tool Windows → Logcat

Or via command line:
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb logcat | grep "budgie"
```

---

## 📝 Build Information

### Current Build Status
✅ Project builds successfully with some deprecation warnings (non-critical)

### Warnings (Safe to Ignore)
- Deprecated icon APIs (cosmetic, doesn't affect functionality)
- Experimental Coroutines API usage (already stable in production)
- menuAnchor deprecated API (UI component, works fine)

### Build Output Location
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 🔄 Development Workflow

### Quick Development Cycle

1. Make code changes in Android Studio
2. Click Run (Android Studio auto-installs and launches)
3. Or use: `./gradlew installDebug` from terminal

### View Logs
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb logcat
```

Filter for your app:
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb logcat | grep -i budgie
```

### Take Screenshots
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb shell screencap -p /sdcard/screen.png
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb pull /sdcard/screen.png
```

---

## 📦 Dependencies

The project uses these major libraries:
- AndroidX Core KTX
- Jetpack Compose (UI, Material3, Navigation)
- Room Database (Runtime, KTX, Compiler via KSP)
- Lifecycle & ViewModel
- Kotlin Coroutines
- DataStore Preferences
- Gson (JSON serialization)
- Material Icons Extended

All dependencies are managed via Gradle Version Catalogs (`gradle/libs.versions.toml`).

---

## 🎯 Next Steps

1. **Connect your physical device** using the steps above
2. **Open the project in Android Studio**
3. **Select your device** from the device dropdown
4. **Click Run** and enjoy testing Budgie!

The app is fully functional with:
- Local database for storing transactions, budgets, bills
- Multiple screens for comprehensive financial management
- Material3 design with dark/light theme support
- Navigation between different financial tracking features

---

## 💡 Tips

- **Keep USB Debugging enabled** while developing
- **Use "Stay Awake"** option in Developer Options to prevent screen timeout while charging
- **Enable "Don't keep activities"** in Developer Options to test app state restoration
- The app stores data locally using Room Database - data persists across app restarts
- First launch will initialize an empty database

---

## 📞 Support

If you encounter issues:
1. Check the Troubleshooting section above
2. Verify your device meets minimum requirements (Android 7.0+)
3. Ensure USB debugging is properly enabled
4. Check Android Studio's Event Log for detailed error messages

---

**Happy Budget Tracking! 💰📊**

