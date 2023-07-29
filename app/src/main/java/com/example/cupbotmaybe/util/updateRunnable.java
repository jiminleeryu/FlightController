package com.example.cupbotmaybe.util;

import static com.example.cupbotmaybe.util.BluetoothLeService.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.cupbotmaybe.ui.Game;

import java.util.Arrays;
import java.util.UUID;

public class updateRunnable implements Runnable{
    private volatile boolean stopReq = false;
    private long lastExecutionTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Game game;
    private BluetoothLeService service;
    private static final UUID MSP_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000" +
            "-00805f9b34fb"); //SUUID 3
    private static final UUID MSP_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"); //CHARUUID 6

    private byte[] packet;
    private BluetoothGattCharacteristic targetCharacteristic;
    private BluetoothGattService targetService;
    public void requestStop(boolean request){
        stopReq = request;
        if(request){
            handler.removeCallbacks(this);
        }
    }
    public updateRunnable(Game game, BluetoothLeService service){
        this.game = game;
        this.service = service;
        targetService =
                service.getBluetoothGatt().getService(MSP_SERVICE_UUID);
        Log.e(TAG, "updateRunnable - Service UUID: " + MSP_SERVICE_UUID);
        targetCharacteristic = targetService.getCharacteristic(MSP_CHARACTERISTIC_UUID);
    }
    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastExecutionTime;
        long delayMillis = Math.max(0, 100 - elapsedTime);
        handler.postDelayed(this, delayMillis);
        lastExecutionTime = currentTime;

        if(this.game.getUnlockState()){
            Log.e(TAG, "Unlock State: " + this.game.getUnlockState());
            readJoystick();
        }
    }
    @SuppressLint("MissingPermission")
    private void readJoystick(){
        Log.e(TAG, "Steer Y: " + this.game.getSteerActuatorY());
        Log.e(TAG, "Steer X: " + this.game.getSteerActuatorX());
        Log.e(TAG, "ThrottleY: " + this.game.getThrottleActuatorY());
        packet = createMSPPacket((byte) 150);

        if(this.game.getThrottleActuatorY() >= 0.75){// the stop for the drone

            Log.e(TAG, "readJoystick: SEND STOP");
        }

        if(this.game.getThrottleActuatorY() < 0.75){
            byte[] packet = createMSPPacket((byte) 151);
            sendPacket(packet);
            Log.e(TAG, "readJoystick: SEND GO");
        }
    }

    @SuppressLint("MissingPermission")
    private void sendPacket(byte[] packet){
        targetCharacteristic.setValue(packet);
        Log.e(TAG, "THROTTLE: PACKET: " + Arrays.toString(packet));
        service.getBluetoothGatt().writeCharacteristic(targetCharacteristic);
    }

    public static byte[] createMSPPacket(byte command) {
        // Create the MSP packet
        byte dataSize = 5;
        byte[] packet = new byte[11];
        packet[0] = '$';
        packet[1] = 'M';
        packet[2] = '<';
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