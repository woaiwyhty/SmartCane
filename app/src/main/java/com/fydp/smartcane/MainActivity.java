package com.fydp.smartcane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

        initBluetoothPermission();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == RESULT_CANCELED) {
                Log.d(Integer.toString(requestCode), "request denied");
                return;
            }

            switch (requestCode) {
                case BT_CONN_REQ_CODE:
                    this.bluetooth_conn_status.setText("bt discoverable permissions granted");
                    wait_for_pi_connection();
            }
        } catch (Exception ex) {
            Log.d(Integer.toString(requestCode), "exception in processing request");
        }
    }


    @SuppressWarnings("deprecation")
    private void initBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            this.bluetooth_conn_status.setText("requesting bt discoverable permission");
            String[] bt_permissions = {
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
            };
            ActivityCompat.requestPermissions(this, bt_permissions, BT_CONN_REQ_CODE);
//            Intent discoverableIntent =
//                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            // wait for 5 minutes
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivityForResult(discoverableIntent, BT_CONN_REQ_CODE);
        } else {
            this.bluetooth_conn_status.setText("bt permission already granted");
        }
        wait_for_pi_connection();
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

        this.bluetooth_conn_status = findViewById(R.id.bluetooth_conn_status);
    }

    @SuppressLint("MissingPermission")
    private void wait_for_pi_connection() {
        bluetooth_conn_status.setText("Waiting for smart cane to come online");
        // TODO: voice output the above as well
        this.bt_adapter = BluetoothAdapter.getDefaultAdapter();
        class AcceptThread extends Thread {
            private BluetoothServerSocket bt_server_socket = null;

            public AcceptThread() {
                try {
                    // MY_UUID is the app's UUID string, also used by the client code.
                    //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID?
                    UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID?
                    this.bt_server_socket = bt_adapter.listenUsingRfcommWithServiceRecord("smartcane-phone", uuid);
                } catch (IOException e) {
                    Log.e("socket", "Socket's listen() method failed", e);
                }

            }

            public void run() {
                BluetoothSocket socket = null;
                // Keep listening until exception occurs or a socket is returned.
                while (true) {
                    try {
                        bluetooth_conn_status.setText("listening on socket");
                        socket = bt_server_socket.accept();
                    } catch (IOException e) {
                        bluetooth_conn_status.setText("exception accepting connection");
                        break;
                    }

                    if (socket != null) {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        bluetooth_conn_status.setText("connection accepted!");
                        try {
                            bt_server_socket.close();
                            InputStream tmpIn = socket.getInputStream();
                            byte[] buf;
                            while (true) {
                                buf = new byte[1024];
                                int numBytes; // bytes returned from read()
                                // Read from the InputStream.
                                numBytes = tmpIn.read(buf);
                                String str = new String(buf, StandardCharsets.UTF_8);
                                bluetooth_conn_status.setText(str);
                            }
                        } catch (IOException e) {
                            bluetooth_conn_status.setText("exception reading from connection");
                            break;
                        }
                    }
                }
            }

            // Closes the connect socket and causes the thread to finish.
            public void cancel() {
                try {
                    bt_server_socket.close();
                } catch (IOException e) {
                    Log.e("socket", "Could not close the connect socket", e);
                }
            }
        }

        (new Thread(new AcceptThread())).start();
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

    // connect to bluetooth
    private static final int BT_CONN_REQ_CODE = 1;
    private TextView bluetooth_conn_status;
    private BluetoothAdapter bt_adapter;
}
