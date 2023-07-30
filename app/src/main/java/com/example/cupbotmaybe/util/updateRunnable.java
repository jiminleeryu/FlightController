package com.example.cupbotmaybe.util;

import static com.example.cupbotmaybe.util.BluetoothLeService.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.cupbotmaybe.ui.Game;

import java.util.UUID;

public class updateRunnable implements Runnable{
    private long lastExecutionTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Game game;
    private BluetoothLeService service;
    private static final UUID MSP_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000" +
            "-00805f9b34fb"); //Custom service UUID obtained by the Drone
    private static final UUID MSP_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private byte[] packet;
    private BluetoothGattCharacteristic targetCharacteristic;
    private BluetoothGattService targetService;

    /**
     * updateRunnable constructor, takes in a instance of Game and BluetoothLeService for
     * accessing game joystick values and Service methods
     */
    public updateRunnable(Game game, BluetoothLeService service){
        this.game = game;
        this.service = service;
        targetService =
                service.getBluetoothGatt().getService(service.getServiceUuids().get(2));
        targetCharacteristic =
                targetService.getCharacteristic(service.getCharacteristicUuids().get(6));
    }

    /**
     * Every 100 milliseconds the joystick will send packets only when the device is set to the
     * unlocked state.
     */
    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastExecutionTime;
        long delayMillis = Math.max(0, 100 - elapsedTime);
        handler.postDelayed(this, delayMillis);
        lastExecutionTime = currentTime;

        if(this.game.getUnlockState()){
            readJoystick();
        }
    }
    @SuppressLint("MissingPermission")
    private void readJoystick(){
        Log.e(TAG, "Steer Y: " + this.game.getSteerActuatorY());
        Log.e(TAG, "Steer X: " + this.game.getSteerActuatorX());
        Log.e(TAG, "ThrottleY: " + this.game.getThrottleActuatorY());// Values used for debugging
        // and reference purposes
        packet = createMSPPacket((byte) 150);

        if(this.game.getThrottleActuatorY() >= 0.75){// the stop for the drone, joystick is on
            // the bottom
            Log.e(TAG, "readJoystick: SEND STOP");
        }

        if(this.game.getThrottleActuatorY() < 0.75){ // Joystick throttle is above the stop point
            byte[] packet = createMSPPacket((byte) 151); // Command to set throttle value
            sendPacket(packet);
            Log.e(TAG, "readJoystick: SEND GO");
        }
    }

    /**
     * Sends packet to the Bluetooth LE module via BluetoothLeService Gatt, and writing the
     * characteristic.
     * @param packet the packet to send over to the drone
     */
    @SuppressLint("MissingPermission")
    private void sendPacket(byte[] packet){
        targetCharacteristic.setValue(packet);
        service.getBluetoothGatt().writeCharacteristic(targetCharacteristic);
    }

    /**
     * Creating MSP packet based on MultiWii protocol requirements
     */
    public static byte[] createMSPPacket(byte command) {
        // Create the MSP packet
        byte dataSize = 5;
        byte[] packet = new byte[11];
        //The preamble is defined by the protocol.
        //Every message must begin with the characters $M
        packet[0] = '$';
        packet[1] = 'M';
        packet[2] = '<'; //Character that denotes information being passed to the MultiWii
        packet[3] = 5; // Data size
        packet[4] = command; // Command byte (the specific command you want to send)
        packet[5] = 0;
        packet[6] = 0;
        packet[7] = 0;
        packet[8] = 125;
        packet[9] = 5;

        // Calculate the checksum for the entire packet (including size and command)
        byte checksum = 0;
        for (int i = 4; i < 9; i++) {
            checksum ^= packet[i];
        }
        // Append the final checksum
        packet[4 + dataSize + 1] = checksum;

        return packet;
    }
}