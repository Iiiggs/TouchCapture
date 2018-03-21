package com.microvision.apps.touchcapture;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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

    boolean touchdown = false;
    int x = -1;
    int y = -1;

    public void setTouch(boolean down, int x, int y){
        touchdown = down;
        this.x = x;
        this.y = y;
    }

    CameraDelegate(File externalStorageDirectory){
        this.externalStorageDirectory = externalStorageDirectory;

        // todo: ask for camera permission
        mCamera = Camera.open();
        mCamera.setPreviewCallback(this);
        Camera.Parameters parameters=mCamera.getParameters();
        List<Camera.Size> sizes= parameters.getSupportedPreviewSizes();
        int previewSizeWidth = sizes.get(0).width;
        int previewSizeHeight =  sizes.get(0).height;
        //List<Integer> formats = parameters.getSupportedPreviewFormats();
        int imageFormat = ImageFormat.YUY2;

        parameters.setPreviewSize(previewSizeWidth, previewSizeHeight);
        parameters.setPreviewFormat(imageFormat);
        mCamera.setParameters(parameters);
        mCamera.startPreview();

    }

    void cleanup (){
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;

    }

    int [][] depthArray = new int [720][120];
    int [][] amplitudeArray = new int [720][120];

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getDepthEncoded() {
        int [][] depthArrayCopy = this.depthArray.clone();

        return twoDimensionalArrayToJson(depthArrayCopy);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getAmplitudeEncoded() {
        int [][] amplitudeArrayCopy = this.amplitudeArray.clone();

        return twoDimensionalArrayToJson(amplitudeArrayCopy);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String twoDimensionalArrayToJson(int[][] array) {
        JSONArray rows = new JSONArray();
        for(int i = 0; i < array.length; i++){

            try {
                JSONArray row = new JSONArray(array[i]);
                rows.put(row);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return rows.toString();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onPreviewFrame(byte[] frame, Camera var2){
        byte[] frameCopy = frame.clone();

        if(this.startTime == 0){
            this.startTime = System.currentTimeMillis();
        }

        // chop up into 120x720x4 then combine high and low bytes
//        int k = 0;
//        for(int i = 0; i < 720; i++){
//            for(int j=0; j < 120; j++){
//                byte d0 = frameCopy[k++];
//                byte d1 = frameCopy[k++];
//                byte a0 = frameCopy[k++];
//                byte a1 = frameCopy[k++];
//
//                int amplitude = ((a1 & 0xff) << 8) | (a0 & 0xff);
//                int depth = ((d1 & 0xff) << 8) | (d0 & 0xff);
//
//                depthArray[i][j] = depth;
//                amplitudeArray[i][j] = amplitude;
//
//            }
//        }
        // showel depth and amplitude into file

        this.currentTime = System.currentTimeMillis();

        // todo: for now, encode touch up/down and x/y into file anme
        // todo: later, add a file header
        @SuppressLint("DefaultLocale") String filename = "";
        if(this.touchdown){
             filename = String.format("/tof/depth+amp-at_%d-touch_1-x_%d-y_%d.data", this.currentTime, this.x, this.y);
        } else {
            filename = String.format("/tof/depth+amp-at_%d-touch_0-x_None-y_None.data", this.currentTime);
        }

        File outputFile  = new File(this.externalStorageDirectory, filename);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(frame);
        } catch (IOException e){
            Log.e("CameraDelegate", "Can't write file");
        }

        // calculate fps
        this.framesCount += 1;

        this.fps = this.framesCount / ((this.currentTime - this.startTime) / 1000.0);
        Log.i("FPS", String.format("%f - %s", this.fps, touchdown ? "Down" : "Up"));

//        if(bmp != null){
//            Log.d("CameraDelegate", "Got a bitmap with size: (" + bmp.getWidth() + ", " + bmp.getHeight() + ")" );
//        }
    }

}
