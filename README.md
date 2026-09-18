# ReelForge AI v5 — Full Stack Prototype

This is the next step after v4: Android client + Python API backend.

## Architecture
Android app → FastAPI backend → Whisper/LLM/FFmpeg → generated Shorts → Android status/result

## Backend
cd backend
pip install -r requirements.txt
python server.py

Default API: http://127.0.0.1:8000

For an Android phone to reach a computer running the server, use the computer's LAN IP instead of 127.0.0.1.

Optional environment variables:
OPENAI_API_KEY
OPENAI_BASE_URL
OPENAI_MODEL
WHISPER_MODEL

## Important
This is a runnable integration prototype, not a production cloud service. The backend performs real upload handling and job orchestration. AI processing uses Faster-Whisper/FFmpeg when installed; LLM enhancement falls back locally if no API key is configured.


## Build APK without Android Studio on your PC

This project now includes a GitHub Actions workflow:
`android/.github/workflows/build-apk.yml`

### Steps
1. Create a GitHub account and a new repository.
2. Upload the contents of this ZIP to the repository.
3. Open **Actions** → **Build ReelForge AI APK**.
4. Run the workflow with **Run workflow**.
5. When it finishes, open the workflow run and download the artifact:
   **ReelForge-AI-debug-APK**
6. Extract it and install `app-debug.apk` on your Android phone.

### Important
The APK produced by this workflow is the Android client. The current v5 backend still needs to run on a computer/server for real video processing. The app is therefore not yet a fully standalone offline AI editor.
