#!/bin/bash
# Quick status check for ScreenLockService

echo "╔════════════════════════════════════════════════════════════╗"
echo "║        Budgie Screen Lock Service Status Check            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if app is installed
echo "📱 Checking if Budgie is installed..."
if adb shell pm list packages | grep -q "com.example.budgie"; then
    echo "   ✅ Budgie is installed"
else
    echo "   ❌ Budgie is NOT installed"
    exit 1
fi

echo ""

# Check if service is running
echo "🔧 Checking if ScreenLockService is running..."
SERVICE_STATUS=$(adb shell dumpsys activity services | grep -A 5 "ScreenLockService")

if [ -n "$SERVICE_STATUS" ]; then
    echo "   ✅ ScreenLockService IS RUNNING"
    echo ""
    echo "   Service Details:"
    echo "$SERVICE_STATUS" | grep -E "app=|isForeground=|createTime=|lastActivity=" | sed 's/^/   /'
else
    echo "   ❌ ScreenLockService is NOT running"
    echo ""
    echo "   Attempting to start app to initialize service..."
    adb shell am start -n com.example.budgie/.MainActivity
    sleep 2
    echo "   Rechecking..."
    if adb shell dumpsys activity services | grep -q "ScreenLockService"; then
        echo "   ✅ Service started successfully"
    else
        echo "   ❌ Service failed to start - Check logs"
    fi
fi

echo ""

# Check recent service logs
echo "📋 Recent ScreenLockService logs (last 10):"
LOGS=$(adb logcat -s ScreenLockService:V -d -t 10 2>/dev/null)
if [ -n "$LOGS" ]; then
    echo "$LOGS" | sed 's/^/   /'
else
    echo "   ⚠️  No logs found (service might be starting)"
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "🧪 To test auto-lock:"
echo "   1. Make sure the app is open and unlocked"
echo "   2. Lock your device (power button)"
echo "   3. Wait 2 seconds"
echo "   4. Unlock your device"
echo "   5. Open Budgie app"
echo "   6. Expected: Lock screen should appear"
echo ""
echo "🔍 To monitor live events:"
echo "   ./test-screen-lock.sh"
echo ""
echo "═══════════════════════════════════════════════════════════"

