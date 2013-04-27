package com.github.matt.williams.argolf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class SwingActivity extends Activity implements SensorEventListener {
    public String EXTRA_POWER = "POWER";
    public String EXTRA_SLICE = "SLICE";
    public String EXTRA_TOP = "TOP";

    private boolean mLeftThumbPressed;
    private boolean mRightThumbPressed;
    private TextView mPromptTextView;
    private State mState = State.INITIAL;
    private float mPower;
    private float mSlice;
    private float mTop;
    private SensorManager mSensorManager;
    private Sensor mGravitySensor;
    private Vibrator mVibrator;
    private final Handler mHandler = new Handler();
    private final static long PRE_STEADY_DELAY_MILLISECONDS = 100;
    private final static long STEADY_DELAY_MILLISECONDS = 700;
    private boolean mPositioned;
    private final Runnable mPreSteadyRunnable = new Runnable() {
        @Override
        public void run() {
            updateState(true, false, false, false);
        }
    };
    private final Runnable mSteadyRunnable = new Runnable() {
        @Override
        public void run() {
            updateState(false, true, false, false);
        }
    };
    private SoundPool mSoundPool;
    private int mGolfSound;
    private float mLastX;

    private enum State {
        INITIAL(R.string.swing_prompt_initial),
        POSITION(R.string.swing_prompt_position),
        PRE_STEADY(R.string.swing_prompt_position),
        STEADY(R.string.swing_prompt_steady),
        BACKSWING(R.string.swing_prompt_swing),
        SWING(R.string.swing_prompt_swing),
        RELEASE(R.string.swing_prompt_release);

        private final int mPromptId;
        private State(int promptId) {
            mPromptId = promptId;
        }

        private int getPromptId() {
            return mPromptId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swing);

        mPromptTextView = (TextView)findViewById(R.id.promptTextView);

        ((ImageView)findViewById(R.id.leftThumbImageView)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mLeftThumbPressed = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mLeftThumbPressed = false;
                }
                updateState(false, false, false, false);
                return true;
            }
        });

        ((ImageView)findViewById(R.id.rightThumbImageView)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mRightThumbPressed = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mRightThumbPressed = false;
                }
                updateState(false, false, false, false);
                return true;
            }
        });

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mGolfSound = mSoundPool.load(this, R.raw.golf, 1);

        mPromptTextView.setText(getResources().getString(mState.getPromptId()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private void updateState(boolean preSteadied, boolean steadied, boolean backswung, boolean hit) {
        State oldState = mState;
        if (mState == State.INITIAL) {
            if ((mLeftThumbPressed) &&
                (mRightThumbPressed)) {
                mState = State.POSITION;
            }
        } else if (mState == State.POSITION) {
            if (mPositioned) {
                mState = State.PRE_STEADY;
            } else if ((!mLeftThumbPressed) ||
                       (!mRightThumbPressed)) {
                mState = State.INITIAL;
            }
        } else if (mState == State.PRE_STEADY) {
            if (preSteadied) {
                mState = State.STEADY;
            } else if (!mPositioned) {
                mState = State.POSITION;
            } else if ((!mLeftThumbPressed) ||
                       (!mRightThumbPressed)) {
                mState = State.INITIAL;
            }
        } else if (mState == State.STEADY) {
            if (steadied) {
                mState = State.BACKSWING;
            } else if (!mPositioned) {
                mState = State.POSITION;
            } else if ((!mLeftThumbPressed) ||
                       (!mRightThumbPressed)) {
                mState = State.INITIAL;
            }
        } else if (mState == State.BACKSWING) {
            if (hit) {
                mState = State.RELEASE;
            } else if ((!mLeftThumbPressed) ||
                       (!mRightThumbPressed)) {
                mState = State.INITIAL;
            }
        } else if (mState == State.SWING) {
            if (hit) {
                mState = State.RELEASE;
            } else if ((!mLeftThumbPressed) ||
                       (!mRightThumbPressed)) {
                mState = State.INITIAL;
            }
        } else if (mState == State.RELEASE) {
            if ((!mLeftThumbPressed) &&
                (!mRightThumbPressed)) {
                Intent intent = getIntent();
                intent.putExtra(EXTRA_POWER, mPower);
                intent.putExtra(EXTRA_SLICE, mSlice);
                intent.putExtra(EXTRA_TOP, mTop);
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        if (mState != oldState) {
            if (mState == State.PRE_STEADY) {
                mHandler.postDelayed(mPreSteadyRunnable, PRE_STEADY_DELAY_MILLISECONDS);
            } else if (oldState == State.PRE_STEADY) {
                mHandler.removeCallbacks(mPreSteadyRunnable);
            }
            if (mState == State.STEADY) {
                mHandler.postDelayed(mSteadyRunnable, STEADY_DELAY_MILLISECONDS);
            } else if (oldState == State.STEADY) {
                mHandler.removeCallbacks(mSteadyRunnable);
            }
            if (mState == State.RELEASE) {
                mVibrator.vibrate(100);
                mSoundPool.play(mGolfSound, 1.0f, 1.0f, 0, 0, 1.0f);
            }

            mPromptTextView.setText(getResources().getString(mState.getPromptId()));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float x = event.values[0];
        final float y = event.values[1];
        final float z = event.values[2];
        final float a = (float)Math.sqrt(x * x + y * y + z * z);
        mPositioned = ((a > 0.9 * SensorManager.GRAVITY_EARTH) &&
                       (a < 1.1 * SensorManager.GRAVITY_EARTH) &&
                       (Math.abs(x) < 0.1 * SensorManager.GRAVITY_EARTH) &&
                       (y < 0) &&
                       (Math.abs(y) > Math.abs(z)));
        boolean backswung = (Math.abs(x) > 0.1 * SensorManager.GRAVITY_EARTH);
        boolean hit = (Math.signum(x) != Math.signum(mLastX));
        mLastX = x;

        updateState(false, false, backswung, hit);
    }
}
