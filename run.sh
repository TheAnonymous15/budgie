 cd /Users/danielkinyua/Downloads/projects/budgie && ./gradlew assembleDebug && adb -d  install -r app/build/outputs/apk/debug/app-debug.apk && adb -d shell am start -n com.example.budgie/.MainActivity

