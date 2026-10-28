# Floating Interview Copilot (Android starter)

Native Kotlin / Jetpack Compose Android Studio project. This is a starter prototype, not a verified release APK.

## Current scope
- Main Compose screen with permission request, start/stop controls, editable question field, and AI-app handoff button.
- Microphone foreground service using Android `SpeechRecognizer`.
- Optional draggable overlay bubble, enabled only after the user grants overlay permission.
- Handoff uses Android `ACTION_SEND` chooser with a concise interview-copilot prompt. The user may need to select ChatGPT and complete the send action manually.
- Accessibility service is declared as a transparent placeholder; it does not automate ChatGPT or press Send.

## Important limitation
There is no verified public ChatGPT Android API/deep link that accepts arbitrary text and guarantees submission into an existing native conversation. This starter therefore does not claim automatic ChatGPT submission. The share chooser behavior depends on which apps Android offers and how those apps handle shared text.

## Build
1. Open this folder in Android Studio (current stable version).
2. Install Android SDK Platform 35 and JDK 17.
3. Allow Gradle sync to complete.
4. Run the `app` configuration on a physical Android device (recommended).
5. Grant microphone permission. Tap Enable floating bubble and approve Android's overlay permission.
6. Tap Start listening. SpeechRecognizer may stop after one utterance; tap Start again for another question.
7. Review or edit the question in the main app and tap Transfer question to AI app.

## Current limitations / next work
- Physical-device compilation and integration testing have not been performed in this environment.
- Current overlay shows status only; transcript preview/editing and handoff are in the main app.
- SpeechRecognizer may rely on the device's configured recognition provider and may use remote processing depending on device settings.
- Speaker separation is not implemented.
- Robust continuous listening, silence-based segmentation, duplicate windowing, and app-specific integration adapters are future work.
- Accessibility automation is not implemented; if added, it must be explicitly enabled, disclosed, and reviewed against Android and store policies.
- Foreground microphone service start behavior varies by Android version and app state. Start from the visible activity and test on the target device.
- Do not use this tool to secretly record people. Follow local consent and interview rules.

## Permissions
`RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, and `SYSTEM_ALERT_WINDOW` (only when overlay is requested). Accessibility is optional and currently does not perform UI automation.

## Integration verification checklist
- [ ] Gradle sync/build succeeds.
- [ ] Microphone permission granted and foreground notification visible.
- [ ] SpeechRecognizer returns a transcript on the test phone.
- [ ] Overlay permission can be granted and bubble appears.
- [ ] Share chooser offers ChatGPT on the target device.
- [ ] Verify whether ChatGPT accepts the shared text and what manual actions are required.
- [ ] Do not mark submission automatic unless confirmed on the actual device.
