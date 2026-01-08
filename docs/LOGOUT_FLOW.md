# Logout Flow in Budgie App

## Overview
The Budgie app has **two types of logout mechanisms**:

1. **Manual Logout** - User-initiated through the UI
2. **Auto-Lock Logout** - Automatic when device screen locks (if enabled)

---

## 1. Manual Logout Flow

### Initiation Points

#### A. Dashboard Menu
Location: `ui/screens/DashboardScreen.kt` - Top app bar menu

**User Journey:**
1. User opens the dashboard
2. User clicks the menu icon (top right)
3. User selects "Logout" from the dropdown menu
4. A premium confirmation dialog appears
5. User confirms logout

**Code Flow:**
```
DashboardTopBar() 
  → DropdownMenu item "Logout" clicked
  → showLogoutDialog = true
  → PremiumConfirmationDialog shown
  → User clicks "Logout" button
  → onLogout() callback invoked
```

**Dialog Details:**
- **Title:** "Logout Session"
- **Subtitle:** "Secure Sign Out"
- **Message:** "Your session will be ended and you'll need to authenticate again to access your financial data."
- **Confirm Button:** "Logout" (Amber color)
- **Cancel Button:** "Stay Logged In"

### Logout Execution

Location: `ui/navigation/BudgieNavigation.kt` (lines 183-186)

```kotlin
onLogout = {
    securityManager.setAuthenticated(false)
    isLocked = true
}
```

**Steps:**
1. **Clear Authentication State**
   - Calls `securityManager.setAuthenticated(false)`
   - This writes to encrypted SharedPreferences
   - Key: `is_authenticated` → value: `false`

2. **Lock the App**
   - Sets `isLocked = true` in the navigation state
   - This triggers the lock screen to appear
   - User must re-authenticate to access the app

---

## 2. Auto-Lock Logout Flow

### Trigger Condition
When the **device screen locks** (not the app, the actual device)

### Prerequisites
- Auto-lock must be enabled in Settings
- User must have security enabled (PIN or Biometric)

### Service Architecture

#### ScreenLockService
Location: `security/ScreenLockService.kt`

**Service Type:** Foreground Service
- Starts when app launches
- Runs continuously in background
- Monitors device screen state via BroadcastReceivers

**Important Notes:**
- **No WakeLock needed**: BroadcastReceivers for screen state changes (`ACTION_SCREEN_OFF`, `ACTION_SCREEN_ON`, `ACTION_USER_PRESENT`) work without a WakeLock
- The service uses a foreground notification to ensure it stays running
- Android system delivers screen state broadcasts even when the app is in the background

**Broadcast Receivers:**
The service listens for these system intents:
- `Intent.ACTION_SCREEN_OFF` - Device screen turned off/locked
- `Intent.ACTION_SCREEN_ON` - Device screen turned on
- `Intent.ACTION_USER_PRESENT` - Device unlocked by user

### Auto-Lock Flow

**Step-by-Step:**

1. **Device Lock Detection**
   ```
   Device screen locks
   → System broadcasts Intent.ACTION_SCREEN_OFF
   → ScreenLockService.screenLockReceiver receives it
   ```

2. **Auto-Lock Check**
   ```kotlin
   handleScreenLock() {
       if (securityManager.isAutoLockEnabled()) {
           // Proceed with logout
       }
   }
   ```

3. **Session Destruction**
   ```kotlin
   securityManager.destroySession()
   // Internally calls: setAuthenticated(false)
   ```

4. **Broadcast Event**
   ```kotlin
   val broadcastIntent = Intent(ACTION_SESSION_DESTROYED)
   sendBroadcast(broadcastIntent)
   ```

5. **Activity Recreation**
   ```
   MainActivity.sessionDestroyedReceiver receives broadcast
   → MainActivity.recreate() called
   → App restarts
   → BudgieNavigation checks authentication
   → isLocked = true (user not authenticated)
   → Lock screen appears
   ```

---

## 3. SecurityManager Methods

Location: `security/SecurityManager.kt`

### Authentication State Management

```kotlin
// Set authentication state
fun setAuthenticated(authenticated: Boolean) {
    encryptedPrefs.edit()
        .putBoolean(KEY_IS_AUTHENTICATED, authenticated)
        .apply()
}

// Check authentication state
fun isAuthenticated(): Boolean {
    return encryptedPrefs.getBoolean(KEY_IS_AUTHENTICATED, false)
}

// Destroy session (logout)
fun destroySession() {
    setAuthenticated(false)
}
```

### Auto-Lock Setting

