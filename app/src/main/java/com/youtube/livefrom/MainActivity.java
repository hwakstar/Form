package com.youtube.livefrom;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.ThumbnailSetResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vastreaming.capture.AudioCaptureSource;
import com.vastreaming.capture.IVideoCaptureSource;
import com.vastreaming.capture.VideoCaptureSource;
import com.vastreaming.common.Codec_t;
import com.vastreaming.common.FileLog;
import com.vastreaming.common.GlobalContext;
import com.vastreaming.common.Rational;
import com.vastreaming.media.FileSink;
import com.vastreaming.media.IMediaSink;
import com.vastreaming.media.MediaSession;
import com.vastreaming.media.MediaState;
import com.vastreaming.media.MediaStateListener;
import com.vastreaming.media.OverlayImage;
import com.vastreaming.media.OverlayText;
import com.vastreaming.rtmp.RtmpPublisherSink;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.AfterPermissionGranted;

import static com.youtube.livefrom.AppConfig.JsonConfigFilename;

public class MainActivity extends AppCompatActivity {

    // Youtube Data Api v3 properties
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int PROGRESS_BAR_ACTIVITY = 1006;
    static final int TRIM_ACTIVITY = 1007;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {YouTubeScopes.YOUTUBE};


    private final int PERMISSIONS_REQUEST_CAPTURE = 1;
    private Intent serviceIntent = null;
    private Handler handler = new Handler();
    private Handler handlerGoLive = new Handler();
    private static Runnable runnableStartService = null;
    private static Runnable runnableInitiatePreview = null;
    private static float Zoom = 1.0f;
    private static int ZoomMax = 1;
    private static Runnable runnableZoom = null;
    public static final Object lockInitiatePreview = new Object();
    private Handler handlerInitiatePreview = new Handler();
    private int cameraDeviceId = 0;
    private int streamSeconds = 0;
    private boolean isFrontCamera = false;
    private int micDeviceId = 0;
    private List<OverlayItem> overlayItems = null;
    private int cameraCaptureWidth = 0;
    private int cameraCaptureHeight = 0;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private int videoBitrate = 0;
    private int videoStreamingFailedCounter = 0;
    private int videoRotation = 0;
    private int deviceCurrentOrientation;
    private int deviceOrientation;
    private Size[] camSizes = null;
    private String youtubeServerURL = "rtmp://a.rtmp.youtube.com/live2/";
    private MyEnums.StreamingStatus currentStreamingStatus = MyEnums.StreamingStatus.Stopped;
    private MyEnums.RecordStatus currentRecordStatus = MyEnums.RecordStatus.Stop;

    private float mLastTouchY = 0;
    private float mPrevTouchY = 0;

    Adapter_VideoFolder obj_adapter;
    ArrayList<Model_Video> al_video = new ArrayList<>();
    RecyclerView videoContainer;
    RecyclerView.LayoutManager recyclerViewLayoutManager;
    private static final int REQUEST_PERMISSIONS = 100;
    private boolean IsFlash = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        if (!HelperMethods.isJsonConfigFileExists()) {
            try {
                InputStream assetFile = getResources().getAssets().open(JsonConfigFilename);
                File file = new File(AppController.getAppContext().getFilesDir(),AppConfig.JsonConfigFilename);
                OutputStream copyFile = new FileOutputStream(file);
                copy(assetFile, copyFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (TextUtils.isEmpty(AppConfig.getJsonURL())) {
            AppConfig.setJsonURL("http://74.208.154.80/YouTube.json");
            AppConfig.setVideoTitle(AppConfig.AppName);
        }

        Button btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.btnRecord).setVisibility(View.VISIBLE);
                findViewById(R.id.btnLive).setVisibility(View.VISIBLE);
                findViewById(R.id.videoContainer).setVisibility(View.VISIBLE);
                findViewById(R.id.Menu).setVisibility(View.GONE);
                findViewById(R.id.btnSettings).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnSwitchCamera).setVisibility(View.INVISIBLE);
                //findViewById(R.id.previewContainer).getLayoutParams().height= ViewGroup.LayoutParams.MATCH_PARENT;
                initiatePreview();
            }
        });

        Button btnFlash = findViewById(R.id.btnFlash);
        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button btnFlashs = findViewById(R.id.btnFlash);
                if (IsFlash) {
                    btnFlashs.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_flash_off, 0);
                } else {
                    btnFlashs.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_flash, 0);
                }
                CameraManager mCameraManager;
                mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                try {
                    String mCameraId;
                    mCameraId = mCameraManager.getCameraIdList()[0];
                    mCameraManager.setTorchMode(mCameraId, IsFlash);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                IsFlash = !IsFlash;
            }
        });

        Button btnRecord = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.btnRecord).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnLive).setVisibility(View.INVISIBLE);
                findViewById(R.id.videoContainer).setVisibility(View.INVISIBLE);
                findViewById(R.id.Menu).setVisibility(View.VISIBLE);
                findViewById(R.id.btnSettings).setVisibility(View.VISIBLE);
                findViewById(R.id.btnSwitchCamera).setVisibility(View.VISIBLE);
                //findViewById(R.id.previewContainer).getLayoutParams().height= ViewGroup.LayoutParams.MATCH_PARENT;
                initiatePreview();
            }
        });

        Button btnLive = findViewById(R.id.btnLive);
        btnLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoStreamingFailedCounter = 0;
                findViewById(R.id.btnRecord).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnLive).setVisibility(View.INVISIBLE);
                findViewById(R.id.videoContainer).setVisibility(View.INVISIBLE);
                findViewById(R.id.Menu).setVisibility(View.VISIBLE);
                findViewById(R.id.btnSettings).setVisibility(View.VISIBLE);
                findViewById(R.id.btnSwitchCamera).setVisibility(View.VISIBLE);
                //findViewById(R.id.previewContainer).getLayoutParams().height= ViewGroup.LayoutParams.MATCH_PARENT;
                initiatePreview();
            }
        });

        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        FloatingActionButton btnStartRecording = findViewById(R.id.btnUpload);
        btnStartRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiateRecording();
            }
        });

        FloatingActionButton btnStartStreaming = findViewById(R.id.btnStreaming);
        btnStartStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiateStreaming();
            }
        });

        FloatingActionButton btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Processing API task...");
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());

        HelperMethods.logMessage("Starting service...");
        GlobalContext.context = getApplicationContext();
        synchronized (GlobalContext.lock) {
            GlobalContext.mainActivity = this;
            if (GlobalContext.mainService == null) {
                this.serviceIntent = new Intent(this, CaptureService.class);
                startService(this.serviceIntent);
            } else if (((CaptureService) GlobalContext.mainService).isStopping()) {
                runnableStartService = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (GlobalContext.lock) {
                            if (GlobalContext.mainService == null) {
                                serviceIntent = new Intent(MainActivity.this, CaptureService.class);
                                startService(serviceIntent);
                            } else if (((CaptureService) GlobalContext.mainService).isStopping()) {
                                handler.postDelayed(runnableStartService, 100);
                            }
                        }
                    }
                };
                handler.postDelayed(runnableStartService, 100);
            }
        }
        if (BuildConfig.DEBUG) {
            FileLog.Initialize(FileLog.LOG_DEBUG);
        } else {
            FileLog.Initialize(FileLog.LOG_DEBUG);
        }

        videoContainer = (RecyclerView) findViewById(R.id.videoContainer);
        recyclerViewLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        videoContainer.setLayoutManager(recyclerViewLayoutManager);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(new GridLayoutManager(this, 1, RecyclerView.VERTICAL, false));

        AdjustVideoColumn();
        LoadVideos();

//        Intent j = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        j.setType("video/*");
//        startActivityForResult(j, 10);

//        String videoAbs = "/storage/emulated/0/LiveFrom/20191216_202417.mp4";
//        Uri uriAbs = Uri.fromFile(new File(videoAbs));
//
//        File file = new File(videoAbs);
//        MediaScannerConnection.scanFile(this,
//                new String[] { file.getAbsolutePath() }, null,
//                new MediaScannerConnection.OnScanCompletedListener() {
//                    public void onScanCompleted(String path, Uri uri) {
//                        String videoAbs1 = uri.getPath();
//                        videoAbs1 = "content://media" + videoAbs1;
//                        Log.i("onScanCompleted", uri.getPath());
//                    }
//                });


