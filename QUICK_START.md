# 🚀 Quick Start Guide - Budgie App

## Project Structure

```
budgie/
├── app/
│   ├── src/main/java/com/example/budgie/
│   │   ├── MainActivity.kt                    # App entry point
│   │   ├── data/
│   │   │   ├── model/                        # Data models
│   │   │   │   ├── Expense.kt               # Expense entity
│   │   │   │   ├── Income.kt                # Income entity
│   │   │   │   ├── Budget.kt                # Budget entity
│   │   │   │   ├── Bill.kt                  # Bill entity
│   │   │   │   └── FinancialModels.kt       # Additional models
│   │   │   ├── local/                        # Room Database
│   │   │   │   ├── BudgieDatabase.kt        # Database instance
│   │   │   │   ├── ExpenseDao.kt            # Expense queries
│   │   │   │   ├── IncomeDao.kt             # Income queries
│   │   │   │   ├── BudgetDao.kt             # Budget queries
│   │   │   │   ├── BillDao.kt               # Bill queries
│   │   │   │   └── Converters.kt            # Type converters
│   │   │   └── repository/
│   │   │       └── FinanceRepository.kt      # Data repository
│   │   ├── ui/
│   │   │   ├── screens/                      # Compose screens
│   │   │   │   ├── DashboardScreen.kt       # Main dashboard
│   │   │   │   ├── ExpenseIncomeScreens.kt  # Expense/Income screens
│   │   │   │   ├── BudgetScreen.kt          # Budget management
│   │   │   │   ├── BillsScreen.kt           # Bills tracking
│   │   │   │   ├── InsightsInvestmentsScreen.kt
│   │   │   │   └── WealthProjectionScreen.kt
│   │   │   ├── navigation/
│   │   │   │   ├── Screen.kt                # Screen routes
│   │   │   │   └── BudgieNavigation.kt      # Navigation graph
│   │   │   ├── viewmodel/
│   │   │   │   └── MainViewModel.kt         # App view model
│   │   │   ├── components/
│   │   │   │   └── CommonComponents.kt      # Reusable UI components
│   │   │   └── theme/                        # Material3 theme
│   │   │       ├── Theme.kt
│   │   │       ├── Color.kt
│   │   │       └── Type.kt
│   │   └── ai/
│   │       └── FinancialAdvisor.kt          # AI financial insights
│   └── build.gradle.kts                      # App-level build config
├── gradle/
│   └── libs.versions.toml                    # Dependency versions
├── build.gradle.kts                          # Project-level build config
└── settings.gradle.kts                       # Project settings
```

---

## 🏃 Run on Physical Device in 3 Steps

### 1️⃣ Enable USB Debugging on Your Phone

1. Settings → About Phone → Tap "Build Number" 7 times
2. Settings → Developer Options → Enable "USB Debugging"
3. Connect phone to computer via USB
4. Accept USB debugging prompt on phone

### 2️⃣ Verify Connection

Open terminal and run:
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb devices
```

You should see your device listed.

### 3️⃣ Run the App

**Option A - Android Studio (Easiest):**
1. Open project in Android Studio
2. Select your device from dropdown
3. Click Run ▶️

**Option B - Command Line:**
```bash
cd /Users/danielkinyua/Downloads/projects/budgie
./gradlew installDebug
```

---

## 📱 App Features

When you run the app, you'll have access to:

### 💰 Dashboard
- Overview of total income, expenses, and savings
- Recent transactions
- Quick access to all features

### 💸 Expense Tracking
- Add/edit/delete expenses
- Categorize expenses
- View expense history
- Track spending by category

### 💵 Income Tracking
- Record income sources
- Track income over time
- Categorize income types

### 📊 Budget Management
- Create monthly/yearly budgets
- Set spending limits by category
- Monitor budget usage
- Get alerts when approaching limits

### 📅 Bills Management
- Track recurring bills
- Set payment reminders
- Mark bills as paid
- View payment history

### 📈 Insights & Analytics
- Spending trends
- Income vs. Expense charts
- Category breakdown
- Financial health score

### 💎 Investments
- Track investment portfolio
- Monitor returns
- Investment categories

### 🔮 Wealth Projection
- Future wealth predictions
- Savings goals
- Financial planning tools

---

## 🎨 UI/UX Highlights

- **Material3 Design** - Modern, clean interface
- **Dark/Light Theme** - Automatic theme switching
- **Smooth Navigation** - Jetpack Navigation Compose
- **Responsive** - Works on phones and tablets
- **Fast & Offline** - All data stored locally with Room Database

---

## 🔧 Development Commands

### Build the Project
```bash
./gradlew build
```

### Clean Build
```bash
./gradlew clean build
```

### Install Debug APK
```bash
./gradlew installDebug
```

### Uninstall App
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb uninstall com.example.budgie
```

### View Logs
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb logcat | grep -i budgie
```

### Build Release APK (unsigned)
```bash
./gradlew assembleRelease
```

---

## 📊 Database Information

The app uses **Room Database** with these tables:

- **expenses** - All expense records
- **income** - All income records
- **budgets** - Budget configurations
- **bills** - Recurring bills

Database Location on Device:
```
/data/data/com.example.budgie/databases/budgie_database
```

View database (requires root or debug build):
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb shell
cd /data/data/com.example.budgie/databases/
ls -la
```

---

## 🐛 Common Issues & Solutions

### "Device not found"
```bash
# Restart ADB
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb kill-server
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb start-server
```

### "Installation failed"
```bash
# Uninstall and retry
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb uninstall com.example.budgie
./gradlew installDebug
```

### "Build failed"
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### App crashes on launch
Check logs:
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb logcat *:E
```

---

## 🎯 Testing the App

### First Launch
1. App will initialize empty database
2. Navigate through different screens
3. Add sample expense/income entries
4. Create a budget
5. Add a bill
6. Check insights and dashboard

### Sample Data to Add
- **Expense**: Groceries, $50, Food category
- **Income**: Salary, $3000, Work category
- **Budget**: Monthly Food Budget, $500 limit
- **Bill**: Rent, $1200, Due on 1st of month

---

## 📝 Build Configuration

- **Package Name**: com.example.budgie
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Version**: 1.0 (versionCode: 1)

---

## 💡 Pro Tips

1. **Keep Developer Options On** - Makes testing easier
2. **Enable "Stay Awake"** - Screen won't sleep while charging
3. **Use Android Studio Logcat** - Better log filtering and colors
4. **Compose Preview** - View UI components without running app
5. **Hot Reload** - Make UI changes and see them instantly (in Android Studio)

---

## 🔄 Next Steps

After running the app successfully:

1. **Explore the codebase** - Check out the well-organized structure
2. **Modify UI** - Try changing colors in `ui/theme/Color.kt`
3. **Add features** - Extend existing screens or add new ones
4. **Test thoroughly** - Try all features with real-world data
5. **Customize** - Make it your own!

---

**For detailed instructions, see [RUNNING_ON_DEVICE.md](RUNNING_ON_DEVICE.md)**

**Happy Coding! 🎉**

