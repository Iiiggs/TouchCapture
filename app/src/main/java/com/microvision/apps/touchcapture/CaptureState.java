package com.microvision.apps.touchcapture;

public class CaptureState {
    public enum RecordingState {
        RECORDING,
        STOPPED
    }

    boolean recording = false;
    boolean touchdown = false;
    int x = -1;
    int y = -1;

    long framesCount = 0;
    long startTime = 0;
    long currentTime = 0;
    double fps = 0.0;


    int framesSinceTouchdown = 0;

    public CaptureState(){
    }
}
