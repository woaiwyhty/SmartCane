package com.fydp.smartcane;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class TTS {
    private static TTS mInstance = null;
    private static TextToSpeech mTTS;
    private final Context mContext;
    private final Activity mActivity;

    private TTS(Context pContext, Activity pActivity) {
        mContext = pContext;
        mActivity = pActivity;
        mTTS = new TextToSpeech(mContext.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                mTTS.setLanguage(Locale.US);
                mActivity.runOnUiThread(() -> Toast.makeText(mContext.getApplicationContext(), "TTS Started", Toast.LENGTH_SHORT).show());
            } else {
                mActivity.runOnUiThread(() -> Toast.makeText(mContext.getApplicationContext(), "TTS Failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    public static TTS getTTS(Context pContext, Activity pActivity) {
        if (mInstance == null) {
            mInstance = new TTS(pContext, pActivity);
        }
        return mInstance;
    }

    public static TTS getTTS() {
        if (mInstance == null) {
            throw new InternalError();
        }
        return mInstance;
    }

    public void textToVoice(String pString) {
        int result = mTTS.speak(pString, TextToSpeech.QUEUE_ADD, null, null);
        if (result != 0) {
            mActivity.runOnUiThread(() -> Toast.makeText(mContext.getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show());
        } else {
            mActivity.runOnUiThread(() -> Toast.makeText(mContext.getApplicationContext(), pString, Toast.LENGTH_SHORT).show());
        }
    }
}
