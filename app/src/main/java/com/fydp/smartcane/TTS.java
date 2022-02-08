package com.fydp.smartcane;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import android.widget.Toast;

public class TTS {
    private static TextToSpeech mTTS;
    private Context mContext;
    private static Integer mCount = 0;

    public TTS(Context pContext) {
        mContext = pContext;
        mTTS = new TextToSpeech(mContext.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    mTTS.setLanguage(Locale.UK);
                    Toast.makeText(mContext.getApplicationContext(), "TTS Started", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(mContext.getApplicationContext(), "TTS Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void textToVoice(String pString) {
        int result = mTTS.speak(pString, TextToSpeech.QUEUE_FLUSH, null, null);
        if (result != 0)
        {
            Toast.makeText(mContext.getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(mContext.getApplicationContext(), pString, Toast.LENGTH_SHORT).show();
        }
        mCount++;
    }
}