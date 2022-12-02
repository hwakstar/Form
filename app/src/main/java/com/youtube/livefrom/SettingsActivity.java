package com.youtube.livefrom;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.vastreaming.common.FileLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class SettingsActivity extends AppCompatActivity {

    String picturePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText tbxJsonUrl = findViewById(R.id.tbxJsonURL);
        tbxJsonUrl.setText(AppConfig.getJsonURL());

        EditText tbxVideoTitle = findViewById(R.id.tbxVideoTitle);
        tbxVideoTitle.setText(AppConfig.getVideoTitle());

        EditText tbxVideoHashTag = findViewById(R.id.tbxHashTag);
        tbxVideoHashTag.setText(AppConfig.getVideoHashTag());

        EditText tbxVideoDescription = findViewById(R.id.tbxDescription);
        tbxVideoDescription.setText(AppConfig.getVideoDescription());

        EditText tbxVideoBitrate = findViewById(R.id.tbxVideoBitrate);
        tbxVideoBitrate.setText(AppConfig.getVideoBitrate());

        EditText tbxVideoFrameRate = findViewById(R.id.tbxVideoFrameRate);
        tbxVideoFrameRate.setText(AppConfig.getVideoFramerate());

        ImageView thumbnailView = findViewById(R.id.ThumbnailView);
        picturePath = AppConfig.getThumbnailFilename();
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        thumbnailView.setImageBitmap(bitmap);

        Spinner dropdown = findViewById(R.id.spinner1);
        String[] items = new String[AppConfig.CamSizes.length];
        for (int i = 0; i < items.length; i++) {
            Size size = AppConfig.CamSizes[i];
            items[i] = (size.getWidth() + "x" + size.getHeight());
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        if (!AppConfig.getCameraResolution().isEmpty()) {
            dropdown.setSelection(adapter.getPosition(AppConfig.getCameraResolution()));
        } else {
            dropdown.setSelection(0);
        }
        if(!TextUtils.isEmpty(AppConfig.getCameraResolution())){
            setBitrateRange(Integer.parseInt(AppConfig.getCameraResolution().split("x")[1]));
        }
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                setBitrateRange(Integer.parseInt(adapter.getItem(position).split("x")[1]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
        if(dropdown.getSelectedItem() == null){
            dropdown.setSelection(0);
        }
        Object sItem = dropdown.getSelectedItem();
        setStreamResolution(Integer.parseInt(sItem.toString().split("x")[1]));
        TextView lblStreamResolution = findViewById(R.id.tbxStreamResolution);
        lblStreamResolution.setText("Stream Resolution: " + AppConfig.getStreamResolution());
        doSaveSettings();
    }

    public void setBitrateRange(int resolution) {



        int minBitrate = 0;
        int maxBitrate = 0;
        if(resolution <= 240){
            AppConfig.setStreamResolution("240p");
            minBitrate = 300;
            maxBitrate = 700;
        }
        else if(resolution <= 360) {
            AppConfig.setStreamResolution("360p");
            minBitrate = 400;
            maxBitrate = 1000;
        }
        else if(resolution <= 480) {
            AppConfig.setStreamResolution("480p");
            minBitrate = 500;
            maxBitrate = 2000;
        }
        else if(resolution <= 720) {
            AppConfig.setStreamResolution("720p");
            minBitrate = 1500;
            maxBitrate = 4000;
        }
        else if(resolution <= 1080) {
            AppConfig.setStreamResolution("1080p");
            minBitrate = 3000;
            maxBitrate = 6000;
        }
        else if(resolution <= 1440) {
            AppConfig.setStreamResolution("1440p");
            minBitrate = 6000;
            maxBitrate = 13000;
        }
        if (minBitrate > 0 || maxBitrate > 0) {
            TextView lblVideoBitrate = findViewById(R.id.lblVideoBitrate);
            lblVideoBitrate.setText("Video Bitrate ( " + minBitrate + " - " + maxBitrate + " kb)");
            EditText tbxVideoBitrate = findViewById(R.id.tbxVideoBitrate);
            //tbxVideoBitrate.setVisibility(View.VISIBLE);
            String oldValue = tbxVideoBitrate.getText().toString();
            if(!TextUtils.isEmpty(oldValue)){
                int oldV = Integer.parseInt(oldValue);
                String newVal = Integer.toString (minBitrate + ((maxBitrate - minBitrate) / 2));
                if(oldV > maxBitrate || oldV < minBitrate){
                    tbxVideoBitrate.setText(newVal);
                }
            }
        }
    }

    public void setStreamResolution(int resolution) {

        if(resolution <= 240){
            AppConfig.setStreamResolution("240p");
        }
        else if(resolution <= 360) {
            AppConfig.setStreamResolution("360p");
        }
        else if(resolution <= 480) {
            AppConfig.setStreamResolution("480p");
        }
        else if(resolution <= 720) {
            AppConfig.setStreamResolution("720p");
        }
        else if(resolution <= 1080) {
            AppConfig.setStreamResolution("1080p");
        }
        else if(resolution <= 1440) {
            AppConfig.setStreamResolution("1440p");
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void saveSettings(View v) {
        doSaveSettings();
        HelperMethods.showMessage(this, "Settings saved successfully.");
    }

    private  void doSaveSettings(){

        EditText tbxJsonUrl = findViewById(R.id.tbxJsonURL);
        EditText tbxVideoTitle = findViewById(R.id.tbxVideoTitle);
        EditText tbxHashTag = findViewById(R.id.tbxHashTag);
        EditText tbxDescription = findViewById(R.id.tbxDescription);
        EditText tbxVideoBitrate = findViewById(R.id.tbxVideoBitrate);
        EditText tbxVideoFrameRate = findViewById(R.id.tbxVideoFrameRate);
        AppConfig.setJsonURL(tbxJsonUrl.getText().toString());
        AppConfig.setVideoTitle(tbxVideoTitle.getText().toString());
        AppConfig.setVideoHashTag(tbxHashTag.getText().toString());
        AppConfig.setVideoDescription(tbxDescription.getText().toString());
        String vRate = tbxVideoBitrate.getText().toString();
        if (TextUtils.isEmpty(vRate)) {
            vRate = "0";
        }
        String vFrameRate = tbxVideoFrameRate.getText().toString();
        if (TextUtils.isEmpty(vFrameRate)) {
            vFrameRate = "0";
        }
        Spinner dropdown = findViewById(R.id.spinner1);
        AppConfig.setCameraResolution(dropdown.getSelectedItem().toString());
        AppConfig.setVideoBitrate(vRate);
        AppConfig.setVideoFramerate(vFrameRate);
        AppConfig.setThumbnailFilename(picturePath);

        //Abid, Hardcode for testing
        AppConfig.setCameraResolution("1920x1080");
        AppConfig.setVideoBitrate("4500");
        AppConfig.setVideoFramerate("30");

        //Abid commented this
        //setStreamResolution(Integer.parseInt(dropdown.getSelectedItem().toString().split("x")[1]));

        TextView lblStreamResolution = findViewById(R.id.tbxStreamResolution);
        lblStreamResolution.setText("Stream Resolution: " + AppConfig.getStreamResolution());
    }

    public void downloadOverlay(View v) {
        EditText tbxJsonUrl = findViewById(R.id.tbxJsonURL);
        String filename = new File(AppController.getAppContext().getFilesDir(), AppConfig.JsonConfigFilename).getPath();
        new DownloadManager(this, new String[]{filename}, true).execute(tbxJsonUrl.getText().toString());
    }

    public void PickImage(View v) {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, 1);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();


            ImageView thumbnailView = findViewById(R.id.ThumbnailView);

            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            thumbnailView.setImageBitmap(bitmap);

            //InputStream inputStream = context.getContentResolver().openInputStream(data.getData());
            //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
        }
    }

}
