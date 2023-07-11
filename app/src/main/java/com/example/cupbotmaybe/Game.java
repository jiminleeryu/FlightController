package com.example.cupbotmaybe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.cupbotmaybe.ui.ControlsButton;
import com.example.cupbotmaybe.ui.Joystick;
import com.example.cupbotmaybe.util.BluetoothLeService;

public class Game extends SurfaceView implements SurfaceHolder.Callback {
    private GameLoop gameLoop;
    private Joystick throttle;
    private Joystick steer;
    private ControlsButton controlButton;
    private int steerPointerId = 0;
    private int throttlePointerId = 0;
    private int screenWidth;
    private int screenHeight;
    private Context context;

    public Game(Context context) {
        super(context);
        this.context = context;
        init(context);
    }

    private void init(Context context) {
        // Initialize SurfaceHolder
        //enable us to render things on a screen
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        this.screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        this.screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        gameLoop = new GameLoop(this, surfaceHolder);
        steer = new Joystick(screenWidth - 500,
                screenHeight - 600, 300, 150);
        throttle = new Joystick(500,
                screenHeight - 600, 300, 150);
        controlButton = new ControlsButton((screenWidth / 2) - 300 , (screenHeight / 2) - 300, screenWidth / 2 + 300, screenHeight / 2, context);

        throttle.setActuator(495, screenHeight - 375);

        setFocusable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //handle different touch activities
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                final int pointerIndex = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIndex);

                if (steer.isPressed(event.getX(pointerIndex), event.getY(pointerIndex))) {
                    steerPointerId = pointerId;
                    steer.setIsPressed(true);
                }
                if (throttle.isPressed(event.getX(pointerIndex), event.getY(pointerIndex))) {
                    throttlePointerId = pointerId;
                    throttle.setIsPressed(true);
                }

                if(controlButton.isPressed(event.getX(), event.getY())){
                    int color = ContextCompat.getColor(getContext(), R.color.controlsSelectedColor);
                    Paint paint = new Paint();
                    paint.setColor(color);
                    controlButton.setColor(paint);
                    controlButton.setIsPressed(true);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if(steerPointerId == event.getPointerId(event.getActionIndex())){
                    steer.setIsPressed(false);
                    steer.resetActuator();
                }
                if(throttlePointerId == event.getPointerId(event.getActionIndex())){
                    throttle.setIsPressed(false);
                }
                else if(controlButton.getIsPressed()){
                    controlButton.setIsPressed(false);
                }
                return true;


            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    float newX = event.getX(i);
                    float newY = event.getY(i);

                    if(newY > screenHeight - 375){
                        newY = screenHeight - 375;
                    }else if (newY < screenHeight - 825){
                        newY = screenHeight - 825;
                    }

                    if (id == steerPointerId && steer.getIsPressed()) {
                        steer.setActuator(newX, newY);
                    } else if (id == throttlePointerId && throttle.getIsPressed()) {
                        throttle.setActuator(495, newY);
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }



    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        gameLoop.startLoop();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        gameLoop.stopLoop();
        if (holder.getSurface() != null) {
            holder.getSurface().release();
        }
        holder.removeCallback(this);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawUPS(canvas);
        drawFPS(canvas);

        throttle.draw(canvas);
        steer.draw(canvas);
        controlButton.draw(canvas);
    }

    public void drawUPS(Canvas canvas) {
        String averageUPS = Double.toString(gameLoop.getAverageUPS());
        Paint paint = new Paint();
        int color = ContextCompat.getColor(getContext(), R.color.magenta);
        paint.setColor(color);
        paint.setTextSize(50);
        canvas.drawText("UPS: " + averageUPS, 100, 100, paint);
    }

    public void drawFPS(Canvas canvas) {
        String averageFPS = Double.toString(gameLoop.getAverageFPS());
        Paint paint = new Paint();
        int color = ContextCompat.getColor(getContext(), R.color.magenta);
        paint.setColor(color);
        paint.setTextSize(50);
        canvas.drawText("FPS: " + averageFPS, 100, 200, paint);
    }

    public void updateButton(){
        if(controlButton.getIsPressed()){
            controlButton.setIsPressed(false);
            Intent intent = new Intent(this.context, DeviceList.class);
            context.startActivity(intent);
        }
    }

    public void update() {
        throttle.update();
        steer.update();
        updateButton();
    }

    public double getThrottleActuatorY(){
        return this.throttle.getActuatorY();
    }
    public double getSteerActuatorY(){
        return this.steer.getActuatorY();
    }
    public double getSteerActuatorX(){
        return this.steer.getActuatorX();
    }

    public double getScreenWidth(){
        return this.screenWidth;
    }
    public double getScreenHeight(){
        return this.screenHeight;
    }
}
