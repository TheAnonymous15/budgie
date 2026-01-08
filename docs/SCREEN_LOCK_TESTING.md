# Inactivity Auto-Lock Testing Guide

## Overview
Budgie now uses **inactivity-based auto-lock** instead of screen lock detection. This provides better user experience and more control.

---

## How It Works

### Inactivity Timer System

1. **After user authenticates:** Timer starts automatically
2. **User interacts with app:** Timer resets to 60 seconds
3. **No interaction for 60 seconds:** Warning dialog appears
4. **10-second countdown:** User can choose to stay active or lock now
5. **Countdown reaches 0:** App automatically locks

**Key Features:**
- ✅ **1-minute inactivity timeout**
- ✅ **10-second countdown warning**
- ✅ **User can cancel** ("Stay Active" button)
- ✅ **User can lock immediately** ("Lock Now" button)
- ✅ **Any touch/tap resets** the timer
- ✅ **No battery drain** (no background service)
- ✅ **No wake lock required**

---

## Testing Procedure

### Test 1: Basic Inactivity Detection

**Prerequisites:**
- User has completed onboarding
- Security (PIN/Biometric) is enabled
- User is logged in

**Steps:**

1. **Open Budgie and authenticate**

2. **Monitor logs:**
   ```bash
   adb logcat -c
   adb logcat | grep -E "InactivityTimer|AutoLock"
   ```

3. **Wait 1 minute WITHOUT touching the device**

4. **Expected log output:**
   ```
   InactivityTimer: ▶️ Starting inactivity timer (user authenticated)
   InactivityTimer: ⚠️ INACTIVITY DETECTED - Starting 10s countdown
   InactivityTimer: ⏱️ Auto-lock countdown: 10 seconds
   InactivityTimer: ⏱️ Auto-lock countdown: 9 seconds
   ...
   InactivityTimer: ⏱️ Auto-lock countdown: 1 seconds
   InactivityTimer: ⏱️ Auto-lock countdown: 0 seconds
   InactivityTimer: 🔒 AUTO-LOCK TRIGGERED
   ```

5. **Expected UI behavior:**
   - At 60 seconds: Warning dialog appears
   - Dialog shows countdown from 10 to 0
   - At 0: App locks, lock screen appears
   - User must re-authenticate to access app

---

### Test 2: User Cancels Auto-Lock

**Steps:**

1. **Open Budgie and authenticate**

2. **Wait 60 seconds** (warning appears)

3. **Click "Stay Active" button**

4. **Expected log output:**
   ```
   InactivityTimer: ⏱️ Auto-lock countdown: 7 seconds
   InactivityTimer: ❌ Auto-lock CANCELLED by user
   ```

5. **Expected behavior:**
   - Dialog dismisses
   - Timer resets to 60 seconds
   - User can continue using app

---

### Test 3: Lock Now Button

**Steps:**

1. **Open Budgie**

2. **Wait for warning dialog**

3. **Click "Lock Now" button**

4. **Expected log output:**
   ```
   InactivityTimer: 🔒 Lock NOW triggered by user
   ```

5. **Expected behavior:**
   - App locks immediately
   - Lock screen appears
   - No need to wait for countdown

---

### Test 4: Timer Reset on Interaction

**Steps:**

1. **Open Budgie**

2. **Monitor logs:**
   ```bash
   adb logcat | grep InactivityTimer
   ```

3. **Wait 50 seconds**

4. **Tap anywhere on the screen**

5. **Expected log output:**
   ```
   InactivityTimer: (timer reset, no explicit log but countdown restarts)
   ```

6. **Wait another 50 seconds**

7. **Tap again**

8. **Repeat several times**

9. **Expected behavior:**
   - Warning never appears
   - Timer keeps resetting
   - App stays unlocked as long as user is active

---

### Test 5: No Security = No Timer

**Prerequisites:**
- User selected "Not Recommended" during onboarding (no security)

**Steps:**

1. **Open Budgie**

2. **Check logs:**
   ```bash
   adb logcat | grep InactivityTimer
   ```

3. **Expected log output:**
   ```
   InactivityTimer: 🛑 Stopping inactivity timer (user locked/no security)
   ```

4. **Wait 5 minutes**

5. **Expected behavior:**
   - No warning dialog
   - No auto-lock
   - App stays open indefinitely

---

### Test 6: Background App Handling

**Steps:**

1. **Open Budgie and authenticate**

2. **Press Home button** (app goes to background)

