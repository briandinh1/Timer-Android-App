package com.brian.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // used to reinstantiate program after orientation change / termination
    private static final String SHARED_PREFS = "timer_shared_preferences";
    private static final String START_TIME = "timer_start_time";
    private static final String END_TIME = "timer_end_time";
    private static final String TIME_REMAINING = "timer_time_remaining";
    private static final String TIMER_ACTIVE = "timer_timer_active";
    private static final String TIMER_PAUSED = "timer_timer_paused";
    private static final long NO_TIME_SET = -1;

    private long mStartTime;
    private long mEndTime; // prevent orientation changes from lagging
    private long mTimeRemaining; // in milliseconds
    private boolean mTimerActive;
    private boolean mTimerPaused;
    private TextView mTextViewCountDown;
    private EditText mEditTextInput;
    private Button mSetButton;
    private Button mStartButton;
    private Button mPauseButton;
    private Button mResetButton;
    private CountDownTimer mCountDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEndTime = NO_TIME_SET;
        mTimeRemaining = NO_TIME_SET;
        mTextViewCountDown = findViewById(R.id.text_view_timer);
        mEditTextInput = findViewById(R.id.edit_text_input);
        mSetButton = findViewById(R.id.set_button);
        mStartButton = findViewById(R.id.start_button);
        mPauseButton = findViewById(R.id.pause_resume_button);
        mResetButton = findViewById(R.id.reset_button);

        mSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = mEditTextInput.getText().toString(); // user inputs in minutes
                if (input.length() == 0) {
                    Toast.makeText(MainActivity.this, "Time cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (int i = 0; i < input.length(); i++) {
                    if (!Character.isDigit(input.charAt(i))) {
                        Toast.makeText(MainActivity.this, "Invalid format", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                long time = Long.parseLong(input) * 60000; // multiply by 60k to turn mins into millisec
                if (time == 0) {
                    Toast.makeText(MainActivity.this, "Time cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                setTimer(time);
            }
        });

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerActive) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });
    }

    private void setTimer(long milliseconds) {
        mStartTime = Math.min(milliseconds, 360000000); // hardcoded for 100 hours
        mTimeRemaining = mStartTime;
        updateCountdown();
        closeKeyboard();
    }

    private void startTimer() {
        // save where the time should actually end
        // used to restore time more accurately after an orientation change
        mEndTime = System.currentTimeMillis() + mTimeRemaining;

        if (mTimeRemaining == NO_TIME_SET) {
            Toast.makeText(MainActivity.this, "Input a time first", Toast.LENGTH_SHORT).show();
            return;
        }

        // count down every 1 second
        mCountDownTimer = new CountDownTimer(mTimeRemaining, 1000) {
            @Override
            public void onTick(long l) {
                mTimeRemaining = l;
                updateCountdown();
            }

            @Override
            public void onFinish() {
                mTimerActive = false;
                mTimerPaused = false;
            }
        }.start();

        mTimerActive = true;
        mTimerPaused = false;
        updateLayout();
    }

    private void pauseTimer() {
        if (mCountDownTimer != null)
            mCountDownTimer.cancel();
        mTimerActive = false;
        mTimerPaused = true;
        updateLayout();
    }

    private void resetTimer() {
        if (mCountDownTimer != null)
            mCountDownTimer.cancel();
        mTimerActive = false;
        mTimerPaused = false;
        mTimeRemaining = NO_TIME_SET;
        updateCountdown();
        resetLayout();
    }

    private void resetLayout() {
        mEditTextInput.setVisibility(View.VISIBLE);
        mEditTextInput.getText().clear();
        mSetButton.setVisibility(View.VISIBLE);
        mStartButton.setVisibility(View.VISIBLE);
        mPauseButton.setVisibility(View.INVISIBLE);
        mResetButton.setVisibility(View.INVISIBLE);
    }

    private void updateLayout() {
        mEditTextInput.setVisibility(View.INVISIBLE);
        mSetButton.setVisibility(View.INVISIBLE);
        mStartButton.setVisibility(View.INVISIBLE);
        mPauseButton.setVisibility(View.VISIBLE);
        mResetButton.setVisibility(View.VISIBLE);
        if (mTimerActive)
            mPauseButton.setText("pause");
        else if (mTimerPaused)
            mPauseButton.setText("resume");
    }

    private void updateCountdown() {
        // calculate from millisecs then format as string for textview
        int hours = (int) mTimeRemaining / 1000 / 3600; // any hours > 99 get capped out
        int minutes = (int) mTimeRemaining / 1000 % 3600 / 60;
        int seconds = (int) mTimeRemaining / 1000 % 60;
        String time;
        if (hours > 0)
            time = String.format(Locale.getDefault(),"%02d:%02d:%02d", hours, minutes, seconds);
        else
            time = String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds);
        mTextViewCountDown.setText(time);
        if (hours == 0 && minutes == 0 && seconds == 0) {
            mPauseButton.setVisibility(View.INVISIBLE);
        }
    }

    // so the user doesn't have to manually close (annoying!!) after setting the time
    private void closeKeyboard() {
        View view = this.getCurrentFocus(); // get the edit text
        if (view != null) {
            InputMethodManager input =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            input.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // orientation changes and exits out of program destroy the current instance
    // save it to shared preferences, then restore state so the app looks like it never stopped
    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(START_TIME, mStartTime);
        editor.putLong(END_TIME, mEndTime);
        editor.putLong(TIME_REMAINING, mTimeRemaining);
        editor.putBoolean(TIMER_ACTIVE, mTimerActive);
        editor.putBoolean(TIMER_PAUSED, mTimerPaused);
        editor.apply();
        if (mCountDownTimer != null)
            mCountDownTimer.cancel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        mStartTime = sharedPreferences.getLong(START_TIME, 0);
        mTimeRemaining = sharedPreferences.getLong(TIME_REMAINING, NO_TIME_SET);
        mTimerActive = sharedPreferences.getBoolean(TIMER_ACTIVE, false);
        mTimerPaused = sharedPreferences.getBoolean(TIMER_PAUSED, false);

        if (mTimerActive) {
            mEndTime = sharedPreferences.getLong(END_TIME, 0);
            mTimeRemaining = mEndTime - System.currentTimeMillis();
            // check if time has run out when program is reopened
            if (mTimeRemaining < 0) {
                mTimeRemaining = 0;
                mTimerActive = false;
            }
            else
                startTimer();
        }

        if (!mTimerActive && !mTimerPaused) // first instance of a program, or after a reset
            resetLayout();
        else // timer was already running or paused before
            updateLayout();
        updateCountdown();
    }
}
