# main.py
import json
import os
import threading
from deep_translator import GoogleTranslator
from gtts import gTTS
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.spinner import Spinner
from kivy.core.audio import SoundLoader
from kivy.clock import Clock
from kivy.utils import platform

with open("languages.json", "r") as f:
    LANGUAGES = json.load(f)

ANDROID_LOCALES = {
    "English": "en-US",
    "Hausa": "ha-NG",
    "Igbo": "ig-NG",
    "Yoruba": "yo-NG",
    "French": "fr-FR",
    "Arabic": "ar-SA",
    "Spanish": "es-ES",
    "Portuguese": "pt-PT",
    "Swahili": "sw-KE",
    "German": "de-DE",
    "Italian": "it-IT",
    "Russian": "ru-RU",
    "Chinese": "zh-CN",
    "Japanese": "ja-JP",
    "Korean": "ko-KR",
    "Hindi": "hi-IN",
    "Dutch": "nl-NL",
    "Polish": "pl-PL",
    "Turkish": "tr-TR",
    "Vietnamese": "vi-VN",
    "Thai": "th-TH",
    "Indonesian": "id-ID",
    "Greek": "el-GR",
    "Portuguese (Brazil)": "pt-BR",
}

if platform == "android":
    from jnius import autoclass, PythonJavaClass, java_method
    from android.permissions import request_permissions, Permission

    SpeechRecognizer = autoclass("android.speech.SpeechRecognizer")
    RecognizerIntent = autoclass("android.speech.RecognizerIntent")
    Intent = autoclass("android.content.Intent")
    PythonActivity = autoclass("org.kivy.android.PythonActivity")

    class RecognitionListener(PythonJavaClass):
        __javainterfaces__ = ["android/speech/RecognitionListener"]
        __javacontext__ = "app"

        def __init__(self, callback):
            super().__init__()
            self.callback = callback

        @java_method("(Landroid/os/Bundle;)V")
        def onResults(self, bundle):
            matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if matches and matches.size() > 0:
                self.callback(matches.get(0), None)
            else:
                self.callback(None, "No speech detected")

        @java_method("(I)V")
        def onError(self, error):
            error_messages = {
                1: "Network timeout",
                2: "Network error",
                3: "Audio recording error",
                4: "Server error",
                5: "Client error",
                6: "No speech input (timeout)",
                7: "No match found",
                8: "Recognizer busy",
                9: "Insufficient permissions",
                11: "Language not supported",
                12: "Language unavailable",
            }
            msg = error_messages.get(error, f"Unknown error ({error})")
            self.callback(None, msg)

        @java_method("(Landroid/os/Bundle;)V")
        def onReadyForSpeech(self, bundle):
            pass

        @java_method("()V")
        def onBeginningOfSpeech(self):
            pass

        @java_method("(F)V")
        def onRmsChanged(self, rmsdB):
            pass

        @java_method("([B)V")
        def onBufferReceived(self, buffer):
            pass

        @java_method("()V")
        def onEndOfSpeech(self):
            pass

        @java_method("(ILandroid/os/Bundle;)V")
        def onPartialResults(self, bundle):
            pass

        @java_method("(Landroid/os/Bundle;)V")
        def onEvent(self, eventType, bundle):
            pass


if platform == "android":
    Locale = autoclass("java.util.Locale")


def get_device_language():
    if platform == "android":
        code = Locale.getDefault().getLanguage()
        return code if code in LANGUAGES.values() else "en"
    return "en"


class TranslatorApp(App):
    def build(self):
        self.layout = BoxLayout(orientation="vertical", padding=20, spacing=12)

        self.target_lang = get_device_language()
        target_display = next(
            (name for name, code in LANGUAGES.items() if code == self.target_lang),
            "English",
        )

        self.status_label = Label(
            text="Select spoken language, then press Speak", size_hint=(1, 0.2)
        )
        self.layout.add_widget(self.status_label)

        self.layout.add_widget(
            Label(
                text=f"Translating to: {target_display} (device language)",
                size_hint=(1, 0.1),
            )
        )

        self.original_label = Label(text="", size_hint=(1, 0.15))
        self.layout.add_widget(self.original_label)

        self.translated_label = Label(text="", size_hint=(1, 0.15))
        self.layout.add_widget(self.translated_label)

        self.layout.add_widget(Label(text="I will speak in:", size_hint=(1, 0.08)))
        self.from_spinner = Spinner(
            text="Hausa", values=list(LANGUAGES.keys()), size_hint=(1, 0.15)
        )
        self.layout.add_widget(self.from_spinner)

        self.record_btn = Button(text="🎤 Speak", size_hint=(1, 0.17))
        self.record_btn.bind(on_press=self.start_listening)
        self.layout.add_widget(self.record_btn)

        if platform == "android":
            request_permissions([Permission.RECORD_AUDIO, Permission.INTERNET])
            self.recognizer = SpeechRecognizer.createSpeechRecognizer(
                PythonActivity.mActivity
            )
            self.listener = RecognitionListener(self.on_speech_result)
            self.recognizer.setRecognitionListener(self.listener)

        return self.layout

    def start_listening(self, instance):
        if platform != "android":
            self.status_label.text = (
                "Speech recognition only works on an Android device"
            )
            return

        from_lang = self.from_spinner.text
        source_code = LANGUAGES[from_lang]

        if source_code == self.target_lang:
            self.status_label.text = (
                "Spoken language matches device language — nothing to translate"
            )
            return

        self.status_label.text = "Listening..."
        self.original_label.text = ""
        self.translated_label.text = ""

        if from_lang in ANDROID_LOCALES:
            locale_code = ANDROID_LOCALES[from_lang]
        else:
            code = LANGUAGES.get(from_lang, "en")
            locale_code = f"{code}-{code.upper()}"

        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale_code)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, False)

        try:
            self.recognizer.startListening(intent)
        except Exception as e:
            self.status_label.text = f"Failed to start: {e}"

    def on_speech_result(self, text, error):
        if error:
            Clock.schedule_once(lambda dt: self.update_status(f"Error: {error}"))
            return

        Clock.schedule_once(lambda dt: self.update_original(text))
        threading.Thread(
            target=self._translate_and_speak_bg, args=(text,), daemon=True
        ).start()

    def _translate_and_speak_bg(self, text):
        try:
            source_lang = LANGUAGES[self.from_spinner.text]
            target_lang = self.target_lang

            translated = GoogleTranslator(
                source=source_lang, target=target_lang
            ).translate(text)
            Clock.schedule_once(lambda dt: self.update_translated(translated))
            self.speak(translated, target_lang)

        except Exception as e:
            Clock.schedule_once(
                lambda dt: self.update_status(f"Translation error: {e}")
            )

    def update_original(self, text):
        self.original_label.text = f"Heard: {text}"

    def update_translated(self, text):
        self.translated_label.text = f"Translation: {text}"

    def update_status(self, text):
        self.status_label.text = text

    def speak(self, text, lang):
        try:
            tts = gTTS(text=text, lang=lang)
            filepath = os.path.join(self.user_data_dir, "output.mp3")
            tts.save(filepath)

            sound = SoundLoader.load(filepath)
            if sound:
                sound.play()
            Clock.schedule_once(
                lambda dt: self.update_status("Done. Press Speak to try again.")
            )

        except Exception as e:
            Clock.schedule_once(
                lambda dt: self.update_status(f"TTS error ({lang}): {e}")
            )


if __name__ == "__main__":
    TranslatorApp().run()
