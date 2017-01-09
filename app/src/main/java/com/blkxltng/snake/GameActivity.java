package com.blkxltng.snake;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
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
//    int sample1 = -1;
//    int sample2 = -1;
//    int sample3 = -1;
//    int sample4 = -1;
    int sndEat = -1;
    int sndDeath = -1;

    //for snake movement
    int directionOfTravel = 0; //0 = up, 1 = right, 2 = down, 3 = left;

    int screenWidth;
    int screenHeight;
    int topGap;

    //Statistics
    long lastFrameTime;
    int fps;
    int score;

    //Game objects
    int [] snakeX;
    int [] snakeY;
    int [] snakeH; //For snake section directions
    int snakeLength;
    int appleX;
    int appleY;

    //The size in pixels of a place on the game board
    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    //Swipe Detection
    //GestureDetectorCompat mGestureDetectorCompat;

    //For flowers
    Bitmap flowerBitmap;

    //For animating the flower
    //The portion of the bitmap to be drawn in the current frame
    Rect flowerRectToBeDrawn;
    //The dimensions of a single frame
    //frame width and height need to be dynamic
    //based on block size
    int frameHeight;
    int frameWidth;
    int flowerNumFrames = 2;
    int flowerFrameNumber;
    //Measure how often the flower is animated
    int flowerAnimTimer = 0;

    //Some matrix objects to rotate our snake segments
    //Facing right is the normal orientation
    //Here are the other 3
    Matrix matrix90 = new Matrix();
    Matrix matrix180 = new Matrix();
    Matrix matrix270 = new Matrix();
    //This one reverses the head
    //a slightly different effect to the tail and body
    //because otherwise the head will be upside down
    Matrix matrixHeadFlip = new Matrix();
    //We initialize these in the configureDisplay method


    //And the pretty flowers
    int [] flowersX;
    int [] flowersY;

    //For the High Score
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;
    String dataName = "MyData";
    String intName = "MyScore";
    int defaultInt = 0;
    int highScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadSound();
        configureDisplay();
        mSnakeView = new SnakeView(this);
        setContentView(mSnakeView);

        //for highscore
        mSharedPreferences = getSharedPreferences(dataName, MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        highScore = mSharedPreferences.getInt(intName, defaultInt);
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
//            descriptor = assetManager.openFd("sample1.ogg");
//            sample1 = mSoundPool.load(descriptor, 0);
//
//            descriptor = assetManager.openFd("sample2.ogg");
//            sample2 = mSoundPool.load(descriptor, 0);
//
//            descriptor = assetManager.openFd("sample3.ogg");
//            sample3 = mSoundPool.load(descriptor, 0);
//
//            descriptor = assetManager.openFd("sample4.ogg");
//            sample4 = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("eaten.ogg");
            sndEat = mSoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("death.ogg");
            sndDeath = mSoundPool.load(descriptor, 0);

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
        headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.head3);
        bodyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.body3);
        tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tail3);
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple3);

        //Scale the bitmaps to match the block size
        headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize, blockSize, false);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap, blockSize, blockSize, false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);

