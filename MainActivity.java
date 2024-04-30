package com.example.color_detector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.hardware.Camera;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;
import android.speech.tts.TextToSpeech;

import android.Manifest;


import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class MainActivity extends Activity implements SurfaceHolder.Callback, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, TextToSpeech.OnInitListener {
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private GestureDetector gestureDetector;

    private TextToSpeech textToSpeech;
    private boolean isPreviewActive = true;

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 200;

    private static final int REQUEST_PERMISSIONS_CODE = 1002;


    // Initialization of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.camera_preview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        gestureDetector = new GestureDetector(this, this);
        gestureDetector.setOnDoubleTapListener(this);
        textToSpeech = new TextToSpeech(this, this);

        requestPermissions();  // Demander les permissions audio dès le démarrage
    }

    // Handling touch events to detect gestures
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    // Response to double-tap gesture
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d("DoubleTap", "Double tap detected");
        if (isPreviewActive) {
            Log.d("CameraAction", "Taking photo");
            takePhoto();
        } else {
            Log.d("CameraAction", "Restarting preview");
            restartCameraPreview();
        }
        isPreviewActive = !isPreviewActive;
        return true;
    }


    // Method to capture a photo
    private void takePhoto() {
        if (camera != null) {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bitmap != null) {
                        extractDominantColor(bitmap);
                    }
                    restartCameraPreview(); // Restart preview after taking photo
                }
            });
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Extract the dominant color from a bitmap image
    private void extractDominantColor(Bitmap bitmap) {
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Palette.Swatch dominantSwatch = palette.getDominantSwatch();
                    if (dominantSwatch != null) {
                        int dominantColor = dominantSwatch.getRgb();
                        updateUIWithColor(dominantColor);
                    }
                }
            });
        }


    // Handle the result of permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // La permission de la caméra a été accordée
                }  else if (permissions[i].equals(Manifest.permission.RECORD_AUDIO) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // La permission d'enregistrement audio a été accordée
                } else {
                    Toast.makeText(this, "Permission denied: " + permissions[i], Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    // Initialize Text-to-Speech engine
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                loadLanguageSetting();
                promptForLanguage();
                textToSpeech.setSpeechRate(0.75f);
            }
        } else {
            Log.e("TTS", "Initialization Failed!");
        }
    }


    // Request necessary permissions
    private void requestPermissions() {
            List<String> permissionsNeeded = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.CAMERA);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            if (!permissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
            }
        }


    // Start the voice recognition activity
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak now");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    // Handle language response from voice recognition
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches.size() > 0) {
                String spokenText = matches.get(0);
                handleLanguageResponse(spokenText);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Handle language response from voice recognition
    private void handleLanguageResponse(String spokenText) {
        if (spokenText.toLowerCase().contains("english")) {
            textToSpeech.setLanguage(Locale.US);
            speakOut("You have selected English.");
            saveLanguageSetting(Locale.US);
        } else if (spokenText.toLowerCase().contains("french")) {
            textToSpeech.setLanguage(Locale.FRENCH);
            speakOut("Vous avez sélectionné le français.");
            saveLanguageSetting(Locale.FRENCH);
        } else {
            speakOut("Language not recognized, please try again.");
            promptForLanguage();
        }
    }

    // Save the selected language setting
    private void saveLanguageSetting(Locale locale) {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("Language", locale.toLanguageTag());
        editor.apply();
    }

    // Load the saved language setting
    private void loadLanguageSetting() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String languageTag = prefs.getString("Language", Locale.US.toLanguageTag());
        Locale locale = Locale.forLanguageTag(languageTag);
        textToSpeech.setLanguage(locale);
    }

    // Use Text-to-Speech to speak out text
    private void promptForLanguage() {
        speakOut("Please say which language you would like to use: English or French.");
        startVoiceRecognitionActivity();
    }

    // Use Text-to-Speech to speak out text
    private void speakOut(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // Update the UI with the detected color
    private void updateUIWithColor(int color) {
        new FetchColorNameTask().execute(color);  // Lancer la tâche asynchrone pour obtenir le nom de la couleur
    }

    // Tell to the user the color
    private void updateUI(String colorName, String colorHex) {
        runOnUiThread(() -> {
            View view = findViewById(R.id.color_view);
            try {
                int color = Color.parseColor(colorHex);
                view.setBackgroundColor(color);

                // Obtenir le nom de la couleur le plus proche
                String closestColorName = ColorDictionary.getClosestColorName(color);
                Toast.makeText(MainActivity.this, "Dominant color: " + colorName + "(" + closestColorName + ")", Toast.LENGTH_LONG).show();
                speakOut("The dominant color is " + colorName + ", which is close to " + closestColorName);

            } catch (IllegalArgumentException ex) {
                Toast.makeText(MainActivity.this, "Invalid color format: " + colorName, Toast.LENGTH_LONG).show();
                speakOut("Error: Invalid color format.");
            }
        });
    }


    // Implement additional necessary GestureDetector methods
    @Override
    public boolean onDown(MotionEvent e) { return true; }
    @Override
    public void onShowPress(MotionEvent e) {}
    @Override
    public boolean onSingleTapUp(MotionEvent e) { return true; }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return true; }
    @Override
    public void onLongPress(MotionEvent e) {}
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) { return true; }
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) { return true; }
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) { return true; }

    // SurfaceHolder.Callback methods to manage the camera lifecycle
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Check if the surface is ready to receive camera data
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    // Restart camera preview
    private void restartCameraPreview() {
        if (camera != null) {
            camera.stopPreview();
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                isPreviewActive = true; // Ensure the preview state is active
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // Clean up resources on destroy
    @Override
        protected void onDestroy() {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
            super.onDestroy();
        }

    // AsyncTask to fetch color name from an API
    private class FetchColorNameTask extends AsyncTask<Integer, Void, ColorInfo> {
        @Override
        protected ColorInfo doInBackground(Integer... colors) {
            String colorHex = String.format("%06X", (0xFFFFFF & colors[0]));
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://www.thecolorapi.com/id?hex=" + colorHex);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();

                    JSONObject jsonObject = new JSONObject(response);
                    String name = jsonObject.getJSONObject("name").getString("value");
                    return new ColorInfo(name, "#" + colorHex);
                } else {
                    return new ColorInfo("Error", "#000000"); // Default fallback
                }
            } catch (Exception e) {
                e.printStackTrace();
                return new ColorInfo("Error: " + e.getMessage(), "#000000");
            }
        }

        @Override
        protected void onPostExecute(ColorInfo colorInfo) {
            updateUI(colorInfo.name, colorInfo.hex);
        }
    }




}






