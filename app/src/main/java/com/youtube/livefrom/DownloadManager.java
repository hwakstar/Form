package com.youtube.livefrom;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.webkit.URLUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DownloadManager extends AsyncTask<String, String, String> {

    private ProgressDialog progressDialog;
    private String[] localFilename;
    private Activity currentActivity = null;
    private boolean isJsonFileDownloaded = false;
    private  int i = 0;

    public DownloadManager(Activity activity, String[] filename, boolean isJsonFile) {
        localFilename = filename;
        currentActivity = activity;
        isJsonFileDownloaded = isJsonFile;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.progressDialog = new ProgressDialog(currentActivity);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog.setCancelable(false);
        this.progressDialog.show();
    }

    @Override
    protected String doInBackground(String... f_url) {
        int count;
        try {
            for ( i = 0; i < f_url.length; i++) {
                URL url = new URL(f_url[i]);
                URLConnection connection = url.openConnection();
                connection.connect();
                int lengthOfFile = connection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                OutputStream output = new FileOutputStream(localFilename[i]);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            }
            return "Downloaded";

        } catch (Exception e) {
            HelperMethods.logError("Unable to download file.", e.toString());
            return "Unable to download. " + e.toString();
        }
    }

    protected void onProgressUpdate(String... progress) {
        progressDialog.setProgress((100/localFilename.length*i) +  (Integer.parseInt(progress[0]) / localFilename.length));
    }


    @Override
    protected void onPostExecute(String message) {
        HelperMethods.logMessage("File downloaded successfully. " + localFilename[0]);
        if (isJsonFileDownloaded && new File(localFilename[0]).exists()) {
            downloadExtraItems();
            this.progressDialog.dismiss();
        }
        else{
            this.progressDialog.dismiss();
        }
    }

    private void downloadExtraItems() {
        try {
            String data = HelperMethods.readFile(new File(currentActivity.getFilesDir(), AppConfig.JsonConfigFilename).getPath());
            if (!data.isEmpty()) {
                Gson gson = new Gson();
                Type overlayItemListType = new TypeToken<Collection<OverlayItem>>() {
                }.getType();
                List<OverlayItem> overlayItems = gson.fromJson(data, overlayItemListType);
                ArrayList<String> urls = new ArrayList<>();
                ArrayList<String> fullFilenames = new ArrayList<>();
                for (OverlayItem item : overlayItems) {
                    if (item.Source.toLowerCase().startsWith("http")) {
                        String filename = URLUtil.guessFileName(item.Source, null, null);
                        File fileItem = new File(AppController.getAppContext().getFilesDir(), item.Type + "_" + filename);
                        if (fileItem.exists()) {
                            fileItem.delete();
                        }
                        urls.add(item.Source);
                        fullFilenames.add(fileItem.getPath());
                    }
                }
                if(urls.size() > 0){
                    new DownloadManager(currentActivity,fullFilenames.toArray(new String[fullFilenames.size()]), false).execute(urls.toArray(new String[urls.size()]));
                }
            }
        } catch (Exception ex) {
            HelperMethods.showErrorMessage(currentActivity, "Unable to download overlay items. ", ex.toString());
        }
    }
}