//        //for the tail
//        tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tail_sprite_sheet2);
//        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize*flowerNumFrames, blockSize, false);

        //for the flower
        flowerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.newflowersway);
        flowerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.newflowersway);
        flowerBitmap = Bitmap.createScaledBitmap(flowerBitmap, blockSize*flowerNumFrames, blockSize, false);

        //These two lines work for the flower and the tail
        frameWidth=flowerBitmap.getWidth()/flowerNumFrames;
        frameHeight=flowerBitmap.getHeight();

        //Initialize matrix objects ready for us in drawGame
        matrix90.postRotate(90);
        matrix180.postRotate(180);
        matrix270.postRotate(270);
        //And now the head flipper
        matrixHeadFlip.setScale(-1,1);
        matrixHeadFlip.postTranslate(headBitmap.getWidth(),0);

        //setup the first frame of the flower drawing
        flowerRectToBeDrawn = new Rect((flowerFrameNumber * frameWidth), 0,
                (flowerFrameNumber * frameWidth +frameWidth)-1, frameHeight);
    }

    class SnakeView extends SurfaceView implements Runnable {

        Thread mThread = null;
        SurfaceHolder mHolder;
        volatile boolean playingSnake;
        Paint mPaint;

        GestureDetectorCompat mDetectorCompat;

        public SnakeView(Context context) {
            super(context);
            mHolder = getHolder();
            mPaint = new Paint();

            snakeX = new int[200];
            snakeY = new int[200];
            snakeH = new int[200];

            mDetectorCompat = new GestureDetectorCompat(getApplicationContext(), mSimpleOnGestureListener);

            //Plants some flowers
            plantFlowers();
            //Our starting snake
            getSnake();
            //Get apples
            getApple();
        }

        public void plantFlowers(){
            Random random = new Random();
            int x = 0;
            int y = 0;
            flowersX = new int[200];
            flowersY = new int[200];

            for(int i = 0;i < 10; i++){
                x = random.nextInt(numBlocksWide-1)+1;
                y = random.nextInt(numBlocksHigh-1)+1;
                flowersX[i] = x;
                flowersY[i] = y;
            }

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
                mSoundPool.play(sndEat, 1, 1, 0, 0, 1);
            }

            //move the body - starting from the back
            for(int i = snakeLength; i > 0; i--) {
                snakeX[i] = snakeX[i-1];
                snakeY[i] = snakeY[i-1];

                //change heading
                snakeH[i] = snakeH[i-1];
            }

            //move the head in the right direction
            switch (directionOfTravel) {
                case 0:
                    snakeY[0]--; //up
                    snakeH[0] = 0;
                    break;

                case 1:
                    snakeX[0]++; //right
                    snakeH[0] = 1;
                    break;

                case 2:
                    snakeY[0]++; //down
                    snakeH[0] = 2;
                    break;

                case 3:
                    snakeX[0]--; //left
                    snakeH[0] = 3;
                    break;
            }

            //Have we collided?
            boolean dead = false;
            //with a wall
            if(snakeX[0] == -1) dead = true;
            if(snakeX[0] >= numBlocksWide) dead = true;
            if(snakeY[0] == -1) dead = true;
            if(snakeY[0] == numBlocksHigh) dead = true;
            //or eaten ourself?
            for(int i = snakeLength-1; i > 0; i--) {
                if((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i])) {
                    dead = true;
                }
            }

            if(dead) {
                //start again
                mSoundPool.play(sndDeath, 1, 1, 0, 0, 1);
                if(score > highScore) {
                    highScore = score;
                    mEditor.putInt(intName, highScore);
                    mEditor.commit();
                    //Toast.makeText(getApplicationContext(), "New High Score!", Toast.LENGTH_SHORT).show();
                }
                Intent i = new Intent(getContext(), GameOverActivity.class);
                i.putExtra("Score", score);
                startActivity(i);
                score = 0;
                getSnake();
            }
        }

        public void drawGame() {

            if (mHolder.getSurface().isValid()) {
                mCanvas = mHolder.lockCanvas();
                //Paint paint = new Paint();
                mCanvas.drawColor(Color.argb(255, 145, 227, 151)); //Background color
                mPaint.setColor(Color.argb(255, 255, 255, 255));
                mPaint.setTextSize(topGap/2);
                mCanvas.drawText("Score: " + score + " HighScore: " + highScore, 10, topGap-6, mPaint);

                //draw a border - 4 lines: top, right, bottom, left
                mPaint.setStrokeWidth(3); //3 pixel border
                mCanvas.drawLine(1, topGap, screenWidth-1, topGap, mPaint);
                mCanvas.drawLine(screenWidth-1, topGap, screenWidth-1, topGap+(numBlocksHigh*blockSize), mPaint);
                mCanvas.drawLine(screenWidth-1, topGap+(numBlocksHigh*blockSize), 1, topGap+(numBlocksHigh*blockSize), mPaint);
                mCanvas.drawLine(1, topGap, 1, topGap+(numBlocksHigh*blockSize), mPaint);

                //Draw our flowers
                Rect destRect;
                Bitmap rotatedBitmap;
                Bitmap rotatedTailBitmap;

                for (int i = 0;i < 10;i++){
                    destRect = new Rect(flowersX[i]*blockSize, (flowersY[i]*blockSize)+topGap, (flowersX[i]*blockSize)+blockSize, (flowersY[i]*blockSize)+topGap+blockSize);
                    mCanvas.drawBitmap(flowerBitmap, flowerRectToBeDrawn, destRect, mPaint);
                }

//                //Draw the snake
//                mCanvas.drawBitmap(headBitmap, snakeX[0]*blockSize, (snakeY[0]*blockSize)+topGap, mPaint);
//                //body
//                for(int i = 1; i < snakeLength-1; i++) {
//                    mCanvas.drawBitmap(bodyBitmap, snakeX[i]*blockSize, (snakeY[i]*blockSize)+topGap, mPaint);
//                }
//                //tail
//                mCanvas.drawBitmap(tailBitmap, snakeX[snakeLength-1]*blockSize, (snakeY[snakeLength-1]*blockSize)+topGap, mPaint);

                //Draw the snake
                rotatedBitmap = headBitmap;
                switch (snakeH[0]){
                    case 0://up
                        rotatedBitmap = Bitmap.createBitmap(rotatedBitmap , 0, 0, rotatedBitmap .getWidth(), rotatedBitmap .getHeight(), matrix270, true);
                        break;
                    case 1://right
                        //no rotation necessary

                        break;
                    case 2://down
                        rotatedBitmap = Bitmap.createBitmap(rotatedBitmap , 0, 0, rotatedBitmap .getWidth(), rotatedBitmap .getHeight(), matrix90, true);
                        break;

                    case 3://left
                        rotatedBitmap = Bitmap.createBitmap(rotatedBitmap , 0, 0, rotatedBitmap .getWidth(), rotatedBitmap .getHeight(), matrixHeadFlip, true);
                        break;


                }
                mCanvas.drawBitmap(rotatedBitmap, snakeX[0]*blockSize, (snakeY[0]*blockSize)+topGap, mPaint);
                //Draw the body

                rotatedBitmap = bodyBitmap;
                for(int i = 1; i < snakeLength-1;i++){

                    switch (snakeH[i]){
                        case 0://up
                            rotatedBitmap = Bitmap.createBitmap(bodyBitmap , 0, 0, bodyBitmap .getWidth(), bodyBitmap .getHeight(), matrix270, true);
                            break;
                        case 1://right
                            //no rotation necessary

                            break;
                        case 2://down
                            rotatedBitmap = Bitmap.createBitmap(bodyBitmap , 0, 0, bodyBitmap .getWidth(), bodyBitmap .getHeight(), matrix90, true);
                            break;

                        case 3://left
                            rotatedBitmap = Bitmap.createBitmap(bodyBitmap , 0, 0, bodyBitmap .getWidth(), bodyBitmap .getHeight(), matrix180, true);
                            break;


                    }

                    mCanvas.drawBitmap(rotatedBitmap, snakeX[i]*blockSize, (snakeY[i]*blockSize)+topGap, mPaint);
                }


                //draw the tail
                //make rotated bitmap hold just the current frame of the tail
                //Otherwise we will get strange effects when rotating
                rotatedTailBitmap = tailBitmap; //Bitmap.createBitmap(tailBitmap, flowerRectToBeDrawn.left, flowerRectToBeDrawn.top, flowerRectToBeDrawn.right - flowerRectToBeDrawn.left, flowerRectToBeDrawn.bottom);

                switch (snakeH[snakeLength-1]){
                    case 0://up
                        rotatedTailBitmap = Bitmap.createBitmap(rotatedTailBitmap , 0, 0, rotatedTailBitmap .getWidth(), rotatedTailBitmap .getHeight(), matrix270, true);
                        break;
                    case 1://right
                        //no rotation necessary

                        break;
                    case 2://down
                        rotatedTailBitmap = Bitmap.createBitmap(rotatedTailBitmap , 0, 0, rotatedTailBitmap .getWidth(), rotatedTailBitmap .getHeight(), matrix90, true);
                        break;

                    case 3://left
                        rotatedTailBitmap = Bitmap.createBitmap(rotatedTailBitmap , 0, 0, rotatedTailBitmap .getWidth(), rotatedTailBitmap .getHeight(), matrix180, true);
                        break;


                }

                mCanvas.drawBitmap(rotatedTailBitmap, snakeX[snakeLength-1]*blockSize, (snakeY[snakeLength-1]*blockSize)+topGap, mPaint);

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

            //control the flower animation
            flowerAnimTimer++;
            //change the frame every 6 game frames
            if(flowerAnimTimer == 6){
                //which frame should we draw
                if(flowerFrameNumber == 1){
                    flowerFrameNumber = 0;
                }else{
                    flowerFrameNumber =1;
                }

                flowerRectToBeDrawn = new Rect((flowerFrameNumber * frameWidth), 0,
                        (flowerFrameNumber * frameWidth +frameWidth)-1, frameHeight);

                flowerAnimTimer = 0;
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

//            switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                case MotionEvent.ACTION_UP:
//                    if(event.getX() >= screenWidth/2) {
//                        //turn right
//                        directionOfTravel++;
//
//                        if(directionOfTravel == 4) {
//                            //no such direction, loop back to 0
//                            directionOfTravel = 0;
//                        }
//                    } else { //turn left
//                        directionOfTravel--;
//                        if(directionOfTravel == -1) {
//                            //no such direction, loop back to 3
//                            directionOfTravel = 3;
//                        }
//                    }
//            }

            return mDetectorCompat.onTouchEvent(event);
        }

        GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener =
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                        switch (getSlope(event1.getX(), event1.getY(), event2.getX(), event2.getY())) {
                            case 1:
                                Log.d("Fling", "up");
                                if(directionOfTravel != 2) directionOfTravel = 0;
                                return true;
                            case 2:
                                Log.d("Fling", "left");
                                if(directionOfTravel != 1) directionOfTravel = 3;
                                return true;
                            case 3:
                                Log.d("Fling", "down");
                                if(directionOfTravel != 0) directionOfTravel = 2;
                                return true;
                            case 4:
                                Log.d("Fling", "right");
                                if(directionOfTravel != 3) directionOfTravel = 1;
                                return true;
                        }
                        return false;
                    }

                    private int getSlope(float x1, float y1, float x2, float y2) {
                        Double angle = Math.toDegrees(Math.atan2(y1 - y2, x2 - x1));
                        if (angle > 45 && angle <= 135)
                            // top
                            return 1;
                        if (angle >= 135 && angle < 180 || angle < -135 && angle > -180)
                            // left
                            return 2;
                        if (angle < -45 && angle>= -135)
                            // down
                            return 3;
                        if (angle > -45 && angle <= 45)
                            // right
                            return 4;
                        return 0;
                    }
                };
    }

}