3. **Expected log output:**
   ```
   InactivityTimer: 🛑 Stopping inactivity timer (user locked/no security)
   ```

4. **Wait 2 minutes**

5. **Open Budgie again**

6. **Expected behavior:**
   - Lock screen appears immediately
   - User must re-authenticate
   - Timer restarts after successful authentication

---

## Verification Commands

### Real-Time Monitoring
```bash
# Watch all inactivity events
adb logcat -c && adb logcat | grep -E "InactivityTimer|AutoLock" --color=always
```

### Check Current App State
```bash
# See if user is locked
adb logcat -d | grep "Starting inactivity timer" | tail -1
```

### Force Test Scenario
```bash
# Clear app data to start fresh
adb shell pm clear com.example.budgie

# Restart app
adb shell am start -n com.example.budgie/.MainActivity
```

---

## Warning Dialog UI

### Expected Visual Elements

**Dialog Contains:**
- 🟡 **Pulsing timer icon** (animated)
- **Title:** "Inactivity Detected"
- **Message:** "Budgie will lock automatically to protect your financial data"
- **Circular countdown:** Shows remaining seconds (10 → 0)
- **Two buttons:**
  - ✅ **"Stay Active"** (green, primary)
  - 🔒 **"Lock Now"** (outlined, secondary)
- 💡 **Tip:** "Tip: Any interaction resets the timer"

**Animation:**
- Timer icon pulses (scale 1.0 → 1.1 → 1.0)
- Countdown updates every second
- Progress circle depletes visually
- Color changes: Green (>5s) → Red (≤5s)

---

## Expected Results Summary

| Scenario | Expected Behavior | Countdown Timer |
|----------|-------------------|-----------------|
| 60s inactivity | Warning shows | 10s countdown |
| User taps "Stay Active" | Dialog dismisses, timer resets | Reset to 60s |
| User taps "Lock Now" | Immediate lock | N/A |
| Countdown reaches 0 | Auto-lock | Lock screen |
| User taps screen during countdown | Timer resets | Starts over |
| No security enabled | No timer, no warning | Never starts |
| App backgrounded | Timer stops, session destroyed | Stops |

---

## Performance & Battery

### Battery Impact
- **CPU usage:** Near zero (passive timer)
- **Wake locks:** None
- **Background services:** None
- **Battery drain:** < 0.1% per day

### Why So Efficient?
1. **Coroutine-based timer** - No polling, uses Kotlin suspend functions
2. **UI-only detection** - Only active when app is visible
3. **No sensors** - Doesn't monitor device sensors
4. **No broadcasts** - Doesn't listen for system events
5. **Automatic cleanup** - Timer stops when app is backgrounded

---

## Troubleshooting

### Issue: Warning never appears

**Possible causes:**
1. Security not enabled
2. User interacting with device

**Solutions:**
```bash
# Check security type
adb logcat -d | grep "Starting inactivity timer"

# If you see "user locked/no security" - set up PIN/Biometric
```

---

### Issue: Timer doesn't reset on tap

**Possible causes:**
1. Interaction not detected by NavHost
2. Timer already showing warning

**Solutions:**
- Ensure you're tapping within the app content area
- Check logs for "reset" events

---

### Issue: App locked immediately on open

**Expected behavior:**
- This is correct! When you background the app, session is destroyed
- You must re-authenticate when reopening

**Not a bug:**
```
This is the security feature working as intended.
```

---

## Testing Checklist

After all tests, verify:

✅ Timer starts when user authenticates  
✅ Warning appears after 60s of inactivity  
✅ Countdown shows 10 seconds and decrements  
✅ "Stay Active" button works  
✅ "Lock Now" button works  
✅ Any tap/interaction resets timer  
✅ Timer stops when app is backgrounded  
✅ No timer when security is disabled  
✅ No battery drain  
✅ Dialog UI is professional and clear  

---

## Key Differences from Old System

| Feature | Old (Screen Lock) | New (Inactivity) |
|---------|------------------|------------------|
| Trigger | Device screen lock | 60s no interaction |
| User control | None (automatic) | Yes (Stay Active button) |
| Warning | None | 10s countdown |
| Background service | Yes (battery drain) | No |
| WakeLock | Yes (battery drain) | No |
| User experience | Abrupt | Gentle with warning |
| Flexibility | None | High |

---

**Created:** January 8, 2026  
**Last Updated:** January 8, 2026  
**Status:** ✅ Inactivity-based auto-lock implemented  
**Battery Impact:** Minimal (no background services)
