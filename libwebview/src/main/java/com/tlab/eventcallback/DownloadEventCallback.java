package com.tlab.eventcallback;

public class DownloadEventCallback {
    public String onStart;
    public String onFinish;
    public String varDlUrlName;
    public String varDlUriName;
    public String varDlIdName;

    public boolean isOnStartEmpty() {
        return (onStart == null || onStart.isEmpty());
    }

    public boolean isOnFinishEmpty() {
        return (onFinish == null || onFinish.isEmpty());
    }
}
