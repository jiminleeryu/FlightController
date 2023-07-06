package com.example.cupbotmaybe;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_SCAN;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cupbotmaybe.databinding.ActivityDeviceListBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceList extends AppCompatActivity {

    private ActivityDeviceListBinding binding;
    private Set<BluetoothDevice> mPairedDevices;

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int PERMISSION_REQUEST_CODE = 1234;
    String[] appPermissions = {ACCESS_FINE_LOCATION};
    private BroadcastReceiver receiver;
    private ArrayList<BluetoothDevice> searchDevicesList = new ArrayList<>();
    private boolean isReceiverRegistered = false;
    private ListView deviceView;
    private ListView pairedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDeviceListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Snackbar.make(findViewById(android.R.id.content), "This device does not support Bluetooth",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        //broadcastReceiver to read for different bluetooth devices
        receiver = new BroadcastReceiver() {

            @SuppressLint("MissingPermission")
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                System.out.println("receiving action");
                // checking if the action received is one of a bluetooth device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    System.out.println("device found");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    System.out.println("logging device");

                    if(askPermission() && device != null){
                        if(device.getName() != null && device.getAddress() != null){
                            Log.e("device_name", device.getName());
                            Log.e("device_add", device.getAddress());

                            searchDevicesList.add(device);
                            System.out.println("Found device" + device.getName() + " - " + device.getAddress());
                        }
                    }
                }
            }
        };

        binding.selectDeviceRefresh.setOnClickListener(v -> deviceConnectList());
        binding.controlsButton.setOnClickListener(v -> startActivity(new Intent(DeviceList.this, MainActivity.class)));
    }

    @SuppressLint("MissingPermission")
    private void deviceConnectList() {
        deviceView = findViewById(R.id.select_device_list);

        pairedDeviceList();

        mBluetoothAdapter.startDiscovery();

        List<String> devices = new ArrayList<>();

        for (BluetoothDevice device : searchDevicesList) {
            if(askPermission()){
                @SuppressLint("MissingPermission")
                String dev = device.getName() + " | " + device.getAddress();

                if(searchDevicesList.contains(device)){
                    devices.remove(dev);
                }
                devices.add(dev);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, devices);
        deviceView.setAdapter(adapter);

        deviceView.setOnItemClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Connection")
                    .setCancelable(true)
                    .setMessage("Connect to " + devices.get(position) + "?")
                    .setPositiveButton("Connect", (dialog, which) -> {
                        dialog.dismiss();
                        //connect();
                    }).setNegativeButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                    }).create().show();
        });


    }

    public boolean askPermission() {
        List<String> permissionsNeeded = new ArrayList<>();

        for (String permission : appPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[permissionsNeeded.size()]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(DeviceList.this, MainActivity.class));
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        this.isReceiverRegistered = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSearch();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        isReceiverRegistered = true;

        // Register the BroadcastReceiver
        if(askPermission()){
            mBluetoothAdapter.startDiscovery();
            System.out.println("starting discovery");
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
        }

        binding.selectDeviceRefresh.setOnClickListener(v -> deviceConnectList());
        binding.controlsButton.setOnClickListener(v -> startActivity(new Intent(DeviceList.this, MainActivity.class)));
    }

    @SuppressLint("MissingPermission")
    private void stopSearch() {
        isReceiverRegistered = false;

        if(askPermission()){
            mBluetoothAdapter.cancelDiscovery();
        }
        // Unregister the BroadcastReceiver
        unregisterReceiver(receiver);
    }

    private void pairedDeviceList() {
        if (mBluetoothAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                List<String> permissions = new ArrayList<>();
                permissions.add(BLUETOOTH_SCAN);
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[1]), PERMISSION_REQUEST_CODE);
                return;
            }
            mPairedDevices = mBluetoothAdapter.getBondedDevices();
        }

        ArrayList<String> pairedList = new ArrayList<>();

        if (!mPairedDevices.isEmpty()) {
            for (BluetoothDevice device : mPairedDevices) {
                String address = device.getAddress();
                String name = device.getName();
                pairedList.add(name + " | " + address);
                Log.i("Device", " " + device);
            }
        } else {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "No paired devices found", Snackbar.LENGTH_LONG).show();
        }

        pairedView = findViewById(R.id.paired_device_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, pairedList);
        pairedView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}