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
    private TextView voice_input_status;
    private Button bt_conn_button;
    private Context context;

    private BluetoothAdapter bt_adapter;
    private BluetoothSocket bt_socket;
    private BluetoothDevice bt_pi;

    private TTS tts;

    private VoiceInputService voiceInputService;

    private boolean emg_on;
    private boolean emg_current_state;

    private final String TAG = "BluetoothService";

    public BluetoothService(TextView bt_status, Button bt_conn_button, TextView voice_input_status, Context context) {
        this.bt_status = bt_status;
        this.voice_input_status = voice_input_status;
        this.bt_conn_button = bt_conn_button;
        this.context = context;
        this.bt_adapter = BluetoothAdapter.getDefaultAdapter();
        this.tts = TTS.getTTS();
        this.emg_on = false;
        this.emg_current_state = false;
        this.voice_input_status.setText("testing connected");
        this.voiceInputService = VoiceInputService.getInstance(this.voice_input_status, context);
    }

    public boolean hasBtPermission() {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED);
    }
    @SuppressLint("SetTextI18n")
    public void getBtPermissions() {
        if (!hasBtPermission()) {
            this.bt_status.setText("requesting bt permissions");
            this.tts.textToVoice("requesting bt permissions");
            Log.d(TAG, "requesting bt permissions");
            String[] bt_permissions = {
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
            };
            int BT_CONN_REQ_CODE = 1;
            ActivityCompat.requestPermissions((Activity) context, bt_permissions, BT_CONN_REQ_CODE);
        } else {
            this.bt_status.setText("bt permission already granted");
            this.tts.textToVoice("bt permission already granted");
            Log.d(TAG, "bt permission already granted");
        }
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    private boolean findPi(String pi_name) {
        if (!hasBtPermission()) {
            this.bt_status.setText("bt permission not granted, please grant permission and try again");
            this.tts.textToVoice("bt permission not granted, please grant permission and try again");
            Log.d(TAG, "bt permission not granted, please grant permission and try again");
            getBtPermissions();
            return false;
        }
        Set<BluetoothDevice> pairedDevices = bt_adapter.getBondedDevices();

        // There are paired devices. Get the name and address of each paired device.
        for (BluetoothDevice device : pairedDevices) {
            this.bt_status.setText("Looking for the cane");
            this.tts.textToVoice("Looking for the cane");
            Log.d(TAG, "Looking for the cane");
            String deviceName = device.getName();
//            String deviceHardwareAddress = device.getAddress(); // MAC address
            if (deviceName.equals(pi_name)) {
                this.bt_status.setText("Found the cane in paired devices");
                this.tts.textToVoice("Found the cane in paired devices");
                Log.d(TAG, "Found the cane in paired devices");
                this.bt_pi = device;
                return true;
            }
        }

        this.bt_status.setText("Cannot find your cane. Please pair it with your phone and try again");
        this.tts.textToVoice("Cannot find your cane. Please pair it with your phone and try again");
        Log.d(TAG, "Cannot find your cane. Please pair it with your phone and try again");
        return false;
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
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
        tts.textToVoice("Connecting to cane. Make sure it's on and in range");
        (new ConnectThread()).start();
        new CountDownTimer(50000, 5000) {
            private void onConnected() {
                bt_status.setText("Established bluetooth connection to the cane");
                tts.textToVoice("Established bluetooth connection to the cane");
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
                tts.textToVoice("Cannot connect to the cane. Please try again");
                Log.d(TAG, "Cannot connect to the cane. Please try again");
            }
        }.start();
    }

    private class emergencyProtocolThread extends Thread {

        @SuppressLint("SetTextI18n")
        public void run(){
            try{
                if (emg_on == false){
                    return;
                }
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bt_status.setText("An emergency happened. Need help. Please call 911.");
                        Log.d(TAG, "An emergency happened. Need help. Please call 911.");
                    }
                });
                while(true){
                    if (emg_on == false){
                        return;
                    }
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tts.textToVoice("An emergency happened. Need help. Please call 911.");
                        }
                    });
                    sleep(9000);
                }
            }catch(Exception e){
                Log.d(TAG, "Failed to launch emergency protocol.", e);
            }
        }
    }

    private class msgProcessThread extends Thread {
        private String[] parsedMsg;

        public msgProcessThread(String[] parsedMsg){
            this.parsedMsg = parsedMsg;
        }

        private void processMsg(String[] parsedMsg) throws Exception {
            // parsedMsg = "NAME:data"

            // 1. emergency button pressed
            //    - if is_pressed == false -> init emergency protocol (play sound etc) call TTS
            //    - if is_pressed == true -> turn off emergency protocol
            // 2. lidar input received
            // 3. Press to speak

            String name = parsedMsg[0];
            switch(name) {
                case "emergencyButton":
                    try{
                        if(parsedMsg[1].equals("true")){
                            emg_on = true;
                            if(emg_on == emg_current_state){
                                Log.d(TAG, parsedMsg[0] + ":" + parsedMsg[1] + " sent twice. Emergency protocol remain unchanged.");
                                throw new Exception(parsedMsg[0] + ":" + parsedMsg[1] + "Emergency protocol remain unchanged.");
                            }

                            // update and launch emergency protocol
                            emg_current_state = emg_on;
                            (new emergencyProtocolThread()).start();
                        }

                        if(parsedMsg[1].equals("false")){
                            //set emergency protocol tp off and notify
                            emg_on = false;
                            if(emg_on == emg_current_state){
                                Log.d(TAG, parsedMsg[0] + ":" + parsedMsg[1] + " sent twice. Emergency protocol remain unchanged.");
                                throw new Exception(parsedMsg[0] + ":" + parsedMsg[1] + "Emergency protocol remain unchanged.");
                            }

                            emg_current_state = emg_on;
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    bt_status.setText("Emergency situation has been resolved. Thank you.");
                                    tts.textToVoice("Emergency situation has been resolved. Thank you.");
                                    Log.d(TAG, "Emergency situation has been resolved. Thank you.");
                                }
                            });
                        }

                        if(!parsedMsg[1].equals("false") && !parsedMsg[1].equals("true")){
                            Log.d(TAG, "Invalid emergency button status.");
                            throw new Exception("Invalid emergency button status.");
                        }
                    }catch(InterruptedException e){
                        Log.d(TAG,"previous emergency protocol interrupted.", e);
                    }
                    break;
                case "lidar":
                    float distance_unit_cm = Float.parseFloat(parsedMsg[1]);
                    // might be useful
                    float lidar_strength = Float.parseFloat(parsedMsg[2]);
                    float lidar_temperature = Float.parseFloat(parsedMsg[3]);


                    // TODO: need to be connected to distance alarming/OpenCV service
                    // Note: lidar sending speed REALLY FAST! careful about queue etc
                    // This is a Dummy block for testing. Need to be changed to meaningful calls