//        Intent i = new Intent(MainActivity.this, TrimActivity.class);
////        String videoUri = "content://media/external/video/media/89513";
//        String videoUri = "file:///storage/emulated/0/LiveFrom/20191216_202417.mp4";
//        i.putExtra("videoUri", videoUri);
//        startActivityForResult(i, TRIM_ACTIVITY);
    }

    public void LoadVideos() {

        int int_position = 0;
        Uri uri;
        Cursor cursor;
        int column_index_data, column_index_folder_name, column_id, thum;

        String absolutePathOfImage = null;
        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media._ID, MediaStore.Video.Thumbnails.DATA};

        final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
        cursor = getApplicationContext().getContentResolver().query(uri, projection, null, null, orderBy + " DESC");

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        column_index_folder_name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
        column_id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        thum = cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA);

        while (cursor.moveToNext()) {
            try {
                absolutePathOfImage = cursor.getString(column_index_data);
                Log.e("Column", absolutePathOfImage);
                Log.e("Folder", cursor.getString(column_index_folder_name));
                Log.e("column_id", cursor.getString(column_id));
                Log.e("thum", cursor.getString(thum));

                Model_Video obj_model = new Model_Video();
                obj_model.setBoolean_selected(false);
                obj_model.setStr_path(absolutePathOfImage);
                obj_model.setStr_thumb(cursor.getString(thum));

                al_video.add(obj_model);
            } catch (Exception e) {

            }
        }

        try {
            obj_adapter = new Adapter_VideoFolder(getApplicationContext(), al_video, MainActivity.this);
            videoContainer.setAdapter(obj_adapter);
        } catch (Exception e) {
            Log.e("thum", e.getMessage());
        }
    }

    public void AdjustVideoColumn() {
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            AppConfig.ColumnCount = 3;
        } else {
            AppConfig.ColumnCount = 6;
        }
        GridLayoutManager layoutManager = new GridLayoutManager(this, AppConfig.ColumnCount);
        videoContainer.setLayoutManager(layoutManager);
        videoContainer.addItemDecoration(new SpaceGrid(AppConfig.ColumnCount, 0, false));
    }

    private class SpaceGrid extends RecyclerView.ItemDecoration {
        private int mSpanCount;
        private int mSpacing;
        private boolean mIncludeEdge;

        private SpaceGrid(int spanCount, int spacing, boolean includeEdge) {
            mSpanCount = spanCount;
            mSpacing = spacing;
            mIncludeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % mSpanCount;
            if (mIncludeEdge) {
                outRect.left = mSpacing - column * mSpacing / mSpanCount;
                outRect.right = (column + 1) * mSpacing / mSpanCount;
                if (position < mSpanCount) {
                    outRect.top = mSpacing;
                }
                outRect.bottom = mSpacing;
            } else {
                outRect.left = column * mSpacing / mSpanCount;
                outRect.right = mSpacing - (column + 1) * mSpacing / mSpanCount;
                if (position < mSpanCount) {
                    outRect.top = mSpacing;
                }
            }
        }
    }

    @Override
    protected void onPause() {
        stopPreview();
        HelperMethods.copyLibLogFile();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initiatePreview();
        HelperMethods.copyLibLogFile();
    }

    @Override
    protected void onDestroy() {
        HelperMethods.copyLibLogFile();
        super.onDestroy();
        HelperMethods.logMessage("Stopping service...");
        synchronized (GlobalContext.lock) {
            GlobalContext.mainActivity = null;
            if (GlobalContext.mainService != null) {
                if (((CaptureService) GlobalContext.mainService).previewSource == null &&
                        ((CaptureService) GlobalContext.mainService).streamingSession == null) {
                    ((CaptureService) GlobalContext.mainService).stopCapturing();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStreamingStatus == MyEnums.StreamingStatus.Stopped) {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAPTURE: {
                boolean allGranted = true;
                for (int res : grantResults) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        checkIfPreviewOk();
        AdjustVideoColumn();
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            HelperMethods.logMessage("Requesting required permissions...");
            ArrayList<String> req = new ArrayList<String>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                req.add(Manifest.permission.CAMERA);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                req.add(Manifest.permission.RECORD_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                req.add(Manifest.permission.INTERNET);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                req.add(Manifest.permission.ACCESS_NETWORK_STATE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                req.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                req.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                req.add(Manifest.permission.GET_ACCOUNTS);
            }
            String[] a = new String[req.size()];
            ActivityCompat.requestPermissions(this, req.toArray(a), PERMISSIONS_REQUEST_CAPTURE);
            return false;
        } else {
            return true;
        }
    }

    public void openSettings() {
        Intent myIntent = new Intent(this, SettingsActivity.class);
        this.startActivity(myIntent);
    }

    public void switchCamera() {
//        HelperMethods.shareLogFile(this);
        isFrontCamera = !isFrontCamera;
        stopPreview();
        initiatePreview();
    }

    private boolean setCameraDeviceId() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] camIds = manager.getCameraIdList();
            for (int i = 0; i < camIds.length; i++) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(camIds[i]);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (isFrontCamera) {
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraDeviceId = i;
                        break;
                    }
                } else {
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraDeviceId = i;
                        break;
                    }
                }
            }
            HelperMethods.logMessage("Camera device successfully set: " + cameraDeviceId);
        } catch (Exception e) {
            HelperMethods.showErrorMessage(this, "Unable to use camera.", e.toString());
            return false;
        }
        return true;
    }

    private void setAudioDeviceId() {
        micDeviceId = android.media.MediaRecorder.AudioSource.MIC;
        HelperMethods.logMessage("Audio device successfully set: " + micDeviceId);
    }

    private void LockOrientation() {
        HelperMethods.logMessage("Locking orientation");
        if (deviceCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE && deviceOrientation == Surface.ROTATION_90) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (deviceCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE && deviceOrientation == Surface.ROTATION_270) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void UnLockOrientation() {
        HelperMethods.logMessage("Unlocking orientation");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void startTimer() {
        if (currentStreamingStatus != MyEnums.StreamingStatus.Live && currentRecordStatus != MyEnums.RecordStatus.Record) {
            HelperMethods.logMessage("Starting timer...");
            if (!TextUtils.isEmpty(AppConfig.getEventStartedTime())) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                Date date = null;
                try {
                    date = format.parse(AppConfig.getEventStartedTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Date cDate = new Date();
                long diffInMs = cDate.getTime() - date.getTime();
                streamSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(diffInMs);
            } else {
                streamSeconds = 0;
            }
            final Handler handler = new Handler();
            final int delay = 1000;
            handler.postDelayed(new Runnable() {
                public void run() {
                    HelperMethods.logMessage("Runnable for live timer...");
                    boolean res = (currentStreamingStatus != MyEnums.StreamingStatus.Stopped || currentRecordStatus != MyEnums.RecordStatus.Stop);
                    TextView lblStreamingTimer = findViewById(R.id.lblStreamingTimer);
                    if (res) {
                        streamSeconds++;
                        long hours = TimeUnit.SECONDS.toHours(streamSeconds);
                        long mins = TimeUnit.SECONDS.toMinutes(streamSeconds) - TimeUnit.HOURS.toMinutes((hours));
                        long sec = TimeUnit.SECONDS.toSeconds(streamSeconds) - TimeUnit.MINUTES.toSeconds(mins);
//                    lblStreamingTimer.setText(String.format("%02d:%02d:%02d", hours, mins, sec) + " , " + videoWidth + "x" + videoHeight + " , " + videoBitrate );
                        lblStreamingTimer.setText(String.format("%02d:%02d:%02d", hours, mins, sec));
                        findViewById(R.id.lblStreamingTimer).setVisibility(View.VISIBLE);
                        handler.postDelayed(this, delay);
                    } else {
                        lblStreamingTimer.setText("");
                        findViewById(R.id.lblStreamingTimer).setVisibility(View.INVISIBLE);
                        HelperMethods.logMessage("Stopping timer...");
                    }
                }
            }, delay);
        }
    }

    private boolean setupStreamQuality() {
        HelperMethods.logMessage("Checking stream quality...");
        boolean res = false;
        try {
            if (!TextUtils.isEmpty(AppConfig.getStreamResolution())) {

                if (AppConfig.getCameraResolution().isEmpty()) {
                    HelperMethods.showErrorMessage(MainActivity.this, "Choose camera resolution first.", "Camera resolution not selected yet from settings page", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
                            btnSettings.performClick();
                        }
                    });
                } else {

                    String[] camResolutions = AppConfig.getCameraResolution().split("x");
                    int dWidth = Integer.parseInt(camResolutions[0].trim());
                    int dHeight = Integer.parseInt(camResolutions[1].trim());

                    Size cMode = new Size(dWidth, dHeight);

                    if (cMode != null) {
                        HelperMethods.logMessage("Target resolution found: " + cMode.getWidth() + "x" + cMode.getHeight());
                        cameraCaptureWidth = cMode.getWidth();
                        cameraCaptureHeight = cMode.getHeight();
                        switch (AppConfig.getStreamResolution()) {
                            case "240p":
                                videoWidth = 426;
                                videoHeight = 240;
                                videoBitrate = 300000;
                                break;
                            case "360p":
                                videoWidth = 640;
                                videoHeight = 360;
                                videoBitrate = 600000;
                                break;
                            case "480p":
                                videoWidth = 854;
                                videoHeight = 480;
                                videoBitrate = 800000;
                                break;
                            case "720p":
                                videoWidth = 1280;
                                videoHeight = 720;
                                videoBitrate = 2000000;
                                break;
                            case "1080p":
                                videoWidth = 1920;
                                videoHeight = 1080;
                                videoBitrate = 3000000;
                                break;
                            case "1440p":
                                videoWidth = 2560;
                                videoHeight = 1440;
                                videoBitrate = 6000000;
                                break;
                        }
                        videoWidth = cMode.getWidth();
                        videoHeight = cMode.getHeight();
                        int vBitrate = Integer.parseInt(AppConfig.getVideoBitrate());
                        if (vBitrate > 0) {
                            videoBitrate = vBitrate * 1000;
                        }
                        res = true;
                        checkIfPreviewOk();

                    } else {
                        throw new Exception("Unable to setup stream quality");
                    }
                }
            } else {
                HelperMethods.showErrorMessage(MainActivity.this, "Choose stream resolution first.", "Select camera resolution and bitrate from settings page", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
                        btnSettings.performClick();
                    }
                });
            }
        } catch (Exception e) {
            HelperMethods.logError("Unable to setup stream quality.", e.toString());
            return false;
        }

        HelperMethods.logMessage("Resolution: " + videoWidth + "x" + videoHeight + " , Capture Resolution: " + cameraCaptureWidth + "x" + cameraCaptureHeight + " , Rotation: " + videoRotation + " , Bitrate: " + videoBitrate);
        return res;
    }

    private void checkIfPreviewOk() {
        HelperMethods.logMessage("Checking preview if ok...");
        HelperMethods.logMessage("Before: Resolution: " + videoWidth + "x" + videoHeight + " , Capture Resolution: " + cameraCaptureWidth + "x" + cameraCaptureHeight + " , Rotation: " + videoRotation + " , Bitrate: " + videoBitrate);
        int vRotation = videoRotation;
        int vWidth = videoWidth;
        int vHeight = videoHeight;
        deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
        deviceCurrentOrientation = getResources().getConfiguration().orientation;
        HelperMethods.logMessage("Current Orientation: " + deviceCurrentOrientation);
        HelperMethods.logMessage("DeviceOrientation: " + deviceOrientation);
        vRotation = (deviceOrientation == Surface.ROTATION_90) ? 0 : (deviceOrientation == Surface.ROTATION_180) ? 270 : (deviceOrientation == Surface.ROTATION_270) ? 180 : 90;
        int resMax = Math.max(videoWidth, videoHeight);
        int resMin = Math.min(videoWidth, videoHeight);
        if (vRotation % 180 == 0) {
            vWidth = resMax;
            vHeight = resMin;
        } else {
            vWidth = resMin;
            vHeight = resMax;
        }
        if (vRotation != videoRotation || vWidth != videoWidth || vHeight != videoHeight) {
            videoRotation = vRotation;
            videoWidth = vWidth;
            videoHeight = vHeight;
            stopPreview();
            stopBoardcast();
            startPreview();
            if (currentStreamingStatus != MyEnums.StreamingStatus.Stopped && !TextUtils.isEmpty(AppConfig.getStreamName())) {
                startBoardcast();
            }
        }
        HelperMethods.logMessage("After: Resolution: " + videoWidth + "x" + videoHeight + " , Capture Resolution: " + cameraCaptureWidth + "x" + cameraCaptureHeight + " , Rotation: " + videoRotation + " , Bitrate: " + videoBitrate);
    }

    private void setVideoSize() {
        int vWidth = videoWidth;
        int vHeight = videoHeight;
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        if (screenWidth < screenHeight && vWidth > vHeight) {
            int bHeight = vWidth;
            vWidth = vHeight;
            vHeight = bHeight;
        }
        float videoProportion = (float) vWidth / (float) vHeight;
        float screenProportion = (float) screenWidth / (float) screenHeight;
        SurfaceView surfaceView = findViewById(R.id.surfaceViewPreview);
        android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        surfaceView.setLayoutParams(lp);
    }

    private void updateRecordStatus(MyEnums.RecordStatus status) {
        HelperMethods.logMessage("Updating streaming status to " + status.toString());
        currentRecordStatus = status;
        FloatingActionButton btnStartUpload = findViewById(R.id.btnUpload);
        TextView lblStreamingTimer = findViewById(R.id.lblStreamingTimer);

        switch (status) {
            case Stop:
                btnStartUpload.setImageResource(R.drawable.ic_record);
                if (currentStreamingStatus == MyEnums.StreamingStatus.Stopped) {
                    findViewById(R.id.btnSettings).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnSwitchCamera).setVisibility(View.VISIBLE);
                    lblStreamingTimer.setText("");
                    findViewById(R.id.lblStreamingTimer).setVisibility(View.INVISIBLE);
                    findViewById(R.id.btnClose).setVisibility(View.VISIBLE);
                }
                break;
            case Record:
                btnStartUpload.setImageResource(R.drawable.ic_record_stop);
                findViewById(R.id.btnSettings).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnSwitchCamera).setVisibility(View.INVISIBLE);
                findViewById(R.id.lblStreamingTimer).setVisibility(View.VISIBLE);
                findViewById(R.id.btnClose).setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void updateStreamingStatus(MyEnums.StreamingStatus status) {
        HelperMethods.logMessage("Updating streaming status to " + status.toString());
        currentStreamingStatus = status;
        FloatingActionButton btnStartStreaming = findViewById(R.id.btnStreaming);
        TextView lblStreamingTimer = findViewById(R.id.lblStreamingTimer);

        switch (status) {
            case Stopped:
                btnStartStreaming.setImageResource(R.drawable.ic_cast);
                if (currentRecordStatus == MyEnums.RecordStatus.Stop) {
                    findViewById(R.id.btnSettings).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnSwitchCamera).setVisibility(View.VISIBLE);
                    lblStreamingTimer.setText("");
                    findViewById(R.id.btnClose).setVisibility(View.VISIBLE);
                }
//                findViewById(R.id.lblStreamingTimer).setVisibility(View.INVISIBLE);
                break;
            case Encoding:
                btnStartStreaming.setImageResource(R.drawable.ic_cast_connect);
                findViewById(R.id.btnSettings).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnSwitchCamera).setVisibility(View.INVISIBLE);
                lblStreamingTimer.setText("");
