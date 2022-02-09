package com.fydp.smartcane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.media.AudioManager;
import android.media.AudioManager;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public final class VoiceInputService {
    private static VoiceInputService INSTANCE = null;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private TextView voiceInputResult;

    private final String TAG = "VoiceInputService";

    private VoiceInputService(TextView voiceInputResult, Context context) {
        this.voiceInputResult = voiceInputResult;
        // set up speech recognizer
        resetSpeechRecognizer(context);
        setRecogniserIntent();
    }

    public static VoiceInputService getInstance(TextView voiceInputResult, Context context) {
        if (INSTANCE == null) {
            INSTANCE = new VoiceInputService(voiceInputResult, context.getApplicationContext());
        }
        return(INSTANCE);
    }

    public void startListening(Context context) {
        Log.d(TAG, "start listening");
        this.speechRecognizer.startListening(speechRecognizerIntent);
    }

    public void stopListening() {
        Log.d(TAG, "stop listening");
        this.speechRecognizer.stopListening();
    }

    private void resetSpeechRecognizer(Context context) {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float v) {

                }

                @Override
                public void onBufferReceived(byte[] bytes) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int i) {

                }

                @Override
                public void onResults(Bundle bundle) {
                    //getting all the matches
                    ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    Log.d(TAG, String.valueOf(matches));
                    //displaying the first match
                    if (matches != null){
                        voiceInputResult.setText(matches.get(0));
                    } else {
                        voiceInputResult.setText("Result Not Available");
                    }
                }

                @Override
                public void onPartialResults(Bundle bundle) {

                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            });
        }
    }

    private void setRecogniserIntent() {
        this.speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        this.speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        this.speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }

}
