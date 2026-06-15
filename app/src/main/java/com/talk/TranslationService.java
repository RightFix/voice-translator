package com.talk;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TranslationService {
    private static final String TAG = "TranslationService";
    private TranslationCallback callback;

    public interface TranslationCallback {
        void onTranslationStart();
        void onTranslationSuccess(String translatedText);
        void onTranslationError(String error);
    }

    public void setCallback(TranslationCallback callback) {
        this.callback = callback;
    }

    public void translate(String text, String sourceLang, String targetLang) {
        if (callback != null) {
            callback.onTranslationStart();
        }
        new TranslateTask().execute(text, sourceLang, targetLang);
    }

    private class TranslateTask extends AsyncTask<String, Void, String> {
        private String errorMessage = null;

        @Override
        protected String doInBackground(String... params) {
            String text = params[0];
            String sourceLang = params[1];
            String targetLang = params[2];

            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
                String urlString = Constants.TRANSLATION_API_URL 
                    + "?client=gtx" 
                    + "&sl=" + sourceLang 
                    + "&tl=" + targetLang 
                    + "&dt=t" 
                    + "&q=" + encodedText;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "Android/1.0");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    return parseTranslationResponse(response.toString());
                } else {
                    errorMessage = "Server error: " + responseCode;
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "Translation error", e);
                errorMessage = "Network error: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (callback != null) {
                if (result != null) {
                    callback.onTranslationSuccess(result);
                } else {
                    callback.onTranslationError(errorMessage != null ? errorMessage : "Unknown error");
                }
            }
        }

        private String parseTranslationResponse(String response) {
            try {
                if (response.startsWith("[")) {
                    StringBuilder translatedText = new StringBuilder();
                    int depth = 0;
                    boolean inQuotes = false;
                    boolean capture = false;
                    StringBuilder current = new StringBuilder();

                    for (int i = 0; i < response.length(); i++) {
                        char c = response.charAt(i);

                        if (c == '\"' && (i == 0 || response.charAt(i - 1) != '\\')) {
                            inQuotes = !inQuotes;
                            if (!inQuotes && capture) {
                                translatedText.append(current.toString());
                                capture = false;
                            }
                            continue;
                        }

                        if (!inQuotes) {
                            if (c == '[') depth++;
                            if (c == ']') depth--;
                            
                            if (depth == 1 && c == ',') {
                                capture = true;
                                current = new StringBuilder();
                            }
                        } else if (capture) {
                            current.append(c);
                        }
                    }
                    return translatedText.toString().trim();
                }
                return response;
            } catch (Exception e) {
                Log.e(TAG, "Parse error", e);
                return response;
            }
        }
    }
}