//                    if (distance_unit_cm < 20){
////                        ((Activity) context).runOnUiThread(new Runnable() {
////                            @Override
////                            public void run() {
////                                bt_status.setText(
////                                        "Lidar: distance = " + distance_unit_cm + " cm" +
////                                                ", strength = " + lidar_strength +
////                                                ", temperature = " + lidar_temperature);
////                            }
////                        });
//                        Log.d(TAG, "Lidar: distance = " + distance_unit_cm + " cm" +
//                                        ", strength = " + lidar_strength +
//                                        ", temperature = " + lidar_temperature);
//                        Log.d(TAG, "original msg: " + parsedMsg[0] + " : " + parsedMsg[1] + " : " + parsedMsg[2] + " : " + parsedMsg[3]);
//                    }
                    Log.d(TAG, "Lidar: distance = " + distance_unit_cm + " cm" +
                            ", strength = " + lidar_strength +
                            ", temperature = " + lidar_temperature);
                    Log.d(TAG, "original msg: " + parsedMsg[0] + " : " + parsedMsg[1] + " : " + parsedMsg[2] + " : " + parsedMsg[3]);
                    break;
                case "pressToSpeak":
                    // TODO: press to speak working. Need to connect the input voice msg to follow up protocols
                    if(parsedMsg[1].equals("true")){
                        // This is a Dummy test of Voice Input start listening
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                voiceInputService.startListening();
                                voice_input_status.setText("");
                                voice_input_status.setHint("Listening...");
                            }
                        });
                    }else if(parsedMsg[1].equals("false")){
                        // This is a Dummy test of Voice Input stop listening
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                voice_input_status.setHint("You will see input here");
                                voiceInputService.stopListening();
                            }
                        });
                    } else {
                        throw new Exception("Invalid press to speak status.");
                    }
                    break;
                case "lowBattery":
                    if(parsedMsg[1].equals("true")){
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                bt_status.setText("Battery low. Please charge now.");
                                tts.textToVoice("Battery low. Please charge now.");
                                Log.d(TAG, "Battery low. Please charge now.");
                            }
                        });
                    }
                    break;
                default:
                    // unrecognized type
                    Log.d(TAG, "Unrecognized message type: " + parsedMsg[0]);
                    throw new Exception("Unrecognized message type: " + parsedMsg[0]);
            }
        }

        public void run(){
            try{
                this.processMsg(parsedMsg);
            }catch(Exception e){
                Log.d(TAG, "Message process failed.", e);
            }
        }
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
                    String[] cleanMsg = msg.split(";");
                    String[] parsedMsg = cleanMsg[0].split(":");
                    (new msgProcessThread(parsedMsg)).start();
                } catch (IOException e) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bt_status.setText("bluetooth connection lost");
                            tts.textToVoice("bluetooth connection lost");
                            bt_conn_button.setText("Connect to cane");
                            bt_conn_button.setEnabled(true);
                        }
                    });
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


