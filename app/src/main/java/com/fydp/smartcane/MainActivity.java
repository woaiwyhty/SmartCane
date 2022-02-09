package com.fydp.smartcane;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;


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
        initPermission();
        initBluetoothService();
        initVoiceInputService();
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

    private void initPermission() {
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION, false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                this.setNotification("Precise location access granted.");
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                this.setNotification("Only approximate location access granted.");
                            } else {
                                this.setNotification("No location access granted.");
                            }
                            Boolean audioGranted = result.getOrDefault(
                                Manifest.permission.RECORD_AUDIO, false);
                            if (audioGranted != null) {
                                this.voiceInputStatus.setText("Audio permission has been granted now.");
                            } else {
                                this.voiceInputStatus.setText("Audio permission is not granted.");
                            }
                        }
                );

        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        });
    }

    @SuppressLint({"WrongViewCast", "ClickableViewAccessibility"})
    private void initContentMain() {
        setContentView(R.layout.content_main);
        // GPS
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
                GoogleRouting gr = GoogleRouting.getInstance(view.getContext());
                gr.CoordinatesToAddress(location);
                setLocation(location);
            }
        });

        // text to speech
        TTS tts = TTS.getTTS(getApplicationContext());
        EditText ed1=(EditText)findViewById(R.id.editTextSpeak);
        Button b1=(Button)findViewById(R.id.buttonRead);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.textToVoice(ed1.getText().toString());
            }
        });
    }

    private void initBluetoothService() {
        this.bluetooth_conn_status = findViewById(R.id.bluetooth_conn_status);
        this.bt_conn_button = findViewById(R.id.bt_conn_button);
        this.bt_service = new BluetoothService(this.bluetooth_conn_status, this.bt_conn_button, MainActivity.this);
        this.bt_service.getBtPermissions();
        this.bt_conn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bt_service.connectToPi(PI_NAME);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initVoiceInputService() {
        this.voiceInputResult = findViewById(R.id.button_voice_input_result);
        this.voiceInputButton = findViewById(R.id.button_voice_input);
        this.voiceInputStatus = findViewById(R.id.audio_status);
        this.voiceInputService = VoiceInputService.getInstance(this.voiceInputResult, MainActivity.this);
        this.voiceInputButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        voiceInputResult.setHint("You will see input here");
                        voiceInputService.stopListening();
                        break;

                    case MotionEvent.ACTION_DOWN:
                        voiceInputService.startListening(v.getContext());
                        voiceInputResult.setText("");
                        voiceInputResult.setHint("Listening...");
                        break;
                }
                return false;
            }
        });
    }

    private void setNotification(String message) {
        tv_notification.setText(message);
    }

    private void setLocation(Location l) {
        @SuppressLint("DefaultLocale") String s = String.format("lon: %f, lat: %f", l.getLongitude(), l.getLatitude());
//        Log.i("Test GPS Result", s);
        tv_location.setText(s);
    }

    // gps
    private TextView tv_notification;
    private TextView tv_location;
    private Button button_test_gps;

    // voice input
    private VoiceInputService voiceInputService;
    private TextView voiceInputStatus;
    private TextView voiceInputResult;
    private Button voiceInputButton;

    // connect to bluetooth
    private TextView bluetooth_conn_status;
    private Button bt_conn_button;
    private BluetoothService bt_service;
    private final String PI_NAME = "raspberrypi-61";

    // text to speech
    private TTS tts;
}
