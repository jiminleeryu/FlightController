package com.example.cupbotmaybe.util;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Singleton;

@Singleton
public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.cupbotmaybe.util.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.cupbotmaybe.util.ACTION_GATT_DISCONNECTED";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;

    private int connectionState;
    private Binder binder = new LocalBinder();
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;
    public static final String TAG = "BluetoothLeService";
    private BluetoothAdapter bluetoothAdapter;
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.cupbotmaybe.ui.ACTION_GATT_SERVICES_DISCOVERED";

    public boolean startConnect = false;
    private Context appContext;
    private final List<UUID> serviceUuids = new ArrayList<>();
    private final List<UUID> characteristicUuids = new ArrayList<>();

    public BluetoothLeService(){
        initialize();
    }


    public void setContext(Context appContext){
        this.appContext = appContext;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        startConnect = true;
        Log.e(TAG, "onStartCommand: On start Command called, startConnect: " + startConnect);
        return START_STICKY;
    }

    public boolean initialize() {
        Log.e(TAG, "Checking initializing BluetoothLE connection");
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || address == null) {
            Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        try {
            this.device = bluetoothAdapter.getRemoteDevice(address);

            // connect to the GATT server on the device
            bluetoothGatt = device.connectGatt(appContext, false, bluetoothGattCallback); //TODO: getBaseContext is null
            Log.e(TAG, "Connecting to Bluetooth device");
            return true;
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "Device not found with provided address.  Unable to connect.");
            return false;
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        appContext.sendBroadcast(intent);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "onConnectionStateChange: Checking if BluetoothGatt is connected");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                connectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.e(TAG, "onConnectionStateChange: CONNECTED");

                // Attempts to discover services after successful connection.
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                Log.e(TAG, "onConnectionStateChange: DISCONNECTED");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                //ADDED
                gatt.requestMtu(517);

                for (BluetoothGattService service : services) {
                    serviceUuids.add(service.getUuid());

                    Log.e(TAG, "Service UUID: " + service.getUuid().toString());

                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        characteristicUuids.add(characteristic.getUuid());
                        Log.e(TAG, "Characteristic UUID: " + characteristic.getUuid().toString());
                    }
                }
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };

    public List<UUID> getServiceUuids(){
        return serviceUuids;
    }

    public List<UUID> getCharacteristicUuids(){
        return characteristicUuids;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public BluetoothGatt getBluetoothGatt(){
        return bluetoothGatt;
    }

    @SuppressLint("MissingPermission")
    private void close() {
        if (bluetoothGatt == null) {
            return;
        }
        startConnect = false;
        bluetoothGatt.close();
        bluetoothGatt = null;
    }
}
