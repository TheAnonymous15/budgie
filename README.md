# 💰 Budgie - Personal Finance Management App

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-orange.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34-brightgreen.svg)
![Build](https://img.shields.io/badge/Build-Passing-success.svg)

**A modern, feature-rich Android application for managing personal finances**

[Features](#-features) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [Documentation](#-documentation) • [Architecture](#-architecture)

</div>

---

## 📱 Overview

**Budgie** is a comprehensive personal finance management application built with the latest Android development technologies. Track expenses, manage budgets, monitor bills, and gain insights into your financial health—all while keeping your data private and secure on your device.

### ✨ Highlights

- 🎨 **Modern UI** - Built with Jetpack Compose and Material Design 3
- 📊 **Comprehensive Tracking** - Expenses, income, budgets, and bills
- 📈 **Smart Insights** - AI-powered financial advice and trend analysis
- 🔒 **Privacy First** - All data stored locally, no internet required
- 🌙 **Theme Support** - Beautiful dark and light themes
- ⚡ **Fast & Responsive** - Smooth animations and instant feedback

---

## 🎯 Features

### Core Functionality

| Feature | Description |
|---------|-------------|
| 💸 **Expense Tracking** | Record and categorize all your spending with detailed history |
| 💵 **Income Management** | Track multiple income sources and view trends |
| 📊 **Budget Planning** | Create custom budgets with category-wise limits and monitoring |
| 📅 **Bill Reminders** | Never miss a payment with recurring bill tracking |
| 📈 **Financial Insights** | Visual analytics, charts, and spending patterns |
| 💎 **Investment Tracking** | Monitor your investment portfolio and returns |
| 🔮 **Wealth Projection** | Plan your financial future with smart projections |
| 🤖 **AI Financial Advisor** | Get personalized financial advice and recommendations |

### User Experience

- ✅ Intuitive navigation with bottom navigation bar
- ✅ Quick add buttons for common actions
- ✅ Swipe gestures for efficient data management
- ✅ Search and filter across all transactions
- ✅ Export data for backup and analysis
- ✅ Customizable categories and tags

---

## 🛠 Tech Stack

### Core Technologies

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Database 2.6.1
- **Async**: Kotlin Coroutines 1.7.3
- **Navigation**: Navigation Compose 2.7.7
- **Build System**: Gradle with Kotlin DSL

### Libraries & Dependencies

```kotlin
// UI & Design
Jetpack Compose (BOM 2024.09.00)
Material Design 3
Material Icons Extended

// Database & Storage
Room Database
DataStore Preferences

// Lifecycle & Architecture
ViewModel
LiveData
Lifecycle Extensions

// Utilities
Gson (JSON serialization)
Kotlin Coroutines
```

### Development Tools

- **KSP** (Kotlin Symbol Processing) - Faster annotation processing
- **Gradle Version Catalogs** - Centralized dependency management
- **ProGuard** - Code optimization for release builds

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: 11 or higher
- **Android SDK**: API 24-34
- **Gradle**: 8.13.2 (included via wrapper)
- **Physical Device**: Android 7.0+ or Emulator

### Quick Start

1. **Clone or Download the Project**
   ```bash
   # Already downloaded to:
   /Users/danielkinyua/Downloads/projects/budgie
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open"
   - Navigate to the budgie folder
   - Wait for Gradle sync to complete

3. **Connect Your Device**
   - Enable Developer Options (tap Build Number 7 times)
   - Enable USB Debugging
   - Connect via USB and accept authorization

4. **Run the App**
   - Select your device from the dropdown
   - Click the Run button (▶️)
   - App will build, install, and launch automatically

### Alternative: Command Line Installation

```bash
cd /Users/danielkinyua/Downloads/projects/budgie

# Build and install
./gradlew installDebug

# Launch the app
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb shell am start -n com.example.budgie/.MainActivity
```

### Using the Dev Commands Script

```bash
cd /Users/danielkinyua/Downloads/projects/budgie
./dev-commands.sh
```

This interactive script provides quick access to:
- Device management
- Building and installing
- Logging and debugging
- Screenshots and more

---

## 📚 Documentation

Comprehensive guides are available in the project:

| Document | Description |
|----------|-------------|
| **[QUICK_START.md](QUICK_START.md)** | Quick reference and 3-step setup guide |
| **[RUNNING_ON_DEVICE.md](RUNNING_ON_DEVICE.md)** | Detailed device setup and troubleshooting |
| **[dev-commands.sh](dev-commands.sh)** | Interactive development commands script |

---

## 🏗 Architecture

### MVVM Pattern

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │  Screens    │  │ Components  │  │   Theme    │ │
│  └─────────────┘  └─────────────┘  └────────────┘ │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                ViewModel Layer                      │
│  ┌──────────────────────────────────────────────┐  │
│  │         MainViewModel                         │  │
│  │  (Business Logic & State Management)         │  │
│  └──────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                 Data Layer                          │
│  ┌──────────────┐  ┌─────────────┐  ┌───────────┐ │
│  │ Repository   │  │  Database   │  │   DAOs    │ │
│  └──────────────┘  └─────────────┘  └───────────┘ │
│  ┌──────────────────────────────────────────────┐  │
│  │         Data Models (Entities)               │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Project Structure

```
com.example.budgie/
├── MainActivity.kt                 # App entry point
├── data/
│   ├── model/                     # Data entities
│   │   ├── Expense.kt
│   │   ├── Income.kt
│   │   ├── Budget.kt
│   │   └── Bill.kt
│   ├── local/                     # Room Database
│   │   ├── BudgieDatabase.kt
│   │   ├── ExpenseDao.kt
│   │   ├── IncomeDao.kt
│   │   ├── BudgetDao.kt
│   │   └── BillDao.kt
│   └── repository/
│       └── FinanceRepository.kt   # Data operations
├── ui/
│   ├── screens/                   # Compose screens
│   │   ├── DashboardScreen.kt
│   │   ├── ExpenseIncomeScreens.kt
│   │   ├── BudgetScreen.kt
│   │   ├── BillsScreen.kt
│   │   ├── InsightsInvestmentsScreen.kt
│   │   └── WealthProjectionScreen.kt
│   ├── navigation/
│   │   ├── Screen.kt              # Route definitions
│   │   └── BudgieNavigation.kt    # Navigation graph
│   ├── viewmodel/
│   │   └── MainViewModel.kt       # App state & logic
│   ├── components/
│   │   └── CommonComponents.kt    # Reusable UI
│   └── theme/                     # Material3 theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── ai/
    └── FinancialAdvisor.kt        # AI insights
```

---

## 💾 Database Schema

### Tables

**expenses**
- id (PrimaryKey)
- amount (Double)
- category (String)
- description (String)
- date (Long)
- type (String)

**income**
- id (PrimaryKey)
- amount (Double)
- source (String)
- description (String)
- date (Long)
- type (String)

**budgets**
- id (PrimaryKey)
- category (String)
- limit (Double)
- period (String)
- startDate (Long)
- endDate (Long)

**bills**
- id (PrimaryKey)
- name (String)
- amount (Double)
- dueDate (Long)
- recurring (Boolean)
- category (String)
- paid (Boolean)

---

## 🔧 Build Configuration

```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.budgie"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

---

## 🧪 Testing

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Specific test
./gradlew test --tests "com.example.budgie.ExampleUnitTest"
```

### Test Coverage

- Unit tests for ViewModels
- Repository tests
- Database tests
- UI tests with Compose Testing

---

## 🐛 Troubleshooting

### Common Issues

**Device not detected**
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb kill-server
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb start-server
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb devices
```

**Build fails**
```bash
./gradlew clean build --stacktrace
```

**App crashes**
```bash
/Users/danielkinyua/Library/Android/sdk/platform-tools/adb logcat | grep -E "(budgie|AndroidRuntime)"
```

For more troubleshooting, see [RUNNING_ON_DEVICE.md](RUNNING_ON_DEVICE.md).

---

## 📊 Current Status

- ✅ **Build Status**: Passing
- ✅ **All Features**: Implemented
- ✅ **Database**: Configured and working
- ✅ **Navigation**: Complete
- ✅ **UI**: Material3 compliant
- ⚠️ **Warnings**: Minor deprecations (non-critical)

---

## 🎨 Screenshots

The app includes these major screens:
1. Dashboard - Financial overview
2. Expenses - Spending tracker
3. Income - Revenue tracking
4. Budget - Budget management
5. Bills - Payment reminders
6. Insights - Analytics
7. Wealth Projection - Future planning

---

## 🔐 Privacy & Security

- ✅ **Offline First**: No internet required
- ✅ **Local Storage**: All data stays on device
- ✅ **No Analytics**: No tracking or telemetry
- ✅ **No Ads**: Clean, ad-free experience
- ✅ **Open Source**: Transparent codebase

---

## 🚦 Roadmap

Future enhancements could include:
- [ ] Cloud backup and sync
- [ ] Multi-currency support
- [ ] Export to CSV/PDF
- [ ] Biometric authentication
- [ ] Widget support
- [ ] Wear OS companion app
- [ ] Share financial reports

---

## 🤝 Contributing

This project follows Android best practices:
- Kotlin coding conventions
- Material Design guidelines
- MVVM architecture pattern
- Jetpack Compose patterns
- Clean code principles

---

## 📄 License

This project is for educational and personal use.

---

## 🙏 Acknowledgments

Built with:
- Android Jetpack libraries
- Material Design 3
- Kotlin Coroutines
- Room Database
- Jetpack Compose

---

## 📞 Support

For issues or questions:
1. Check the documentation files
2. Review the troubleshooting section
3. Check Android Studio's Event Log
4. Enable verbose logging for detailed errors

---

<div align="center">

**Made with ❤️ using Kotlin and Jetpack Compose**

[⬆ Back to Top](#-budgie---personal-finance-management-app)

</div>

# budgie
