package com.youtube.livefrom;

import android.app.Application;
import android.content.Context;

import com.vastreaming.capture.VideoCaptureSource;
import com.vastreaming.common.GlobalContext;
import com.vastreaming.common.License;


public class AppController extends Application
{

    private static Context Context;

    @Override
    public void onCreate()
    {

        super.onCreate();

        AppController.Context = getApplicationContext();

        int expectedNumberOfConnections = 1;
        int expectedMaxWidth = 1920;
        int expectedMaxHeight = 1080;

        // initialize general allocators used across all VASTreaming libraries
        GlobalContext.initialize(expectedNumberOfConnections);

        // now we need to initialize video capture specific allocator
        // which depends on the maximum capture resolution
        VideoCaptureSource.initialize(expectedNumberOfConnections, expectedMaxWidth, expectedMaxHeight);

        //License.setKey("28TDA9gZuEX4tsqb");  // Live for Streaming (Purchased)
        License.setKey("J9FwAf7EE2JCG48x"); // Demo for File writing

    }

    public static Context getAppContext() {
        return AppController.Context;
    }

    @Override
    public void onTerminate()
    {

        VideoCaptureSource.uninitialize();
        GlobalContext.uninitialize();

        super.onTerminate();

    }


}
