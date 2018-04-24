package com.microvision.apps.touchcapture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static java.lang.System.out;

// todo:
//      unit test?
//      refactor for testability/debugging
//      state transitions
//      - move sprite after touch captured



/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements CaptureController.CaptureControllerUIDelegate {

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    View mContentView;
    ImageView mCircleView;
    ProgressBar mProgressView;
    Button mRecordButton;

    CaptureController mCaptureController = new CaptureController(this);

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreenContent);
        mRecordButton = findViewById(R.id.record_button);
        mCircleView = this.findViewById(R.id.circle);
        mProgressView  = this.findViewById(R.id.determinateBar);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                toggle();
                return true;
            }
        });

        mContentView.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event){
                Log.d("Touch", String.format("%s", event.getAction()));

                mCaptureController.processTouchEvent(event);

                return false;
            }
        });

        mRecordButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.e("mRecordButton", "OnClick Event");
                mCaptureController.toggleRecording();
            }
        });


        // attach a click listener
        this.mCircleView.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                // pass for now, but we may want to record this in CaptureController later
            }
        });
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {

        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.mCaptureController.cleanup();
    }


    @Override
    public void recordingStateUpdated(CaptureState.RecordingState s) {
        switch (s){
            case RECORDING:
                this.mRecordButton.setText("Stop");
                break;
            case STOPPED:
                this.mRecordButton.setText("Record");
                break;
        }
    }

    @Override
    public void moveSpriteToNewLocation(final Location location, final int progress) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              // todo: use ObjectAnimator
                              ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mCircleView.getLayoutParams();

                              final int Xs = p.leftMargin;
                              final int Ys = p.topMargin;
                              final int Xf = location.getSpritePosition().x;
                              final int Yf = location.getSpritePosition().y;

                              Animation a = new Animation() {
                                  @Override
                                  protected void applyTransformation(float interpolatedTime, Transformation t) {
                                      Log.d("ApplyTranformation", String.format("%f", interpolatedTime));
                                      float Xt = Xs + (Xf - Xs) * interpolatedTime;
                                      float Yt = Ys + (Yf - Ys) * interpolatedTime;

                                      mProgressView.setProgress(progress, true);

                                      ViewGroup.MarginLayoutParams Pt = (ViewGroup.MarginLayoutParams) mCircleView.getLayoutParams();
                                      Pt.leftMargin = Math.round(Xt);
                                      Pt.topMargin = Math.round(Yt);
                                      mCircleView.setLayoutParams(Pt);
                                  }
                              };

                              a.setDuration(250);
                              mCircleView.startAnimation(a);
                              Log.d("FullscreenActivity", "Starting view animation");
                          }
                      }
        );
    }
}