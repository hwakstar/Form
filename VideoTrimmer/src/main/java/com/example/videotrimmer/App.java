package com.example.videotrimmer;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import nl.bravobit.ffmpeg.FFmpeg;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initFFmpegBinary(this);
    }

    private void initFFmpegBinary(Context context) {
        if (!FFmpeg.getInstance(context).isSupported()) {
            Log.e("ZApplication", "Android cup arch not supported!");
        }
    }
}
