#!/bin/bash
# Screen Lock Service Test Script
# Use this to verify the auto-lock feature is working

echo "=== Budgie Screen Lock Service Test ==="
echo ""

# Check if service is running
echo "1. Checking if ScreenLockService is running..."
adb shell dumpsys activity services | grep -A 10 ScreenLockService

echo ""
echo "2. Checking auto-lock setting..."
adb shell "run-as com.example.budgie cat /data/data/com.example.budgie/shared_prefs/budgie_secure_prefs.xml 2>/dev/null | grep -i auto_lock || echo 'Settings file not accessible (encrypted)'"

echo ""
echo "3. Starting live log monitoring..."
echo "   - Lock your device now to test"
echo "   - Press Ctrl+C to stop"
echo ""
adb logcat -s ScreenLockService:V MainActivity:V SecurityManager:V -v time


