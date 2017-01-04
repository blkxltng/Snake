package com.blkxltng.snake;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends AppCompatActivity {

    Canvas mCanvas;
    SnakeAnimView mSnakeAnimView;

    Bitmap headAnimBitmap; //Snake head animation sprite sheet
    Rect rectToBeDrawn; //Portion of the bitmap to be drawn in the current frame
    //Frame dimensions
    int frameHeight = 64;
    int frameWidth = 64;
    int numFrames = 6;
    int frameNumber;

    int screenWidth;
    int screenHeight;

    //Statistics
    long lastFrameTime;
    int fps;
    int highScore;

    //Start the game
    Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get screen dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        headAnimBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.head_sprite_sheet2);

        mSnakeAnimView = new SnakeAnimView(this);

        setContentView(mSnakeAnimView);

        mIntent = new Intent(this, GameActivity.class);
    }

    @Override
    protected void onStop() {
        super.onStop();

        while(true) {
            mSnakeAnimView.pause();
            break;
        }

        finish();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();

        mSnakeAnimView.resume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //return super.onKeyDown(keyCode, event);
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            mSnakeAnimView.pause();
            finish();
            return true;
        }
        return false;
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSnakeAnimView.pause();
    }

    class SnakeAnimView extends SurfaceView implements Runnable {
        Thread mThread = null;
        SurfaceHolder mHolder;
        volatile boolean playingSnake;
        Paint mPaint;

        public SnakeAnimView(Context context) {
            super(context);
            mHolder = getHolder();
            mPaint = new Paint();
            frameWidth = headAnimBitmap.getWidth() / numFrames;
            frameHeight = headAnimBitmap.getHeight();
        }

        @Override
        public void run() {
            while(playingSnake) {
                update();
                draw();
                controlFPS();
            }
        }

        public void update() {

            //What frames to draw?
            rectToBeDrawn = new Rect((frameNumber * frameWidth) - 1, 0,
                    (frameNumber * frameWidth + frameWidth) - 1, frameHeight);

            //Next frame
            frameNumber++;

            //Don't draw frames that don't exist
            if(frameNumber == numFrames) {
                frameNumber = 0; //Go back to first frame
            }
        }

        public void draw() {

            if(mHolder.getSurface().isValid()) {
                mCanvas = mHolder.lockCanvas();
                //Paint mPaint = new Paint();
                mCanvas.drawColor(Color.BLACK); //Background color
                mPaint.setColor(Color.argb(255, 255, 255, 255));
                mPaint.setTextSize(150);
                mCanvas.drawText("Snake", 10, 150, mPaint);
                mPaint.setTextSize(25);
                mCanvas.drawText(" HighScore: " + highScore, 10, screenHeight-50, mPaint);

                //Draw the snake's head
                //Make this Rect whatever size and location you like
                //(startX, startY, endX, endY)
                Rect destRect = new Rect(screenWidth/2-100, screenHeight/2-100, screenWidth/2+100, screenHeight/2+100);

                mCanvas.drawBitmap(headAnimBitmap, rectToBeDrawn, destRect, mPaint);

                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 500 - timeThisFrame;
            if(timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if(timeToSleep > 0) {
                try {
                    mThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    //insert catch here
                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        public void pause() {
            playingSnake = false;
            try {
                mThread.join();
            } catch (InterruptedException e) {
                //insert catch here
            }
        }

        public void resume() {
            playingSnake = true;
            mThread = new Thread(this);
            mThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            //return super.onTouchEvent(event);
            startActivity(mIntent);
            return true;
        }
    }
}