//                findViewById(R.id.lblStreamingTimer).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnClose).setVisibility(View.INVISIBLE);
                break;
            case Live:
                btnStartStreaming.setImageResource(R.drawable.ic_cast_connect);
                findViewById(R.id.btnSettings).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnSwitchCamera).setVisibility(View.INVISIBLE);
                findViewById(R.id.lblStreamingTimer).setVisibility(View.VISIBLE);
                findViewById(R.id.btnClose).setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void setupEvent() {
        HelperMethods.logMessage("Starting setup event...");
        try {
            boolean isNewEventRequired = true;
            if (AppConfig.CurrentEvent != null) {
                String status = AppConfig.CurrentEvent.getStatus().getLifeCycleStatus();
                HelperMethods.logMessage("Current broadcast event status: " + status);
                if (status.contains("live")) {
                    isNewEventRequired = false;
                    startBoardcast();
                }
            }
            if (isNewEventRequired) {
                AppConfig.setEventStartedTime("");
                AppConfig.setStreamName("");
                AppConfig.setEventId("");
                AppConfig.CurrentEvent = null;
                setupBroadcastEvent();
            }
        } catch (Exception ex) {
            HelperMethods.showErrorMessage(this, "Unable to setup event. " + ex.getMessage(), ex.toString());
            updateStreamingStatus(MyEnums.StreamingStatus.Stopped);
            UnLockOrientation();
        }
    }

    private void initiatePreview() {
        try {
            if (!isGooglePlayServicesAvailable()) {
                acquireGooglePlayServices();
            } else if (mCredential.getSelectedAccountName() == null) {
                chooseAccount();
            } else if (!isDeviceOnline()) {
                HelperMethods.showErrorMessage(this, "No network connection available.", "");
            } else if (checkPermissions()) {

                if (!TextUtils.isEmpty(AppConfig.getEventId())) {
                    new APIGetEvent(mCredential).execute();
                }

                List<Size> sizesList = new ArrayList<>();

//                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//                String[] camIds = manager.getCameraIdList();
//                CameraCharacteristics characteristics = manager.getCameraCharacteristics(camIds[cameraDeviceId]);
//                StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                camSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
//                List<Size> sizesList = Arrays.asList(camSizes);

                Camera camera = null;
                camera = Camera.open(cameraDeviceId);
                Camera.Parameters pars = camera.getParameters();
                List<android.hardware.Camera.Size> sizesList1 = pars.getSupportedVideoSizes();
                for (android.hardware.Camera.Size s : sizesList1) {
                    sizesList.add(new Size(s.width, s.height));
                }
                camera.release();

                Collections.sort(sizesList, new Comparator<Size>() {

                    public int compare(final Size a, final Size b) {
                        return Integer.valueOf(a.getWidth() + a.getHeight()).compareTo(Integer.valueOf(b.getWidth() + b.getHeight()));
                    }
                });
                camSizes = new Size[sizesList.size()];
                camSizes = sizesList.toArray(camSizes);
                AppConfig.CamSizes = camSizes;
                HelperMethods.logMessage("Camera resolutions...");
                for (Size size : camSizes) {
                    HelperMethods.logMessage(size.getWidth() + "x" + size.getHeight());
                }

                HelperMethods.logMessage("Initiating preview...");
                runnableInitiatePreview = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lockInitiatePreview) {
                            if (GlobalContext.mainService != null) {
                                if (!HelperMethods.isJsonConfigFileExists()) {
                                    HelperMethods.showErrorMessage(MainActivity.this, "Json file not found locally.", "Please download it first from server", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
                                            btnSettings.performClick();
                                        }
                                    });
                                } else {
                                    try {
                                        //EnableLibraryLog();
                                        if (setCameraDeviceId()) {
                                            setAudioDeviceId();
                                            String data = HelperMethods.readFile(new File(getFilesDir(), JsonConfigFilename).getPath());
                                            if (TextUtils.isEmpty(data)) {
                                                HelperMethods.showErrorMessage(MainActivity.this, "Invalid json file.", "");
                                            } else {

                                                Gson gson = new Gson();
                                                Type overlayItemListType = new TypeToken<Collection<OverlayItem>>() {
                                                }.getType();

                                                overlayItems = new ArrayList<>();//gson.fromJson(data, overlayItemListType);

                                                if (setupStreamQuality()) {
                                                    startPreview();
                                                } else {
                                                    HelperMethods.showErrorMessage(MainActivity.this, "Unable to set streaming quality.", "Streaming quality not set");
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        HelperMethods.showErrorMessage(MainActivity.this, "Unable to initiate preview. ", ex.toString());
                                    }
                                }
                            } else {
                                handler.postDelayed(runnableInitiatePreview, 500);
                            }
                        }
                    }
                };
                handler.postDelayed(runnableInitiatePreview, 500);
            }

        } catch (Exception e) {
            HelperMethods.logError("Failed to load camera resolutions.", e.toString());
        }
    }

    private void startPreview() {
        HelperMethods.logMessage("Checking aspect ratio...");
        setVideoSize();
        HelperMethods.logMessage("Starting preview...");
        VideoCaptureSource previewSourceTemp = ((CaptureService) GlobalContext.mainService).previewSource;
        if (previewSourceTemp != null) {
            return;
        }
        try {
            HelperMethods.logMessage("Resolution: " + videoWidth + "x" + videoHeight + " , Capture Resolution: " + cameraCaptureWidth + "x" + cameraCaptureHeight + " , Rotation: " + videoRotation + " , Bitrate: " + videoBitrate + " , FrameRate: " + AppConfig.getVideoFramerate());

            ((CaptureService) GlobalContext.mainService).previewSource = new VideoCaptureSource();
            final VideoCaptureSource previewSource = ((CaptureService) GlobalContext.mainService).previewSource;
            previewSource.addRef();
            SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceViewPreview);
            previewSource.isCameraEnabled(true);
            previewSource.cameraId(cameraDeviceId);
            previewSource.previewControl(sv);
            Size pMode = null;
            if (pMode == null) {
                previewSource.captureWidth(cameraCaptureWidth);
                previewSource.captureHeight(cameraCaptureHeight);
                HelperMethods.logMessage("Preview resolution set: " + cameraCaptureWidth + "x" + cameraCaptureHeight);
            } else {
                previewSource.captureWidth(pMode.getWidth());
                previewSource.captureHeight(pMode.getHeight());
                HelperMethods.logMessage("Preview resolution set: " + pMode.getWidth() + "x" + pMode.getHeight());
            }
            previewSource.cameraRotation(videoRotation);
            previewSource.framerate(new Rational(Integer.parseInt(AppConfig.getVideoFramerate())));

            previewSource.start();

            if (findViewById(R.id.btnRecord).getVisibility() == View.VISIBLE) {
                findViewById(R.id.btnUpload).setVisibility(View.INVISIBLE);
                findViewById(R.id.btnStreaming).setVisibility(View.INVISIBLE);
            } else {
                findViewById(R.id.btnUpload).setVisibility(View.VISIBLE);
                findViewById(R.id.btnStreaming).setVisibility(View.VISIBLE);
            }
        } catch (
                Exception ex) {
            HelperMethods.logError("Failed to start preview.", ex.toString());
            stopPreview();
        }
    }

    private void stopPreview() {
        try {
            HelperMethods.logMessage("Stopping preview...");
            if (GlobalContext.mainService != null) {
                HelperMethods.logMessage("Stopping and releasing service...");
                IVideoCaptureSource previewSource = ((CaptureService) GlobalContext.mainService).previewSource;
                if (previewSource != null) {
                    previewSource.release();
                    ((CaptureService) GlobalContext.mainService).previewSource = null;
                }
            }
            findViewById(R.id.btnUpload).setVisibility(View.INVISIBLE);
            findViewById(R.id.btnStreaming).setVisibility(View.INVISIBLE);
            //findViewById(R.id.btnSwitchCamera).setVisibility(View.INVISIBLE);
        } catch (Exception ex) {
            HelperMethods.logError("Failed to stop preview.", ex.toString());
        }
    }

    private int getDistance(MotionEvent event) {
        int x = (int) (event.getX(0) - event.getX(1));
        int y = (int) (event.getY(0) - event.getY(1));
        return (int) (Math.sqrt(x * x + y * y));
    }

    final static float move = 100;
    float ratio = 1.0f;
    int baseDist;
    float baseRatio;
    float prevDis;
    VideoCaptureSource previewSource;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {

            int action = event.getAction();
            int mainaction = action & MotionEvent.ACTION_MASK;
            if (mainaction == MotionEvent.ACTION_POINTER_DOWN) {
                previewSource = ((CaptureService) GlobalContext.mainService).previewSource;
                ZoomMax = previewSource.getMaxZoom();
                baseDist = getDistance(event);
                prevDis = baseDist;
                baseRatio = ratio;
            } else if (mainaction == MotionEvent.ACTION_MOVE) {
                float dist = getDistance(event);
                float scale = (dist - baseDist) / move;
                float factor = (float) Math.pow(2, scale);
                ratio = Math.min(1.0f, Math.max(ZoomMax, baseRatio * factor));
                if (prevDis > dist) {
                    Zoom = Zoom - ratio;
                } else {
                    Zoom = Zoom + ratio;
                }
                prevDis = dist;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (GlobalContext.lock) {
                            try {
                                if (previewSource.isZoomSupported()) {

                                    if (Zoom > 0 && Zoom <= ZoomMax) {
                                        previewSource.setZoom((int) Zoom);
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }).start();
                HelperMethods.logMessage(String.valueOf(ratio));
            }
        }
        return true;
    }
//    @Override
//    public boolean onTouchEvent(MotionEvent motionEvent) {
//        final VideoCaptureSource previewSource = ((CaptureService) GlobalContext.mainService).previewSource;
//        int action = motionEvent.getActionMasked();
//
//        switch (action) {
//            case MotionEvent.ACTION_DOWN: {
//                mLastTouchY = mPrevTouchY = motionEvent.getRawY();
//
//                break;
//            }
//            case MotionEvent.ACTION_MOVE: {
//
//                final float dy = motionEvent.getRawY();
//                if (dy > mLastTouchY) {
//                    mLastTouchY = dy;
//                    /* Move down */
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            synchronized (GlobalContext.lock) {
//                                try {
//                                    HelperMethods.logMessage("Setting zoom...");
//                                    if (previewSource.isZoomSupported()) {
//                                        ZoomMax = previewSource.getMaxZoom();
//                                        if(Zoom > 0) {
//                                            previewSource.setZoom(Zoom);
//                                            Zoom--;
//                                        }
//                                    }
//                                }
//                                catch (Exception e){
//                                }
//                            }
//                        }
//                    }).start();
//
//                } else if (dy < mLastTouchY) {
//                    mLastTouchY = dy;
//                    /* Move up */
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            synchronized (GlobalContext.lock) {
//                                try {
//                                    HelperMethods.logMessage("Setting zoom...");
//                                    if (previewSource.isZoomSupported()) {
//                                        ZoomMax = previewSource.getMaxZoom();
//                                        if(Zoom < ZoomMax) {
//                                            previewSource.setZoom(Zoom);
//                                            Zoom++;
//                                        }
//                                    }
//                                }
//                                catch (Exception e){
//                                }
//                            }
//                        }
//                    }).start();
//
//                }
//                break;
//            }
//            case MotionEvent.ACTION_CANCEL:
//            case MotionEvent.ACTION_OUTSIDE:
//            case MotionEvent.ACTION_UP: {
//                // snap page
//
//                break;
//            }
//        }
//
//        return true;
//    }

    public void initiateRecording() {
        HelperMethods.logMessage("Initiating recording...");
        if (currentRecordStatus == MyEnums.RecordStatus.Stop) {
            startRecording();
        } else {
            stopRecording(true);
        }
    }

    public void initiateStreaming() {
        HelperMethods.logMessage("Initiating streaming...");
        if (currentStreamingStatus == MyEnums.StreamingStatus.Stopped) {
            startStreaming();
            if (currentRecordStatus == MyEnums.RecordStatus.Stop) {
                startRecording();
            }
        } else {
            stopStreaming(true);
            if (currentRecordStatus == MyEnums.RecordStatus.Record) {
                stopRecording(true);
            }
        }
    }

    public void startRecording() {
        HelperMethods.logMessage("Starting recroding...");
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Date date = new Date();
            String dateTime = dateFormat.format(date);
            AppConfig.setEventStartedTime(dateTime);

            LockOrientation();
            if (currentRecordStatus != MyEnums.RecordStatus.Stop) {
                return;
            }
            HelperMethods.logMessage("Starting recording...");
            try {
                setVideoParams(1);
                HelperMethods.logMessage("Preparing file writing...");
                File folder = new File(Environment.getExternalStorageDirectory() + "/LiveFrom");
                if (!folder.exists()) {
                    folder.mkdir();
                }
                AppConfig.RecordingFile1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMMddHHmmss").format(date) + ".mp4";
                AppConfig.RecordingFile2 = Environment.getExternalStorageDirectory() + "/LiveFrom/" + new SimpleDateFormat("yyyyMMddHHmmss").format(date) + ".mp4";
                IMediaSink fileSink = new FileSink();
                fileSink.uri(AppConfig.RecordingFile1);
                fileSink.uri(AppConfig.RecordingFile2);

                CaptureService svc = (CaptureService) GlobalContext.mainService;
                svc.writerSession = new MediaSession();
                svc.writerSession.addSource(svc.commonVideoSource);
                svc.writerSession.addSource(svc.commonAudioSource);
                svc.writerSession.addSink(fileSink);
                svc.writerSession.addMediaStateListener(new MediaStateListener() {
                    @Override
                    public void onError(Object caller, String errorDescription, boolean isCritical) {
                        HelperMethods.showErrorMessage(MainActivity.this, "File writing error: " + errorDescription, "");
                        stopRecording(false);
                    }

                    @Override
                    public void onStateChanged(Object caller, MediaState state) {
                    }
                });
                svc.writerSession.start();
                startTimer();
                updateRecordStatus(MyEnums.RecordStatus.Record);
            } catch (Exception ex) {
                HelperMethods.showErrorMessage(this, "Unable to start capture.", ex.toString());
                stopRecording(true);
            }
        } catch (Exception ex) {
            HelperMethods.showErrorMessage(this, "Unable to start recording", ex.toString());
            updateRecordStatus(MyEnums.RecordStatus.Record);
            UnLockOrientation();
        }
    }

    public void startStreaming() {
        HelperMethods.logMessage("Starting streaming...");
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Date date = new Date();
            String dateTime = dateFormat.format(date);
            AppConfig.setEventStartedTime(dateTime);

            LockOrientation();
            if (currentStreamingStatus != MyEnums.StreamingStatus.Stopped) {
                return;
            }
            startTimer();
            updateStreamingStatus(MyEnums.StreamingStatus.Encoding);
            setupEvent();
        } catch (Exception ex) {
            HelperMethods.showErrorMessage(this, "Unable to start streaming", ex.toString());
            updateStreamingStatus(MyEnums.StreamingStatus.Stopped);
            UnLockOrientation();
        }
    }

    public void stopRecording(boolean isEventCompleted) {
        HelperMethods.logMessage("Stopping recording...");
        MyEnums.RecordStatus st = currentRecordStatus;
        if (currentStreamingStatus == MyEnums.StreamingStatus.Live) {
            CaptureService svc = (CaptureService) GlobalContext.mainService;
            if (svc.writerSession != null) {
                svc.writerSession.stop();
                svc.writerSession.close();
                svc.writerSession = null;
            }
            updateRecordStatus(MyEnums.RecordStatus.Stop);
            UnLockOrientation();
            return;
        }
        HelperMethods.logMessage("stopping capturing...");

        File retFile = FileLog.Copy("");

        try {
            HelperMethods.logMessage("Stopping and releasing common capture sources...");
            CaptureService svc = (CaptureService) GlobalContext.mainService;
            if (svc.captureDevice != null) {
                if (svc.captureDeviceListener != null) {
                    svc.captureDevice.removeEventListener(svc.captureDeviceListener);
                    svc.captureDeviceListener = null;
                }
                svc.captureDevice.release();
                svc.captureDevice = null;
            }
            if (svc.commonVideoSource != null) {
                svc.commonVideoSource.release();
                svc.commonVideoSource = null;
            }
            if (svc.commonAudioSource != null) {
                svc.commonAudioSource.release();
                svc.commonAudioSource = null;
            }

            if (svc.writerSession != null) {
                svc.writerSession.stop();
                svc.writerSession.close();
                svc.writerSession = null;
            }
        } catch (Exception e) {
            HelperMethods.logMessage("Failed to stop capturing. " + e.toString());
        }
        if (isEventCompleted) {
            if (st == MyEnums.RecordStatus.Record) {
                HelperMethods.logMessage("Completing current record event...");
                final File file1 = new File(AppConfig.RecordingFile1);
                final File file2 = new File(AppConfig.RecordingFile2);
                if (file1.exists() || file2.exists()) {
                    HelperMethods.showConfirmMessage(MainActivity.this, "Do you want to upload saved recording?.", "Yes", "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (AppConfig.CurrentEvent == null) {
                                setupBroadcastEvent();
                            }
                            String videoUri = "";
                            if (file1.exists()) {
                                videoUri = file1.getAbsolutePath();
                            } else if (file2.exists()) {
                                videoUri = file2.getAbsolutePath();
                            }
                            File file = new File(videoUri);
                            MediaScannerConnection.scanFile(getApplicationContext(),
                                    new String[]{file.getAbsolutePath()}, null,
                                    new MediaScannerConnection.OnScanCompletedListener() {
                                        public void onScanCompleted(String path, Uri uri) {
                                            String MediaPath = uri.getPath();
                                            MediaPath = "content://media" + MediaPath;
                                            Intent i = new Intent(MainActivity.this, TrimActivity.class);
                                            //videoUri = "/storage/emulated/0/LiveFrom/20191217144220.mp4";
                                            i.putExtra("videoUri", MediaPath);
                                            startActivityForResult(i, TRIM_ACTIVITY);
                                        }
                                    });
                        }
                    }, null);
                }
            } else {
                HelperMethods.logMessage("Deleting current incomplete broadcast event...");
            }
        }
        updateRecordStatus(MyEnums.RecordStatus.Stop);
        UnLockOrientation();
    }

    public void stopStreaming(boolean isEventCompleted) {
        HelperMethods.logMessage("Stopping streaming...");
        MyEnums.StreamingStatus st = currentStreamingStatus;
        stopBoardcast();
        if (isEventCompleted) {

            if (st == MyEnums.StreamingStatus.Live) {
                HelperMethods.logMessage("Completing current live broadcast event...");
                new APIUpdateEvent(mCredential, "complete").execute();
            } else {
                if (AppConfig.CurrentEvent != null && AppConfig.CurrentEvent.getStatus().getLifeCycleStatus().contains("ready")) {
                    HelperMethods.logMessage("Deleting current incomplete broadcast event...");
                    new APIDeleteEvent(mCredential).execute();
                }
            }
        }
        updateStreamingStatus(MyEnums.StreamingStatus.Stopped);
        UnLockOrientation();
    }

    public void setVideoParams(int caller) {
        try {
            int frameRate = Integer.parseInt(AppConfig.getVideoFramerate());
            if (caller == 2) {
                videoBitrate = videoBitrate - (videoStreamingFailedCounter * 1500000);
                if (videoBitrate < 1000000) {
                    videoBitrate = 1000000;
                }
                frameRate = frameRate - (videoStreamingFailedCounter * 10);
                if (frameRate < 10) {
                    frameRate = 10;
                }
            }
            HelperMethods.logMessage("FinalParameters: Resolution: " + videoWidth + "x" + videoHeight + " , Capture Resolution: " + cameraCaptureWidth + "x" + cameraCaptureHeight + " , Rotation: " + videoRotation + " , Bitrate: " + videoBitrate + " , FrameRate: " + frameRate);
            CaptureService svc = (CaptureService) GlobalContext.mainService;
            HelperMethods.logMessage("Setting common audio source...");
            svc.commonAudioSource = new AudioCaptureSource();
            svc.commonAudioSource.addRef();
            svc.commonAudioSource.deviceId(android.media.MediaRecorder.AudioSource.MIC);
            svc.commonAudioSource.codec(Codec_t.AAC);
            svc.commonAudioSource.channels(1);
            svc.commonAudioSource.sampleRate(44100);
            svc.commonAudioSource.bitrate(128000);
            svc.commonVideoSource = new VideoCaptureSource();
            svc.commonVideoSource.addRef();
            svc.commonVideoSource.codec(Codec_t.H264);
            svc.commonVideoSource.isScreenEnabled(false);
            svc.commonVideoSource.screenZOrder(0);

            HelperMethods.logMessage("Setting common video source...");
            svc.commonVideoSource.isManualVideoEnabled(false);
            svc.commonVideoSource.isCameraEnabled(true);
            svc.commonVideoSource.cameraId(cameraDeviceId);

            svc.commonVideoSource.captureWidth(cameraCaptureWidth);
            svc.commonVideoSource.captureHeight(cameraCaptureHeight);

            svc.commonVideoSource.cameraZOrder(1);
            svc.commonVideoSource.width(videoWidth);
            svc.commonVideoSource.height(videoHeight);
            svc.commonVideoSource.cameraRotation(videoRotation);
            svc.commonVideoSource.framerate(new Rational(frameRate));
            svc.commonVideoSource.keyframeInterval(1);
            svc.commonVideoSource.bitrate(videoBitrate);
            svc.commonVideoSource.profile(0);
            svc.commonVideoSource.level(0);

            HelperMethods.logMessage("Adding overlay items...");
            List<OverlayImage> overlayImages = new ArrayList<>();
            List<OverlayText> overlayTexts = new ArrayList<>();
            List<String> sources = new ArrayList<String>();
            List<OverlayItem> streamingOverlayItems = HelperMethods.findOverylayItemsByType(overlayItems, MyEnums.OverlayItemType.LiveStreaming);
            if (streamingOverlayItems != null && streamingOverlayItems.size() > 0) {
                for (OverlayItem oItem : streamingOverlayItems) {
                    if (sources.contains(oItem.Source)) {
                        continue;
                    }
                    if (oItem.Source.toLowerCase().startsWith("http")) {
                        sources.add(oItem.Source);
                        int lastIndSlash = oItem.Source.lastIndexOf("/");
                        int lastIndDot = oItem.Source.lastIndexOf(".");
                        String filename = oItem.Source.substring(lastIndSlash + 1);
                        String extension = oItem.Source.substring(lastIndDot).toLowerCase();
                        String fullFilename = new File(getFilesDir(), oItem.Type + "_" + filename).getPath();
                        if (extension.compareTo(".png") == 0 || extension.compareTo(".jpg") == 0 || extension.compareTo(".jpeg") == 0 || extension.compareTo(".gif") == 0) {
                            Size res = HelperMethods.getFileWidthAndHeight(fullFilename);
                            Rect rect = HelperMethods.getOverlayItemPosition(res, oItem.Position, videoWidth, videoHeight);
                            File fullFileFile = new File(fullFilename);
                            if (rect != null && fullFileFile.exists()) {

                                BufferedInputStream is = new BufferedInputStream(new FileInputStream(fullFileFile));

                                OverlayImage oi = new OverlayImage();
                                oi.location(rect);
                                oi.stream(is);
                                overlayImages.add(oi);
                            }
                        }
                    } else {
                        if (!TextUtils.isEmpty(oItem.Source)) {
                            String[] values = oItem.FontSize.split("\\|");
                            int fSize = Integer.parseInt(values[0].replace("px", ""));
                            int tWidth = Integer.parseInt(values.length > 1 ? values[1] : "0");
                            int tHeight = Integer.parseInt(values.length > 2 ? values[2] : "0");
                            fSize = fSize == 0 ? 20 : fSize;
                            Rect rect = HelperMethods.getOverlayItemPosition(new Size(tWidth, tHeight), oItem.Position, videoWidth, videoHeight);
                            Rect rect1 = new Rect(rect.left, rect.top + 15, 0, 0);
                            OverlayText oText = new OverlayText();
                            oText.text(oItem.Source);
                            oText.fontSize(fSize);
                            oText.fontBold(false);
                            oText.fontItalic(false);
                            oText.location(rect1);
                            oText.fontColor(Color.WHITE);
                            oText.outlineColor(Color.BLACK);
                            oText.outlineWidth(0);
                            overlayTexts.add(oText);
                        }
                    }
                }
            }
            if (overlayImages.size() > 0) {
                svc.commonVideoSource.overlayImages(overlayImages);
                HelperMethods.logMessage("Image overlay items added: " + overlayImages.size());
            }
            if (overlayTexts.size() > 0) {
                svc.commonVideoSource.overlayTexts(overlayTexts);
                HelperMethods.logMessage("Text overlay items added: " + overlayTexts.size());
            }
        } catch (Exception ex) {
            HelperMethods.showErrorMessage(this, "Unable to set video parameters.", ex.toString());
        }
    }

    public void startBoardcast() {
        HelperMethods.logMessage("Starting capturing...");
        try {
            setVideoParams(2);

            HelperMethods.logMessage("Starting stream session...");
            IMediaSink rtmpSink = new RtmpPublisherSink();
            rtmpSink.uri(youtubeServerURL + AppConfig.getStreamName());

            CaptureService svc = (CaptureService) GlobalContext.mainService;
            svc.streamingSession = new MediaSession();
            svc.streamingSession.addSource(svc.commonVideoSource);
            svc.streamingSession.addSource(svc.commonAudioSource);
            svc.streamingSession.addSink(rtmpSink);
            svc.streamingSession.addMediaStateListener(new MediaStateListener() {
                @Override
                public void onError(Object caller, String errorDescription, boolean isCritical) {
                    if (errorDescription.contains("Upload bandwidth of internet connection is too low")) {
                        stopStreaming(false);
                        videoStreamingFailedCounter++;
                        initiateStreaming();
                    } else {
                        HelperMethods.showErrorMessage(MainActivity.this, "Broadcast error: " + errorDescription, "");
                        stopStreaming(false);
                    }
                }

                @Override
                public void onStateChanged(Object caller, MediaState state) {
                }
            });


            svc.streamingSession.start();

            boolean isNewEvent = true;
            if (AppConfig.CurrentEvent != null) {
                String status = AppConfig.CurrentEvent.getStatus().getLifeCycleStatus();
                HelperMethods.logMessage("Current broadcast event status: " + status);
                if (status.contains("live")) {
                    HelperMethods.logMessage("Resuming live streaming......");
                    isNewEvent = false;
                    updateStreamingStatus(MyEnums.StreamingStatus.Live);
                }
            }

            if (isNewEvent) {
                new APIGetEvent(mCredential).execute();
                HelperMethods.logMessage("Transitioning live broadcast event...");
                handlerGoLive.postDelayed(new Runnable() {
                    public void run() {
                        boolean isReRun = true;
                        HelperMethods.logMessage("Runnable for go live...");
                        if (AppConfig.CurrentEvent != null) {
                            if (currentStreamingStatus != MyEnums.StreamingStatus.Stopped) {
                                if (currentStreamingStatus == MyEnums.StreamingStatus.Encoding) {
                                    String status = AppConfig.CurrentEvent.getStatus().getLifeCycleStatus();
                                    HelperMethods.logMessage("Current broadcast event status: " + status);
                                    if (!status.contains("test") && !status.contains("live")) {
                                        HelperMethods.logMessage("Transitioning to 'testing'...");
                                        new APIUpdateEvent(mCredential, "testing").execute();
                                    } else if (currentStreamingStatus != MyEnums.StreamingStatus.Stopped && status.contains("test")) {
                                        HelperMethods.logMessage("Transitioning to 'live'...");
                                        new APIUpdateEvent(mCredential, "live").execute();
                                    } else if (currentStreamingStatus != MyEnums.StreamingStatus.Stopped && status.contains("live")) {
                                        updateStreamingStatus(MyEnums.StreamingStatus.Live);
//                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//                                        Date date = new Date();
//                                        String dateTime = dateFormat.format(date);
//                                        AppConfig.setEventStartedTime(dateTime);
//                                        startStreamingTimer();
                                        isReRun = false;
                                    }
                                }
                            }
                        }
                        if (currentStreamingStatus != MyEnums.StreamingStatus.Stopped && isReRun) {
                            HelperMethods.logMessage("Transition couldn't complete. Trying again...");
                            handlerGoLive.postDelayed(this, 5000);
                        } else {
                            HelperMethods.logMessage("Transition for broadcast event completed...");
                        }
                    }
                }, 5000);
            }

        } catch (Exception ex) {
            HelperMethods.showErrorMessage(this, "Unable to start capture.", ex.toString());
            stopStreaming(true);
        }
    }

    public void stopBoardcast() {
        if (currentRecordStatus == MyEnums.RecordStatus.Record) {
            CaptureService svc = (CaptureService) GlobalContext.mainService;
            if (svc.streamingSession != null) {
                svc.streamingSession.stop();
                svc.streamingSession.close();
                svc.streamingSession = null;
            }
            return;
        }
        HelperMethods.logMessage("stopping capturing...");

        File retFile = FileLog.Copy("");

        try {
            HelperMethods.logMessage("Stopping and releasing common capture sources...");
            CaptureService svc = (CaptureService) GlobalContext.mainService;
            if (svc.captureDevice != null) {
                if (svc.captureDeviceListener != null) {
                    svc.captureDevice.removeEventListener(svc.captureDeviceListener);
                    svc.captureDeviceListener = null;
                }
                svc.captureDevice.release();
                svc.captureDevice = null;
            }
            if (svc.commonVideoSource != null) {
                svc.commonVideoSource.release();
                svc.commonVideoSource = null;
            }
            if (svc.commonAudioSource != null) {
                svc.commonAudioSource.release();
                svc.commonAudioSource = null;
            }
            if (svc.streamingSession != null) {
                svc.streamingSession.stop();
                svc.streamingSession.close();
                svc.streamingSession = null;
            }
        } catch (Exception e) {
            HelperMethods.logMessage("Failed to stop capturing. " + e.toString());
        }
    }
    // Youtube data api v3 code

    private void setupBroadcastEvent() {
        new APISetupEvent(mCredential).execute();
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        HelperMethods.logMessage("Choosing google account...");
        String accountName = getPreferences(Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccount(new Account(accountName, getPackageName()));
            initiatePreview();
        } else {
            startActivityForResult(
                    mCredential.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 10:
                String abc = data.getData().toString();
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    HelperMethods.showErrorMessage(this, "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.", "");
                } else {
                    initiatePreview();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccount(new Account(accountName, getPackageName()));
                        initiatePreview();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    initiatePreview();
                }
                break;
            case TRIM_ACTIVITY:
                final File uploadFile = new File(AppConfig.TrimRecordingFile1);
                if (uploadFile.exists()) {
                    new APIUploadVideo(mCredential, uploadFile).execute();
                    Toast.makeText(getApplicationContext(), AppConfig.TrimRecordingFile1, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    private boolean isDeviceOnline() {
        HelperMethods.logMessage("Checking if device is online...");
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        HelperMethods.logMessage("Checking if google play services available...");
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        HelperMethods.logMessage("Acquiring google play services...");
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private class APISetupEvent extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        APISetupEvent(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(AppConfig.AppName)
                    .build();
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                Calendar StartDate = Calendar.getInstance();
                StartDate.add(Calendar.DATE, 2);
                Calendar EndDate = Calendar.getInstance();
                EndDate.add(Calendar.DATE, 3);
                String sDate = android.text.format.DateFormat.format("yyyy-MM-dd'T'HH:mm:ss.000'Z'", new java.util.Date(StartDate.getTimeInMillis() + 300000)).toString();
                String eDate = android.text.format.DateFormat.format("yyyy-MM-dd'T'HH:mm:ss.000'Z'", new java.util.Date(EndDate.getTimeInMillis() + 600000)).toString();

                LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
                broadcastSnippet.setTitle(AppConfig.getVideoTitle());
                broadcastSnippet.setDescription(AppConfig.getVideoDescription());
                broadcastSnippet.setScheduledStartTime(new DateTime(sDate));
                broadcastSnippet.setScheduledEndTime(new DateTime(eDate));
                LiveBroadcastStatus status = new LiveBroadcastStatus();
                status.setPrivacyStatus("public");

                LiveBroadcast broadcast = new LiveBroadcast();
                broadcast.setKind("youtube#liveBroadcast");
                broadcast.setSnippet(broadcastSnippet);
                broadcast.setStatus(status);

                YouTube.LiveBroadcasts.Insert liveBroadcastInsert =
                        mService.liveBroadcasts().insert("snippet,status", broadcast); //.setOnBehalfOfContentOwnerChannel(AppConfig.getChannelId());
                LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();

                // Set Tags
                Video videoObjectDefiningMetadata = new Video();
                videoObjectDefiningMetadata.setId(returnedBroadcast.getId());


                VideoSnippet snippet = new VideoSnippet();
                snippet.setTitle(AppConfig.getVideoTitle());
                snippet.setDescription(AppConfig.getVideoDescription());
                snippet.setTags(Arrays.asList(new String[]{AppConfig.getVideoHashTag()}));
                snippet.setCategoryId("24");//Entartainment
                videoObjectDefiningMetadata.setSnippet(snippet);

                String PARTS = "snippet";

                YouTube.Videos.Update videoUpdate = mService.videos().update(
                        PARTS,
                        videoObjectDefiningMetadata);
                videoUpdate.execute();


                LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
                streamSnippet.setTitle(AppConfig.getVideoTitle() + "-stream");

                CdnSettings cdnSettings = new CdnSettings();
                cdnSettings.setFormat(AppConfig.getStreamResolution());
                cdnSettings.setIngestionType("rtmp");

                LiveStream stream = new LiveStream();
                stream.setKind("youtube#liveStream");
                stream.setSnippet(streamSnippet);
                stream.setCdn(cdnSettings);

                YouTube.LiveStreams.Insert liveStreamInsert =
                        mService.liveStreams().insert("snippet,cdn", stream); //.setOnBehalfOfContentOwnerChannel(AppConfig.getChannelId());
                LiveStream returnedStream = liveStreamInsert.execute();

                YouTube.LiveBroadcasts.Bind liveBroadcastBind =
                        mService.liveBroadcasts().bind(returnedBroadcast.getId(), "id,contentDetails,status"); //.setOnBehalfOfContentOwnerChannel(AppConfig.getChannelId());
                liveBroadcastBind.setStreamId(returnedStream.getId());
                returnedBroadcast = liveBroadcastBind.execute();

                List<String> res = new ArrayList<String>();
                res.add("Broadcast ID: " + returnedBroadcast.getId());
                res.add("StreamID: " + returnedStream.getId());
                res.add(returnedStream.getCdn().getIngestionInfo().getStreamName());
                AppConfig.setEventId(returnedBroadcast.getId());
                AppConfig.setStreamName(returnedStream.getCdn().getIngestionInfo().getStreamName());
                AppConfig.CurrentEvent = returnedBroadcast;

                return res;
            } catch (Exception e) {
                HelperMethods.logMessage("API Exception: " + e.toString());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            HelperMethods.logMessage("API: Creating broadcast event using google account: " + mCredential.getSelectedAccountName());
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                HelperMethods.showErrorMessage(MainActivity.this, "No results returned.", "API: Failed to create broadcast event...");
                updateStreamingStatus(MyEnums.StreamingStatus.Stopped);
                UnLockOrientation();
            } else {
                HelperMethods.logMessage("API: Creating broadcast event done: Response: \n" + TextUtils.join("\n", output));
//                startBoardcast();
                new APIUploadThumbnail(mCredential).execute();
            }
        }

        @Override
        protected void onCancelled() {
            HelperMethods.logMessage("API: Creating broadcast event cancelled...");
            mProgress.hide();
            updateStreamingStatus(MyEnums.StreamingStatus.Stopped);
            UnLockOrientation();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    HelperMethods.showErrorMessage(MainActivity.this, "Following error occurred. " + mLastError.getMessage(), mLastError.getMessage());
                }
            } else {
                HelperMethods.showErrorMessage(MainActivity.this, "Request cancelled", "");
            }
        }
    }

    private class APIUploadThumbnail extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        APIUploadThumbnail(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(AppConfig.AppName)
                    .build();
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {


                if (TextUtils.isEmpty(AppConfig.getThumbnailFilename())) {
                    return null;
                }

                final String IMAGE_FILE_FORMAT = "image/png";

                File imageFile = new File(AppConfig.getThumbnailFilename()); // Thumbnail image

                if (!imageFile.exists()) {
                    return null;
                }

                InputStreamContent mediaContent = new InputStreamContent(
                        IMAGE_FILE_FORMAT, new BufferedInputStream(new FileInputStream(imageFile)));
                mediaContent.setLength(imageFile.length());

                YouTube.Thumbnails.Set thumbnailSet = mService.thumbnails().set(AppConfig.getEventId(), mediaContent);

                // Set the upload type and add an event listener.
                MediaHttpUploader uploader = thumbnailSet.getMediaHttpUploader();

                // one try request. set false to make it resumeable.
                uploader.setDirectUploadEnabled(true);

//                // Set the upload state for the thumbnail image.
//                MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
//                    @Override
//                    public void progressChanged(MediaHttpUploader uploader) throws IOException {
//                        switch (uploader.getUploadState()) {
//                            // This value is set before the initiation request is
//                            // sent.
//                            case INITIATION_STARTED:
//                                System.out.println("Initiation Started");
//                                break;
//                            // This value is set after the initiation request
//                            //  completes.
//                            case INITIATION_COMPLETE:
//                                System.out.println("Initiation Completed");
//                                break;
//                            // This value is set after a media file chunk is
//                            // uploaded.
//                            case MEDIA_IN_PROGRESS:
//                                System.out.println("Upload in progress");
//                                System.out.println("Upload percentage: " + uploader.getProgress());
//                                break;
//                            // This value is set after the entire media file has
//                            //  been successfully uploaded.
//                            case MEDIA_COMPLETE:
//                                System.out.println("Upload Completed!");
//                                startBoardcast();
//                                break;
//                            // This value indicates that the upload process has
//                            //  not started yet.
//                            case NOT_STARTED:
//                                System.out.println("Upload Not Started!");
//                                break;
//                        }
//                    }
//                };
//                uploader.setProgressListener(progressListener);

                // Upload the image and set it as the specified video's thumbnail.
                ThumbnailSetResponse setResponse = thumbnailSet.execute();

                return null;

            } catch (Exception e) {
                HelperMethods.logMessage("API Exception (Setting thumbnail): " + e.toString());
                updateStreamingStatus(MyEnums.StreamingStatus.Stopped);
                UnLockOrientation();
                mLastError = e;
                cancel(true);
                return null;
            }

        }

        @Override
        protected void onPreExecute() {
            HelperMethods.logMessage("API: setting thumbnail...");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            HelperMethods.logMessage("Starting broadcast from thumbnail...");
            startBoardcast();
        }
    }

    private class APIDeleteEvent extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        APIDeleteEvent(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(AppConfig.AppName)
                    .build();
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                YouTube.LiveBroadcasts.Delete liveBroadcastEvent =
                        mService.liveBroadcasts().delete(AppConfig.getEventId()); //.setOnBehalfOfContentOwnerChannel(AppConfig.getChannelId());
                liveBroadcastEvent.execute();
                return null;
            } catch (Exception e) {
                HelperMethods.logMessage("API Exception: " + e.toString());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            HelperMethods.logMessage("API: Deleting broadcast event...");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
        }
    }

    private class APIUpdateEvent extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;
        private String newStatus = "";

        APIUpdateEvent(GoogleAccountCredential credential, String status) {
            newStatus = status;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(AppConfig.AppName)
                    .build();
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                YouTube.LiveBroadcasts.Transition liveBroadcastEvent =
                        mService.liveBroadcasts().transition(newStatus, AppConfig.getEventId(), "snippet,status"); //.setOnBehalfOfContentOwnerChannel(AppConfig.getChannelId());
                AppConfig.CurrentEvent = liveBroadcastEvent.execute();
                return null;
            } catch (Exception e) {
                HelperMethods.logMessage("API Exception: " + e.toString());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            HelperMethods.logMessage("API: Transitioning broadcast event to " + newStatus + "...");

//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
        }
    }

    private class APIGetEvent extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        APIGetEvent(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(AppConfig.AppName)
                    .build();
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                try {
                    YouTube.LiveBroadcasts.List liveBroadcastEvent =
                            mService.liveBroadcasts().list("snippet,contentDetails,status").setId(AppConfig.getEventId()); //.setOnBehalfOfContentOwnerChannel(AppConfig.getChannelId());
                    LiveBroadcastListResponse res = liveBroadcastEvent.execute();
                    if (res.getItems().size() > 0) {
                        AppConfig.CurrentEvent = res.getItems().get(0);
                    }
                } catch (Exception e) {
                    HelperMethods.logMessage("API Exception: " + e.toString());
                }
                return null;
            } catch (Exception e) {
                HelperMethods.logMessage("API Exception: " + e.toString());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            HelperMethods.logMessage("API: Getting broadcast event information...");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
        }
    }

    private class APIGetChannels extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        APIGetChannels(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(AppConfig.AppName)
                    .build();
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                try {
                    YouTube.Channels.List channelsList = mService.channels().list("snippet,contentDetails,statistics").setMine(true);
                    ChannelListResponse channels = channelsList.execute();
                    if (channels.getItems().size() > 0) {
                        List<String> res = new ArrayList<String>();
                        List<Channel> channelItems = channels.getItems();
                        for (int i = 0; i < channelItems.size(); i++) {
                            Channel channel = channelItems.get(i);
                            res.add(channel.getId() + ": " + channel.getSnippet().getTitle());
                        }
                        return res;
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    HelperMethods.logMessage("API Exception: " + e.toString());
                }
                return null;
            } catch (Exception e) {
                HelperMethods.logMessage("API Exception: " + e.toString());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            HelperMethods.logMessage("API: Getting channels list...");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(final List<String> output) {
            mProgress.hide();
            if (output != null) {
                if (output.size() == 1) {
                    AppConfig.setChannelId(output.get(0));
                    initiatePreview();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Choose channel");
                    String[] channelsData = new String[output.size()];
                    for (int i = 0; i < output.size(); i++) {
                        channelsData[i] = output.get(i);
                    }
                    int checkedItem = 0;
                    builder.setSingleChoiceItems(channelsData, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // user checked an item
                        }
                    });

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppConfig.setChannelId(output.get(which));
                            initiatePreview();
                        }
                    });
                    builder.setNegativeButton("Cancel", null);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            } else {
                HelperMethods.showErrorMessage(MainActivity.this, "Unable to get channels info.", "");
            }
        }
    }

    private class APIUploadVideo extends AsyncTask<Void, Void, PostResult> {
        File fileToUpload = null;
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        APIUploadVideo(GoogleAccountCredential credential, File file) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(AppConfig.AppName)
                    .build();
            fileToUpload = file;
        }

        @Override
        protected PostResult doInBackground(Void... params) {
            PostResult result = new PostResult();
            try {
                String PRIVACY_STATUS = "unlisted"; // or public,private
                String PARTS = "snippet,status,contentDetails";

                String videoId = null;
                try {
                    Video videoObjectDefiningMetadata = new Video();
                    videoObjectDefiningMetadata.setStatus(new VideoStatus().setPrivacyStatus(PRIVACY_STATUS));

                    VideoSnippet snippet = new VideoSnippet();
                    snippet.setTitle(AppConfig.getVideoTitle());
                    snippet.setDescription(AppConfig.getVideoDescription());
                    snippet.setTags(Arrays.asList(new String[]{AppConfig.getVideoHashTag()}));
                    videoObjectDefiningMetadata.setSnippet(snippet);

                    YouTube.Videos.Insert videoInsert = mService.videos().insert(
                            PARTS,
                            videoObjectDefiningMetadata,
                            getMediaContent(fileToUpload));

                    MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
                    uploader.setDirectUploadEnabled(false);

                    MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                        public void progressChanged(MediaHttpUploader uploader) throws IOException {
                            HelperMethods.logMessage("progressChanged: " + uploader.getUploadState());
                            switch (uploader.getUploadState()) {
                                case INITIATION_STARTED:
                                    break;
                                case INITIATION_COMPLETE:
                                    break;
                                case MEDIA_IN_PROGRESS:
                                    break;
                                case MEDIA_COMPLETE:
                                case NOT_STARTED:
                                    HelperMethods.logMessage("progressChanged: upload_not_started");
                                    break;
                            }
                        }
                    };
                    uploader.setProgressListener(progressListener);

                    HelperMethods.logMessage("Uploading video...");
                    Video returnedVideo = videoInsert.execute();
                    HelperMethods.logMessage("Video uploaded...");
                    videoId = returnedVideo.getId();
                    HelperMethods.logMessage(String.format("videoId = [%s]", videoId));
                } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                    result.Success = false;
                    result.Messages.add(availabilityException.getMessage());
                    HelperMethods.logError("Video UploadError: GooglePlayServicesAvailabilityIOException", availabilityException.getMessage());
                } catch (UserRecoverableAuthIOException userRecoverableException) {
                    result.Success = false;
                    result.Messages.add(userRecoverableException.getMessage());
                    HelperMethods.logError("Video UploadError: UserRecoverableAuthIOException", userRecoverableException.getMessage());
                } catch (IOException e) {
                    result.Success = false;
                    result.Messages.add(e.getMessage());
                    HelperMethods.logError("Video UploadError: IOException", e.getMessage());
                }
                if (videoId != null) {
                    result.Success = true;
                    result.Messages.add(videoId);
                }
                return result;
            } catch (Exception e) {
                result.Success = false;
                result.Messages.add(e.getMessage());
                HelperMethods.logMessage("API Exception: " + e.toString());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            HelperMethods.logMessage("API: Uploading video using google account: " + mCredential.getSelectedAccountName());
            mProgress.setMessage("Uploading video...");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(PostResult result) {
            mProgress.hide();
            if (result.Success == false) {
                HelperMethods.showErrorMessage(MainActivity.this, result.Messages.get(0), result.Messages.get(0));
            } else {
                HelperMethods.logMessage("API: Uploading video done: Response: \n" + result.Messages.get(0));
            }
        }

        @Override
        protected void onCancelled() {
            HelperMethods.logMessage("API: Uploading video cancelled...");
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    HelperMethods.showErrorMessage(MainActivity.this, "Following error occurred. " + mLastError.getMessage(), mLastError.getMessage());
                }
            } else {
                HelperMethods.showErrorMessage(MainActivity.this, "Request cancelled", "");
            }
        }
    }

    private AbstractInputStreamContent getMediaContent(File file) throws FileNotFoundException {
        InputStreamContent mediaContent = new InputStreamContent(
                "video/*",
                new BufferedInputStream(new FileInputStream(file)));
        mediaContent.setLength(file.length());
        return mediaContent;
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}