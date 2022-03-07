package com.fydp.smartcane;

import android.util.Log;

public class ObjectDetectionService {
    enum ObjectState {
        IDLE(600),    // 5m < d
        LEVEL_APPROACHING(300),  // 2m < d <= 5m
        LEVEL_CLOSE(0);          // 0m < d <= 2m

        int distance;
        ObjectState(int d) {
            distance = d;
        }
        int getDistance() {
            return distance;
        }
    }

    private float upperBound;
    private float lowerBound;
    private static ObjectDetectionService INSTANCE = null;

    public ObjectDetectionService() {
        upperBound = 10000;
        lowerBound = 10000;
    }

    public static ObjectDetectionService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ObjectDetectionService();
        }
        return (INSTANCE);
    }

    public void logState(float distance) {
        // within range
        if (distance <= upperBound && distance >= lowerBound) {
            return;
        }
        // compute the incoming state based on distance
        ObjectState incomingState = this.computeState(distance);
        int THRESHOLD = 40;
        upperBound = distance + THRESHOLD;
        lowerBound = distance - THRESHOLD;
        Log.d("ObjectDetectionService", "Lidar: distance = " + distance + " cm, state: " + incomingState );
        playWarning(incomingState);
    }

    /*
    *   Map distance to state
    */
    private ObjectState computeState(float distance) {
        ObjectState nextState;
        if (distance > ObjectState.IDLE.getDistance()) {
            nextState = ObjectState.IDLE;
        } else if (distance > ObjectState.LEVEL_APPROACHING.getDistance()) {
            nextState = ObjectState.LEVEL_APPROACHING;
        } else {
            nextState = ObjectState.LEVEL_CLOSE;
        }
        return nextState;
    }

    private void playWarning(ObjectState state) {
        switch (state) {
            case LEVEL_APPROACHING:
                TTS.getTTS().textToVoice("Short Beep");
                break;
            case LEVEL_CLOSE:
                TTS.getTTS().textToVoice("Long Beep");
                break;
            default:
                break;
        }
    }
}
