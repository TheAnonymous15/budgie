#!/bin/zsh
# Budgie App - Quick Command Reference
# Run this script for common development tasks

echo "🎯 Budgie App - Development Commands\n"

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Project directory
PROJECT_DIR="/Users/danielkinyua/Downloads/projects/budgie"
ADB="/Users/danielkinyua/Library/Android/sdk/platform-tools/adb"

# Function definitions
check_device() {
    echo "${BLUE}📱 Checking connected devices...${NC}"
    $ADB devices
}

install_debug() {
    echo "${BLUE}📦 Building and installing debug APK...${NC}"
    cd $PROJECT_DIR
    ./gradlew installDebug
}

uninstall_app() {
    echo "${YELLOW}🗑️  Uninstalling Budgie app...${NC}"
    $ADB uninstall com.example.budgie
}

launch_app() {
    echo "${GREEN}🚀 Launching Budgie app...${NC}"
    $ADB shell am start -n com.example.budgie/.MainActivity
}

view_logs() {
    echo "${BLUE}📋 Viewing app logs (Ctrl+C to exit)...${NC}"
    $ADB logcat | grep -i budgie
}

clean_build() {
    echo "${BLUE}🧹 Cleaning and building project...${NC}"
    cd $PROJECT_DIR
    ./gradlew clean build
}

build_debug() {
    echo "${BLUE}🔨 Building debug APK...${NC}"
    cd $PROJECT_DIR
    ./gradlew assembleDebug
    echo "${GREEN}✅ Debug APK location: app/build/outputs/apk/debug/app-debug.apk${NC}"
}

restart_adb() {
    echo "${YELLOW}🔄 Restarting ADB server...${NC}"
    $ADB kill-server
    $ADB start-server
    $ADB devices
}

clear_app_data() {
    echo "${YELLOW}🗑️  Clearing app data...${NC}"
    $ADB shell pm clear com.example.budgie
}

take_screenshot() {
    echo "${BLUE}📸 Taking screenshot...${NC}"
    $ADB shell screencap -p /sdcard/budgie_screenshot.png
    $ADB pull /sdcard/budgie_screenshot.png ~/Desktop/budgie_screenshot.png
    echo "${GREEN}✅ Screenshot saved to Desktop${NC}"
}

# Main menu
echo "Select an option:"
echo "1) Check connected devices"
echo "2) Build and install debug APK"
echo "3) Launch app"
echo "4) View app logs"
echo "5) Clean and rebuild project"
echo "6) Build debug APK only"
echo "7) Uninstall app"
echo "8) Clear app data"
echo "9) Restart ADB server"
echo "10) Take screenshot"
echo "11) Full workflow (clean → build → install → launch)"
echo ""
read -p "Enter choice [1-11]: " choice

case $choice in
    1)
        check_device
        ;;
    2)
        install_debug
        ;;
    3)
        launch_app
        ;;
    4)
        view_logs
        ;;
    5)
        clean_build
        ;;
    6)
        build_debug
        ;;
    7)
        uninstall_app
        ;;
    8)
        clear_app_data
        ;;
    9)
        restart_adb
        ;;
    10)
        take_screenshot
        ;;
    11)
        echo "${BLUE}🎯 Running full workflow...${NC}"
        clean_build
        echo ""
        install_debug
        echo ""
        launch_app
        echo "${GREEN}✅ Complete!${NC}"
        ;;
    *)
        echo "${YELLOW}Invalid option${NC}"
        exit 1
        ;;
esac

echo "\n${GREEN}Done!${NC}"

