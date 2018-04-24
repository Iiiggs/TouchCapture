package com.microvision.apps.touchcapture;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

public class CaptureController implements PointCloudAdapter.PointCloudDelegate {


    public interface CaptureControllerUIDelegate {
        void recordingStateUpdated(CaptureState.RecordingState s);
        void moveSpriteToNewLocation(final Location location, final int progress);
    }

    CaptureControllerUIDelegate mUIDelegate;
    CaptureState mCaptureState = new CaptureState();
    LocationIterator mLocationIterator = new LocationIterator(Constants.TILES_WIDTH, Constants.TILES_HEIGHT);
    RingBuffer mFrameBuffer = new RingBuffer(Constants.BUFFER_SIZE);
    PointCloudAdapter mCameraDelegate = new PointCloudAdapter(this);

    public CaptureController(CaptureControllerUIDelegate delegate){
        mUIDelegate = delegate;
    }

    private String getFilename(){
        @SuppressLint("DefaultLocale") String filename = "/tof/depth+amp";
        if (mCaptureState.touchdown) {
            filename = String.format("%s-at_%d-touch_1-x_%d-y_%d.dat", filename, mCaptureState.currentTime, mCaptureState.x, mCaptureState.y);
        } else {
            filename = String.format("%s-at_%d-touch_0-x_None-y_None.dat", filename, mCaptureState.currentTime);
        }

        return filename;
    }


    void writeFrameToFile(byte[] frame, String filename){
        File outputFile  = new File(Constants.STORAGE_FOLDER, filename);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(frame);
        } catch (IOException e){
            Log.e("CameraDelegate", "Can't write file");
        }
    }

    void putFrameInBuffer(byte [] frame){
        FrameWithMetadata payload = new FrameWithMetadata(frame, getFilename());

        if(mFrameBuffer.put(payload)){
            Log.d("CameraDelegate", "Put some frame in the buffer");
        }
        else {
            Log.d("CameraDelegate", "Not able to put some frames in buffer");
        }

        if(mCaptureState.touchdown){
            if(++mCaptureState.framesSinceTouchdown == Constants.FRAMES_PER_TOUCH){
                // now we want to record last Constants.FRAMES_PER_TOUCH*2 frames
                archiveFramesFromBuffer(Constants.FRAMES_PER_TOUCH*2);
                mCaptureState.framesSinceTouchdown = 0;
            }
        }
    }

    void archiveFramesFromBuffer(int N){
        if(mFrameBuffer.elements.length < N){
            Log.d("CameraDelegate", "Not enough frames to save");
            return;
        }

        for(int i = 0; i < N; i++){
            FrameWithMetadata payload = mFrameBuffer.take();
            if(payload != null){
                writeFrameToFile(payload.frame, payload.metadata);
            } else {
                Log.d("CameraDelegate", "Got null back when expecting a frame from a full buffer");
            }

        }

        // throw away anything that's left
        mFrameBuffer.reset();

        // now move to next position
    }

    void processTouchEvent(MotionEvent event){
        // toggle things
        boolean down;
        if(event.getAction() == ACTION_DOWN || event.getAction() == ACTION_MOVE){
            down = true;
        } else if(event.getAction() == ACTION_UP) {
            down = false;
        } else {
            Log.e("TouchDelegate", String.format("Don't know how to work with touch action %d", event.getAction()));
            return;
        }

        setTouch(down, Math.round(event.getX()), Math.round(event.getY()));

        moveSpriteToNextPosition();
    }

    void toggleRecording(){
        mCaptureState.recording = !mCaptureState.recording;

        mUIDelegate.recordingStateUpdated(mCaptureState.recording ? CaptureState.RecordingState.RECORDING : CaptureState.RecordingState.STOPPED);
    }

    void cleanup(){
        this.mCameraDelegate.cleanup();
    }

    // todo: when to call this?
    boolean moveSpriteToNextPosition(){
        final Location nextLocation = mLocationIterator.nextLocation();

        if (nextLocation == null || !mCaptureState.recording) return false;

        Log.d("NextPosition", nextLocation.toString());

        this.mUIDelegate.moveSpriteToNewLocation(nextLocation, mLocationIterator.getProgress());

        return true;
    }

    public void setTouch(boolean down, int x, int y) {
        mCaptureState.touchdown = down;
        mCaptureState.x = x;
        mCaptureState.y = y;
    }

    @Override
    public void processFrame(byte [] frame){
        recordTimeFrameReceived();

        if(!mCaptureState.recording){
            return;
        }

        putFrameInBuffer(frame);
    }

    private void recordTimeFrameReceived(){
        if (mCaptureState.startTime == 0) {
            mCaptureState.startTime = System.currentTimeMillis();
        }

        mCaptureState.currentTime = System.currentTimeMillis();

        // calculate fps
        mCaptureState.framesCount += 1;
        mCaptureState.fps = mCaptureState.framesCount / ((mCaptureState.currentTime - mCaptureState.startTime) / 1000.0);
    }

}
