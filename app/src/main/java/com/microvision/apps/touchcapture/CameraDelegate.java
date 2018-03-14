package com.microvision.apps.touchcapture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Base64;

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
    private ByteBuffer buffer;
    private Camera mCamera = null;

    CameraDelegate(){
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

    @Override
    public void onPreviewFrame(byte[] frame, Camera var2){
        byte[] frameCopy = frame.clone();

        // chop up into 120x720x4 then combine high and low bytes
        int k = 0;
        for(int i = 0; i < 720; i++){
            for(int j=0; j < 120; j++){
                byte d0 = frameCopy[k++];
                byte d1 = frameCopy[k++];
                byte a0 = frameCopy[k++];
                byte a1 = frameCopy[k++];

                int amplitude = ((a1 & 0xff) << 8) | (a0 & 0xff);
                int depth = ((d1 & 0xff) << 8) | (d0 & 0xff);

                depthArray[i][j] = depth;
                amplitudeArray[i][j] = amplitude;
            }
        }

//        if(bmp != null){
//            Log.d("CameraDelegate", "Got a bitmap with size: (" + bmp.getWidth() + ", " + bmp.getHeight() + ")" );
//        }
    }

}
