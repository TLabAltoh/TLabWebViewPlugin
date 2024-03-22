package com.tlab.libwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.HardwareBuffer;
import android.net.Uri;
import android.net.http.SslError;
import android.opengl.GLES30;
import android.os.Environment;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.self.viewtoglrendering.CustomGLSurfaceView;
import com.unity3d.player.UnityPlayer;

import com.robot9.shared.SharedTexture;
import com.self.viewtoglrendering.ViewToGLRenderer;
import com.self.viewtoglrendering.GLLinearLayout;

import java.util.ArrayDeque;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

public class UnityConnect extends Fragment {

    // ---------------------------------------------------------------------------------------------------------
    // Renderer
    //

    private ViewToGLRenderer mViewToGlRenderer;
    private CustomGLSurfaceView mGLSurfaceView;

    // ---------------------------------------------------------------------------------------------------------
    // Views.
    //

    private BitmapWebView mWebView;

    // ---------------------------------------------------------------------------------------------------------
    // View Group
    //

    private RelativeLayout mLayout;
    private GLLinearLayout mGlLayout;

    // ---------------------------------------------------------------------------------------------------------
    // Web variables.
    //

    private enum DlOption{
        applicationFolder,
        downloadFolder
    }

    private int webWidth;
    private int webHeight;
    private int textureWidth;
    private int textureHeight;
    private int screenWidth;
    private int screenHeight;
    private int dlOption;

    private float downloadProgress = 0.0f;

    private String onPageFinish;
    private String onDownloadStart;
    private String onDownloadFinish;
    private String subDir;
    private String loadUrl;
    private String actualUrl;
    private String htmlCash;

    private boolean canGoBack;
    private boolean canGoForward;
    private boolean initialized = false;

    private static final Hashtable texturePairQueueDic = new Hashtable();
    private SharedTexture sharedTexture;
    private int sharedTextureId = 0;

    private String androiString;
    private String userAgent;
    private Hashtable<String, String> mCustomHeaders;

    private BroadcastReceiver onDownloadComplete;

    private final static String TAG = "libwebview";

    // ---------------------------------------------------------------------------------------------------------
    // Initialize this class
    //

    public void initialize(int webWidth, int webHeight,
                           int textureWidth, int textureHeight,
                           int screenWidth, int screenHeight,
                           String url, int dlOption, String subDir,
                           String onPageFinish, String onDownloadStart, String onDownloadFinish)
    {
        if(webWidth <= 0 || webHeight <= 0) {
            return;
        }

        this.webWidth = webWidth;
        this.webHeight = webHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.loadUrl = url;
        this.subDir = subDir;
        this.dlOption = dlOption;

        this.onPageFinish = onPageFinish;

        this.onDownloadStart = onDownloadStart;
        this.onDownloadFinish = onDownloadFinish;

        initWebView();
    }

