<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/f46f8ed7-f04f-4ebd-8d72-9fcdbea2ef77

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

App Preview 

 <img width="400" height="800" alt="WhatsApp Image 2026-06-30 at 10 15 11 (2)" src="https://github.com/user-attachments/assets/1e44c288-4041-4529-95af-f834bd7f0b8b" />
<img width="400" height="800" alt="WhatsApp Image 2026-06-30 at 10 15 12" src="https://github.com/user-attachments/assets/2d22ca4d-79ac-4582-b738-98c43e3bb766" />
<img width="400" height="800" alt="WhatsApp Image 2026-06-30 at 10 15 12 (2)" src="https://github.com/user-attachments/assets/0161aa88-8be1-4481-9d26-c667703aacb7" />
<img width="400" height="800" alt="WhatsApp Image 2026-06-30 at 10 15 12 (1)" src="https://github.com/user-attachments/assets/d2192afb-50b9-4487-a246-a06c92e734cd" />
