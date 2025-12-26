package com.fastkeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WhisperAPI {
    private static final String TAG = "FastKeyboard";
    private static final String PREFS_NAME = "FastKeyboardPrefs";
    private static final String KEY_API_URL = "whisper_api_url";
    private static final String KEY_API_KEY = "whisper_api_key";

    public interface TranscriptionCallback {
        void onSuccess(String transcription);
        void onError(String error);
    }

    public static void transcribeAudio(final Context context, final File audioFile, final TranscriptionCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "WhisperAPI: Starting transcription");
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String apiUrl = prefs.getString(KEY_API_URL, "");
                    String apiKey = prefs.getString(KEY_API_KEY, "");

                    Log.d(TAG, "WhisperAPI: URL=" + (apiUrl.isEmpty() ? "EMPTY" : "SET") + ", Key=" + (apiKey.isEmpty() ? "EMPTY" : "SET"));

                    if (apiUrl.isEmpty() || apiKey.isEmpty()) {
                        Log.e(TAG, "WhisperAPI: API not configured");
                        callback.onError("API not configured. Please open FastKeyboard app and configure settings.");
                        return;
                    }

                    String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                    String CRLF = "\r\n";

                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    conn.setUseCaches(false);

                    DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                    // Add model parameter
                    request.writeBytes("--" + boundary + CRLF);
                    request.writeBytes("Content-Disposition: form-data; name=\"model\"" + CRLF);
                    request.writeBytes(CRLF);
                    request.writeBytes("whisper-1" + CRLF);

                    // Add response_format parameter
                    request.writeBytes("--" + boundary + CRLF);
                    request.writeBytes("Content-Disposition: form-data; name=\"response_format\"" + CRLF);
                    request.writeBytes(CRLF);
                    request.writeBytes("json" + CRLF);

                    // Add file
                    request.writeBytes("--" + boundary + CRLF);
                    request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + audioFile.getName() + "\"" + CRLF);
                    String contentType = audioFile.getName().endsWith(".m4a") ? "audio/mp4" : "audio/mpeg";
                    request.writeBytes("Content-Type: " + contentType + CRLF);
                    request.writeBytes(CRLF);

                    FileInputStream fileInputStream = new FileInputStream(audioFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        request.write(buffer, 0, bytesRead);
                    }
                    fileInputStream.close();

                    request.writeBytes(CRLF);
                    request.writeBytes("--" + boundary + "--" + CRLF);

                    request.flush();
                    request.close();

                    int responseCode = conn.getResponseCode();
                    Log.d(TAG, "WhisperAPI: Response code=" + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        // Parse JSON response - simple extraction
                        String jsonResponse = response.toString();
                        String text = extractTextFromJSON(jsonResponse);

                        Log.d(TAG, "WhisperAPI: Extracted text=" + (text != null ? text : "NULL"));

                        if (text != null && !text.isEmpty()) {
                            callback.onSuccess(text);
                        } else {
                            Log.e(TAG, "WhisperAPI: No transcription found in response");
                            callback.onError("No transcription found");
                        }
                    } else {
                        // Read error response
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        errorReader.close();

                        String errorMsg = errorResponse.toString();
                        Log.e(TAG, "WhisperAPI: HTTP Error " + responseCode + ": " + errorMsg);
                        callback.onError("HTTP Error " + responseCode + ": " + errorMsg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "WhisperAPI: Exception", e);
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private static String extractTextFromJSON(String json) {
        // Simple JSON parsing to extract "text" field
        try {
            int textIndex = json.indexOf("\"text\"");
            if (textIndex == -1) return null;

            int colonIndex = json.indexOf(":", textIndex);
            int startQuote = json.indexOf("\"", colonIndex);
            int endQuote = json.indexOf("\"", startQuote + 1);

            if (startQuote != -1 && endQuote != -1) {
                return json.substring(startQuote + 1, endQuote);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
