package com.voicetranslator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private SpeechRecognizer speechRecognizer;
    private TranslationService translationService;
    private TextToSpeechHelper ttsHelper;
    
    private ExtendedFloatingActionButton fabSpeak;
    private MaterialButton btnPlayTranslation;
    private MaterialTextView tvStatus;
    private MaterialTextView tvTargetLanguage;
    private MaterialTextView tvOriginalText;
    private MaterialTextView tvTranslatedText;
    private MaterialCardView cardOriginal;
    private MaterialCardView cardTranslated;
    private CircularProgressIndicator progressIndicator;
    
    private ArrayAdapter<Language> languageAdapter;
    private Language selectedSourceLanguage;
    private Language targetLanguage;
    private String currentTranslatedText = "";
    
    private final ActivityResultLauncher<String[]> permissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean audioGranted = result.getOrDefault(Manifest.permission.RECORD_AUDIO, false);
            if (audioGranted != null && audioGranted) {
                startListening();
            } else {
                updateStatus("Microphone permission required");
                enableFab(true);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initSpeechRecognizer();
        initTranslationService();
        initTextToSpeech();
        setupLanguageSpinner();
        detectDeviceLanguage();
    }

    private void initViews() {
        fabSpeak = findViewById(R.id.fabSpeak);
        btnPlayTranslation = findViewById(R.id.btnPlayTranslation);
        tvStatus = findViewById(R.id.tvStatus);
        tvTargetLanguage = findViewById(R.id.tvTargetLanguage);
        tvOriginalText = findViewById(R.id.tvOriginalText);
        tvTranslatedText = findViewById(R.id.tvTranslatedText);
        cardOriginal = findViewById(R.id.cardOriginal);
        cardTranslated = findViewById(R.id.cardTranslated);
        progressIndicator = findViewById(R.id.progressIndicator);
        
        fabSpeak.setOnClickListener(v -> onFabClicked());
        btnPlayTranslation.setOnClickListener(v -> playTranslation());
        
        updateStatus("Select a language and tap the button to speak");
        cardOriginal.setVisibility(View.GONE);
        cardTranslated.setVisibility(View.GONE);
        btnPlayTranslation.setVisibility(View.GONE);
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                    updateStatus("Listening...");
                    showListeningAnimation(true);
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    updateStatus("Processing...");
                    showListeningAnimation(false);
                }

                @Override
                public void onError(int error) {
                    String errorMessage = getErrorMessage(error);
                    Log.e(TAG, "Speech error: " + error);
                    updateStatus(errorMessage);
                    showListeningAnimation(false);
                    enableFab(true);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        Log.d(TAG, "Recognized: " + spokenText);
                        handleRecognizedText(spokenText);
                    } else {
                        updateStatus("No speech detected. Try again.");
                        enableFab(true);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            updateStatus("Speech recognition not available on this device");
            fabSpeak.setEnabled(false);
        }
    }

    private void initTranslationService() {
        translationService = new TranslationService();
        translationService.setCallback(new TranslationService.TranslationCallback() {
            @Override
            public void onTranslationStart() {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.VISIBLE);
                    updateStatus("Translating...");
                });
            }

            @Override
            public void onTranslationSuccess(String translatedText) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    currentTranslatedText = translatedText;
                    tvTranslatedText.setText(translatedText);
                    cardTranslated.setVisibility(View.VISIBLE);
                    btnPlayTranslation.setVisibility(View.VISIBLE);
                    updateStatus("Translation complete!");
                    
                    ttsHelper.speak(translatedText, targetLanguage.getCode());
                });
            }

            @Override
            public void onTranslationError(String error) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    updateStatus("Error: " + error);
                    enableFab(true);
                });
            }
        });
    }

    private void initTextToSpeech() {
        ttsHelper = new TextToSpeechHelper(this);
        ttsHelper.setCallback(new TextToSpeechHelper.TtsCallback() {
            @Override
            public void onTtsInitialized(boolean success) {
                runOnUiThread(() -> {
                    if (!success) {
                        Toast.makeText(MainActivity.this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
                        btnPlayTranslation.setEnabled(false);
                    }
                });
            }

            @Override
            public void onStart() {
                runOnUiThread(() -> updateStatus("Speaking..."));
            }

            @Override
            public void onDone() {
                runOnUiThread(() -> {
                    updateStatus("Done. Tap to speak again.");
                    enableFab(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateStatus("TTS error: " + error);
                    enableFab(true);
                });
            }
        });
    }

    private void setupLanguageSpinner() {
        android.widget.Spinner spinnerSource = findViewById(R.id.spinnerSource);
        
        List<String> languageNames = new ArrayList<>();
        for (Language lang : Constants.LANGUAGES) {
            languageNames.add(lang.getName());
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(adapter);
        
        spinnerSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSourceLanguage = Constants.LANGUAGES.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Set default to Hausa
        int hausaIndex = languageNames.indexOf("Hausa");
        if (hausaIndex >= 0) {
            spinnerSource.setSelection(hausaIndex);
        }
    }

    private void detectDeviceLanguage() {
        String deviceLanguage = Locale.getDefault().getLanguage();
        Log.d(TAG, "Device language: " + deviceLanguage);
        
        for (Language lang : Constants.LANGUAGES) {
            if (lang.getCode().equals(deviceLanguage)) {
                targetLanguage = lang;
                break;
            }
        }
        
        if (targetLanguage == null) {
            targetLanguage = new Language("English", "en", "en-US");
        }
        
        tvTargetLanguage.setText("Translating to: " + targetLanguage.getName() + " (device language)");
    }

    private void onFabClicked() {
        if (selectedSourceLanguage == null) {
            updateStatus("Please select a language first");
            return;
        }
        
        if (selectedSourceLanguage.getCode().equals(targetLanguage.getCode())) {
            updateStatus("Source and target languages are the same");
            return;
        }
        
        if (!hasAudioPermission()) {
            requestAudioPermission();
        } else {
            startListening();
        }
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        permissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
    }

    private void startListening() {
        enableFab(false);
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedSourceLanguage.getLocale());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition", e);
            updateStatus("Error starting speech recognition");
            enableFab(true);
        }
    }

    private void handleRecognizedText(String text) {
        tvOriginalText.setText(text);
        cardOriginal.setVisibility(View.VISIBLE);
        
        String sourceLang = selectedSourceLanguage.getCode();
        String targetLang = targetLanguage.getCode();
        
        translationService.translate(text, sourceLang, targetLang);
    }

    private void playTranslation() {
        if (currentTranslatedText != null && !currentTranslatedText.isEmpty()) {
            ttsHelper.speak(currentTranslatedText, targetLanguage.getCode());
        }
    }

    private void updateStatus(String status) {
        tvStatus.setText(status);
    }

    private void enableFab(boolean enabled) {
        fabSpeak.setEnabled(enabled);
        if (enabled) {
            fabSpeak.setText("Speak");
        }
    }

    private void showListeningAnimation(boolean show) {
        if (show) {
            progressIndicator.setVisibility(View.VISIBLE);
            fabSpeak.setText("Listening...");
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech detected. Try again.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
    }
}