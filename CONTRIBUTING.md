# 🤝 Contributing to MiRearUltra

Thank you for your interest in contributing to **MiRearUltra**! As an open-source project in its **Alpha** stage, community contributions, bug reports, and suggestions are vital to making this the definitive secondary display engine for HyperOS.

---

## 🛠️ How to Contribute

### 1. Reporting Bugs
- Search existing issues before creating a new one.
- Provide a clear, descriptive title.
- Mention your exact **HyperOS build version**, **Android version**, and device serial/model.
- Attach logcat snippets (`adb logcat -s RearEngine:* NativeHyperOsRearView:*`) or screen recordings if applicable.

### 2. Suggesting Enhancements & New Modules
- Open an Issue with the tag `[Feature Request]`.
- Describe the expected visual appearance and touch interactions on the 976x596 secondary display.
- Keep in mind performance: the secondary display runs at 120Hz LTPO; animations must avoid allocations during draw loops.

### 3. Submitting Pull Requests (PRs)
1. Fork the repository and create your branch from `master` (`git checkout -b feature/amazing-module`).
2. Adhere to Kotlin and Jetpack Compose best practices.
3. Keep Canvas drawing routines allocation-free (reuse `Paint`, `RectF`, `Path` objects inside `NativeHyperOsRearView`).
4. Ensure string constants have entries in `RearI18n` and all 10 `values-*/strings.xml` resource folders.
5. Compile and test the debug build (`gradlew assembleDebug`).
6. Open a PR with a clear summary of your changes.

---

## 🎨 Code Style Guidelines
- **Kotlin:** Follow official Kotlin coding conventions.
- **Canvas Rendering:** Avoid instantiating new objects inside `onDraw()` or ticker frames to maintain consistent 120 FPS frame timing.
- **Localization:** Always use `RearI18n.t(key)` for canvas text and string resources for Compose UI.
