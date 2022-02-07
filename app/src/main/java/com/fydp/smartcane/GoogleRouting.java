package com.fydp.smartcane;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.JsonReader;
import android.util.Log;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

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

    private String ProcessFormattedAddress(String result) {
        try {
            JSONObject obj = new JSONObject(result);
            JSONArray results = obj.getJSONArray("results");
            if (results.length() == 0) {
                return "Sorry, we couldn't find a corresponding formatted address given the current location coordinates.";
            }

            JSONObject first_address = results.getJSONObject(0);
            return first_address.getString("formatted_address");
        } catch (org.json.JSONException ex) {
            return ex.getMessage();
        }
    }

    private String ProcessDirectionInformation(String result) {
        try {
            JSONObject obj = new JSONObject(result);
            JSONArray results = obj.getJSONArray("results");
            if (results.length() == 0) {
                return "Sorry, we couldn't find a corresponding formatted address given the current location coordinates.";
            }

            JSONObject first_address = results.getJSONObject(0);
            return first_address.getString("formatted_address");
        } catch (org.json.JSONException ex) {
            return ex.getMessage();
        }
    }

    private void SendHttpRequest(String destUrl, HTTP_REQUEST_TYPE type) {
        Thread thread = new Thread("New Thread") {
            public void run(){
                try {
                    StringBuilder result = new StringBuilder();
                    URL url = new URL(destUrl);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.connect();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(con.getInputStream()))) {
                        for (String line; (line = reader.readLine()) != null; ) {
                            result.append(line);
                        }
                    }

                    con.disconnect();
                    if (type == HTTP_REQUEST_TYPE.COORDINATES_TO_ADDRESS) {
                        // CoordinatesToAddress
                        Log.i("SendHttpRequest CoordinatesToAddress Success", ProcessFormattedAddress(result.toString()));

                    } else if (type == HTTP_REQUEST_TYPE.GET_DIRECTION) {
                        // GetDirectionByGoogleAPI
                        Log.i("SendHttpRequest GetDirectionByGoogleAPI Success", result.toString());
                    }
                } catch (IOException ex) {
                    Log.e("SendHttpRequest Failed", ex.getMessage());
                }
            }
        };
        thread.start();
    }

    public void CoordinatesToAddress(Location l) {
        @SuppressLint("DefaultLocale") String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s&language=en",
                l.getLatitude(), l.getLongitude(), GoogleRouting.API_KEY);

        SendHttpRequest(url, HTTP_REQUEST_TYPE.COORDINATES_TO_ADDRESS);
    }

    public void GetDirectionByGoogleAPI(Location origin, String destination) {
        @SuppressLint("DefaultLocale") String url = String.format(
                "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%s&key=%s&language=en&mode=walking",
                origin.getLatitude(), origin.getLongitude(), GoogleRouting.API_KEY);

        SendHttpRequest(url, HTTP_REQUEST_TYPE.GET_DIRECTION);
    }

    private static String API_KEY = "AIzaSyDSWCOyQcEtQD5pnS5KXd1-KG5YkI_4CKw";
    private enum HTTP_REQUEST_TYPE {
        COORDINATES_TO_ADDRESS,
        GET_DIRECTION,
    };
}
