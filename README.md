# Smart Expense Manager

A modern, privacy-focused Android application for tracking personal finances with smart insights and local-first security.

## 🌟 Features

### 🏦 Core Finance Tracking
*   **Income & Expense Logging:** Easily record transactions with titles, amounts, and customizable categories.
*   **Recurring Transactions:** Set up repeat intervals for subscriptions or regular bills to keep your future budget in mind.
*   **Real-time Summary:** View your total balance, monthly income, and monthly expenses at a glance on the dashboard.

### 🧠 Smart Insights
*   **Dynamic Analysis:** The app automatically analyzes your spending patterns.
*   **Smart Alerts:** Get notified when a specific category's spending is unusually high or when you're approaching your monthly budget cap.
*   **Budgeting:** Set a monthly spending limit and receive visual warnings (80% and 90% thresholds) to help you stay on track.

### 📊 Reports & Visualization
*   **Category Breakdown:** View an interactive pie chart of your expenses by category for any given month.
*   **Net Balance Tracking:** See your income vs. expenses to understand your monthly savings.
*   **CSV Export:** Export all your transaction data to a CSV file for backup or further analysis in spreadsheets.

### 📸 AI-Powered Bill Scanning
*   **On-Device OCR:** Use the "Scan Bill" feature to automatically extract titles, amounts, and dates from photos of physical receipts using Google ML Kit.
*   **Privacy-First:** All text recognition happens locally on your device—your receipt photos are never uploaded to a server.

### 🔒 Security & Privacy
*   **App Lock:** Secure your financial data with a custom PIN.
*   **Biometric Authentication:** Support for Fingerprint and Face Unlock (using Android BiometricPrompt).
*   **Local Storage:** All data is stored in a local SQLite database (Room), ensuring your financial history stays on your phone.

### 🎨 Modern UI/UX
*   **Material 3 Design:** Fully compliant with the latest Android design standards.
*   **Dark Mode:** Automatic and manual toggle for system-wide dark theme support.
*   **Dynamic Color:** Support for Material You (Android 12+) to match the app's theme with your system wallpaper.

## 🛠️ Tech Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Database:** Room (SQLite)
*   **Preference Management:** DataStore
*   **Dependency Injection:** Manual/Factory-based
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Machine Learning:** Google ML Kit (Text Recognition)
*   **Architecture:** MVVM (Model-View-ViewModel)

## 🚀 Getting Started
1.  Clone the repository.
2.  Open the project in **Android Studio (Ladybug or newer)**.
3.  Sync Gradle and run the `:app` module on an emulator or physical device.

---
*Built with ❤️ for financial freedom.*
