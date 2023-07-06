package com.example.cupbotmaybe;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableFullScreen(); //enables full screen without title bar
        setContentView(new Game(this));
    }

    private void enableFullScreen() {
        // Get the Window object
        Window window = getWindow();

        // Set the WindowManager.LayoutParams.FLAG_FULLSCREEN flag to enable fullscreen
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}