    public boolean IsInitialized() {
        return initialized;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Shared Texture Utility
    //

    private static String createTableKey(Integer a, Integer b) {
        return a.toString() + "x" + b.toString();
    }

    public static void generateSharedTexture(int textureWidth, int textureHeight) {
        String key = createTableKey(textureWidth, textureHeight);

        ArrayDeque<SharedTexturePair> queue;
        if(texturePairQueueDic.containsKey(key)){
            queue = (ArrayDeque<SharedTexturePair>)texturePairQueueDic.get(key);
        }else{
            queue = new ArrayDeque<>();
            texturePairQueueDic.put(key, queue);
        }

        int[] textures = new int[1];
        GLES30.glGenTextures(textures.length, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        SharedTexture sharedTexture = new SharedTexture(textureWidth, textureHeight);

        sharedTexture.bindTexture(textures[0]);

        assert queue != null;
        queue.add(new SharedTexturePair(textures[0], sharedTexture));
    }

    private boolean bindUnityTexture() {
        String key = createTableKey(textureWidth, textureHeight);
        ArrayDeque<SharedTexturePair> queue;
        if(texturePairQueueDic.containsKey(key)){
            queue = (ArrayDeque<SharedTexturePair>)texturePairQueueDic.get(key);
        }else{
            Log.i(TAG, "target queue is not exist");
            return false;
        }

        assert queue != null;
        SharedTexturePair sharedTexturePair = queue.poll();

        assert sharedTexturePair != null;
        sharedTextureId = sharedTexturePair.id;
        sharedTexture = sharedTexturePair.texture;

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Initialize webview
    //

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        final UnityConnect self = this;

        // -----------------------------------------------------------
        // Hierarchical structure.
        // parent -----
        //            |
        //            |
        //            | mLayout -----
        //                          |
        //                          |
        //                          | mGlLayout -----
        //                          |               |
        //                          |               |
        //                          |               | mWebView
        //                          |
        //                          |
        //                          | mGLSurfaceView

        if(!bindUnityTexture()){
            return;
        }

        UnityPlayer.currentActivity.runOnUiThread(() -> {

            mViewToGlRenderer = new ViewToGLRenderer();
            mViewToGlRenderer.SetTextureResolution(textureWidth, textureHeight);
            mViewToGlRenderer.SetWebResolution(webWidth, webHeight);
            HardwareBuffer sharedBuffer = sharedTexture.getHardwareBuffer();

            if (sharedBuffer == null) {
                Log.i(TAG, "shared buffer is null");
            }

            mViewToGlRenderer.createTextureCapture(
                    UnityPlayer.currentActivity,
                    R.raw.vertex,
                    R.raw.fragment_oes,
                    sharedBuffer
            );

            // mLayout settings
            mLayout = new RelativeLayout(UnityPlayer.currentActivity);
            mLayout.setGravity(Gravity.TOP);
            // set view to out of display.
            mLayout.setX(screenWidth);
            mLayout.setY(screenHeight);
            mLayout.setBackgroundColor(0xFFFFFFFF);

            // mGLSurfaceView settings
            mGLSurfaceView = new CustomGLSurfaceView(UnityPlayer.currentActivity);
            mGLSurfaceView.setEGLContextClientVersion(3);
            mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
            mGLSurfaceView.setPreserveEGLContextOnPause(true);
            mGLSurfaceView.setRenderer(mViewToGlRenderer);
            mGLSurfaceView.setBackgroundColor(0x00000000);

            // mGlLayout settings
            mGlLayout = new GLLinearLayout(
                    UnityPlayer.currentActivity,
                    (float)textureWidth / webWidth,
                    (float)textureHeight / webHeight
            );
            mGlLayout.setOrientation(GLLinearLayout.VERTICAL);
            mGlLayout.setGravity(Gravity.START);
            mGlLayout.setViewToGLRenderer(mViewToGlRenderer);
            mGlLayout.setBackgroundColor(Color.WHITE);

            if (mWebView == null) {
                mWebView = new BitmapWebView(UnityPlayer.currentActivity);
            }

            // --------------------------------------------------------------------------------------------------------
            // Settings each View and View Group
            //

            // mWebView settings
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler,
                                                      final String host, final String realm) {
                    String userName = null;
                    String userPass = null;

                    if (handler.useHttpAuthUsernamePassword() && view != null) {
                        String[] haup = view.getHttpAuthUsernamePassword(host, realm);
                        if (haup != null && haup.length == 2) {
                            userName = haup[0];
                            userPass = haup[1];
                        }
                    }

                    if (userName != null && userPass != null) {
                        handler.proceed(userName, userPass);
                    } else {
                        showHttpAuthDialog(handler, host, realm, null, null, null);
                    }
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    canGoBack = mWebView.canGoBack();
                    canGoForward = mWebView.canGoForward();
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    canGoBack = mWebView.canGoBack();
                    canGoForward = mWebView.canGoForward();
                    actualUrl = url;

                    if (mWebView.getSettings().getJavaScriptEnabled() && onPageFinish != null && !onPageFinish.isEmpty()) {
                        evaluateJS(onPageFinish);
                    }
                }

                @Override
                public void onLoadResource(WebView view, String url) {
                    canGoBack = mWebView.canGoBack();
                    canGoForward = mWebView.canGoForward();
                }

                @SuppressLint("WebViewClientOnReceivedSslError")
                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    handler.proceed();
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    canGoBack = mWebView.canGoBack();
                    canGoForward = mWebView.canGoForward();
                    if (url.startsWith("http://") || url.startsWith("https://") ||  url.startsWith("file://") || url.startsWith("javascript:")) {
                        // Let webview handle the URL
                        return false;
                    } else if (url.startsWith("unity:")) {
                        String message = url.substring(6);
                        return true;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                    return true;
                }
            });
            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                    return false;
                }

                /**
                 * <a href="https://qiita.com/NaokiHaba/items/eb0ad99ac56af4748227">...</a>
                 * <a href="https://www.wired-cat.com/entry/2023/02/17/205235#google_vignette">...</a>
                 *
                 * @param view     is the View object to be shown.
                 * @param callback invoke this callback to request the page to exit
                 *                 full screen mode.
                 */
                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    super.onShowCustomView(view, callback);
                    mWebView.addView(
                            view,
                            new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )
                    );
                }

                @Override
                public void onHideCustomView() {
                    super.onHideCustomView();
                    mWebView.removeAllViews();
                }

                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    final String[] requestedResources = request.getResources();
                    for (String r : requestedResources) {
                        if ((r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                                || (r.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                                || r.equals(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                            request.grant(requestedResources);
                            break;
                        }
                    }
                }
            });
            mWebView.setDownloadListener(new DownloadListener() {
                // https://gist.github.com/miktam/107a414ec43de181b481
                // https://teratail.com/questions/115988
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                    Context context = UnityPlayer.currentActivity.getApplicationContext();

                    String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
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

                    if(dlOption == DlOption.applicationFolder.ordinal()) {
                        request.setDestinationInExternalFilesDir(context, subDir, filename);
                    }else if(dlOption == DlOption.downloadFolder.ordinal()) {
                        String downloadDir = Environment.DIRECTORY_DOWNLOADS;
                        request.setDestinationInExternalPublicDir(downloadDir, filename);
                    }

                    DownloadManager dm = (DownloadManager) UnityPlayer.currentActivity.getSystemService(Context.DOWNLOAD_SERVICE);
                    long id = dm.enqueue(request);

                    if (mWebView.getSettings().getJavaScriptEnabled() && onDownloadStart != null && !onDownloadStart.isEmpty()) {
                        String argument = "var unity_webview_dl_url = '" + url + "'; " + "var unity_webview_dl_id = " + id + "; ";
                        evaluateJS(argument + onDownloadStart);
                    }
                }
            });

            // create download complete event receiver.
            onDownloadComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long downloadedID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri uri = manager.getUriForDownloadedFile(downloadedID);

                    if (mWebView.getSettings().getJavaScriptEnabled() && onDownloadFinish != null && !onDownloadFinish.isEmpty()) {
                        String argument = "var unity_webview_dl_url = '" + uri + "'; " + "var unity_webview_dl_id = " + downloadedID + "; ";
                        evaluateJS(argument + onDownloadFinish);
                    }
                }
            };
            UnityPlayer.currentActivity.registerReceiver(
                    onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            mWebView.setInitialScale(100);
            mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            // --------- drawing cache setting
            mWebView.clearCache(true);
            // ---------
            mWebView.setLongClickable(false);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.setVerticalScrollBarEnabled(true);
            mWebView.setBackgroundColor(0x00000000);
            mWebView.addJavascriptInterface(new TLabWebViewJSInterface(), "TLabWebViewActivity");
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setLoadWithOverviewMode(true);
            // --------- enable cache
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            webSettings.setAppCacheEnabled(true); // deprecated in API level 33.
            // ---------
            webSettings.setUseWideViewPort(true);
            webSettings.setSupportZoom(true);
            webSettings.setSupportMultipleWindows(true);    // add
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(true);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            if (userAgent != null && !userAgent.isEmpty()){
                webSettings.setUserAgentString(userAgent);
            }
            webSettings.setDefaultTextEncodingName("utf-8");
            webSettings.setDatabaseEnabled(true);
            webSettings.setDomStorageEnabled(true);

            UnityPlayer.currentActivity.addContentView(
                    mLayout,
                    new RelativeLayout.LayoutParams(
                            webWidth,
                            webHeight
                    )
            );
            mGlLayout.addView(
                    mWebView,
                    new GLLinearLayout.LayoutParams(
                            GLLinearLayout.LayoutParams.MATCH_PARENT,
                            GLLinearLayout.LayoutParams.MATCH_PARENT
                    )
            );
            mLayout.addView(
                    mGLSurfaceView,
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                    )
            );
            mLayout.addView(
                    mGlLayout,
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                    )
            );

            if (loadUrl != null) loadUrl(loadUrl);

            initialized = true;
        });
    }

    public void Destroy() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;

            mWebView.stopLoading();
            mGlLayout.removeView(mWebView);
            mWebView.destroy();
            mWebView = null;

            if(sharedTexture != null){
                sharedTexture.release();
                sharedTexture = null;
            }

            UnityPlayer.currentActivity.unregisterReceiver(
                    onDownloadComplete);
        });
    }

    // ---------------------------------------------------------------------------------------------------------
    // javascript interface
    //

    public class TLabWebViewJSInterface
    {
        @JavascriptInterface
        public void viewSource(final String src) {
            htmlCash = src;
        }

        @JavascriptInterface
        public void unitySendMessage(String go, String method, String message) {
            UnityPlayer.UnitySendMessage(go, method, message);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // java's unity interface.
    //

    public void updateSurface() {
        mGlLayout.postInvalidate();
    }

    public byte[] getPixel() {
        byte[] data = mViewToGlRenderer.getTexturePixels();
        updateSurface();
        return data;
    }

    public int getTexturePtr() {
        updateSurface();
        return sharedTextureId;
    }

    public String getCaptured() {
        return htmlCash;
    }

    public void captureElementById(String id) { loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.getElementById('" + id + "').outerHTML)"); }

    public void capturePage() { loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.documentElement.outerHTML)"); }

    public void setUserAgent(final String ua, final boolean reload) {
        // https://developer.mozilla.org/ja/docs/Web/HTTP/Headers/User-Agent/Firefox
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) return;

            try {
                mWebView.getSettings().setUserAgentString(ua);

                if(reload) {
                    mWebView.reload();
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        });

        userAgent = ua;
    }

    public void captureUserAgent() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            userAgent = mWebView.getSettings().getUserAgentString();
        });
    }

    public String getUserAgent() { return userAgent; }

    public String getCurrentUrl() { return actualUrl; }

    public void loadUrl(String url) {
        loadUrl = url;
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }

            if (mCustomHeaders != null && !mCustomHeaders.isEmpty()){
                mWebView.loadUrl(loadUrl, mCustomHeaders);
            }
            else{
                mWebView.loadUrl(loadUrl);
            }
        });
    }

    public void loadHtml(final String html, final String baseURL) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.loadDataWithBaseURL(baseURL, html, "text/html", "UTF8", null);
        });
    }

    public void zoomIn() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.zoomIn();
        });
    }

    public void zoomOut() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.zoomOut();
        });
    }

    public void registerOnPageFinishCallback(String onPageFinish) {
        this.onPageFinish = onPageFinish;
    }

    public void evaluateJS(String js) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.loadUrl("javascript:" + js);
        });
    }

    public void touchEvent(int x, int y, int eventNum) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }

            // Obtain MotionEvent object
            // https://banbara-studio.hatenablog.com/entry/2018/04/02/130902
            final long downTime = SystemClock.uptimeMillis();
            final long eventTime = SystemClock.uptimeMillis() + 50;
            final int source = InputDevice.SOURCE_CLASS_POINTER;

            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
            final int metaState = 0;
            MotionEvent event = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    eventNum,
                    x,
                    y,
                    metaState
            );
            event.setSource(source);

            // Dispatch touch event to view
            mWebView.dispatchTouchEvent(event);
        });
    }

    public void keyEvent(char key) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            KeyEvent[] events = kcm.getEvents(new char[]{key});
            for (KeyEvent event : events) mWebView.dispatchKeyEvent(event);
        });
    }

    public void backSpaceKey() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        });
    }

    public void goBack() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null || !canGoBack) {
                return;
            }
            mWebView.goBack();
        });
    }

    public void goForward() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null || !canGoForward) {
                return;
            }
            mWebView.goForward();
        });
    }

    public void clearCash(boolean includeDiskFiles) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            mWebView.clearCache(includeDiskFiles);
        });
    }

    public void clearHistory() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            mWebView.clearHistory();
        });
    }

    public void clearCookie() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        });
    }

    public void setVisible(boolean visible) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }

            if (visible) {
                mWebView.setVisibility(View.VISIBLE);
                mWebView.requestFocus();
            } else{
                mWebView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setOnPageFinish(String onPageFinish) {
        this.onPageFinish = onPageFinish;
    }

    public void setOnDownloadStart(String onDownloadStart) {
        this.onDownloadStart = onDownloadStart;
    }

    public void setOnDownloadFinish(String onDownloadFinish) {
        this.onDownloadFinish = onDownloadFinish;
    }

    public void requestCaptureDownloadProgress() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            DownloadManager dm = (DownloadManager) UnityPlayer.currentActivity.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = dm.query(new DownloadManager.Query());

            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                @SuppressLint("Range") int bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                downloadProgress = (float) bytesDownloadedSoFar / totalBytes;
            }
        });
    }

    public float getDownloadProgress() {
        return this.downloadProgress;
    }

    // ---------------------------------------------------------------------------------------------------------
    //
    //

    private void SetMargins(int left, int top, int right, int bottom) {
        final FrameLayout.LayoutParams params
                = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.NO_GRAVITY);
        params.setMargins(left, top, right, bottom);
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.setLayoutParams(params);
        });
    }

    private void AddCustomHeader(final String headerKey, final String headerValue) {
        if (mCustomHeaders == null) {
            return;
        }
        mCustomHeaders.put(headerKey, headerValue);
    }

    private String GetCustomHeaderValue(final String headerKey) {
        if (mCustomHeaders == null) {
            return null;
        }
        if (!mCustomHeaders.containsKey(headerKey)) {
            return null;
        }
        return this.mCustomHeaders.get(headerKey);
    }

    private void RemoveCustomHeader(final String headerKey) {
        if (mCustomHeaders == null) {
            return;
        }
        this.mCustomHeaders.remove(headerKey);
    }

    private void ClearCustomHeader() {
        if (mCustomHeaders == null) {
            return;
        }
        this.mCustomHeaders.clear();
    }

    private void showHttpAuthDialog(final HttpAuthHandler handler, final String host,
                                    final String realm, final String title,
                                    final String name, final String password)
    {
        final Activity activity = UnityPlayer.currentActivity;
        final AlertDialog.Builder mHttpAuthDialog = new AlertDialog.Builder(activity);
        LinearLayout layout = new LinearLayout(activity);

        mHttpAuthDialog.setTitle("Enter the password").setCancelable(false);
        final EditText etUserName = new EditText(activity);
        etUserName.setWidth(100);
        layout.addView(etUserName);
        final EditText etUserPass = new EditText(activity);
        etUserPass.setWidth(100);
        layout.addView(etUserPass);
        mHttpAuthDialog.setView(layout);

        mHttpAuthDialog.setPositiveButton("OK", (dialog, whichButton) -> {
            String userName = etUserName.getText().toString();
            String userPass = etUserPass.getText().toString();
            mWebView.setHttpAuthUsernamePassword(host, realm, userName, userPass);
            handler.proceed(userName, userPass);
            //mHttpAuthDialog = null;
        });
        mHttpAuthDialog.setNegativeButton("Cancel", (dialog, whichButton) -> {
            handler.cancel();
            //mHttpAuthDialog = null;
        });
        mHttpAuthDialog.create().show();
    }
}
