package com.example.cupbotmaybe;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cupbotmaybe.util.BluetoothLeService;

public class MainActivity extends AppCompatActivity {
    private BluetoothLeService bluetoothService;
    private ServiceConnection serviceConnection;
    private boolean registered = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gatt;
    private Game game;
    private static final int MSP_SET_MOTOR = 214; //TODO: double check if correct

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.game = new Game(this);
        enableFullScreen(); //enables full screen without title bar
        setContentView(this.game);
        }

    private void enableFullScreen() {
        // Get the Window object
        Window window = getWindow();

        // Set the WindowManager.LayoutParams.FLAG_FULLSCREEN flag to enable fullscreen
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        Intent serviceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
//
//        Log.e(TAG, "onResume: main activity is resumed");
//        startService(serviceIntent);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.e(TAG, "onServiceConnected: Initializing bluetooth service");

                //ADDED
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

                bluetoothService = ((BluetoothLeService.LocalBinder) service).getService();

                Log.e(TAG, "onServiceConnected: " + bluetoothService.getConnectionState());
                if(bluetoothService.getConnectionState() && bluetoothService != null){
                    if (!bluetoothService.initialize()) {
                        Log.e(TAG, "Unable to initialize Bluetooth");
                    }
                    // perform device connection
                    System.out.println(bluetoothService.getAddress());
                    bluetoothService.connect(bluetoothService.getAddress());
                    registerReceiver(bluetoothService.getGattUpdateReceiver(), makeGattUpdateIntentFilter());

                    Log.e(TAG, "Connect request result=" + bluetoothService.connect(bluetoothService.getAddress()));

                    Thread updateThread = new Thread(updateRunnable);
                    updateThread.start();

                    Log.e(TAG, "onServiceConnected: Performing device connection");
                }
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                bluetoothService = null;
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(registered){
            unregisterReceiver(bluetoothService.getGattUpdateReceiver());
            registered = false;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    private Handler updateHandler = new Handler();
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "run: Thread is running");
            if(game.getThrottleActuatorY() > game.getScreenHeight() - 875){
                byte[] payload = bluetoothService.preparePayloadForCommand(MSP_SET_MOTOR, new int[4]);
                bluetoothService.sendMSPCommand(MSP_SET_MOTOR, payload);
            }

            updateHandler.postDelayed(this, 100);
        }
    };
}