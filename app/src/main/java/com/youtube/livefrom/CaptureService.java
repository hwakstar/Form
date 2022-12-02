///////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2016-2019 VASTreaming
//
// Licensee is granted permission to use, copy and modify this file.
// Licensee can distribute and sell this file in a binary form as a part of the
// licensee's product. Licensee is prohibited from selling this file and
// library separately from the licensee's products. Licensee is prohibited from
// disclosing this file to any 3rd party. Licensee is prohibited from openly
// publishing this file as a part of open-source software or any other means.
//
///////////////////////////////////////////////////////////////////////////////


package com.youtube.livefrom;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.vastreaming.capture.AudioCaptureSource;
import com.vastreaming.capture.VideoCaptureDevice;
import com.vastreaming.capture.VideoCaptureSource;
import com.vastreaming.common.FileLog;
import com.vastreaming.common.GlobalContext;
import com.vastreaming.media.MediaSession;


public class CaptureService extends Service
{


    public VideoCaptureDevice captureDevice = null;
    public VideoCaptureDevice.EventListener captureDeviceListener = null;
    public VideoCaptureSource previewSource = null;
    public VideoCaptureSource commonVideoSource = null;
    public AudioCaptureSource commonAudioSource = null;
    public MediaSession streamingSession = null;
    public MediaSession writerSession = null;
    public MediaProjectionManager mediaProjectionManager = null;
    public int mediaProjectionResultCode = 0;
    public Intent mediaProjectionResultData = null;
    public volatile boolean isStarted = false;

    private Handler handler = null;
    private static Runnable runnable = null;
    private volatile boolean isRunning = true;
    private volatile boolean restartMainActivity = false;
    private long lastAliveLogged = 0;
    private long mainActivityDestroyed = 0;


    public CaptureService()
    {
    }


    public boolean isStopping()
    {
        return !this.isRunning;
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    @Override
    public void onCreate()
    {

        super.onCreate();

        GlobalContext.context = getApplicationContext();
        GlobalContext.applicationName = getString(R.string.app_name);
        synchronized (GlobalContext.lock)
        {
            GlobalContext.mainService = this;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            this.mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        this.handler = new Handler();
        runnable = new Runnable()
        {
            public void run()
            {
                process();
                if (isRunning)
                {
                    handler.postDelayed(runnable, 1000);
                }
                else
                {
                    stopSelf();
                }
            }
        };

        this.handler.postDelayed(runnable, 0);

        if (BuildConfig.DEBUG)
        {
            FileLog.Initialize(FileLog.LOG_DEBUG);
        }
        else
        {
            FileLog.Initialize(FileLog.LOG_DEBUG);
        }

        this.isStarted = true;
        FileLog.Logd("CaptureService", "onCreate()");

    }


    @Override
    public void onDestroy()
    {
        FileLog.Logd("CaptureService", "onDestroy()");
        this.handler.removeCallbacks(runnable);
        Context context = GlobalContext.context;
        cleanupCaptureService();
        if (this.restartMainActivity)
        {
            Log.d("CaptureService", "Re-creating main activity...");
            final Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        super.onDestroy();
    }


    public void stopCapturing()
    {
        this.isRunning = false;
    }


    private void cleanupCaptureService()
    {

        // release everything
        if (this.captureDevice != null)
        {

            if (this.captureDeviceListener != null)
            {
                this.captureDevice.removeEventListener(this.captureDeviceListener);
                this.captureDeviceListener = null;
            }

            // reset device preview surface, just in case
            this.captureDevice.surfaceView(null);
            this.captureDevice.release();
            this.captureDevice = null;

        }

        if (this.previewSource != null)
        {
            this.previewSource.release();
            this.previewSource = null;
        }

        if (this.commonVideoSource != null)
        {
            this.commonVideoSource.release();
            this.commonVideoSource = null;
        }

        if (this.commonAudioSource != null)
        {
            this.commonAudioSource.release();
            this.commonAudioSource = null;
        }

        if (this.streamingSession != null)
        {
            FileLog.Logd("CaptureService", "Stopping streaming...");
            this.streamingSession.stop();
            this.streamingSession.close();
            this.streamingSession = null;
            FileLog.Logd("CaptureService", "Streaming stopped");
        }

        if (this.writerSession != null)
        {
            FileLog.Logd("CaptureService", "Stopping writing...");
            this.writerSession.stop();
            this.writerSession.close();
            this.writerSession = null;
            FileLog.Logd("CaptureService", "Writing stopped");
        }

        this.mediaProjectionManager = null;
        this.mediaProjectionResultCode = 0;
        this.mediaProjectionResultData = null;

        synchronized (GlobalContext.lock)
        {
            GlobalContext.mainService = null;
        }
        GlobalContext.context = null;

    }


    private void process()
    {

        if (System.currentTimeMillis() - this.lastAliveLogged >= 10000)
        {
            if (this.previewSource != null || this.streamingSession != null)
            {
                FileLog.Logd("CaptureService", "Alive");
            }
            this.lastAliveLogged = System.currentTimeMillis();
        }

        synchronized (GlobalContext.lock)
        {
            if (GlobalContext.mainActivity == null)
            {
                if (this.previewSource != null || this.streamingSession != null)
                {
//                    if (this.mainActivityDestroyed == 0)
//                    {
//                        FileLog.Logd("CaptureService", "Detected destroyed main activity");
//                        this.mainActivityDestroyed = System.currentTimeMillis();
//                    }
//                    else if (System.currentTimeMillis() - this.mainActivityDestroyed >= 1000)
                    {
                        FileLog.Logd("CaptureService", "Need to re-create main activity");
                        this.restartMainActivity = true;
                        this.isRunning = false;
                    }
                }
                else
                {
                    FileLog.Logd("CaptureService", "Main activity is destroyed and service is unused, destroying it...");
                    this.isRunning = false;
                }
            }
        }

    }


}
