# Voice Translator

A Kivy-based Android app that translates spoken text to your device's default language using speech recognition and Google Translate.

## Features

- **Speech Recognition**: Uses Android's built-in SpeechRecognizer
- **Translation**: Powered by Google Translate (130+ languages)
- **Text-to-Speech**: Speaks the translation aloud
- **Auto-detect**: Automatically detects your device's language

## Requirements

- Python 3.x
- Kivy
- Buildozer (for building Android APK)

## Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Build the Android APK:
```bash
buildozer android debug
```

## Usage

1. Select the language you'll speak from the dropdown
2. Press the "Speak" button
3. Speak your text
4. The app will translate and speak the result

## Project Structure

- `main.py` - Main application code
- `languages.json` - Supported languages (130+)
- `buildozer.spec` - Buildozer configuration
- `requirements.txt` - Python dependencies