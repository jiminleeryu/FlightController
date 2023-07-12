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

public class BluetoothLeService extends Service {

    //The preamble is defined by the protocol.
    //Every message must begin with the characters $M
    private static final String PREAMBLE = "$M";
    //Character that denotes information being passed to the MultiWii
    private static final char TO_MUTLIWII = '<';
    //Character that denotes information being requested from by the MultiWii
    private static final char FROM_MUTLIWII = '>';
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
    private BluetoothGattCharacteristic controlCharacteristic;
    private Context appContext;
    private List<UUID> serviceUuids = new ArrayList<UUID>();
    private List<UUID> characteristicUuids = new ArrayList<UUID>();

    public BluetoothLeService(){
    }

    public void setContext(Context appContext){
        this.appContext = appContext;
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.e(TAG, "onReceive: DEVICE_GATT_CONNECTED");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.e(TAG, "onReceive: DEVICE_GATT_DISCONNECTED");
            }
        }
    };

    public BroadcastReceiver getGattUpdateReceiver(){
        return gattUpdateReceiver;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Handle any initialization or setup logic here
        startConnect = true;
        Log.e(TAG, "onStartCommand: On start Command called, startConnect: " + startConnect);
        // Return the desired behavior for the service (e.g., START_STICKY, START_NOT_STICKY)
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

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

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

    public String getAddress(){
        if(device != null){
            return device.getAddress();
        }
        return "";
    }

    @SuppressLint("MissingPermission")
    public void sendMSPCommand(int commandCode, byte[] payload) {
        if (bluetoothGatt != null && controlCharacteristic != null) {
            byte[] packet = createMSPPacket(commandCode, payload);
            controlCharacteristic.setValue(packet);
            bluetoothGatt.writeCharacteristic(controlCharacteristic);
        }
    }

    private byte[] createMSPPacket(int commandCode, byte[] payload) {
        int packetLength = 6 + payload.length;
        byte[] packet = new byte[packetLength];
        packet[0] = (byte) '$';
        packet[1] = 'M';
        packet[2] = '<';
        packet[3] = (byte) (payload.length & 0xFF);
        packet[4] = (byte) (commandCode & 0xFF);
        System.arraycopy(payload, 0, packet, 5, payload.length);
        packet[packetLength - 1] = calculateChecksum(packet);
        return packet;
    }

    // Example method to calculate the MSP packet checksum
    //TODO: change the method to calculate the checksum
    private byte calculateChecksum(byte[] packet) {
        byte checksum = 0;
        for (int i = 3; i < packet.length - 1; i++) {
            checksum ^= packet[i];
        }
        return checksum;
    }

    public byte[] preparePayloadForCommand(int commandCode, int[] motorSpeeds) {
        if (commandCode == 214 && motorSpeeds.length == 4) {
            // Prepare the payload for the motor command
            byte[] payload = new byte[9];
            payload[0] = (byte) 214; // Command code for motor control

            // Set the motor speeds
            for (int i = 0; i < 4; i++) {
                payload[i + 1] = (byte) (motorSpeeds[i] & 0xFF); // Set the speed for each motor
            }

            return payload;
        } else {
            // Handle other command codes or unsupported motor speeds
            return new byte[0]; // Return an empty payload for unsupported commands
        }
    }

    @SuppressLint("MissingPermission")
    public String getName(){
        return device.getName();
    }

    public boolean getConnectionState(){
        return this.startConnect;
    }
}
