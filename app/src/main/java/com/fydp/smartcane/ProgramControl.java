package com.fydp.smartcane;

import android.content.Context;
import android.location.Location;
import android.util.Log;

public class ProgramControl {
    private static String TAG = ProgramControl.class.getSimpleName();
    private static ProgramControl mInstance;
    private Context mContext;
    private GoogleRouting routing;
    private GPS gps;
    private boolean inNavigation = false;
    private NavigationThread nvThread;

    private ProgramControl(Context context) {
        this.mContext = context;
    }

    public static ProgramControl getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ProgramControl(context.getApplicationContext());
        }
        return mInstance;
    }

    public void StartNavigation(String dest) {
        // assume that dest is in correct format such as 258+King+Street+Waterloo+ON
//        Log.i(ProgramControl.TAG, "Start getting current location");
//        Location currrent_location = getGps().getLocation();
//        HttpThread t = getRouting().GetDirectionByGoogleAPI(currrent_location, dest);
        if (inNavigation) {
            return;
        }

        inNavigation = true;
        nvThread = new NavigationThread(dest, this.mContext);
        nvThread.run();
    }

    public void EndNavigation() {
        if (!inNavigation) {
            return;
        }

        inNavigation = false;
        // TODO: add a proper way to terminate the navigation thread
    }

    private GoogleRouting getRouting() {
        if (routing == null) {
            routing = GoogleRouting.getInstance(this.mContext);
        }
        return routing;
    }

    private GPS getGps() {
        if (gps == null) {
            gps = GPS.getInstance(this.mContext);
        }
        return gps;
    }
}
