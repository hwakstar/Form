package com.youtube.livefrom;

public class MyEnums {

    public static enum OverlayItemType {
        Intro,
        LiveStreaming,
        Credits,
        Quality,
    }

    public static enum RecordStatus {
        Record,
        Stop
    }

    public static enum StreamingStatus {
        Encoding,
        Live,
        Stopped,
    }

    public static enum OverlayItemPosition {
        TopLeft,
        TopCenter,
        TopRight,
        CenterLeft,
        CenterCenter,
        CenterRight,
        BottomLeft,
        BottomCenter,
        BottomRight,
    }

}
