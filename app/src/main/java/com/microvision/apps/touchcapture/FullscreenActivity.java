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

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

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
    private View mContentView;

    boolean down = false;

    ImageView circle;
    ProgressBar progress;

    LocationIterator locationIterator = new LocationIterator(Constants.TILES_WIDTH, Constants.TILES_HEIGHT);


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

    CameraDelegate mCameraDelegate = null;



    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                Log.d("FullscreenActivity", "Toggle something");

                mCameraDelegate.recording = !mCameraDelegate.recording;
                if(mCameraDelegate.recording){
                    ((Button)view).setText("Stop");
                } else {
                    ((Button)view).setText("Record");
                }
            }

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

                if(event.getAction() == ACTION_DOWN || event.getAction() == ACTION_MOVE){
                    down = true;
                } else if(event.getAction() == ACTION_UP) {
                    down = false;
                } else {
                    Log.e("TouchDelegate", String.format("Don't know how to work with touch action %d", event.getAction()));
                    return false;
                }

                mCameraDelegate.setTouch(down, Math.round(event.getX()), Math.round(event.getY()));

                return false;
            }
        });

        this.mCameraDelegate = new CameraDelegate(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);


        this.circle = this.findViewById(R.id.circle);
        this.progress = this.findViewById(R.id.determinateBar);

        // attach a click listener
        this.circle.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                // move to next location
                moveSpriteToNextPosition();
            }
        });
    }

    private void testSprite() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                while(moveSpriteToNextPosition()){
                    try {
                        Thread.sleep(350);
                    } catch (InterruptedException e){
                        Log.e("FullscreenActivity", e.getLocalizedMessage());
                    }
                };
            }
        });
    }

    boolean moveSpriteToNextPosition(){
        final Location nextLocation = locationIterator.nextLocation();

        if (nextLocation == null || !mCameraDelegate.recording) return false;

        Log.d("NextPosition", nextLocation.toString());

        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {

                              // todo: use ObjectAnimator
                              ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) circle.getLayoutParams();

                              final int Xs = p.leftMargin;
                              final int Ys = p.topMargin;
                              final int Xf = nextLocation.getSpritePosition().x;
                              final int Yf = nextLocation.getSpritePosition().y;

                              Animation a = new Animation() {
                                  @Override
                                  protected void applyTransformation(float interpolatedTime, Transformation t) {
                                      Log.d("ApplyTranformation", String.format("%f", interpolatedTime));
                                      float Xt = Xs + (Xf - Xs) * interpolatedTime;
                                      float Yt = Ys + (Yf - Ys) * interpolatedTime;

                                      progress.setProgress(locationIterator.getProgress(), true);

                                      ViewGroup.MarginLayoutParams Pt = (ViewGroup.MarginLayoutParams) circle.getLayoutParams();
                                      Pt.leftMargin = Math.round(Xt);
                                      Pt.topMargin = Math.round(Yt);
                                      circle.setLayoutParams(Pt);
                                  }
                              };

                              a.setDuration(250);
                              circle.startAnimation(a);
                              Log.d("FullscreenActivity", "Starting view animation");
                          }
                      }
        );

        return true;
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
//            testSprite();
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

        this.mCameraDelegate.cleanup();
    }
}

