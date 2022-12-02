package com.youtube.livefrom;

import android.graphics.Camera;
import android.text.TextUtils;
import android.util.Size;

import com.google.api.services.youtube.model.LiveBroadcast;

public class AppConfig {
    public static String AppName = "LiveFrom";
    public static String JsonConfigFilename = "jsonConfig.txt";
    private static String StreamName = "";
    private  static String StreamNameKey = "streamStreamName";
    private static String EventId = "";
    private  static String EventIdKey = "streamEventId";
    private static String VideoTitle = "";
    private static String VideoTitleKey = "settingsVideoTitle";
    private static String VideoHashTag = "";
    private static String VideoHashTagKey = "settingsVideoHashTag";
    private static String VideoDescription = "";
    private static String VideoDescriptionKey = "settingsVideoDescription";
    private static String JsonURL = "";
    private static String JsonURLKey = "settingsJsonURL";
    private  static String StreamResolution = "";
    private  static String StreamResolutionKey = "streamResolutionKey";
    private static String VideoBitrate = "";
    private static String VideoBitrateKey = "settingsVideoBitrate";
    private static String VideoFramerate = "";
    private static String VideoFramerateKey = "settingsVideoFramerate";
    private static String ChannelId = "";
    private static String ChannelIdKey = "settomgsChannelId";
    private static String CameraResolution = "";
    private static String CameraResolutionKey = "cameraResolutionKey";
    private static String EventStartedTime = "";
    private static String EventStartedTimeKey = "eventStartedTimeKey";
    private static String ThumbnailFilename = "";
    private static String ThumbnailFilenameKey = "thumbnailImageKey";
    public static LiveBroadcast CurrentEvent = null;
    public static Size[] CamSizes = null;
    public static String RecordingFile1 = "";
    public static String RecordingFile2 = "";
    public static String TrimRecordingFile1 = "";
    public static String TrimRecordingFile2 = "";

    public static int ColumnCount = 3;

    public static void setStreamResolution(String streamResolution) {
        StreamResolution = streamResolution;
        HelperMethods.saveSettings(StreamResolutionKey,streamResolution);
    }

    public static String getStreamResolution() {
        if(StreamResolution.isEmpty()){
            StreamResolution= HelperMethods.loadSettings(StreamResolutionKey);
        }
        return StreamResolution;
    }

    public static void setVideoTitle(String videoTitle) {
        VideoTitle = videoTitle;
        HelperMethods.saveSettings(VideoTitleKey,videoTitle);
    }

    public static String getVideoTitle() {
        if(VideoTitle.isEmpty()){
            VideoTitle = HelperMethods.loadSettings(VideoTitleKey);
        }
        return VideoTitle;
    }

    public static void setVideoHashTag(String videoHashTag) {
        VideoHashTag = videoHashTag;
        HelperMethods.saveSettings(VideoHashTagKey,videoHashTag);
    }

    public static String getVideoHashTag() {
        if(VideoHashTag.isEmpty()){
            VideoHashTag = HelperMethods.loadSettings(VideoHashTagKey);
        }
        return VideoHashTag;
    }

    public static void setVideoDescription(String videoDescription) {
        VideoDescription = videoDescription;
        HelperMethods.saveSettings(VideoDescriptionKey,videoDescription);
    }

    public static String getVideoDescription() {
        if(VideoDescription.isEmpty()){
            VideoDescription = HelperMethods.loadSettings(VideoDescriptionKey);
        }
        return VideoDescription;
    }

    public static void setVideoBitrate(String videoBitrate) {
        VideoBitrate = videoBitrate;
        HelperMethods.saveSettings(VideoBitrateKey,videoBitrate);
    }

    public static String getVideoBitrate() {
        if(VideoBitrate.isEmpty()){
            VideoBitrate = HelperMethods.loadSettings(VideoBitrateKey);
        }
        if(TextUtils.isEmpty(VideoBitrate)){
            return  "0";
        }
        else{
            return VideoBitrate;
        }
    }

    public static void setVideoFramerate(String videoFramerate) {
        VideoFramerate = videoFramerate;
        HelperMethods.saveSettings(VideoFramerateKey,videoFramerate);
    }

    public static String getVideoFramerate() {
        if(VideoFramerate.isEmpty()){
            VideoFramerate = HelperMethods.loadSettings(VideoFramerateKey);
        }
        if(TextUtils.isEmpty(VideoFramerate)){
            return  "30";
        }
        else{
            return VideoFramerate;
        }
    }


    public static void setJsonURL(String jsonURL) {
        JsonURL = jsonURL;
        HelperMethods.saveSettings(JsonURLKey,jsonURL);
    }

    public static String getJsonURL() {
        if(JsonURL.isEmpty()){
            JsonURL = HelperMethods.loadSettings(JsonURLKey);
        }
        return JsonURL;
    }

    public static void setEventId(String eventId) {
        EventId = eventId;
        HelperMethods.saveSettings(EventIdKey,eventId);
    }

    public static String getEventId() {
        if(EventId.isEmpty()){
            EventId = HelperMethods.loadSettings(EventIdKey);
        }
        return EventId;
    }

    public static void setStreamName(String streamName) {
        StreamName = streamName;
        HelperMethods.saveSettings(StreamNameKey,streamName);
    }

    public static String getStreamName() {
        if(StreamName.isEmpty()){
            StreamName = HelperMethods.loadSettings(StreamNameKey);
        }
        return StreamName;
    }

    public static void setChannelId(String channelId) {
        ChannelId = channelId;
        HelperMethods.saveSettings(ChannelIdKey,channelId);
    }

    public static String getChannelId() {
        if(ChannelId.isEmpty()){
            ChannelId = HelperMethods.loadSettings(ChannelIdKey);
        }
        if(TextUtils.isEmpty(ChannelId)){
            return ChannelId;
        }
        else{
            return  ChannelId.split(":")[0];
        }
    }
    public static void setCameraResolution(String cameraResolution) {
        CameraResolution = cameraResolution;
        HelperMethods.saveSettings(CameraResolutionKey,cameraResolution);
    }

    public static String getCameraResolution() {
        if(CameraResolution.isEmpty()){
            CameraResolution = HelperMethods.loadSettings(CameraResolutionKey);
        }
        return CameraResolution;
    }
    public static void setEventStartedTime(String eventStartedTime) {
        EventStartedTime = eventStartedTime;
        HelperMethods.saveSettings(EventStartedTimeKey,eventStartedTime);
    }

    public static String getEventStartedTime() {
        if(EventStartedTime.isEmpty()){
            EventStartedTime = HelperMethods.loadSettings(EventStartedTimeKey);
        }
        return EventStartedTime;
    }

    public static void setThumbnailFilename(String thumbnailFilename) {
        ThumbnailFilename = thumbnailFilename;
        HelperMethods.saveSettings(ThumbnailFilenameKey,thumbnailFilename);
    }

    public static String getThumbnailFilename() {
        if(ThumbnailFilename.isEmpty()){
            ThumbnailFilename = HelperMethods.loadSettings(ThumbnailFilenameKey);
        }
        return ThumbnailFilename;
    }
}
