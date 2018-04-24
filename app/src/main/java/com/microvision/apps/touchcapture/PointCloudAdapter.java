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

public class PointCloudAdapter implements Camera.PreviewCallback {

    // todo: move everything not related to camera into separate class
    Camera mCamera = null;


    interface PointCloudDelegate {
        // got a new frame
        void processFrame(byte [] frame);
    }

    PointCloudDelegate mPointCloudDelegate;

    // todo: pass a delegate to capture controller
    PointCloudAdapter(PointCloudDelegate delegate) {
        mPointCloudDelegate = delegate;

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
               mCamera.startPreview();
            }
        } else {
            Log.d("PointCloudAdapter", "Camera not opened correctly");
        }
    }

    void cleanup() {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }


    @Override
    public void onPreviewFrame(byte[] frame, Camera camera) {

        mPointCloudDelegate.processFrame(frame);
    }




}
