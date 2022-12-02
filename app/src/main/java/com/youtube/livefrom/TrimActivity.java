package com.youtube.livefrom;

import static com.youtube.livefrom.MainActivity.PROGRESS_BAR_ACTIVITY;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;

public class TrimActivity extends AppCompatActivity {

    Uri uri;
    ImageView imageView;
    VideoView videoView;
    TextView textViewLeft, textViewRight;
    RangeSeekBar rangeSeekBar;
    ImageButton btnTrim;
    boolean isPlaying = false;
    int duration;
    String filePrefix;
    String[] command;
    File dest;
    String original_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        imageView = (ImageView)findViewById(R.id.pause);
        videoView = (VideoView)findViewById(R.id.videoView);
        textViewLeft = (TextView)findViewById(R.id.tvvLeft);
        textViewRight = (TextView)findViewById(R.id.tvvRight);
        rangeSeekBar = (RangeSeekBar)findViewById(R.id.seekbar);
        btnTrim = (ImageButton) findViewById(R.id.btnTrim);

        Intent i = getIntent();
        if(i!= null){
            uri = Uri.parse(i.getStringExtra("videoUri"));
        }
        videoView.setVideoURI(uri);
        imageView.setImageResource(R.drawable.ic_play);
        setListners();
    }

    private void setListners(){
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying){
                    imageView.setImageResource(R.drawable.ic_play);
                    videoView.pause();
                    isPlaying = false;
                }
                else{
                    videoView.start();
                    imageView.setImageResource(R.drawable.ic_pause);
                    isPlaying = true;
                }
            }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
            @Override
            public  void onPrepared(MediaPlayer mediaPlayer){
                duration = mediaPlayer.getDuration()/1000;
                textViewLeft.setText("00:00:00");
                textViewRight.setText(getTime(mediaPlayer.getDuration()/1000));
                mediaPlayer.setLooping(true);
                rangeSeekBar.setRangeValues(0, duration);
                rangeSeekBar.setSelectedMaxValue(duration);
                rangeSeekBar.setSelectedMinValue(0);
                rangeSeekBar.setEnabled(true);

                rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Number minValue, Number maxValue) {
                        videoView.seekTo((int)minValue*1000);

                        textViewLeft.setText(getTime((int)bar.getAbsoluteMinValue()));
                        textViewRight.setText(getTime((int)bar.getAbsoluteMaxValue()));
                    }
                });
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(videoView.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue().intValue()*1000){
                            videoView.seekTo(rangeSeekBar.getSelectedMinValue().intValue()*1000);
                        }
                    }
                }, 1000);
            }
        });
        btnTrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout linearLayout = new LinearLayout(TrimActivity.this);
                linearLayout.setOrientation(linearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(50, 0, 50, 100);
                final EditText input = new EditText(TrimActivity.this);
                input.setLayoutParams(lp);
                input.setGravity(Gravity.TOP|Gravity.START);
                input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                linearLayout.addView(input, lp);

                filePrefix = android.text.format.DateFormat.format("yyyyMMddhhmmss", new java.util.Date()).toString();
                trimVideo(rangeSeekBar.getSelectedMinValue().intValue()*1000, rangeSeekBar.getSelectedMaxValue().intValue()*1000, filePrefix);

                Intent myIntent = new Intent(TrimActivity.this, ProgressBarActivity.class);
                myIntent.putExtra("duration", duration);
                myIntent.putExtra("command", command);
                myIntent.putExtra("destination", dest.getAbsolutePath());
                startActivityForResult(myIntent, PROGRESS_BAR_ACTIVITY);
            }
        });
    }
    private String getTime(int seconds){
        int hr = seconds/3600;
        int rem = seconds % 3600;
        int mn = rem/60;
        int sec = rem % 60;
        return String.format("%02d",hr) + ":"+String.format("%02d",mn) + ":"+String.format("%02d",sec);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void trimVideo(int startMs, int endMs, String fileName){
        File folder = new File(Environment.getExternalStorageDirectory() + "/LiveFrom/Trim");
        if(!folder.exists()){
            folder.mkdir();
        }
        filePrefix = fileName;
        String fileExt = ".mp4";
        dest = new File(folder, filePrefix + fileExt);
        AppConfig.TrimRecordingFile1 = dest.getAbsolutePath();
        original_path = getRealPathFromUri(getApplicationContext(), uri);
        duration = (endMs - startMs)/1000;
        command = new String[]{"-ss", ""+startMs/1000, "-y", "-i", original_path, "-t", ""+(endMs-startMs)/1000, "-vcodec", "mpeg4", "-b:v", "2097152", "-b:a", "48000", "-ac", "2", "-ar", "22050", dest.getAbsolutePath()};
    }
    private String getRealPathFromUri(Context context, Uri contentUri){
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch(Exception e){
            e.printStackTrace();
            return "";
        }
        finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.trim_menu, menu);
        return  true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROGRESS_BAR_ACTIVITY) {
            finish();
        }
    }

}