```kotlin
// Check if auto-lock is enabled
fun isAutoLockEnabled(): Boolean {
    return encryptedPrefs.getBoolean(KEY_AUTO_LOCK_ENABLED, true)
    // Default: true (enabled by default)
}

// Enable/disable auto-lock
fun setAutoLockEnabled(enabled: Boolean) {
    encryptedPrefs.edit()
        .putBoolean(KEY_AUTO_LOCK_ENABLED, enabled)
        .apply()
}
```

---

## 4. Lock Screen Behavior

After logout (manual or auto), the app shows the lock screen:

Location: `ui/screens/lockscreen/LockScreen.kt`

**User must authenticate using:**
- PIN (5 digits)
- Biometric (fingerprint/face)
- PIN + Biometric (depending on security settings)

**Upon successful authentication:**
```kotlin
securityManager.setAuthenticated(true)
isLocked = false
// User can access the dashboard
```

---

## 5. Session Lifecycle

### App Launch
```
App starts
→ SecurityManager initialized
→ isAuthenticated() checked
→ If false: Lock screen shown
→ If true: Dashboard shown
```

### App Background (onStop)
```
App goes to background
→ MainActivity.onStop() called
→ securityManager.destroySession() called
→ Session destroyed
→ When app returns: Lock screen shown
```

### Device Lock (with auto-lock enabled)
```
Device locks
→ ScreenLockService detects
→ Session destroyed
→ Broadcast sent
→ MainActivity recreated
→ Lock screen shown
```

---

## 6. User Settings

Users can control auto-lock in:

**Location:** Settings Screen → "Auto-Lock on Screen Lock" toggle

```kotlin
SettingsToggleItem(
    icon = Icons.Default.PhoneLocked,
    title = "Auto-Lock on Screen Lock",
    subtitle = "Sign out when device locks",
    checked = autoLockEnabled,
    onCheckedChange = {
        autoLockEnabled = it
        securityManager.setAutoLockEnabled(it)
    }
)
```

**Default:** Enabled (true)

---

## 7. Security Features

### Encrypted Storage
All security data stored using:
- **EncryptedSharedPreferences**
- **AES256_GCM** encryption
- **MasterKey** with KeyStore

### Data Protected
- Authentication state
- PIN hash (SHA-256)
- Auto-lock preference
- Security type (none/PIN/biometric)

---

## 8. Logout vs Exit

| Feature | Logout | Exit |
|---------|--------|------|
| **Purpose** | End session, lock app | Close app completely |
| **Authentication State** | Cleared (`isAuthenticated = false`) | Cleared |
| **App Process** | Continues running | Terminated (`finishAffinity()`) |
| **Next Launch** | Shows lock screen | Fresh start with lock screen |
| **Use Case** | Secure the app while keeping it running | Completely close the app |

---

## 9. Testing the Logout Flow

### Manual Logout Test
1. Open Budgie app
2. Click menu icon (top right)
3. Select "Logout"
4. Confirm in dialog
5. ✅ Lock screen should appear
6. Re-authenticate
7. ✅ Dashboard should load

### Auto-Lock Test
1. Enable "Auto-Lock on Screen Lock" in Settings
2. Lock your device (press power button)
3. Unlock your device
4. Open Budgie app
5. ✅ Lock screen should appear (session destroyed)
6. Re-authenticate
7. ✅ Dashboard should load

### Logcat Monitoring
```bash
# Watch auto-lock logs
adb logcat | grep -E "ScreenLockService|MainActivity"

# Expected output when device locks:
# ScreenLockService: ===== SCREEN TURNED OFF - DEVICE LOCKED =====
# ScreenLockService: Auto-lock enabled: true
# ScreenLockService: ===== USER SESSION DESTROYED - SIGNED OUT =====
# MainActivity: Session destroyed - Device locked, recreating activity
```

---

## 10. Summary

**Logout is initiated through:**

1. **UI Action**
   - User clicks "Logout" in dashboard menu
   - Confirmation dialog shown
   - `onLogout()` callback triggered
   - `securityManager.setAuthenticated(false)` called
   - `isLocked = true` set
   - Lock screen appears

2. **Auto-Lock**
   - Device screen locks
   - ScreenLockService detects `ACTION_SCREEN_OFF`
   - Checks if auto-lock enabled
   - Calls `destroySession()`
   - Broadcasts session destroyed event
   - MainActivity recreates
   - Lock screen appears

**Both methods result in:**
- Authentication state cleared
- User must re-authenticate
- Secure app locking
- Data remains encrypted and protected

---

**Created:** January 8, 2026  
**Version:** 1.0  
**Author:** Budgie Development Team

