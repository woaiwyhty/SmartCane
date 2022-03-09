package com.fydp.smartcane;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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

    private List<Float> queue;
    private static ObjectDetectionService INSTANCE = null;
    private final int QUEUE_SIZE = 33;

    public ObjectDetectionService() {
        queue = new ArrayList<>();
    }

    public static ObjectDetectionService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ObjectDetectionService();
        }
        return (INSTANCE);
    }

    public void logState(float distance) {
        // compute the incoming state based on distance
        if (queue.size() >= QUEUE_SIZE) {
            Double avg = queue.stream().mapToDouble(d -> d).average().orElse(0.0);
            ObjectState incomingState = this.computeState(avg);
            playWarning(incomingState);
            Log.d("ObjectDetectionService", "Lidar: distance = " + distance + " cm, avg: " + avg + " cm, state: " + incomingState );
            queue.clear();
        }

        queue.add(distance);
    }

    /*
    *   Map distance to state
    */
    private ObjectState computeState(Double distance) {
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
