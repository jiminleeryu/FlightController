package com.example.cupbotmaybe.util;

import android.annotation.SuppressLint;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class BluetoothLeViewModel extends AndroidViewModel {
    @SuppressLint("StaticFieldLeak")
    private final BluetoothLeService service;
    public BluetoothLeViewModel(@NonNull Application application) {
        super(application);
        service = new BluetoothLeService();
    }

    public BluetoothLeService getBluetoothLeService() {
        return service;
    }
}
