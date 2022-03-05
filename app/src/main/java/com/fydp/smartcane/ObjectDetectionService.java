package com.fydp.smartcane;

import android.util.Log;

public class ObjectDetectionService {
    enum ObjectState {
        IDLE(1100),              // d > 11m
        LEVEL_SUPER_FAR(600),    // 6m < d <= 11m
        LEVEL_APPROACHING(200),  // 1m < d <= 5m 提前预判空间
        LEVEL_CLOSE(0);          // 0m < d <= 1m

        int distance;
        ObjectState(int d) {
            distance = d;
        }
        int getDistance() {
            return distance;
        }
    }

    private ObjectState lastState;
    private float upperBound;
    private float lowerBound;
    private static ObjectDetectionService INSTANCE = null;

    public ObjectDetectionService() {
        lastState = ObjectState.IDLE;
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
        int THRESHOLD = 50;
        upperBound = distance + THRESHOLD;
        lowerBound = distance - THRESHOLD;
        Log.d("ObjectDetectionService", "Lidar: distance = " + distance + " cm, state: " + incomingState );
        if (lastState != incomingState) {
            playWarning(incomingState);
            lastState = incomingState;
        }
    }

    /*
    *   Map distance to state
    */
    private ObjectState computeState(float distance) {
        ObjectState nextState;
        if (distance > ObjectState.IDLE.getDistance()) {
            nextState = ObjectState.IDLE;
        } else if (distance > ObjectState.LEVEL_SUPER_FAR.getDistance()) {
            nextState = ObjectState.LEVEL_SUPER_FAR;
        } else if (distance > ObjectState.LEVEL_APPROACHING.getDistance()) {
            nextState = ObjectState.LEVEL_APPROACHING;
        } else {
            nextState = ObjectState.LEVEL_CLOSE;
        }
        return nextState;
    }

    private void playWarning(ObjectState state) {
        switch (state) {
            case LEVEL_SUPER_FAR:
                // 5 < d < 10
                TTS.getTTS().textToVoice("10 meters alert");
                break;
            case LEVEL_APPROACHING:
                // 2 < d < 5
                TTS.getTTS().textToVoice("5 meters alert.");
                break;
            case LEVEL_CLOSE:
                // d < 2
                TTS.getTTS().textToVoice("Beep Beep Beep Beep Beep");
                break;
            default:
                break;
        }
    }
}
