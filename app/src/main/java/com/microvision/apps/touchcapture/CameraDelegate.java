package com.microvision.apps.touchcapture;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Time of Flight camera capture
 */

public class CameraDelegate implements Camera.PreviewCallback {
    ByteBuffer buffer;
    Camera mCamera = null;

    long framesCount = 0;
    long startTime = 0;
    long currentTime = 0;
    double fps = 0.0;

    File externalStorageDirectory;

    boolean recording = false;
    boolean touchdown = false;
    int x = -1;
    int y = -1;


    int framesSinceTouchdown = 0;

//    RingBuffer recordingFrameBuffer = new RingBuffer(Constants.BUFFER_SIZE);
//    RingBuffer archivingFrameBuffer = new RingBuffer(Constants.BUFFER_SIZE);
    RingBuffer frameBuffer = new RingBuffer(Constants.BUFFER_SIZE);


    public void setTouch(boolean down, int x, int y) {
        touchdown = down;
        this.x = x;
        this.y = y;

    }

//    public void swapBuffersAndArchive(){
//        // recordingFrameBuffer is ready to be archived
//        // archivingFrameBuffer is ready to start recording
//        RingBuffer temp = this.recordingFrameBuffer;
//        this.recordingFrameBuffer = this.archivingFrameBuffer;
//        this.archivingFrameBuffer = temp;
//        archiveFramesFromBuffer(Constants.FRAMES_PER_TOUCH * 2);
//    }

    CameraDelegate(File externalStorageDirectory) {
        this.externalStorageDirectory = externalStorageDirectory;

        // todo: ask for camera permission
        mCamera = Camera.open();
        if(mCamera != null) {
            mCamera.setPreviewCallback(this);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            int previewSizeWidth = sizes.get(0).width;
            int previewSizeHeight = sizes.get(0).height;
            //List<Integer> formats = parameters.getSupportedPreviewFormats();
            int imageFormat = ImageFormat.YUY2;

            parameters.setPreviewSize(previewSizeWidth, previewSizeHeight);
            parameters.setPreviewFormat(imageFormat);
            mCamera.setParameters(parameters);


           if (Constants.CAPTURE_TOF) {
//               recording = true;
               mCamera.startPreview();
            }
        } else {
            Log.d("CameraDelegate", "Camera not opened correctly");
        }
    }

    void cleanup() {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;

    }

    int[] depthArray = new int[720*120];
    int[] amplitudeArray = new int[720*120];


//    @SuppressLint("DefaultLocale")


    @Override
    public void onPreviewFrame(byte[] frame, Camera camera) {
        if(!this.recording){
            return;
        }

        // record N frames before and after touch
        // circular buffer of last N frames
        // if touch started, record next M frames to file? and N frames before?
        // need separate queue for writing to file - perhaps first fill up N*2 buffer then write?

        recordTimeFrameReceived();
        putFrameInBuffer(frame);
    }

    private String getFilename(){
        @SuppressLint("DefaultLocale") String filename = "/tof/depth+amp";
        if (this.touchdown) {
            filename = String.format("%s-at_%d-touch_1-x_%d-y_%d.dat", filename, this.currentTime, this.x, this.y);
        } else {
            filename = String.format("%s-at_%d-touch_0-x_None-y_None.dat", filename, this.currentTime);
        }

        return filename;
    }

    private void recordTimeFrameReceived(){
        if (this.startTime == 0) {
            this.startTime = System.currentTimeMillis();
        }

        this.currentTime = System.currentTimeMillis();

        // calculate fps
        this.framesCount += 1;
        this.fps = this.framesCount / ((this.currentTime - this.startTime) / 1000.0);
        Log.i("FPS", String.format("%f - %s", this.fps, touchdown ? "Down" : "Up"));

    }

    void writeFrameToFile(byte[] frame, String filename){
        File outputFile  = new File(this.externalStorageDirectory, filename);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(frame);
        } catch (IOException e){
            Log.e("CameraDelegate", "Can't write file");
        }
    }

    void putFrameInBuffer(byte [] frame){
        FrameWithMetadata payload = new FrameWithMetadata(frame, getFilename());

        if(frameBuffer.put(payload)){
            Log.d("CameraDelegate", "Put some frame in the buffer");
        }
        else {
            Log.d("CameraDelegate", "Not able to put some frames in buffer");
        }

        if(touchdown){
            if(++framesSinceTouchdown == Constants.FRAMES_PER_TOUCH){
                // now we want to record last Constants.FRAMES_PER_TOUCH*2 frames
                archiveFramesFromBuffer(Constants.FRAMES_PER_TOUCH*2);
                this.framesSinceTouchdown = 0;
            }
        }
    }

    void archiveFramesFromBuffer(int N){
        if(frameBuffer.elements.length < N){
            Log.d("CameraDelegate", "Not enough frames to save");
            return;
        }

        for(int i = 0; i < N; i++){
            FrameWithMetadata payload = frameBuffer.take();
            if(payload != null){
                writeFrameToFile(payload.frame, payload.metadata);
            } else {
                Log.d("CameraDelegate", "Got null back when expecting a frame from a full buffer");
            }

        }

        // throw away anything that's left
        frameBuffer.reset();

        // now move to next position

    }
}
