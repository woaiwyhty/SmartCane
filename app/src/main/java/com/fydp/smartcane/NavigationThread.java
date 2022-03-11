package com.fydp.smartcane;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.Hashtable;

public class NavigationThread implements Runnable {
    private final String dest;
    private final Context mContext;
    private final ProgramControl mPrgCtrl;
    private GoogleRouting routing;
    private GPS gps;

    public NavigationThread(String dest, Context context, ProgramControl programControl) {
        this.dest = dest;
        this.mContext = context;
        this.mPrgCtrl = programControl;
    }

    public String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    @Override
    public void run() {
        boolean ifStart = false;
        String lastNavigationText = null;
        Hashtable<String, Boolean> duplicateSteps = new Hashtable<>();
        while (mPrgCtrl.mCurrState == ProgramState.IN_NAVIGATION) {
            Log.i("Navigation Thread", "Start getting current location");
            Location current_location = getGps().getLocation();
            HttpThread t = getRouting().GetDirectionByGoogleAPI(current_location, dest);
            try {
                t.threadWrapper.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                lastNavigationText = "Failed to get the direction!";
                break;
            }
            Log.i("Navigation Thread", t.route.toString());
            try {
                JSONObject distance = t.route.getJSONObject("distance");
                JSONObject duration = t.route.getJSONObject("duration");
                String end_address = t.route.getString("end_address");

                JSONArray steps = t.route.getJSONArray("steps");
                if (steps.length() == 0) {
                    lastNavigationText = "Navigation is done! You have arrived the destination.";
                    break;
                }

                JSONObject first_step = steps.getJSONObject(0);
                if (steps.length() == 1 && first_step.getJSONObject("distance").getInt("value") < 2) {
                    lastNavigationText = "Navigation is done! You have arrived the destination.";
                    break;
                }

                if (!ifStart) {
                    String distance_duration_text = String.format(
                            "Head to %s, the estimated distance is %s and it would roughly take %s",
                            end_address,
                            distance.getString("text"),
                            duration.getString("text")
                    );

                    TTS.getTTS().textToVoice(distance_duration_text);
                    ifStart = true;
                }

                String text_instruction = html2text(first_step.getString("html_instructions"));
                Log.i("test", text_instruction);
                if (!duplicateSteps.containsKey(text_instruction)) {
                    duplicateSteps.put(text_instruction, true);
                    TTS.getTTS().textToVoice(String.format(
                            "%s for %s in %s",
                            text_instruction,
                            first_step.getJSONObject("distance").getString("text"),
                            first_step.getJSONObject("duration").getString("text")
                            )
                    );
                }


                Thread.sleep(2000);

            } catch (JSONException | InterruptedException e) {
                lastNavigationText = "Failed to get the direction!";
                break;
            }
        }
        TTS.getTTS().textToVoice(lastNavigationText);
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
