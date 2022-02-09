package com.fydp.smartcane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    // TODO: add voice output for all changes to bt_status

    private TextView bt_status;
    private Button bt_conn_button;
    private Context context;

    private BluetoothAdapter bt_adapter;
    private BluetoothSocket bt_socket;
    private BluetoothDevice bt_pi;

    private final int BT_CONN_REQ_CODE = 1;
    private final String TAG = "BluetoothService";

    public BluetoothService(TextView bt_status, Button bt_conn_button, Context context) {
        this.bt_status = bt_status;
        this.bt_conn_button = bt_conn_button;
        this.context = context;
        this.bt_adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean hasBtPermission() {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED);
    }
    public void getBtPermissions() {
        if (!hasBtPermission()) {
            this.bt_status.setText("requesting bt permissions");
            Log.d(TAG, "requesting bt permissions");
            String[] bt_permissions = {
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
            };
            ActivityCompat.requestPermissions((Activity) context, bt_permissions, BT_CONN_REQ_CODE);
        } else {
            this.bt_status.setText("bt permission already granted");
            Log.d(TAG, "bt permission already granted");
        }
    }

    @SuppressLint("MissingPermission")
    private boolean findPi(String pi_name) {
        if (!hasBtPermission()) {
            this.bt_status.setText("bt permission not granted, please grant permission and try again");
            Log.d(TAG, "bt permission not granted, please grant permission and try again");
            getBtPermissions();
            return false;
        }
        Set<BluetoothDevice> pairedDevices = bt_adapter.getBondedDevices();

        // There are paired devices. Get the name and address of each paired device.
        for (BluetoothDevice device : pairedDevices) {
            this.bt_status.setText("Looking for the cane");
            Log.d(TAG, "Looking for the cane");
            String deviceName = device.getName();
//            String deviceHardwareAddress = device.getAddress(); // MAC address
            if (deviceName.equals(pi_name)) {
                this.bt_status.setText("Found the cane in paired devices");
                Log.d(TAG, "Found the cane in paired devices");
                this.bt_pi = device;
                return true;
            }
        }

        this.bt_status.setText("Cannot find your cane. Please pair it with your phone and try again");
        Log.d(TAG, "Cannot find your cane. Please pair it with your phone and try again");
        return false;
    }

    @SuppressLint("MissingPermission")
    public void connectToPi(String pi_name) {
        bt_conn_button.setEnabled(false);

        if (!bt_adapter.isEnabled()) {
            // enable bluetooth if not already enabled
            bt_adapter.enable();
        }
        if (!findPi(pi_name)) {
            bt_conn_button.setEnabled(true);
            return;
        }

        // Cancel discovery because it otherwise slows down the connection.
        bt_adapter.cancelDiscovery();

        // Connect to the remote device through the socket. connect() blocks
        // until it succeeds or throws an exception. So use a countdowntimer
        // to try repeatedly
        // try to connect for 10 times, each with 5 sec interval
        bt_status.setText("Connecting to cane. Make sure it's on and in range");
        (new ConnectThread()).start();
        new CountDownTimer(50000, 5000) {
            private void onConnected() {
                bt_status.setText("Established bluetooth connection to the cane");
                Log.d(TAG, "Established bluetooth connection to the cane");
                bt_conn_button.setText("Already connected to cane");
                bt_conn_button.setEnabled(false);
                (new RecvThread()).start();
            }

            public void onTick(long millisUntilFinished) {
                if (bt_socket.isConnected()) {
                    onConnected();
                    cancel(); // cancel the timer
                    return;
                }

                // try again
                try {
                    bt_socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the client socket", e);
                }
                (new ConnectThread()).start();
            }

            public void onFinish() {
                if (bt_socket.isConnected()) {
                    onConnected();
                    return;
                }
                try {
                    bt_socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the client socket", e);
                }
                bt_conn_button.setEnabled(true);
                bt_status.setText("Cannot connect to the cane. Please try again");
                Log.d(TAG, "Cannot connect to the cane. Please try again");
            }
        }.start();
    }

    private void processMsg(String msg) {
        // TODO: change this to more meaningful processing
        // Could potentially put this is a separate thread if neccessary?

        // 1. emergency button pressed
        //    - if is_pressed == false -> init emergency protocol (play sound etc)
        //    - if is_pressed == true -> turn off emergency protocol

        // 2. lidar input received

        // updates to the ui must be done on the main thread
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                bt_status.setText(msg);

            }
        });
    }

    private class ConnectThread extends Thread {
        @SuppressLint("MissingPermission")
        public ConnectThread() {
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                // UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID?
                // UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID?
                UUID uuid = UUID.fromString("815425a5-bfac-47bf-9321-c5ff980b5e11");
                bt_socket = bt_pi.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
        }

        @SuppressLint("MissingPermission")
        public void run() {
            try {
                bt_socket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    bt_socket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                bt_socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class RecvThread extends Thread {
        private InputStream inputStream;
        private byte[] buf; // mmBuffer store for the stream

        public RecvThread() {
            try {
                inputStream = bt_socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
        }

        public void run() {
            buf = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = inputStream.read(buf);
                    String msg = new String(buf, StandardCharsets.UTF_8);
                    processMsg(msg);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                bt_socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}


