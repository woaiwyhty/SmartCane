package com.fydp.smartcane;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public final class VoiceInputService {
    private static VoiceInputService INSTANCE = null;
    private final TextView voiceInputResult;
    private final Context mContext;
    private final String TAG = "VoiceInputService";
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    private VoiceInputService(TextView voiceInputResult, Context context) {
        this.voiceInputResult = voiceInputResult;
        // set up speech recognizer
        resetSpeechRecognizer(context);
        setRecogniserIntent();
        this.mContext = context;
    }

    public static VoiceInputService getInstance(TextView voiceInputResult, Context context) {
        if (INSTANCE == null) {
            INSTANCE = new VoiceInputService(voiceInputResult, context.getApplicationContext());
        }
        return (INSTANCE);
    }

    public void startListening() {
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
                    TTS.getTTS().textToVoice("I did not hear you");
                }

                @Override
                public void onResults(Bundle bundle) {
                    //getting all the matches
                    ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    Log.d(TAG, String.valueOf(matches));
                    //displaying the first match
                    if (matches != null) {
                        voiceInputResult.setText(matches.get(0));
//                        String result = matches.get(0);
                        ProgramControl.getInstance(mContext).processVoiceInput(matches);
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
        this.speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        this.speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }


}
