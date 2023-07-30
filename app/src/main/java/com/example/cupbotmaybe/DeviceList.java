package com.example.cupbotmaybe;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static com.example.cupbotmaybe.util.BluetoothLeService.ACTION_GATT_CONNECTED;
import static com.example.cupbotmaybe.util.BluetoothLeService.ACTION_GATT_DISCONNECTED;
import static com.example.cupbotmaybe.util.BluetoothLeService.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cupbotmaybe.ui.Game;
import com.example.cupbotmaybe.util.BluetoothLeService;
import com.example.cupbotmaybe.util.BluetoothLeViewModel;
import com.example.cupbotmaybe.util.updateRunnable;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DeviceList extends AppCompatActivity{
    private Set<BluetoothDevice> mPairedDevices;
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int PERMISSION_REQUEST_CODE = 1234;
    String[] appPermissions = {ACCESS_FINE_LOCATION};
    private BroadcastReceiver receiver;
    private ArrayList<BluetoothDevice> searchDevicesList = new ArrayList<>();
    private Intent serviceIntent;
    private List<String> devicesList = new ArrayList<>();
    private ListView deviceView;
    private boolean registered, connected = false;
    private BluetoothLeService bluetoothService;
    private Button controls, search;
    private BluetoothLeViewModel BLEViewModel;
    private Runnable runnable;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        controls = findViewById(R.id.controls_button);
        search = findViewById(R.id.select_device_refresh);

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
                // checking if the action received is one of a bluetooth device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (askPermission() && device != null) {
                        if (device.getName() != null && device.getAddress() != null) {
                            Log.e("device_name", device.getName());
                            Log.e("device_add", device.getAddress());

                            //removeNonDiscoverableDevice(device);
                            devicesList.add(device.getName() + " | " + device.getAddress());
                            searchDevicesList.add(device);
                            System.out.println("Found device " + device.getName() + " - " + device.getAddress());
                        }
                    }
                }
            }
        };

        controls.setOnClickListener(v -> start());
        search.setOnClickListener(v -> deviceConnectList());
    }

    public void start(){
        Game game = new Game(this);
        setContentView(game);
        if(connected){
            startControlling(game);
            stopSearch();
        }
    }

    private void startControlling(Game game) {
        if(bluetoothService != null){
            this.runnable = new updateRunnable(game, bluetoothService);
            Thread updateThread = new Thread(runnable);
            updateThread.start();

            Log.e(ContentValues.TAG, "onResume: BluetoothLeService is not null");
        }
    }

    @SuppressLint("MissingPermission")
    private void removeNonDiscoverableDevice(BluetoothDevice device) {
        Iterator<BluetoothDevice> iterator = searchDevicesList.iterator();
        while (iterator.hasNext()) {
            BluetoothDevice d = iterator.next();
            if (d.getAddress().equals(device.getAddress())) {
                iterator.remove();
                devicesList.remove(d.getName() + " | " + d.getAddress());
            }
        }

        if(registered){
            deviceView = findViewById(R.id.select_device_list);
            adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, devicesList);
            adapter.notifyDataSetChanged();
            deviceView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("MissingPermission")
    private void deviceConnectList() {

        // Register the BroadcastReceiver
        if(askPermission()){
            mBluetoothAdapter.startDiscovery();
            System.out.println("starting discovery");
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
            registered = true;
        }

        deviceView = findViewById(R.id.select_device_list);
        adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, devicesList);
        deviceView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        deviceView.setOnItemClickListener((parent, view, position, id) -> new AlertDialog.Builder(this)
                .setTitle("Connection")
                .setCancelable(true)
                .setMessage("Connect to " + devicesList.get(position) + "?")
                .setPositiveButton("Connect", (dialog, which) -> {

                    //ADDED
                    deviceView = findViewById(R.id.select_device_list);
                    adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, devicesList);
                    deviceView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                    serviceIntent = new Intent(this, BluetoothLeService.class);
                    startService(serviceIntent);

                    String address = searchDevicesList.get(position).getAddress();
                    Log.e(TAG, "Address: " + address);

                    this.BLEViewModel = new BluetoothLeViewModel(getApplication());

                    bluetoothService = BLEViewModel.getBluetoothLeService();
                    bluetoothService.setContext(getApplicationContext());
                    bluetoothService.connect(address);

                    connected = true;

                    Log.e(TAG, "deviceConnectList: Connect button was pressed");
                    //TODO: 1. establish connection, 2. once a connection is made from the bluetoothGatt, then add the device to the pairedDevices list.
                    dialog.dismiss();
                }).setNegativeButton("Dismiss", (dialog, which) -> {
                    dialog.dismiss();
                }).create().show());

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
        Intent intent = new Intent(DeviceList.this, MainActivity.class);
        startActivity(new Intent(intent));
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
    }

    @SuppressLint("MissingPermission")
    private void stopSearch() {

        if(askPermission()){
            mBluetoothAdapter.cancelDiscovery();
        }
        if(registered){
            Log.e(TAG, "stopSearch: Unregistered receiver");
            unregisterReceiver(receiver);
            registered = false;
        }
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

        ListView pairedView = findViewById(R.id.paired_device_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, pairedList);
        pairedView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}