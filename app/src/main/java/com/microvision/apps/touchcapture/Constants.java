package com.microvision.apps.touchcapture;

import android.os.Environment;

public class Constants {
    static final int SCREEN_WIDTH = 1920;
    static final int SCREEN_HEIGHT = 1016;
    static final int SPRITE_WIDTH = 60;
    static final int SPRITE_HEIGHT = 60;
    static final int TILES_WIDTH = 32;
    static final int TILES_HEIGHT = 18;
    static final boolean CAPTURE_TOF = false;
    static final int BUFFER_SIZE = 20;
    static final int FRAMES_PER_TOUCH = 10;
    static final String STORAGE_FOLDER = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
}
