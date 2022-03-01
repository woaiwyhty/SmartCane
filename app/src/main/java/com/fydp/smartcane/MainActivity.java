package com.fydp.smartcane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class MainActivity extends AppCompatActivity {
    private final String PI_NAME = "raspberrypi-61";
    // gps
    private TextView tv_notification;
    private TextView tv_location;
    // voice input
    private VoiceInputService voiceInputService;
    private TextView voiceInputStatus;
    private TextView voiceInputResult;
    private TextView bluetoothConnStatus;
    private BluetoothService bt_service;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


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

    @RequiresApi(api = Build.VERSION_CODES.N)
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
        Button button_test_gps = findViewById(R.id.button_get_gps);

        button_test_gps.setOnClickListener(view -> {
            GPS gps = GPS.getInstance(view.getContext());
            if (!gps.isLocationProviderEnabled()) {
                setNotification("failed to find an enabled location provider");
                return;
            }

            Location location = gps.getLocation();
            GoogleRouting gr = GoogleRouting.getInstance(view.getContext());
            gr.CoordinatesToAddress(location);
            setLocation(location);
        });

        // text to speech
        TTS tts = TTS.getTTS(getApplicationContext(), MainActivity.this);
        EditText ed1 = findViewById(R.id.editTextSpeak);
        Button b1 = findViewById(R.id.buttonRead);
        b1.setOnClickListener(view -> tts.textToVoice(ed1.getText().toString()));
    }

    private void initBluetoothService() {
        // connect to bluetooth
        this.bluetoothConnStatus = findViewById(R.id.bluetooth_conn_status);
        this.voiceInputResult = findViewById(R.id.voice_input_result);
        Button bt_conn_button = findViewById(R.id.bt_conn_button);
        this.bt_service = new BluetoothService(this.bluetoothConnStatus, bt_conn_button, this.voiceInputResult, MainActivity.this);
        this.bt_service.getBtPermissions();
        bt_conn_button.setOnClickListener(view -> bt_service.connectToPi(PI_NAME));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initVoiceInputService() {
        Button voiceInputButton = findViewById(R.id.button_voice_input);
        this.voiceInputStatus = findViewById(R.id.audio_status);
        this.voiceInputResult = findViewById(R.id.voice_input_result);
        this.voiceInputService = VoiceInputService.getInstance(this.voiceInputResult, MainActivity.this);
        voiceInputButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    voiceInputResult.setHint("You will see input here");
                    voiceInputService.stopListening();
                    break;

                case MotionEvent.ACTION_DOWN:
                    voiceInputService.startListening();
                    voiceInputResult.setText("");
                    voiceInputResult.setHint("Listening...");
                    break;
            }
            return false;
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

    // text to speech
}
