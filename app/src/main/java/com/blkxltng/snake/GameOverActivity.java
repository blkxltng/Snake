package com.blkxltng.snake;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GameOverActivity extends AppCompatActivity implements View.OnClickListener {

    //For the High Score
    SharedPreferences mSharedPreferences;
    String dataName = "MyData";
    String intName = "MyScore";
    int defaultInt = 0;
    int highScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        TextView textGameOver = (TextView) findViewById(R.id.textGameOver);
        TextView textScore = (TextView) findViewById(R.id.textScore);
        TextView textHighScore = (TextView) findViewById(R.id.textHighScore);


        Typeface typeGameboy = Typeface.createFromAsset(getAssets(),"fonts/gameboy.ttf");
        textGameOver.setTypeface(typeGameboy);

        int score = getIntent().getIntExtra("Score", 0);
        textScore.setText("Your Score: " + score);

        mSharedPreferences = getSharedPreferences(dataName, MODE_PRIVATE);
        highScore = mSharedPreferences.getInt(intName, defaultInt);

        textHighScore.setText("High Score: " + highScore);

        Button buttonPlayAgain = (Button) findViewById(R.id.buttonPlayAgain);
        buttonPlayAgain.setOnClickListener(this);
        buttonPlayAgain.setTypeface(typeGameboy);


    }

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.buttonPlayAgain) {
            Intent i = new Intent(getApplicationContext(), GameActivity.class);
            startActivity(i);
            finish();
        }
    }
}
