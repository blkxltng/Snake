package com.blkxltng.snake;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    Canvas mCanvas;
    SnakeView mSnakeView;

    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;

    //Sound
    //Initialize sound variable
    private SoundPool mSoundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //for snake movement
    int directionOfTravel = 0; //0 = up, 1 = right, 2 = down, 3 = left;

    int screenWidth;
    int screenHeight;
    int topGap;

    //Statistics
    long lastFrameTime;
    int fps;
    int score;
    int highScore;

    //Game objects
    int [] snakeX;
    int [] snakeY;
    int snakeLength;
    int appleX;
    int appleY;

    //The size in pixels of a place on the game board
    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadSound();
        configureDisplay();
        mSnakeView = new SnakeView(this);
        setContentView(mSnakeView);
    }

    @Override
    protected void onStop() {
        super.onStop();

        while(true) {
            mSnakeView.pause();
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
        mSnakeView.resume();
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSnakeView.pause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //return super.onKeyDown(keyCode, event);

        if(keyCode == KeyEvent.KEYCODE_BACK) {
            mSnakeView.pause();
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }

        return false;
    }

    public void loadSound() {
        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {

            //Create objects of the 2 required classes
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //create the sound effects
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = mSoundPool.load(descriptor, 0);

        } catch (IOException e) {
            //Print an error
            Log.e("error", "failed to load sound files");
        }
    }

    public void configureDisplay() {

        //get screen dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        topGap = screenHeight / 14;
        //Determine the size of each block/place on the game board
        blockSize  = screenWidth / 40;

        //Determine how many blocks will fit into the height and width
        //leave a block for the score at the top
        numBlocksWide = 40;
        numBlocksHigh = (screenHeight - topGap) / blockSize;

        //Load bitmaps
        headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.head2);
        bodyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.body2);
        tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tail2);
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple2);

        //Scale the bitmaps to match the block size
        headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize, blockSize, false);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap, blockSize, blockSize, false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);
    }

    class SnakeView extends SurfaceView implements Runnable {

        Thread mThread = null;
        SurfaceHolder mHolder;
        volatile boolean playingSnake;
        Paint mPaint;

        public SnakeView(Context context) {
            super(context);
            mHolder = getHolder();
            mPaint = new Paint();

            snakeX = new int[200];
            snakeY = new int[200];

            //Our starting snake
            getSnake();
            //Get apples
            getApple();
        }

        public void getSnake() {
            snakeLength = 3;
            //start snake head in the middle of the screen
            snakeX[0] = numBlocksWide / 2;
            snakeY[0] = numBlocksHigh / 2;

            //Then the body
            snakeX[1] = snakeX[0] - 1;
            snakeY[1] = snakeY[0];

            //Last the tail
            snakeX[1] = snakeX[1] - 1;
            snakeY[1] = snakeY[0];
        }

        public void getApple() {
            Random random = new Random();
            appleX = random.nextInt(numBlocksWide-1)+1;
            appleY = random.nextInt(numBlocksHigh-1)+1;
        }

        @Override
        public void run() {

            while (playingSnake) {
                updateGame();
                drawGame();
                controlFPS();
            }
        }

        public void updateGame() {

            //Did they get the apple?
            if(snakeX[0] == appleX && snakeY[0] == appleY) {
                //grow the snake
                snakeLength++;
                //replace the apple
                getApple();
                //add to the score
                score = score + snakeLength;
                mSoundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            //move the body - starting from the back
            for(int i = snakeLength; i > 0; i--) {
                snakeX[i] = snakeX[i-1];
                snakeY[i] = snakeY[i-1];
            }

            //move the head in the right direction
            switch (directionOfTravel) {
                case 0:
                    snakeY[0]--; //up
                    break;

                case 1:
                    snakeX[0]++; //right
                    break;

                case 2:
                    snakeY[0]++; //down
                    break;

                case 3:
                    snakeX[0]--; //left
                    break;
            }

            //Have we collided?
            boolean dead = false;
            //with a wall
            if(snakeX[0] == -1) dead = true;
            if(snakeX[0] >= numBlocksWide) dead = true;
            if(snakeY[0] == -1) dead = true;
            if(snakeX[0] == numBlocksHigh) dead = true;
            //or eaten ourself?
            for(int i = snakeLength-1; i > 0; i--) {
                if((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i])) {
                    dead = true;
                }
            }

            if(dead) {
                //start again
                mSoundPool.play(sample4, 1, 1, 0, 0, 1);
                score = 0;
                getSnake();
            }
        }

        public void drawGame() {

            if (mHolder.getSurface().isValid()) {
                mCanvas = mHolder.lockCanvas();
                //Paint paint = new Paint();
                mCanvas.drawColor(Color.BLACK); //Background color
                mPaint.setColor(Color.argb(255, 255, 255, 255));
                mPaint.setTextSize(topGap/2);
                mCanvas.drawText("Score: " + score + " HighScore: " + highScore, 10, topGap-6, mPaint);

                //draw a border - 4 lines: top, right, bottom, left
                mPaint.setStrokeWidth(3); //3 pixel border
                mCanvas.drawLine(1, topGap, screenWidth-1, topGap, mPaint);
                mCanvas.drawLine(screenWidth-1, topGap, screenWidth-1, topGap+(numBlocksHigh*blockSize), mPaint);
                mCanvas.drawLine(screenWidth-1, topGap+(numBlocksHigh*blockSize), 1, topGap+(numBlocksHigh*blockSize), mPaint);
                mCanvas.drawLine(1, topGap, 1, topGap+(numBlocksHigh*blockSize), mPaint);

                //Draw the snake
                mCanvas.drawBitmap(headBitmap, snakeX[0]*blockSize, (snakeY[0]*blockSize)+topGap, mPaint);
                //body
                for(int i = 1; i < snakeLength-1; i++) {
                    mCanvas.drawBitmap(bodyBitmap, snakeX[i]*blockSize, (snakeY[i]*blockSize)+topGap, mPaint);
                }
                //tail
                mCanvas.drawBitmap(tailBitmap, snakeX[snakeLength-1]*blockSize, (snakeY[snakeLength-1]*blockSize)+topGap, mPaint);

                //Draw the apple
                mCanvas.drawBitmap(appleBitmap, appleX*blockSize, (appleY*blockSize) + topGap, mPaint);

                mHolder.unlockCanvasAndPost(mCanvas);

            }
        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 100 - timeThisFrame;
            if(timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if(timeToSleep > 0) {
                try {
                    mThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    //Insert catches here
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

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    if(event.getX() >= screenWidth/2) {
                        //turn right
                        directionOfTravel++;

                        if(directionOfTravel == 4) {
                            //no such direction, loop back to 0
                            directionOfTravel = 0;
                        }
                    } else { //turn left
                        directionOfTravel--;
                        if(directionOfTravel == -1) {
                            //no such direction, loop back to 3
                            directionOfTravel = 3;
                        }
                    }
            }

            return true;
        }


    }
}
