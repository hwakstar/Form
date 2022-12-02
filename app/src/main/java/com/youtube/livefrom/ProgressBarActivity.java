package com.youtube.livefrom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;

public class ProgressBarActivity extends AppCompatActivity {

    CircleProgressBar circleProgressBar;
    int duration;
    String[] command;
    String path;

    ServiceConnection mConnection;
    FFMpegService ffMpegService;
    Integer res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_bar);
        circleProgressBar = (CircleProgressBar)findViewById(R.id.circleProgressBar);
        circleProgressBar.setMax(100);

        Intent i = getIntent();
        if(i != null){
            duration = i.getIntExtra("duration", 0);
            command = i.getStringArrayExtra("command");
            path = i.getStringExtra("destination");

            final Intent myIntent = new Intent(ProgressBarActivity.this, FFMpegService.class);
            myIntent.putExtra("duration", String.valueOf(duration));
            myIntent.putExtra("command", command);
            myIntent.putExtra("destination", path);
            try {
                startService(myIntent);
            }catch (Exception e){
                e.printStackTrace();
            }

            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    FFMpegService.LocalBinder binder = (FFMpegService.LocalBinder)iBinder;
                    ffMpegService = binder.getServiceInstance();
                    ffMpegService.registerClient(getParent());

                    final Observer<Integer> resultObserver = new Observer<Integer>() {
                        @Override
                        public void onChanged(Integer integer) {
                            res = integer;
                            if(res < 100){
                                circleProgressBar.setProgress(res);
                            }
                            if(res == 100){
                                circleProgressBar.setProgress(res);
                                stopService(myIntent);
                                Toast.makeText(getApplicationContext(), "Video trimmed successfully", Toast.LENGTH_LONG).show();
                                Handler handler = new Handler();

                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        finish();
                                    }
                                }, 1000);
                            }
                        }
                    };
                    try {
                        ffMpegService.getPercentage().observe(ProgressBarActivity.this, resultObserver);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            try {
                bindService(myIntent, mConnection, Context.BIND_AUTO_CREATE);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
