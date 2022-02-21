package com.fydp.smartcane;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpThread implements Runnable {
    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */

    public String destUrl;
    public GoogleRouting.HTTP_REQUEST_TYPE type;
    public JSONObject route;
    public Thread threadWrapper;

    public HttpThread(String url_, GoogleRouting.HTTP_REQUEST_TYPE type_) {
        this.destUrl = url_;
        this.type = type_;
    }

    @Override
    public void run() {
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
            if (type == GoogleRouting.HTTP_REQUEST_TYPE.COORDINATES_TO_ADDRESS) {
                // CoordinatesToAddress
                Log.i("SendHttpRequest CoordinatesToAddress Success", ProcessFormattedAddress(result.toString()));

            } else if (type == GoogleRouting.HTTP_REQUEST_TYPE.GET_DIRECTION) {
                // GetDirectionByGoogleAPI
                Log.i("SendHttpRequest GetDirectionByGoogleAPI Success", ProcessDirectionInformation(result.toString()));

            }

        } catch (IOException ex) {
            Log.e("SendHttpRequest Failed", ex.getMessage());
        }
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
            JSONArray routes = obj.getJSONArray("routes");
            if (routes.length() == 0) {
                return "Sorry, we couldn't find a route given the current location source and target addresses.";
            }

            route = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0);
            return String.format("From %s heading to %s", route.getString("start_address"), route.getString("end_address"));
        } catch (org.json.JSONException ex) {
            return ex.getMessage();
        }
    }
}
