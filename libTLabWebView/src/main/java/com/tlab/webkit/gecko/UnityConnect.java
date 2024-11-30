package com.tlab.webkit.gecko;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.tlab.webkit.IBrowser;
import com.tlab.webkit.Common.*;
import com.tlab.widget.AlertDialog;
import com.unity3d.player.UnityPlayer;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.OffscreenGeckoView;
import org.mozilla.geckoview.WebRequest;
import org.mozilla.geckoview.WebRequestError;
import org.mozilla.geckoview.WebResponse;
import org.mozilla.geckoview.GeckoWebExecutor;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnityConnect extends OffscreenBrowser implements IBrowser {

    private OffscreenGeckoView mWebView;
    private GeckoSession mSession;
    private GeckoRuntime mRuntime;

    private final static String TAG = "TLabWebView (Gecko)";

    /**
     * Initialize WebView if it is not initialized yet.
     *
     * @param webWidth     WebView width
     * @param webHeight    WebView height
     * @param texWidth     Texture width
     * @param texHeight    Texture height
     * @param screenWidth  Screen width
     * @param screenHeight Screen height
     * @param url          init url
     * @param isVulkan     is this app vulkan api
     */
    //@formatter:off
    @SuppressLint("SetJavaScriptEnabled") public void InitNativePlugin(int webWidth, int webHeight,
                           int texWidth, int texHeight,
                           int screenWidth, int screenHeight,
                           String url, boolean isVulkan, int captureMode) {
        //@formatter:on
        if (webWidth <= 0 || webHeight <= 0) return;

        mSessionState.loadUrl = url;

        final UnityConnect self = this;

        // -----------------------------------------------------------
        // Hierarchical structure.
        // parent -----
        //            |
        //            |
        //            | mRootLayout -----
        //                              |
        //                              |
        //                              | mWebView

        setRetainInstance(true);

        final Activity a = UnityPlayer.currentActivity;
        a.getFragmentManager().beginTransaction().add(0, self).commitAllowingStateLoss();

        a.runOnUiThread(() -> {

            initParam(webWidth, webHeight, texWidth, texHeight, screenWidth, screenHeight, isVulkan, CaptureMode.values()[captureMode]);

            init();

            if (mRuntime == null) mRuntime = GeckoRuntime.getDefault(UnityPlayer.currentActivity);
            mRuntime.getSettings().setJavaScriptEnabled(true);
            mRuntime.getSettings().setConsoleOutputEnabled(true);

            if (mSession == null) {
                GeckoSessionSettings.Builder builder = new GeckoSessionSettings.Builder();
                builder.suspendMediaWhenInactive(true);
                builder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
                builder.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
                builder.displayMode(GeckoSessionSettings.DISPLAY_MODE_STANDALONE);
                mSession = new GeckoSession(builder.build());
            }
            mSession.setContentDelegate(new ContentDelegate());
            mSession.setProgressDelegate(new ProgressDelegate());
            mSession.setNavigationDelegate(new NavigationDelegate());
            mSession.setPromptDelegate(new PromptDelegate(UnityPlayer.currentActivity, this, dialog -> {
                if (mOnDialogResult != null) mOnDialogResult.dismiss();
                mOnDialogResult = dialog.getOnResultListener();
                mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDialog, dialog.marshall()));
            }));
            mSession.setPermissionDelegate(new PermissionDelegate(UnityPlayer.currentActivity, this, mRuntime, mSession));

            if (mWebView == null) {
                mWebView = new OffscreenGeckoView(UnityPlayer.currentActivity, mCaptureMode, mViewToBufferRenderer);
                mView = mWebView;
            }

            mWebView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                /**
                 * @param v    The view whose scroll position has changed.
                 * @param x    Current horizontal scroll origin.
                 * @param y    Current vertical scroll origin.
                 * @param oldX Previous horizontal scroll origin.
                 * @param oldY Previous vertical scroll origin.
                 */
                @Override
                public void onScrollChange(View v, int x, int y, int oldX, int oldY) {
                    mScrollState.x = x;
                    mScrollState.y = y;
                }
            });

            // create download complete event receiver.
            mOnDownloadComplete = new BroadcastReceiver() {
                /**
                 * @param context The Context in which the receiver is running.
                 * @param intent  The Intent being received.
                 */
                @Override
                public void onReceive(Context context, Intent intent) {
                    long downloadedID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri uri = dm.getUriForDownloadedFile(downloadedID);
                    mDownloadProgress.remove(downloadedID);

                    mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDownloadFinish, new Download.EventInfo(downloadedID, uri.toString()).marshall()));
                }
            };
            a.registerReceiver(mOnDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);

            mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            mWebView.setLongClickable(false);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.setVerticalScrollBarEnabled(true);
            mWebView.setBackgroundColor(0x00000000);

            GeckoSessionSettings settings = mSession.getSettings();
            settings.setAllowJavascript(true);
            if (mSessionState.userAgent != null && !mSessionState.userAgent.isEmpty())
                settings.setUserAgentOverride(mSessionState.userAgent);

            mRootLayout.addView(mWebView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            mSession.open(mRuntime);
            mWebView.setSession(mSession);

            if (mSessionState.loadUrl != null) LoadUrl(mSessionState.loadUrl);

            new Thread(() -> {
                while (mCaptureThreadKeepAlive) {
                    synchronized (mCaptureThreadMutex) {
                        mRootLayout.postInvalidate();
                    }
                    try {
                        Thread.sleep(1000 / mFps);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

            mInitialized = true;
        });
    }

    private class ProgressDelegate implements GeckoSession.ProgressDelegate {
        private String mUrl;

        /**
         * @param session GeckoSession that initiated the callback.
         * @param url     The resource being loaded.
         */
        @Override
        public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
            Log.d(TAG, "onPageStart: " + url);
            mUrl = url;
        }

        /**
         * @param session GeckoSession that initiated the callback.
         * @param success Whether the page loaded successfully or an error occurred.
         */
        @Override
        public void onPageStop(@NonNull GeckoSession session, boolean success) {
            //Log.d(TAG, "onPageFinish: " + mUrl);
            if (!success) return;
            mSessionState.actualUrl = mUrl;
            EventCallback.Message message = new EventCallback.Message(EventCallback.Type.OnPageFinish, mUrl);
            mUnityPostMessageQueue.add(message);
        }

        @Override
        public void onProgressChange(@NonNull GeckoSession session, int progress) {
            //Log.d(TAG, "onProgressChange: " + progress);
        }

        @Override
        public void onSecurityChange(@NonNull GeckoSession session, @NonNull GeckoSession.ProgressDelegate.SecurityInformation securityInfo) {

        }
    }

    private class NavigationDelegate implements GeckoSession.NavigationDelegate {
        /**
         * @param session   The GeckoSession that initiated the callback.
         * @param canGoBack The new value for the ability.
         */
        @Override
        public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBack) {
            mPageGoState.canGoBack = canGoBack;
        }

        /**
         * @param session      The GeckoSession that initiated the callback.
         * @param canGoForward The new value for the ability.
         */
        @Override
        public void onCanGoForward(@NonNull GeckoSession session, boolean canGoForward) {
            mPageGoState.canGoForward = canGoForward;
        }

        /**
         * @param session The GeckoSession that initiated the callback.
         * @param request The {@link LoadRequest} containing the request details.
         */
        @Override
        public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
            Log.d(TAG, "onLoadRequest: " + request.uri);
            String url = request.uri;

            if (mIntentFilters != null) {
                for (String intentFilter : mIntentFilters) {
                    Pattern pattern = Pattern.compile(intentFilter);
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.matches()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        mWebView.getContext().startActivity(intent);
                        return GeckoResult.fromValue(AllowOrDeny.DENY);
                    }
                }
            }
            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://") || url.startsWith("javascript:") || url.startsWith("data:") || url.startsWith("blob:"))
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            return GeckoResult.fromValue(AllowOrDeny.DENY);
        }

        @Nullable
        @Override
        public GeckoResult<GeckoSession> onNewSession(@NonNull GeckoSession session, @NonNull String uri) {
            return null;
        }

        @Override
        public GeckoResult<String> onLoadError(@NonNull GeckoSession session, String uri, @NonNull WebRequestError error) {
            // https://bugzilla.mozilla.org/show_bug.cgi?id=1553265
            // https://github.com/mozilla-mobile/firefox-android/blob/fe8a71cd70ad5674abe1824fe11dc78372b736c2/fenix/app/src/main/assets/lowMediumErrorPages.js#L122
            AlertDialog.Init dialog = new AlertDialog.Init(AlertDialog.Init.Reason.ERROR, "Load Error", error.code + ": " + error.getMessage());

            boolean showSSLAdvanced;
            switch (error.code) {
                case WebRequestError.ERROR_SECURITY_SSL:
                case WebRequestError.ERROR_SECURITY_BAD_CERT:
                    showSSLAdvanced = true;
                    break;
                default:
                    showSSLAdvanced = false;
            }

            //@formatter:off
            if (showSSLAdvanced) {
                dialog.setNegative("Back to safety", (result) -> EvaluateJS("window.history.back()"));
                dialog.setPositive("Enter", (result) -> EvaluateJS(
                        "async function acceptAndContinue(temporary) {\n" +
                        "    try {\n" +
                        "        await document.addCertException(temporary);\n" +
                        "        location.reload();\n" +
                        "    } catch (error) {\n" +
                        "        console.error(\"Unexpected error: \" + error);\n" +
                        "    }\n" +
                        "};\n" +
                        "acceptAndContinue(true);"));
            }
            //@formatter:on

            if (mOnDialogResult != null) mOnDialogResult.dismiss();
            mOnDialogResult = dialog.getOnResultListener();
            mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDialog, dialog.marshall()));

            return GeckoResult.fromValue("data:text/html;base64,");
        }
    }

    private void enqueueDownloadEvent(String url, String contentDisposition, String mimetype) {
        Objects.requireNonNull(mWebView.getSession()).getUserAgent().then(ua -> {
            Download.Request request = new Download.Request(url, ua, contentDisposition, mimetype);
            EventCallback.Message message = new EventCallback.Message(EventCallback.Type.OnDownload, request.marshall());
            mUnityPostMessageQueue.add(message);
            return null;
        });
    }

    private class ContentDelegate implements GeckoSession.ContentDelegate {
        @UiThread
        public void onExternalResponse(@NonNull final GeckoSession session, @NonNull final WebResponse response) {
            Activity a = UnityPlayer.currentActivity;
            a.runOnUiThread(() -> enqueueDownloadEvent(response.uri, response.headers.get("Content-Disposition"), response.headers.get("Content-Type")));
        }

        private boolean mFullScreen = false;
        private final Object mLock = new Object();

        @UiThread
        public void onFullScreen(@NonNull GeckoSession session, boolean fullScreen) {
            // https://bugzilla.mozilla.org/show_bug.cgi?id=1750416

            synchronized (mLock) {
                mFullScreen = fullScreen;
            }

            new Thread(() -> {
                while (true) {
                    synchronized (mLock) {
                        if (mFullScreen) {
                            UnityPlayer.currentActivity.runOnUiThread(() -> {
                                if (fullScreen) session.exitFullScreen();
                            });
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            continue;
                        }
                        break;
                    }
                }
            }).start();

            Log.w(TAG, "Full screen not currently supported");
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_FILE_PICKER) {
            final PromptDelegate prompt = (PromptDelegate) mSession.getPromptDelegate();
            assert prompt != null;
            prompt.onFileCallbackResult(resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            final PermissionDelegate permission = (PermissionDelegate) mSession.getPermissionDelegate();
            assert permission != null;
            permission.onRequestPermissionsResult(permissions, grantResults);
        } else if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // continueDownloads();
            // TODO:
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void Dispose() {
        ReleaseSharedTexture();

        final Activity a = UnityPlayer.currentActivity;
        final UnityConnect self = this;
        a.runOnUiThread(() -> {

            if (mWebView == null) return;
            abortCaptureThread();

            assert mWebView.getSession() != null;
            mWebView.getSession().close();

            mWebView = null;
            mView = null;
            mSession = null;

            a.unregisterReceiver(mOnDownloadComplete);
            a.getFragmentManager().beginTransaction().remove(self).commitAllowingStateLoss();

            mDisposed = true;
        });
    }

    public void EvaluateJS(String js) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mSession == null) return;
            mSession.loadUri("javascript:(function(){" + js + "})();");
        });
    }

    public void DownloadFromUrl(String url, String userAgent, String contentDisposition, String mimetype) {
        if (url.startsWith("https://") || url.startsWith("http://")) {
            DownloadSupport support = new DownloadSupport(mDownloadOption, mDownloadProgress::put, mUnityPostMessageQueue);
            support.fetchFromDownloadManager(url, userAgent, contentDisposition, mimetype);
        } else if (url.startsWith("data:")) { // data url scheme
            // write base64 data to file stream
            DownloadSupport support = new DownloadSupport(mDownloadOption, mDownloadProgress::put, mUnityPostMessageQueue);
            support.fetchFromDataUrl(url, contentDisposition, mimetype);
        } else if (url.startsWith("blob:")) { // blob url scheme
            GeckoResult<WebResponse> response = new GeckoWebExecutor(mRuntime).fetch(new WebRequest.Builder(url).addHeader("Content-Type", mimetype).addHeader("Content-Disposition", contentDisposition).build());
            response.then((r) -> {
                DownloadSupport support = new DownloadSupport(mDownloadOption, mDownloadProgress::put, mUnityPostMessageQueue);
                assert r != null;
                support.fetchFromInputStream(r.body, url, contentDisposition, mimetype);
                return null;
            });
        }
    }

    public void SetUserAgent(final String ua, final boolean reload) {
        // https://developer.mozilla.org/ja/docs/Web/HTTP/Headers/User-Agent/Firefox
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            try {
                Objects.requireNonNull(mWebView.getSession()).getSettings().setUserAgentOverride(ua);
                if (reload) mSession.reload();
            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        });
        mSessionState.userAgent = ua;
    }

    public int GetUserAgent() {
        int id = mAsyncResult.request();
        if (id == -1) return -1;
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            Objects.requireNonNull(mWebView.getSession()).getUserAgent().then((ua) -> {
                mSessionState.userAgent = ua;
                mAsyncResult.post(new AsyncResult(id, mSessionState.userAgent), AsyncResult.Status.COMPLETE);
                return null;
            });
        });
        return id;
    }

    public String GetUrl() {
        return mSessionState.actualUrl;
    }

    public void LoadUrl(String url) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            if (mIntentFilters != null) {
                for (String intentFilter : mIntentFilters) {
                    Pattern pattern = Pattern.compile(intentFilter);
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.matches()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        mWebView.getContext().startActivity(intent);
                        mSessionState.loadUrl = url;
                        return;
                    }
                }
            }
            if (!url.startsWith("http://") && !url.startsWith("https://"))
                mSessionState.loadUrl = "http://" + url;
            else mSessionState.loadUrl = url;

            Log.e(TAG, "loadUrl: " + mSessionState.loadUrl);

            Objects.requireNonNull(mWebView.getSession()).loadUri(mSessionState.loadUrl);
        });
    }

    public void GoBack() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null || !mPageGoState.canGoBack) return;
            Objects.requireNonNull(mWebView.getSession()).goBack();
        });
    }

    public void GoForward() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null || !mPageGoState.canGoForward) return;
            Objects.requireNonNull(mWebView.getSession()).goForward();
        });
    }

    @Override
    public void SetSurface(Object surfaceObj, int width, int height) {
        super.SetSurface(surfaceObj, width, height);
        if (mWebView == null) return;
        Surface surface = (Surface) surfaceObj;
        if (surface == null) {
            mWebView.removeSurface();
            return;
        }
        mWebView.onSurfaceChanged(surface, width, height);
    }

    @Override
    public void RemoveSurface() {
        if (mWebView == null) return;
        mWebView.removeSurface();
    }

    /**
     * Loads the given HTML.
     *
     * @param html The HTML of the resource to load
     */
    public void LoadHtml(final String html) {
        // https://stackoverflow.com/a/57951004/22575350
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            GeckoSession.Loader request = new GeckoSession.Loader();
            request.data(html, "text/html");
            Objects.requireNonNull(mWebView.getSession()).load(request);
        });
    }

    public void ClearData(int flag) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mRuntime == null) return;
            mRuntime.getStorageController().clearData(flag);
        });
    }
}
