package com.youtube.livefrom;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.util.Size;

import androidx.preference.PreferenceManager;

import com.vastreaming.common.FileLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HelperMethods {
    public static void logMessage(String msg) {
        Log.d("Info",msg);
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        String dateString = format.format(Calendar.getInstance().getTime());
        appendToFile(new File(Environment.getExternalStorageDirectory().getPath(),
                "logs_LiveForm_" + getAppVersionCode() + ".txt"), String.format("[%s][%s] %s",
                dateString, "Info", msg));
    }

    public static void logError(String error, String details) {
        Log.d("Error",error + " " + details);
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        String dateString = format.format(Calendar.getInstance().getTime());
        appendToFile(new File(Environment.getExternalStorageDirectory().getPath(),
                "logs_LiveForm_" + getAppVersionCode() + ".txt"), String.format("[%s][%s] %s",
                dateString, "Info", details + " " + error));
    }

    public static void appendToFile(File file, String text) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
        }
    }
    public static String readFile(String filename) {
        String ret = "";
        try {
            InputStream inputStream = new FileInputStream(filename);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (Exception e) {
            HelperMethods.logError("Unable to read file. ",e.toString());
        }
        return ret;
    }
    public static String getAppVersionCode() {
        try {
            PackageInfo pInfo = AppController.getAppContext().getPackageManager().getPackageInfo(AppController.getAppContext().getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static void showErrorMessage(Activity activity, String error, String details){
        if(!details.isEmpty()){
            logError(error,details);
            Log.d("Error",details);
        }
        new AlertDialog.Builder(activity)
                .setTitle(AppConfig.AppName)
                .setMessage(error)
                .setPositiveButton("OK",null)
                .show();
    }
    public static void showErrorMessage(Activity activity, String error, String details, DialogInterface.OnClickListener callBack){
        if(!details.isEmpty()){
            logError(error,details);
            Log.d("Error",details);
        }
        new AlertDialog.Builder(activity)
                .setTitle(AppConfig.AppName)
                .setMessage(error)
                .setPositiveButton("OK",callBack)
                .show();
    }
    public static void showMessage(Activity activity, String msg){
        new AlertDialog.Builder(activity)
                .setTitle(AppConfig.AppName)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
    public static void showConfirmMessage(Activity activity, String msg, String yesButtonText,String noButtonText, DialogInterface.OnClickListener callBackYes,DialogInterface.OnClickListener callBackNo){
        new AlertDialog.Builder(activity)
                .setTitle(AppConfig.AppName)
                .setMessage(msg)
                .setPositiveButton(yesButtonText,callBackYes)
                .setNegativeButton(noButtonText,callBackNo)
                .show();
    }
    public static void saveSettings(String key, String value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppController.getAppContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key,value);
        editor.apply();
    }

    public static String loadSettings(String key){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppController.getAppContext());
        return preferences.getString(key,"");
    }

    public static boolean isJsonConfigFileExists(){
        return new File(AppController.getAppContext().getFilesDir(),AppConfig.JsonConfigFilename).exists();
    }

    public static OverlayItem findOverylayItemByType(List<OverlayItem> items, MyEnums.OverlayItemType type){
        OverlayItem res = null;
        if(items != null && items.size() > 0){
            for (OverlayItem item : items) {
                if(item.Type.compareTo(type.toString()) == 0){
                    res = item;
                    break;
                }
            }
        }
        return  res;
    }

    public static List<OverlayItem> findOverylayItemsByType(List<OverlayItem> items, MyEnums.OverlayItemType type){
        List<OverlayItem> res = new ArrayList<OverlayItem>();
        if(items != null && items.size() > 0){
            for (OverlayItem item : items) {
                if(item.Type.compareTo(type.toString()) == 0){
                    res.add(item);
                }
            }
        }
        return  res;
    }

    public static Size getCaptureModeHighFirst(Size[] sizes, int height, int width)
    {
        List<Size> sizesList = Arrays.asList(sizes);
        Collections.sort(sizesList, new Comparator<Size>() {

            public int compare(final Size a, final Size b) {
                return Integer.valueOf(a.getWidth() + a.getHeight()).compareTo(Integer.valueOf(b.getWidth() + b.getHeight() ));
            }
        });
        HelperMethods.logMessage("Camera resolutions Sorted...");
        for (Size size : sizesList) {
            HelperMethods.logMessage("Sorted: " + size.getWidth() + "x" + size.getHeight());
        }
        Size cMode = null;
        if(sizesList != null && sizesList.size() > 0){
            for (Size size : sizesList) {
//                if(size.getWidth() != size.getHeight() && size.getHeight() > height && size.getHeight() > width && size.getWidth() > width && size.getWidth() > height){
//                    cMode = size;
//                    break;
//                }
                if( size.getHeight() >= height && size.getWidth() >= width ){
                    cMode = size;
                    break;
                }
            }
        }
        if (cMode == null)
        {
            if(sizesList != null && sizesList.size() > 0){
                for (Size size : sizesList) {
                    if(size.getHeight() <= height && size.getWidth() <= width){
                        cMode = size;
                        break;
                    }
                }
            }
        }
        return cMode;
    }

    public static Size getCaptureMode(Size[] sizes, int width, int height)
    {
        List<Size> sizesList = Arrays.asList(sizes);
        Size cMode = null;
        if(sizesList != null && sizesList.size() > 0){
            for (Size size : sizesList) {
                if(size.getHeight() == height && size.getWidth() == width){
                    cMode = size;
                    break;
                }
            }
        }
        return cMode;
    }

    public static Size getFileWidthAndHeight(String filename)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        return new Size(options.outWidth,options.outHeight);
    }

    public static Rect getOverlayItemPosition(Size size, String position, int videoWidth, int videoHeight)
    {
        int topMargin = 0;
        int leftMargin = 0;
        int rightMargin = 0;
        int bottomMargin = 0;
        if (position.compareTo(MyEnums.OverlayItemPosition.TopLeft.toString()) == 0)
        {
            return new Rect(leftMargin, topMargin, leftMargin + size.getWidth(), topMargin + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.TopCenter.toString()) == 0)
        {
            return new Rect((videoWidth / 2) - (size.getWidth() / 2), topMargin, (videoWidth / 2) - (size.getWidth() / 2) + size.getWidth(), topMargin + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.TopRight.toString()) == 0)
        {
            return new Rect(videoWidth - size.getWidth() - rightMargin, topMargin, videoWidth - size.getWidth() - rightMargin + size.getWidth(), topMargin + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.CenterLeft.toString()) == 0)
        {
            return new Rect(leftMargin, (videoHeight / 2) - (size.getHeight() / 2), leftMargin + size.getWidth(), (videoHeight / 2) - (size.getHeight() / 2) + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.CenterCenter.toString()) == 0)
        {
            return new Rect((videoWidth / 2) - (size.getWidth() / 2), (videoHeight / 2) - (size.getHeight() / 2), (videoWidth / 2) - (size.getWidth() / 2) + size.getWidth(), (videoHeight / 2) - (size.getHeight() / 2) + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.CenterRight.toString()) == 0)
        {
            return new Rect(videoWidth - size.getWidth() - rightMargin, (videoHeight / 2) - (size.getHeight() / 2), videoWidth - size.getWidth() - rightMargin + size.getWidth(), (videoHeight / 2) - (size.getHeight() / 2) + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.BottomLeft.toString()) == 0)
        {
            return new Rect(leftMargin, videoHeight - size.getHeight() - bottomMargin, leftMargin + size.getWidth(), videoHeight - size.getHeight() - bottomMargin + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.BottomCenter.toString()) == 0)
        {
            return new Rect((videoWidth / 2) - (size.getWidth() / 2), videoHeight - size.getHeight() - bottomMargin, (videoWidth / 2) - (size.getWidth() / 2) + size.getWidth(), videoHeight - size.getHeight() - bottomMargin + size.getHeight());
        }
        else if (position .compareTo( MyEnums.OverlayItemPosition.BottomRight.toString()) == 0)
        {
            return new Rect(videoWidth - size.getWidth() - rightMargin, videoHeight - size.getHeight() - bottomMargin, videoWidth - size.getWidth() - rightMargin + size.getWidth(), videoHeight - size.getHeight() - bottomMargin + size.getHeight());
        }
        else
        {
            return new Rect(0,0,size.getWidth(),size.getHeight());
        }
    }
    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
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
    public static void copyLibLogFile(){
        try {
            File src = FileLog.Copy("");
            File dest = new File(Environment.getExternalStorageDirectory().getPath(),"logs_lib_LiveForm_" + HelperMethods.getAppVersionCode() + ".txt");
            if(dest.exists()){
                dest.delete();
            }
            HelperMethods.copyFile(src,dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void shareLogFile(Activity activity){
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        File outputFile = FileLog.Copy("");
        Uri uri = Uri.fromFile(outputFile);
        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        activity.startActivity(share);
    }
}
