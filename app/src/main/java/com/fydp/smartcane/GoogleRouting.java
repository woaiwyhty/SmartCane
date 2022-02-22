package com.fydp.smartcane;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

public class GoogleRouting {
    private static String TAG = GoogleRouting.class.getSimpleName();
    private static GoogleRouting mInstance;
    private Context mContext;

    private GoogleRouting(Context context) {
        this.mContext = context;
    }

    public static GoogleRouting getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new GoogleRouting(context.getApplicationContext());
        }
        return mInstance;
    }


    private HttpThread SendHttpRequest(String destUrl, HTTP_REQUEST_TYPE type) {
        HttpThread myThreadRunner = new HttpThread(destUrl, type);
        Thread thread = new Thread(myThreadRunner);
        myThreadRunner.threadWrapper = thread;
        thread.start();
        return myThreadRunner;
    }

    public HttpThread CoordinatesToAddress(Location l) {
        @SuppressLint("DefaultLocale") String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s&language=en",
                l.getLatitude(), l.getLongitude(), GoogleRouting.API_KEY);

        return SendHttpRequest(url, HTTP_REQUEST_TYPE.COORDINATES_TO_ADDRESS);
    }

    public HttpThread GetDirectionByGoogleAPI(Location origin, String destination) {
        @SuppressLint("DefaultLocale") String url = String.format(
                "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%s&key=%s&language=en&mode=walking",
                origin.getLatitude(), origin.getLongitude(), destination, GoogleRouting.API_KEY);

        return SendHttpRequest(url, HTTP_REQUEST_TYPE.GET_DIRECTION);
    }

    private static String API_KEY = "AIzaSyDSWCOyQcEtQD5pnS5KXd1-KG5YkI_4CKw";
    public enum HTTP_REQUEST_TYPE {
        COORDINATES_TO_ADDRESS,
        GET_DIRECTION,
    };
}
