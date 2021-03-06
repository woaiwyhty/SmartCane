package com.fydp.smartcane;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum ProgramState {
    IDLE,
    CONFIRMING_START,
    PENDING_CITY,
    PENDING_STREET,
    READY_NAVIGATION,
    IN_NAVIGATION,
    CONFIRMING_END,
    CONFIRMING_INPUT,
    CONFIRMING_HOME
}

public class ProgramControl {
    private static ProgramControl mInstance;
    private final Context mContext;
    public ProgramState mCurrState;
    ArrayList<String> mPossibleInputs;
    int mIndex;
    private Thread nvThread;
    private String mCity;
    private String mAddress;
    private ProgramState mPrevState;
    private boolean saveToHomeThisAddress = false;
    private boolean checkingHomeAddress = false;
    public static final String myPref = "preference";


    private ProgramControl(Context context) {
        this.mCurrState = ProgramState.IDLE;
        this.mContext = context;
    }

    public static ProgramControl getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ProgramControl(context.getApplicationContext());
        }
        return mInstance;
    }

    private static boolean voiceRegexCheck(String patternRegex, @NonNull ArrayList<String> voiceInput) {
        Pattern pattern = Pattern.compile(patternRegex, Pattern.CASE_INSENSITIVE);
        for (String result : voiceInput) {
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    private void askForStreet() {
        mCurrState = ProgramState.PENDING_STREET;
        TTS.getTTS().textToVoice("which street address do you want to walk to?");
    }

    private void askForCity() {
        mCurrState = ProgramState.PENDING_CITY;
        TTS.getTTS().textToVoice("Which city and province do you want to walk to?");
    }

    private void gotCity(String result) {
        mCity = result;
    }

    private void gotStreet(String result) {
        mAddress = result + ", " + mCity;
    }

    private void confirmAddress() {
        mCurrState = ProgramState.READY_NAVIGATION;
        if (checkingHomeAddress) {
            TTS.getTTS().textToVoice("is your home: " + mAddress);
        }
        else {
            TTS.getTTS().textToVoice("is your destination: " + mAddress);
        }
    }

    private void startNavigation() {
        mCurrState = ProgramState.IN_NAVIGATION;
        if (saveToHomeThisAddress) {
            storeHomeToConfig(mAddress);
            TTS.getTTS().textToVoice("home is set to " + mAddress);
            saveToHomeThisAddress = false;
        }
        TTS.getTTS().textToVoice("Navigation starting now.");
        nvThread = new Thread(new NavigationThread(mAddress, this.mContext, mInstance));
        nvThread.start();
    }

    private void endNavigation() {
        if (mCurrState == ProgramState.IN_NAVIGATION) {
            nvThread.interrupt();
        }
        mCurrState = ProgramState.IDLE;
        TTS.getTTS().textToVoice("Navigation just ended.");
    }

    public void processVoiceInput(ArrayList<String> voiceInput) {
        switch (mCurrState) {
            case IDLE:
                confirmingStart();
                break;
            case CONFIRMING_START:
                if (voiceRegexCheck(".*(yes|yeah).*", voiceInput)) {
                    checkGoHome();
                } else {
                    endNavigation();
                }
                break;
            case CONFIRMING_HOME:
                if (voiceRegexCheck(".*(yes|yeah).*", voiceInput)) {
                    goHome();
                } else {
                    askForCity();
                }
                break;
            case PENDING_CITY:
            case PENDING_STREET:
                confirmingVoiceInput(voiceInput);
                break;
            case CONFIRMING_INPUT:
                if (voiceRegexCheck(".*(yes|yeah).*", voiceInput)) {
                    confirmedVoiceInput();
                } else {
                    if (mIndex >= mPossibleInputs.size() || mIndex >= 3) {
                        repeatPrevState();
                    } else {
                        checkNextPossibleInput();
                    }
                }
                break;
            case READY_NAVIGATION:
                if (voiceRegexCheck(".*(yes|yeah).*", voiceInput)) {
                    startNavigation();
                } else if (voiceRegexCheck(".*(no|nope).*", voiceInput)) {
                    if (checkingHomeAddress) {
                        checkingHomeAddress = false;
                        askForHomeAddress();
                    }
                    else {
                        askForCity();
                    }
                } else {
                    endNavigation();
                }
                break;
            case IN_NAVIGATION:
                confirmingEnd();
                break;
            case CONFIRMING_END:
                if (voiceRegexCheck(".*(yes|yeah).*", voiceInput)) {
                    endNavigation();
                } else {
                    continueNavigation();
                }
                break;
        }
    }

    private void goHome() {
        mAddress = loadHomeFromConfig();
        if (mAddress == null) {
            askForHomeAddress();
        }
        else{
            checkingHomeAddress = true;
            confirmAddress();
        }
    }

    private void askForHomeAddress() {
        removeHomeFromConfig();
        saveToHomeThisAddress = true;
        TTS.getTTS().textToVoice("please tell me your home address.");
        askForCity();
    }

    private void checkGoHome() {
        mCurrState = ProgramState.CONFIRMING_HOME;
        TTS.getTTS().textToVoice("Do you want to go home?");
    }

    private void repeatPrevState() {
        if (mPrevState == ProgramState.PENDING_CITY) {
            askForCity();
        } else if (mPrevState == ProgramState.PENDING_STREET) {
            askForStreet();
        }
    }

    private void confirmedVoiceInput() {
        if (mPrevState == ProgramState.PENDING_CITY) {
            gotCity(mPossibleInputs.get(mIndex - 1));
            askForStreet();
        } else if (mPrevState == ProgramState.PENDING_STREET) {
            gotStreet(mPossibleInputs.get(mIndex - 1));
            confirmAddress();
        }
    }

    private void confirmingVoiceInput(ArrayList<String> voiceInput) {
        mPrevState = mCurrState;
        mCurrState = ProgramState.CONFIRMING_INPUT;
        mIndex = 0;
        mPossibleInputs = voiceInput;
        checkNextPossibleInput();
    }

    private void checkNextPossibleInput() {
        TTS.getTTS().textToVoice("Did you say " + mPossibleInputs.get(mIndex));
        mIndex++;
    }

    private void continueNavigation() {
        mCurrState = ProgramState.IN_NAVIGATION;
        TTS.getTTS().textToVoice("Navigation will continue.");
    }

    private void confirmingEnd() {
        TTS.getTTS().textToVoice("Do you want to end navigation?");
        mCurrState = ProgramState.CONFIRMING_END;
    }

    private void confirmingStart() {
        TTS.getTTS().textToVoice("Do you want to start navigation?");
        mCurrState = ProgramState.CONFIRMING_START;
    }

    public String loadHomeFromConfig()
    {
        SharedPreferences sp = mContext.getSharedPreferences(myPref,0);
        return sp.getString("myHome",null);
    }

    public void storeHomeToConfig(String homeAddress)
    {
        SharedPreferences shardPref = mContext.getSharedPreferences(myPref,0);
        SharedPreferences.Editor editor = shardPref.edit();
        editor.putString("myHome", homeAddress);
        editor.apply();
    }

    public void removeHomeFromConfig()
    {
        SharedPreferences shardPref = mContext.getSharedPreferences(myPref,0);
        SharedPreferences.Editor editor = shardPref.edit();
        editor.remove("myHome");
        editor.apply();
    }
}
