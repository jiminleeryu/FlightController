package com.example.cupbotmaybe.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import com.example.cupbotmaybe.R;

public class LockToggle {
    private int left;
    private int top;
    private int right;
    private int bottom;
    private Paint color;
    private boolean isPressed = false;

    public LockToggle(int left, int top, int right, int bottom, Context context){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;

        this.color = new Paint();
        this.color.setColor(ContextCompat.getColor(context, R.color.unlockColor));
    }
    public void draw(Canvas canvas) {
        canvas.drawRect(this.left, this.top, this.right, this.bottom, this.color);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(100);

        canvas.drawText("(Un)lock", (this.left + this.right) /2 - 175 ,
                (this.top + this.bottom) /2 + 35, textPaint);
    }

    public boolean getIsPressed(){
        return this.isPressed;
    }

    public void setIsPressed(boolean isPressed){
        this.isPressed = isPressed;
    }

    public boolean isPressed(double x, double y){
        return x > this.left && x < this.right && y > this.top && y < this.bottom;
    }
    public void setColor(Paint color){
        this.color = color;
    }
}
