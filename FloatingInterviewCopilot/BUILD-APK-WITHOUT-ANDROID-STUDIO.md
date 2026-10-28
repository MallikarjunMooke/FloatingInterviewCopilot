# Build the APK online (no Android Studio required)

This project includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`. GitHub's hosted runner will compile a **debug APK** and attach it as a downloadable workflow artifact.

## Steps
1. Sign in to GitHub in your browser and create a new **private** repository, for example `floating-interview-copilot`.
2. Upload the *contents* of this ZIP to the repository's main branch. Ensure `.github/workflows/build-apk.yml` is included. If GitHub's web uploader hides dot-folders, create the workflow file in the GitHub web editor at that exact path.
3. Open the repository's **Actions** tab.
4. Select **Build Android APK** and click **Run workflow** if it did not start automatically after the upload.
5. Open the completed run and find **Artifacts** near the bottom. Download `floating-interview-copilot-debug-apk` and extract it to get `app-debug.apk`.
6. Transfer `app-debug.apk` to your Android phone, open it, and allow installation from that source if Android asks.

## Important
- This produces a **debug/test APK**, not a Play Store release.
- The first build may fail if the source has compilation issues; the APK has not yet been built or device-tested here.
- The app's ChatGPT handoff is a share-sheet handoff and may require manual confirmation. It does **not** guarantee that text is automatically submitted in the official ChatGPT app.
- Install APKs only from repositories and workflow runs you trust.
