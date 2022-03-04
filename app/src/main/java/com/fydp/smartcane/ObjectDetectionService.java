package com.fydp.smartcane;

import android.util.Log;

public class ObjectDetectionService {
    enum ObjectState {
        IDLE(1000),              // d > 10m
        LEVEL_SUPER_FAR(500),    // 5m < d <= 10m
        LEVEL_FAR(300),    // 5m < d <= 10m
        LEVEL_APPROACHING(100),  // 1m < d <= 5m
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
        int THRESHOLD = 100;
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
        } else if (distance > ObjectState.LEVEL_SUPER_FAR.getDistance()) {
            nextState = ObjectState.LEVEL_SUPER_FAR;
        }  else if (distance > ObjectState.LEVEL_FAR.getDistance()) {
            nextState = ObjectState.LEVEL_FAR;
        } else if (distance > ObjectState.LEVEL_APPROACHING.getDistance()) {
            nextState = ObjectState.LEVEL_APPROACHING;
        } else {
            nextState = ObjectState.LEVEL_CLOSE;
        }
        return nextState;
    }

    private void playWarning(ObjectState nextState) {
        if (lastState != nextState) {
            switch (nextState) {
                case IDLE:
                    TTS.getTTS().textToVoice("Object is 10 meters away");
                    break;
                case LEVEL_SUPER_FAR:
                    TTS.getTTS().textToVoice("Object is 5 meters away");
                    break;
                case LEVEL_FAR:
                    TTS.getTTS().textToVoice("Object is 3 meters away");
                    break;
                case LEVEL_APPROACHING:
                    TTS.getTTS().textToVoice("Attention! An object is approaching");
                    break;
                case LEVEL_CLOSE:
                    TTS.getTTS().textToVoice("Attention. Object is nearby.");
                    TTS.getTTS().textToVoice("Beep Beep Beep Beep Beep");
                    break;
                default:
                    TTS.getTTS().textToVoice("Attention!");
            }
        }
        lastState = nextState;
    }


}
