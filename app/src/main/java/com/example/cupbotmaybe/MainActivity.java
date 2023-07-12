package com.example.cupbotmaybe;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
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

    private BroadcastReceiver connectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.e(TAG, "onReceive: Connected, handler will start running");
                Thread updateThread = new Thread(updateRunnable);
                updateThread.start();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.e(TAG, "onReceive: disconnected, handler will stop running");
                updateHandler.removeCallbacksAndMessages(null);
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        registerReceiver(connectionStatusReceiver, filter);
    }

    @Override
    protected void onPause() { //TODO: FIX THIS
        super.onPause();
        unregisterReceiver(connectionStatusReceiver);
//        if(registered){
//            unregisterReceiver(bluetoothService.getGattUpdateReceiver());
//            registered = false;
//        }
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