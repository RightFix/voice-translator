package com.talk;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

public class TextToSpeechHelper implements TextToSpeech.OnInitListener {
    private static final String TAG = "TextToSpeechHelper";
    private TextToSpeech textToSpeech;
    private TtsCallback callback;
    private boolean isInitialized = false;
    private String currentLanguage = "en";

    public interface TtsCallback {
        void onTtsInitialized(boolean success);
        void onStart();
        void onDone();
        void onError(String error);
    }

    public TextToSpeechHelper(Context context) {
        textToSpeech = new TextToSpeech(context, this);
    }

    public void setCallback(TtsCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true;
            Log.d(TAG, "TTS initialized successfully");
            if (callback != null) {
                callback.onTtsInitialized(true);
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
            if (callback != null) {
                callback.onTtsInitialized(false);
            }
        }
    }

    public void speak(String text, String languageCode) {
        if (!isInitialized) {
            if (callback != null) {
                callback.onError("TTS not initialized");
            }
            return;
        }

        Locale locale = new Locale(languageCode);
        int result = textToSpeech.setLanguage(locale);
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: " + languageCode + ", using default");
            textToSpeech.setLanguage(Locale.getDefault());
        }

        currentLanguage = languageCode;

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (callback != null) {
                    callback.onStart();
                }
            }

            @Override
            public void onDone(String utteranceId) {
                if (callback != null) {
                    callback.onDone();
                }
            }

            @Override
            public void onError(String utteranceId) {
                if (callback != null) {
                    callback.onError("TTS error");
                }
            }
        });

        String utteranceId = UUID.randomUUID().toString();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}