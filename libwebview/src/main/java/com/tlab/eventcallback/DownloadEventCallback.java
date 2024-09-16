package com.tlab.eventcallback;

public class DownloadEventCallback {
    public String onStart;
    public String onFinish;
    public String varUrl;
    public String varUri;
    public String varId;

    public boolean isOnStartEmpty() {
        return (onStart == null || onStart.isEmpty());
    }

    public boolean isOnFinishEmpty() {
        return (onFinish == null || onFinish.isEmpty());
    }
}
