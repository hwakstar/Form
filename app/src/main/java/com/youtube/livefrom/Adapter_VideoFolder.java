package com.youtube.livefrom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

public class Adapter_VideoFolder extends RecyclerView.Adapter<Adapter_VideoFolder.ViewHolder> {

    ArrayList<Model_Video> al_video;
    Context context;
    Activity activity;

    public Adapter_VideoFolder(Context context, ArrayList<Model_Video> al_video, Activity activity) {
        this.al_video = al_video;
        this.context = context;
        this.activity = activity;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView iv_image;
        public ViewHolder(View v) {
            super(v);
            iv_image = (ImageView) v.findViewById(R.id.iv_image);
        }
    }

    @Override
    public Adapter_VideoFolder.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_videos, parent, false);
        ViewHolder viewHolder1 = new ViewHolder(view);
        return viewHolder1;
    }

    @Override
    public void onBindViewHolder(final ViewHolder Vholder, final int position) {
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        Vholder.iv_image.getLayoutParams().width = (width/AppConfig.ColumnCount)-5;
        Vholder.iv_image.getLayoutParams().height = (width/AppConfig.ColumnCount)-5;
        Glide.with(context).load("file://" + al_video.get(position).getStr_thumb())
                .skipMemoryCache(false)
                .into(Vholder.iv_image);

        Vholder.iv_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_gallery = new Intent(context,MainActivity.class);
                intent_gallery.putExtra("video",al_video.get(position).getStr_path());
                activity.startActivity(intent_gallery);

                String videoUri = al_video.get(position).getStr_path();

                File file = new File(videoUri);
                MediaScannerConnection.scanFile(context,
                    new String[] { file.getAbsolutePath() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            String MediaPath = uri.getPath();
                            MediaPath = "content://media" + MediaPath;
                            Intent i = new Intent(context, TrimActivity.class);
                            i.putExtra("videoUri", MediaPath);
                            activity.startActivity(i);
                        }
                    }
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return al_video.size();
    }
}