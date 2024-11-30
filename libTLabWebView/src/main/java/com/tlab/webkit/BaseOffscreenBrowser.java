package com.tlab.webkit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.tlab.widget.AlertDialog;
import com.unity3d.player.UnityPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;

public abstract class BaseOffscreenBrowser extends BaseOffscreenFragment implements IOffscreen, IBrowserCommon {
    protected final Common.Vector2Int mScrollState = new Common.Vector2Int();

    public static final int REQUEST_FILE_PICKER = 1;
    public static final int REQUEST_PERMISSIONS = 2;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    protected final Queue<Common.EventCallback.Message> mUnityPostMessageQueue = new ArrayDeque<>();
    protected final Common.AsyncResult.Manager mAsyncResult = new Common.AsyncResult.Manager();

    protected final Common.Download.Option mDownloadOption = new Common.Download.Option();

    protected final HashMap<Long, Float> mDownloadProgress = new HashMap<>();
    protected BroadcastReceiver mOnDownloadComplete;

    protected final Common.SessionState mSessionState = new Common.SessionState();

    protected AlertDialog.Callback mOnDialogResult;

    protected String[] mIntentFilters;

    protected final Common.PageGoState mPageGoState = new Common.PageGoState();

    protected View mView;

    public void SetIntentFilters(String[] intentFilters) {
        mIntentFilters = intentFilters;
    }

    public int GetScrollX() {
        return mScrollState.x;
    }

    public int GetScrollY() {
        return mScrollState.y;
    }

    public String[] DispatchMessageQueue() {
        String[] messages = new String[mUnityPostMessageQueue.size()];
        for (int i = 0; i < messages.length; i++)
            messages[i] = Objects.requireNonNull(mUnityPostMessageQueue.poll()).marshall();
        return messages;
    }

    public void SetDownloadOption(int directory, String subDirectory) {
        mDownloadOption.update(Common.Download.Directory.values()[directory], subDirectory);
    }

    public float GetDownloadProgress(long id) {
        if (!mDownloadProgress.containsKey(id)) return -1;
        Float progress = this.mDownloadProgress.get(id);
        if (progress != null) return progress;
        return -1;
    }

    public String GetAsyncResult(int id) {
        Common.AsyncResult result = mAsyncResult.get(id);
        if (result == null) return "";
        return result.marshall();
    }

    public void CancelAsyncResult(int id) {
        mAsyncResult.post(new Common.AsyncResult(), Common.AsyncResult.Status.CANCEL);
    }

    public void PostDialogResult(int result, String json) {
        if (mOnDialogResult == null) return;

        UnityPlayer.currentActivity.runOnUiThread(() -> {
            try {
                mOnDialogResult.onResult(result, new JSONObject(json));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            mOnDialogResult = null;
        });
    }

    public void ScrollTo(int x, int y) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mView == null) return;
            mView.scrollTo(x, y);
        });
    }

    public void ScrollBy(int x, int y) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mView == null) return;
            mView.scrollBy(x, y);
        });
    }

    public long TouchEvent(int x, int y, int action, long downTime) {
        final Activity a = UnityPlayer.currentActivity;
        final long finalEventTime = SystemClock.uptimeMillis();
        if (action == MotionEvent.ACTION_DOWN) downTime = finalEventTime;
        final long finalDownTime = downTime;
        a.runOnUiThread(() -> {
            if (mView == null) return;

            // Obtain MotionEvent object
            final int source = InputDevice.SOURCE_CLASS_POINTER;

            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
            final int metaState = 0;
            MotionEvent event = MotionEvent.obtain(finalDownTime, finalEventTime, action, x, y, metaState);
            event.setSource(source);

            // Dispatch touch event to view
            mView.dispatchTouchEvent(event);
        });
        return downTime;
    }

    public void KeyEvent(char key) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mView == null) return;
            KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            KeyEvent[] events = kcm.getEvents(new char[]{key});
            for (KeyEvent event : events) mView.dispatchKeyEvent(event);
        });
    }

    public void KeyEvent(int keyCode) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mView == null) return;
            // KEYCODE_DEL: 67
            mView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            mView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        });
    }
}
