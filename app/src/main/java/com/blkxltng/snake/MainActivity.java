package com.blkxltng.snake;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.games.Games;

public class MainActivity extends BaseGameActivity implements View.OnClickListener {

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

    //For the High Score
    SharedPreferences mSharedPreferences;
    String dataName = "MyData";
    String intName = "MyScore";
    int defaultInt = 0;
    public static int highScore;

    //Start the game
    Intent mIntent;

    //Other buttons
    Button buttonLeader;
    Button buttonAchievements;
    com.google.android.gms.common.SignInButton buttonSignIn;
    Button buttonSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get screen dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        headAnimBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.animsnake);

        mSnakeAnimView = new SnakeAnimView(this);

        setContentView(mSnakeAnimView);

        mSharedPreferences = getSharedPreferences(dataName, MODE_PRIVATE);
        highScore = mSharedPreferences.getInt(intName, defaultInt);

        mIntent = new Intent(this, GameActivity.class);

        //Load the UI on top of the SnakeAnimView
        LayoutInflater mInflater = LayoutInflater.from(this);
        View hudView = mInflater.inflate(R.layout.activity_main, null);

        this.addContentView(hudView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        //Game Service Buttons
        buttonSignIn = (com.google.android.gms.common.SignInButton) findViewById(R.id.buttonSignIn);
        buttonSignIn.setOnClickListener(this);
        buttonSignOut = (Button) findViewById(R.id.buttonSignOut);
        buttonSignOut.setOnClickListener(this);
        buttonAchievements = (Button) findViewById(R.id.buttonAchievements);
        buttonAchievements.setOnClickListener(this);
        buttonLeader = (Button) findViewById(R.id.buttonLeader);
        buttonLeader.setOnClickListener(this);
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

    /**
     * Called when sign-in fails. As a result, a "Sign-In" button can be
     * shown to the user; when that button is clicked, call
     *
     * @link{GamesHelper#beginUserInitiatedSignIn . Note that not all calls
     * to this method mean an
     * error; it may be a result
     * of the fact that automatic
     * sign-in could not proceed
     * because user interaction
     * was required (consent
     * dialogs). So
     * implementations of this
     * method should NOT display
     * an error message unless a
     * call to @link{GamesHelper#
     * hasSignInError} indicates
     * that an error indeed
     * occurred.
     */
    @Override
    public void onSignInFailed() {
        //Show sign-in button
        buttonSignIn.setVisibility(View.VISIBLE);
        buttonSignOut.setVisibility(View.GONE);
    }

    /**
     * Called when sign-in succeeds.
     */
    @Override
    public void onSignInSucceeded() {
        //Show sign-out button, hide sign-in button
        buttonSignIn.setVisibility(View.GONE);
        buttonSignOut.setVisibility(View.VISIBLE);
        buttonLeader.setVisibility(View.VISIBLE);
        buttonAchievements.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.buttonSignIn:
                beginUserInitiatedSignIn();
                break;

            case R.id.buttonSignOut:
                signOut();
                buttonSignIn.setVisibility(View.VISIBLE);
                buttonSignOut.setVisibility(View.GONE);
                buttonLeader.setVisibility(View.GONE);
                buttonAchievements.setVisibility(View.GONE);
                break;

            case R.id.buttonAchievements:
                startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), 0);
                break;

            case R.id.buttonLeader:
                startActivityForResult(Games.Leaderboards
                        .getLeaderboardIntent(getApiClient(), getResources().getString(R.string.leaderboard_snake)),0);
                break;
        }
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
                mCanvas.drawColor(Color.argb(255, 145, 227, 151)); //Background color
                mPaint.setColor(Color.argb(255, 255, 255, 255));
                mPaint.setTextSize(150);
                mCanvas.drawText("Snake!", 10, 150, mPaint);
                mPaint.setTextSize(25);
                mCanvas.drawText(" High Score: " + highScore, 10, screenHeight-50, mPaint);

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
