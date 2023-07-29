package com.example.cupbotmaybe;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cupbotmaybe.ui.Game;

public class MainActivity extends AppCompatActivity {
    private Game game;

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

    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
}