package com.fydp.smartcane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Locale;


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
        initAudioPermission();
        this.bt_service = new BluetoothService(this.bluetooth_conn_status, this.bt_conn_button, MainActivity.this);
        this.bt_service.getBtPermissions();
        this.bt_conn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bt_service.connectToPi(PI_NAME);
            }
        });
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
                                    Manifest.permission.ACCESS_COARSE_LOCATION, false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                this.setNotification("Precise location access granted.");
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                this.setNotification("Only approximate location access granted.");
                            } else {
                                this.setNotification("No location access granted.");
                            }
                        }
                );

        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void initAudioPermission() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityResultLauncher<String> audioPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        this.setNotification("audio access granted.");
                    } else {
                        this.setNotification("No audio access granted.");
                    }
            });
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
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

        // voice input
        voiceInputResult = findViewById(R.id.button_voice_input_result);
        voiceInputButton = findViewById(R.id.button_voice_input);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                //getting all the matches
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d("myTag", String.valueOf(matches));
                //displaying the first match
                if (matches != null)

                    voiceInputResult.setText(matches.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        this.voiceInputButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        speechRecognizer.stopListening();
                        voiceInputResult.setHint("You will see input here");
                        break;

                    case MotionEvent.ACTION_DOWN:
                        speechRecognizer.startListening(speechRecognizerIntent);
                        voiceInputResult.setText("");
                        voiceInputResult.setHint("Listening...");
                        break;
                }
                return false;
            }
        });
        this.bluetooth_conn_status = findViewById(R.id.bluetooth_conn_status);
        this.bt_conn_button = findViewById(R.id.bt_conn_button);
    }


    private void setNotification(String message) {
        tv_notification.setText(message);
    }

    private void setLocation(Location l) {
        @SuppressLint("DefaultLocale") String s = String.format("lon: %f, lat: %f", l.getLongitude(), l.getLatitude());
//        Log.i("Test GPS Result", s);
        tv_location.setText(s);
    }

    private TextView tv_notification;
    private TextView tv_location;
    private Button button_test_gps;
    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private TextView voiceInputResult;
    private Button voiceInputButton;

    // connect to bluetooth
    private TextView bluetooth_conn_status;
    private Button bt_conn_button;
    private BluetoothService bt_service;
    private final String PI_NAME = "raspberrypi-61";
}
