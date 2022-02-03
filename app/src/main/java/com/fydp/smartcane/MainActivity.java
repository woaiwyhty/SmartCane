package com.fydp.smartcane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initContentMain();
        initLocationPermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initLocationPermission() {
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                this.setNotification("Precise location access granted.");
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                this.setNotification("Only approximate location access granted.");
                            } else {
                                this.setNotification("No location access granted.");
                            }
                        }
                );

        locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void initContentMain() {
        setContentView(R.layout.content_main);

        this.tv_notification = findViewById(R.id.tv_notification);
        this.tv_location = findViewById(R.id.tv_location);
        this.button_test_gps = findViewById(R.id.button_get_gps);
        this.button_test_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPS gps = GPS.getInstance(view.getContext());
                if (!gps.isLocationProviderEnabled()) {
                    setNotification("failed to find an enabled location provider");
                    return;
                }

                Location location = gps.getLocation();
                setLocation(location);
            }
        });
    }

    private void setNotification(String message) {
        tv_notification.setText(message);
    }

    private void setLocation(Location l) {
        @SuppressLint("DefaultLocale") String s = String.format("lon: %f, lat: %f", l.getLongitude(), l.getLatitude());
        tv_location.setText(s);
    }

    private TextView tv_notification;
    private TextView tv_location;
    private Button button_test_gps;
}
