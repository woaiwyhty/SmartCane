package com.fydp.smartcane;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GPS {
    private static String TAG = GPS.class.getSimpleName();
    private static GPS mInstance;
    private Context mContext;
    private static LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled");

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled");

        }

        @Override
        public void onLocationChanged(Location location) {
        }
    };

    private GPS(Context context) {
        this.mContext = context;
    }

    public static GPS getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new GPS(context.getApplicationContext());
        }
        return mInstance;
    }

    @SuppressLint("MissingPermission")
    public Location getLocation() {
        Location location = null;
        try {
            if (mContext == null) {
                return null;
            }
            LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return null;
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = getLocationByNetwork();
                }
            } else {
                location = getLocationByNetwork();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return location;
    }

    public boolean isLocationProviderEnabled() {
        boolean result = false;
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return result;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            result = true;
        }
        return result;
    }

    @SuppressLint("MissingPermission")
    private Location getLocationByNetwork() {
        Location location = null;
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mLocationListener);
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return location;
    }
}
