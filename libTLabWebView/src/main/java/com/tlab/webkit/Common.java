package com.tlab.webkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.unity3d.player.UnityPlayer;
import com.tlab.util.Common.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Common {
    public static class AsyncResult extends JSONSerialisable {

        public static class Manager {
            private final HashMap<Integer, AsyncResult> mResults = new HashMap<>();
            private final Queue<Integer> mIdAvails = new ArrayDeque<>();

            public Manager() {
                for (int i = 0; i < 10; i++) mIdAvails.add(i);
            }

            public int request() {
                if (mIdAvails.isEmpty()) return -1;
                Integer id = mIdAvails.poll();
                if (id != null) return id;
                return -1;
            }

            public void post(AsyncResult result, int status) {
                int id = result.id;
                if (mIdAvails.contains(id)) return;  // This result id is not valid.

                if (!mResults.containsKey(id)) {
                    result.status = status;
                    mIdAvails.add(id);
                    mResults.put(id, result);
                }
            }

            public AsyncResult get(int id) {
                if (!mResults.containsKey(id)) return null;
                AsyncResult result = mResults.get(id);
                mResults.remove(id);
                return result;
            }
        }

        public static class Status {
            public static final int WAITING = 0;
            public static final int FAILED = 1;
            public static final int CANCEL = 2;
            public static final int COMPLETE = 3;
        }

        public static final String KEY_ID = "id";
        public static final String KEY_STATUS = "status";
        public static final String KEY_INT_VALUE = "i";
        public static final String KEY_BOOL_VALUE = "b";
        public static final String KEY_DOUBLE_VALUE = "d";
        public static final String KEY_STRING_VALUE = "s";

        public int id;
        public int status = Status.WAITING;

        public int i;
        public double d;
        public boolean b;
        public String s = "";

        public AsyncResult() {
        }

        public AsyncResult(int id, int i) {
            this.id = id;
            this.i = i;
        }

        public AsyncResult(int id, double d) {
            this.id = id;
            this.d = d;
        }

        public AsyncResult(int id, boolean b) {
            this.id = id;
            this.b = b;
        }

        public AsyncResult(int id, String s) {
            this.id = id;
            this.s = s == null ? this.s : s;
        }

        @Override
        public JSONObject toJSON() {
            JSONObject jo = new JSONObject();
            try {
                jo.put(KEY_ID, id);
                jo.put(KEY_STATUS, status);
                jo.put(KEY_INT_VALUE, i);
                jo.put(KEY_BOOL_VALUE, b);
                jo.put(KEY_DOUBLE_VALUE, d);
                jo.put(KEY_STRING_VALUE, s);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return jo;
        }

        @Override
        public void overwriteJSON(JSONObject jo) {
            try {
                this.id = jo.getInt(KEY_ID);
                this.status = jo.getInt(KEY_STATUS);
                this.i = jo.getInt(KEY_INT_VALUE);
                this.b = jo.getBoolean(KEY_BOOL_VALUE);
                this.d = jo.getDouble(KEY_DOUBLE_VALUE);
                this.s = jo.getString(KEY_STRING_VALUE);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ResolutionState {
        public Vector2Int view = new Vector2Int();
        public Vector2Int tex = new Vector2Int();
        public Vector2Int screen = new Vector2Int();
    }

    public static class JSUtil {
        public static String toVariable(String name, String value) {
            return "var " + name + " = " + "'" + value + "';\n";
        }

        public static String toVariable(String name, int value) {
            return "var " + name + " = " + value + ";\n";
        }

        public static String toVariable(String name, float value) {
            return "var " + name + " = " + value + ";\n";
        }
    }

    public static class PageGoState {
        public boolean canGoBack = false;
        public boolean canGoForward = false;

        public void update(boolean canGoBack, boolean canGoForward) {
            this.canGoBack = canGoBack;
            this.canGoForward = canGoForward;
        }
    }

    public static class SessionState {
        public String loadUrl;
        public String actualUrl;
        public String userAgent;
    }

    public static class Vector2Int {
        public int x;
        public int y;

        public void update(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Vector2Int() {
        }

        public Vector2Int(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class Download {
        public enum Directory {
            Application, Download
        }

        public static class Option {
            public Directory directory = Directory.Application;
            public String subDirectory;

            public void update(Directory directory, String subDirectory) {
                this.directory = directory;
                this.subDirectory = subDirectory;
            }
        }

        public static class Request extends JSONSerialisable {
            public static final String KEY_URL = "url";
            public static final String KEY_USER_AGENT = "userAgent";
            public static final String KEY_CONTENT_DISPOSITION = "contentDisposition";
            public static final String KEY_MIME_TYPE = "mimeType";

            public String url;
            public String userAgent;
            public String contentDisposition;
            public String mimeType;

            public Request(String url, String userAgent, String contentDisposition, String mimeType) {
                this.url = url;
                this.userAgent = userAgent;
                this.contentDisposition = contentDisposition;
                this.mimeType = mimeType;
            }

            public Request() {
            }

            @Override
            public JSONObject toJSON() {
                JSONObject jo = new JSONObject();
                try {
                    jo.put(KEY_URL, url);
                    jo.put(KEY_USER_AGENT, userAgent);
                    jo.put(KEY_CONTENT_DISPOSITION, contentDisposition);
                    jo.put(KEY_MIME_TYPE, mimeType);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return jo;
            }

            @Override
            public void overwriteJSON(JSONObject jo) {
                try {
                    this.url = jo.getString(KEY_URL);
                    this.userAgent = jo.getString(KEY_USER_AGENT);
                    this.contentDisposition = jo.getString(KEY_CONTENT_DISPOSITION);
                    this.mimeType = jo.getString(KEY_MIME_TYPE);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public static class EventInfo extends JSONSerialisable {
            public static final String KEY_URL = "url";
            public static final String KEY_ID = "id";

            public String url;
            public long id;

            public EventInfo(long id, String url) {
                this.url = url;
                this.id = id;
            }

            public EventInfo() {
            }

            @Override
            public JSONObject toJSON() {
                JSONObject jo = new JSONObject();
                try {
                    jo.put(KEY_URL, url);
                    jo.put(KEY_ID, id);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return jo;
            }

            @Override
            public void overwriteJSON(JSONObject jo) {
                try {
                    this.url = jo.getString(KEY_URL);
                    this.id = jo.getLong(KEY_ID);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class EventCallback {
        public enum Type {
            Raw, OnPageFinish, OnDownload, OnDownloadStart, OnDownloadError, OnDownloadFinish, OnDialog,
        }

        public static class Message extends JSONSerialisable {
            public static final String KEY_TYPE = "type";
            public static final String KEY_PAYLOAD = "payload";

            public Type type;
            public String payload;

            public Message(Type type, String payload) {
                this.type = type;
                this.payload = payload;
            }

            public Message() {
            }

            // https://stackoverflow.com/a/24322913/22575350
            @Override
            public JSONObject toJSON() {
                JSONObject jo = new JSONObject();
                try {
                    jo.put(KEY_TYPE, type.ordinal());
                    jo.put(KEY_PAYLOAD, payload);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return jo;
            }

            @Override
            public void overwriteJSON(JSONObject jo) {
                try {
                    this.type = Type.values()[jo.getInt(KEY_TYPE)];
                    this.payload = jo.getString(KEY_PAYLOAD);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class DownloadSupport {
        public static final String TAG = "DownloadSupport";

        private final Download.Option mDownloadOption;
        private final Callback mCallback;
        private final Queue<EventCallback.Message> mUnityPostMessageQueue;

        public interface Callback {
            void onDownloadProgressChanged(long id, float progress);
        }

        public DownloadSupport(Download.Option downloadOption, Callback callback, Queue<EventCallback.Message> queue) {
            mDownloadOption = downloadOption;
            mCallback = callback;
            mUnityPostMessageQueue = queue;
        }

        public String getExtension(String path) {
            String extension = "";

            int i = path.lastIndexOf('.');
            int p = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));

            if (i > p) extension = path.substring(i + 1);
            return extension;
        }

        public static @NonNull ContentValues getContentValues(String mimeType, String directory, String filename) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.TITLE, filename);
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, directory);
            return contentValues;
        }

        public String getFileName(final String contentDisposition, final String mimetype) {
            @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String extension = MimeTypes.getDefaultExt(mimetype);
            String filename = dateFormat.format(new Date()) + "." + extension;

            if ((contentDisposition != null) && !contentDisposition.isEmpty()) {
                Pattern pattern = Pattern.compile("(filename=\"?)(.+)(\"?)");
                Matcher matcher = pattern.matcher(contentDisposition);
                if (matcher.find()) {
                    String group = matcher.group(2);
                    assert group != null;
                    filename = Objects.requireNonNull(group.substring(0, group.length() - 1)).replaceAll("\\s", "%20");
                }
            }

            return filename;
        }

        public static class StreamInfo {
            public OutputStream out;
            public String filename;
            public String directory;
            public boolean makeCompleteFlag;

            public StreamInfo(OutputStream out, String filename, String directory, boolean makeCompleteFlag) {
                this.out = out;
                this.filename = filename;
                this.directory = directory;
                this.makeCompleteFlag = makeCompleteFlag;
            }
        }

        public StreamInfo open(String filename, String mimeType) {
            Context context = UnityPlayer.currentActivity.getApplicationContext();

            String directory = "";
            boolean makeCompleteFlag = false;

            if (mDownloadOption.directory == Download.Directory.Application) {
                directory = Objects.requireNonNull(context.getExternalFilesDir(null)).getPath();
            } else if (mDownloadOption.directory == Download.Directory.Download) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    directory = Environment.DIRECTORY_DOWNLOADS;
                else {
                    makeCompleteFlag = true;
                    directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                }
            }

            if (!mDownloadOption.subDirectory.isEmpty())
                directory = directory + "/" + mDownloadOption.subDirectory;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, getContentValues(mimeType, directory, filename));
                try {
                    assert uri != null;
                    return new StreamInfo(context.getContentResolver().openOutputStream(uri), filename, directory, makeCompleteFlag);
                } catch (IOException e) {
                    Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                }
            } else {
                File file = new File(directory, filename);

                boolean fileExists = file.exists();

                int i = 1;
                String tmp = filename;
                while (fileExists) {
                    String ext = getExtension(filename);
                    tmp = filename.substring(0, filename.length() - (1 + ext.length())) + " (" + i + ")" + (!ext.isEmpty() ? "." + ext : "");
                    file = new File(directory, tmp);
                    fileExists = file.exists();
                    i++;
                }

                File parent = file.getParentFile();

                assert parent != null;
                fileExists = parent.exists();

                if (!fileExists) fileExists = parent.mkdirs();

                if (fileExists) {
                    fileExists = file.exists();

                    if (!fileExists) {
                        try {
                            fileExists = file.createNewFile();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                if (fileExists) {
                    try {
                        filename = tmp;
                        return new StreamInfo(new FileOutputStream(file, false), filename, directory, makeCompleteFlag);
                    } catch (IOException e) {
                        Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                    }
                }
            }
            return null;
        }

        public void fetchFromInputStream(InputStream input, String url, String contentDisposition, String mimetype) {
            int bufferSize = 1024; // to read in 1Mb increments
            byte[] buffer = new byte[bufferSize];
            try {
                int len;
                String filename = getFileName(contentDisposition, mimetype);
                DownloadSupport.StreamInfo info = open(filename, mimetype);
                int totalLen = 0;
                if (info != null) {
                    while (true) {
                        if ((len = input.read(buffer)) != -1) {
                            info.out.write(buffer, 0, len);
                            totalLen += len;
                            continue;
                        }
                        break;
                    }
                    info.out.flush();
                    info.out.close();
                    Activity context = UnityPlayer.currentActivity;
                    if (info.makeCompleteFlag) {
                        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        if (dm != null)
                            dm.addCompletedDownload(filename, filename, true, mimetype, info.directory + "/" + info.filename, totalLen, true);
                    }
                    mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDownloadFinish, new Download.EventInfo(-1, url).toJSON().toString()));
                }
            } catch (Throwable e) {
                Log.i(TAG, Arrays.toString(e.getStackTrace()));
            }
        }

        public void fetchFromDataUrl(String url, String contentDisposition, String mimetype) {
            String base64 = url.replaceFirst("^data:.+;base64,", "");

            byte[] data = Base64.decode(base64, 0);

            String filename = getFileName(contentDisposition, mimetype);

            try {
                DownloadSupport.StreamInfo info = open(filename, mimetype);
                if (info != null) {
                    info.out.write(data);
                    info.out.flush();
                    info.out.close();

                    Activity context = UnityPlayer.currentActivity;
                    if (info.makeCompleteFlag) {
                        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        if (dm != null)
                            dm.addCompletedDownload(filename, filename, true, mimetype, info.directory + "/" + info.filename, data.length, true);
                    }
                    mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDownloadFinish, new Download.EventInfo(-1, url).toJSON().toString()));
                }
            } catch (IOException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        }

        public void fetchFromDownloadManager(String url, String userAgent, String contentDisposition, String mimetype) {
            final Activity a = UnityPlayer.currentActivity;
            final Context context = a.getApplicationContext();

            DownloadManager dm = (DownloadManager) a.getSystemService(Context.DOWNLOAD_SERVICE);

            String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);

            long id = -1;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            request.setMimeType(mimetype);
            //------------------------COOKIE!!------------------------
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            //------------------------COOKIE!!------------------------
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file...");
            request.setTitle(filename);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            if (mDownloadOption.directory == Download.Directory.Application) {
                request.setDestinationInExternalFilesDir(context, mDownloadOption.subDirectory, filename);
            } else if (mDownloadOption.directory == Download.Directory.Download) {
                if (!mDownloadOption.subDirectory.isEmpty())
                    filename = mDownloadOption.subDirectory + "/" + filename;
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            }

            id = dm.enqueue(request);

            mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDownloadStart, new Download.EventInfo(id, url).toJSON().toString()));

            long finalId = id;
            new Thread(() -> {
                while (true) {
                    DownloadManager.Query query = new DownloadManager.Query().setFilterById(finalId).setFilterByStatus(DownloadManager.STATUS_PENDING | DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_SUCCESSFUL);
                    Cursor cursor = dm.query(query);
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int idStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int idTotalSizeBytes = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        int idBytesDownloadedSoFar = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

                        int totalSizeBytes = cursor.getInt(idTotalSizeBytes);
                        int bytesDownloadedSoFar = cursor.getInt(idBytesDownloadedSoFar);
                        mCallback.onDownloadProgressChanged(finalId, (float) bytesDownloadedSoFar / totalSizeBytes);

                        int status = cursor.getInt(idStatus);
                        switch (status) {
                            case DownloadManager.STATUS_SUCCESSFUL:
                                break;
                            case DownloadManager.STATUS_FAILED:
                                mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDownloadError, new Download.EventInfo(finalId, url).toJSON().toString()));
                                break;
                            default:
                                continue;
                        }
                    }
                    break;
                }
            }).start();
        }
    }
}
