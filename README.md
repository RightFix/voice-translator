# Voice Translator

A native Android app that translates spoken text to your device's default language using speech recognition and Google Translate.

## Features

- **Speech Recognition**: Uses Android's built-in SpeechRecognizer
- **Translation**: Powered by Google Translate (130+ languages)
- **Text-to-Speech**: Speaks the translation aloud
- **Auto-detect**: Automatically detects your device's language
- **Material Design 3**: Modern, clean UI with light/dark theme support

## Requirements

- Java 17
- Android SDK (API 24+)

## Building

### Local Build
```bash
./gradlew assembleDebug
```

### GitHub Actions
The app builds automatically on push to master. APK is available as a workflow artifact.

## Project Structure

- `app/src/main/java/com/voicetranslator/` - Java source code
- `app/src/main/res/` - Resources (layouts, strings, themes)
- `app/build.gradle` - App dependencies and build config

## Permissions

- `RECORD_AUDIO` - For speech recognition
- `INTERNET` - For Google Translate API