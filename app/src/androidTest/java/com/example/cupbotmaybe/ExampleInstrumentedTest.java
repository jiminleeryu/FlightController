package com.example.cupbotmaybe;

import android.content.Context;
import android.view.SurfaceHolder;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private Game game;
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.cupbotmaybe", appContext.getPackageName());
    }

    @Test
    public void testWindowSizes(){
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        this.game = new Game(appContext);

        SurfaceHolder surfaceHolder = this.game.getHolder();
        surfaceHolder.addCallback(this.game);

        System.out.println("left: " + surfaceHolder.getSurfaceFrame().left);
        System.out.println("right: " + surfaceHolder.getSurfaceFrame().right);
        System.out.println("Top: " + surfaceHolder.getSurfaceFrame().top);
        System.out.println("Bottom: " + surfaceHolder.getSurfaceFrame().bottom);

        assertNotEquals(surfaceHolder.getSurfaceFrame().left, surfaceHolder.getSurfaceFrame().centerX());
    }
}