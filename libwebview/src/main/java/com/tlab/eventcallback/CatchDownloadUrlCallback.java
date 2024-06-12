package com.tlab.eventcallback;

public class CatchDownloadUrlCallback {
    public String go;
    public String func;

    public boolean isEmpty() {
        return (go == null || go.isEmpty() || func == null || func.isEmpty());
    }
}
