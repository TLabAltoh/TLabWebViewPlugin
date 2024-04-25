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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Base64;
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

import com.tlab.viewtohardwarebuffer.CustomGLSurfaceView;
import com.unity3d.player.UnityPlayer;

import com.robot9.shared.SharedTexture;
import com.tlab.viewtohardwarebuffer.ViewToHWBRenderer;
import com.tlab.viewtohardwarebuffer.GLLinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Objects;

public class UnityConnect extends Fragment {

    // ---------------------------------------------------------------------------------------------------------
    // Renderer
    //

    private ViewToHWBRenderer mViewToHWBRenderer;
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

    private int mWebWidth;
    private int mWebHeight;
    private int mTexWidth;
    private int mTexHeight;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mDlOption;

    private int mScrollX;
    private int mScrollY;

    private float mDownloadProgress = 0.0f;

    private String mOnPageFinish;
    private String mOnDownloadStart;
    private String mOnDownloadFinish;
    private String mDlUrlName;
    private String mDlUriName;
    private String mDlIdName;
    private String mSubDir;
    private String mLoadUrl;
    private String mActualUrl;
    private String mHtmlCash;

    private boolean mCanGoBack;
    private boolean mCanGoForward;
    private boolean mInitialized = false;

    private SharedTexture mSharedTexture;
    private HardwareBuffer mSharedBuffer;

    private int[] mHWBFboTexID;
    private int[] mHWBFboID;

    private String mUserAgent;
    private Hashtable<String, String> mCustomHeaders;

    private BroadcastReceiver mOnDownloadComplete;

    private final static String TAG = "libwebview";

    // ---------------------------------------------------------------------------------------------------------
    // Initialize this class
    //

    /**
     *
     * @param webWidth
     * @param webHeight
     * @param textureWidth
     * @param textureHeight
     * @param screenWidth
     * @param screenHeight
     * @param url
     */
    public void initialize(int webWidth, int webHeight,
                           int textureWidth, int textureHeight,
                           int screenWidth, int screenHeight,
                           String url)
    {
        if(webWidth <= 0 || webHeight <= 0) {
            return;
        }

        mWebWidth = webWidth;
        mWebHeight = webHeight;
        mTexWidth = textureWidth;
        mTexHeight = textureHeight;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mLoadUrl = url;

        initWebView();
    }

    /**
     *
     * @return
     */
    public boolean IsInitialized() {
        return mInitialized;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Shared Texture Utility
    //

    public void releaseSharedTexture() {
        if (mHWBFboTexID != null) {
            GLES30.glDeleteTextures(mHWBFboTexID.length, mHWBFboTexID, 0);
            mHWBFboTexID = null;
        }

        if (mHWBFboID != null) {
            GLES30.glDeleteTextures(mHWBFboID.length, mHWBFboID, 0);
            mHWBFboID = null;
        }

        if(mSharedTexture != null){
            mSharedTexture.release();
            mSharedTexture = null;
        }
    }

    /**
     *
     *
     */
    public void updateSharedTexture() {

        HardwareBuffer sharedBuffer = mViewToHWBRenderer.getHardwareBuffer();

        if (sharedBuffer == null || mSharedBuffer == sharedBuffer) {
            return;
        }

        releaseSharedTexture();

        mHWBFboTexID = new int[1];
        mHWBFboID = new int[1];
        GLES30.glGenTextures(mHWBFboTexID.length, mHWBFboTexID, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mHWBFboTexID[0]);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        SharedTexture sharedTexture = new SharedTexture(sharedBuffer);

        sharedTexture.bindTexture(mHWBFboTexID[0]);

        mSharedTexture = sharedTexture;
        mSharedBuffer = sharedBuffer;
    }

    // ---------------------------------------------------------------------------------------------------------
    // File download
    //

    public void getBase64FromBlobData(String url, String mimetype) {

        Context context = UnityPlayer.currentActivity.getApplicationContext();

        String extension = MimeTypes.getDefaultExt(mimetype);

        String base64 = url.replaceFirst("^data:.+;base64,", "");

        byte[] data = Base64.decode(base64, 0);

        boolean downloaded = false;

        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String filename = dateFormat.format(new Date()) + "." + extension;

        String downloadDir = "";

        if(mDlOption == DlOption.applicationFolder.ordinal()) {
            downloadDir = Objects.requireNonNull(context.getExternalFilesDir(null)).getPath();
        }
        else if(mDlOption == DlOption.downloadFolder.ordinal()) {
            downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        }

        if (!mSubDir.isEmpty()) {
            downloadDir = downloadDir + "/" + mSubDir;
        }

        File file = new File(downloadDir, filename);

        boolean fileExists = file.exists();

        if (!fileExists) {
            File directory = file.getParentFile();

            assert directory != null;
            fileExists = directory.exists();

            if (!fileExists) {
                fileExists = directory.mkdirs();
            }

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
        }

        if (fileExists) {
            try {
                FileOutputStream fos = new FileOutputStream(file, false);
                fos.write(data);
                fos.flush();
                fos.close();

                downloaded = true;
            } catch (IOException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        }

        if (downloaded) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mWebView.getSettings().getJavaScriptEnabled() && mOnDownloadStart != null && !mOnDownloadStart.isEmpty()) {
                    String argument = "var " + mDlUrlName + " = '" + url + "'; " + "var " + mDlIdName + " = " + -1 + "; ";
                    evaluateJS(argument + mOnDownloadStart);
                }

                if (mWebView.getSettings().getJavaScriptEnabled() && mOnDownloadFinish != null && !mOnDownloadFinish.isEmpty()) {
                    String argument = "var " + mDlUriName + " = '" + "none" + "'; " + "var " + mDlIdName + " = " + -1 + "; ";
                    evaluateJS(argument + mOnDownloadFinish);
                }
            }, 500);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Initialize webview
    //

    /**
     *
     *
     */
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

        UnityPlayer.currentActivity.runOnUiThread(() -> {

            mViewToHWBRenderer = new ViewToHWBRenderer();
            mViewToHWBRenderer.SetTextureResolution(mTexWidth, mTexHeight);

            mLayout = new RelativeLayout(UnityPlayer.currentActivity);
            mLayout.setGravity(Gravity.TOP);
            mLayout.setX(mScreenWidth);
            mLayout.setY(mScreenHeight);
            mLayout.setBackgroundColor(0xFFFFFFFF);

            mGLSurfaceView = new CustomGLSurfaceView(UnityPlayer.currentActivity);
            mGLSurfaceView.setEGLContextClientVersion(3);
            mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
            mGLSurfaceView.setPreserveEGLContextOnPause(true);
            mGLSurfaceView.setRenderer(mViewToHWBRenderer);
            mGLSurfaceView.setBackgroundColor(0x00000000);

            mGlLayout = new GLLinearLayout(
                    UnityPlayer.currentActivity,
                    (float) mTexWidth / mWebWidth,
                    (float) mTexHeight / mWebHeight
            );
            mGlLayout.setOrientation(GLLinearLayout.VERTICAL);
            mGlLayout.setGravity(Gravity.START);
            mGlLayout.setViewToGLRenderer(mViewToHWBRenderer);
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
                        showHttpAuthDialog(handler, host, realm);
                    }
                }

                /**
                 *
                 * @param view The WebView that is initiating the callback.
                 * @param url The url to be loaded.
                 * @param favicon The favicon for this page if it already exists in the
                 *            database.
                 */
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    mCanGoBack = mWebView.canGoBack();
                    mCanGoForward = mWebView.canGoForward();
                }

                /**
                 *
                 * @param view The WebView that is initiating the callback.
                 * @param url The url of the page.
                 */
                @Override
                public void onPageFinished(WebView view, String url) {
                    mCanGoBack = mWebView.canGoBack();
                    mCanGoForward = mWebView.canGoForward();
                    mActualUrl = url;

                    if (mWebView.getSettings().getJavaScriptEnabled() && mOnPageFinish != null && !mOnPageFinish.isEmpty()) {
                        evaluateJS(mOnPageFinish);
                    }
                }

                /**
                 *
                 * @param view The WebView that is initiating the callback.
                 * @param url The url of the resource the WebView will load.
                 */
                @Override
                public void onLoadResource(WebView view, String url) {
                    mCanGoBack = mWebView.canGoBack();
                    mCanGoForward = mWebView.canGoForward();
                }

                /**
                 *
                 * @param view The WebView that is initiating the callback.
                 * @param handler An {@link SslErrorHandler} that will handle the user's
                 *            response.
                 * @param error The SSL error object.
                 */
                @SuppressLint("WebViewClientOnReceivedSslError")
                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    handler.proceed();
                }

                /**
                 *
                 * @param view The WebView that is initiating the callback.
                 * @param url The URL to be loaded.
                 * @return
                 */
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    mCanGoBack = mWebView.canGoBack();
                    mCanGoForward = mWebView.canGoForward();
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

                /**
                 *
                 */
                @Override
                public void onHideCustomView() {
                    super.onHideCustomView();
                    mWebView.removeAllViews();
                }

                /**
                 *
                 * @param request the PermissionRequest from current web content.
                 */
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
                /**
                 * <a href="https://gist.github.com/miktam/107a414ec43de181b481">...</a>
                 * <a href="https://teratail.com/questions/115988">...</a>
                 * <a href="https://www.baeldung.com/java-mime-type-file-extension">...</a>
                 *
                 * @param url                The full url to the content that should be downloaded
                 * @param userAgent          the user agent to be used for the download.
                 * @param contentDisposition Content-disposition http header, if
                 *                           present.
                 * @param mimetype           The mimetype of the content reported by the server
                 * @param contentLength      The file size reported by the server
                 */
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                    if (url.startsWith("https://") || url.startsWith("http://")) {
                        Context context = UnityPlayer.currentActivity.getApplicationContext();

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

                        if(mDlOption == DlOption.applicationFolder.ordinal()) {
                            request.setDestinationInExternalFilesDir(context, mSubDir, filename);
                        }
                        else if(mDlOption == DlOption.downloadFolder.ordinal()) {
                            if (!mSubDir.isEmpty()) {
                                filename = mSubDir + "/" + filename;
                            }
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                        }

                        DownloadManager dm = (DownloadManager) UnityPlayer.currentActivity.getSystemService(Context.DOWNLOAD_SERVICE);
                        id = dm.enqueue(request);

                        if (mWebView.getSettings().getJavaScriptEnabled() && mOnDownloadStart != null && !mOnDownloadStart.isEmpty()) {
                            String argument = "var " + mDlUrlName + " = '" + url + "'; " + "var " + mDlIdName + " = " + id + "; ";
                            evaluateJS(argument + mOnDownloadStart);
                        }
                    }
                    else if (url.startsWith("data:")) { // data url scheme
                        // write base64 data to file stream
                        getBase64FromBlobData(url, mimetype);
                    }
                    else if (url.startsWith("blob:")) { // blob url scheme
                        // get base64 from blob url and write to file stream
                        String js = "var xhr = new XMLHttpRequest();" +
                                "xhr.open('GET', '"+ url +"', true);" +
                                "xhr.setRequestHeader('Content-type','" + mimetype +";charset=UTF-8');" +
                                "xhr.responseType = 'blob';" +
                                "xhr.onload = function(e) {" +
                                "    if (this.status == 200) {" +
                                "        var blobFile = this.response;" +
                                "        var reader = new FileReader();" +
                                "        reader.readAsDataURL(blobFile);" +
                                "        reader.onloadend = function() {" +
                                "            base64data = reader.result;" +
                                "            window.TLabWebViewActivity.getBase64StringFromBlobUrl(base64data, '"+ mimetype +"');" +
                                "        }" +
                                "    }" +
                                "};" +
                                "xhr.send();";

                        evaluateJS(js);
                    }
                }
            });
            mWebView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                /**
                 *
                 * @param v The view whose scroll position has changed.
                 * @param x Current horizontal scroll origin.
                 * @param y Current vertical scroll origin.
                 * @param oldX Previous horizontal scroll origin.
                 * @param oldY Previous vertical scroll origin.
                 */
                @Override
                public void onScrollChange(View v, int x, int y, int oldX, int oldY) {
                    mScrollX = x;
                    mScrollY = y;
                }
            });

            // create download complete event receiver.
            mOnDownloadComplete = new BroadcastReceiver() {
                /**
                 *
                 * @param context The Context in which the receiver is running.
                 * @param intent The Intent being received.
                 */
                @Override
                public void onReceive(Context context, Intent intent) {
                    long downloadedID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri uri = dm.getUriForDownloadedFile(downloadedID);

                    if (mWebView.getSettings().getJavaScriptEnabled() && mOnDownloadFinish != null && !mOnDownloadFinish.isEmpty()) {
                        String argument = "var " + mDlUriName + " = '" + uri + "'; " + "var " + mDlIdName + " = " + downloadedID + "; ";
                        evaluateJS(argument + mOnDownloadFinish);
                    }
                }
            };
            UnityPlayer.currentActivity.registerReceiver(
                    mOnDownloadComplete,
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
            if (mUserAgent != null && !mUserAgent.isEmpty()){
                webSettings.setUserAgentString(mUserAgent);
            }
            webSettings.setDefaultTextEncodingName("utf-8");
            webSettings.setDatabaseEnabled(true);
            webSettings.setDomStorageEnabled(true);

            UnityPlayer.currentActivity.addContentView(
                    mLayout,
                    new RelativeLayout.LayoutParams(
                            mWebWidth,
                            mWebHeight
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

            if (mLoadUrl != null) {
                loadUrl(mLoadUrl);
            }

            mInitialized = true;
        });
    }

    /**
     *
     */
    public void Destroy() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }

            mWebView.stopLoading();
            mGlLayout.removeView(mWebView);
            mWebView.destroy();
            mWebView = null;

            if(mSharedTexture != null){
                mSharedTexture.release();
                mSharedTexture = null;
            }

            UnityPlayer.currentActivity.unregisterReceiver(
                    mOnDownloadComplete);
        });
    }

    // ---------------------------------------------------------------------------------------------------------
    // javascript interface
    //

    public class TLabWebViewJSInterface
    {
        /**
         *
         * @param src
         */
        @JavascriptInterface
        public void viewSource(final String src) {
            mHtmlCash = src;
        }

        /**
         *
         * @param go
         * @param method
         * @param message
         */
        @JavascriptInterface
        public void unitySendMessage(String go, String method, String message) {
            UnityPlayer.UnitySendMessage(go, method, message);
        }

        /**
         *
         * @param url
         * @param mimetype
         */
        @JavascriptInterface
        public void getBase64StringFromBlobUrl(String url, String mimetype) {
            getBase64FromBlobData(url, mimetype);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // java's unity interface.
    //

    /**
     *
     *
     */
    public void updateSurface() {
        mGlLayout.postInvalidate();
    }

    /**
     *
     * @return
     */
    public int getTexturePtr() {
        updateSurface();

        if (mHWBFboTexID == null) {
            return 0;
        }

        return mHWBFboTexID[0];
    }

    /**
     *
     * @return
     */
    public String getCaptured() {
        return mHtmlCash;
    }

    /**
     *
     * @param id
     */
    public void captureElementById(String id) { loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.getElementById('" + id + "').outerHTML)"); }

    /**
     *
     */
    public void capturePage() { loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.documentElement.outerHTML)"); }

    /**
     *
     * @param ua
     * @param reload
     */
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

        mUserAgent = ua;
    }

    /**
     *
     */
    public void captureUserAgent() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            mUserAgent = mWebView.getSettings().getUserAgentString();
        });
    }

    /**
     *
     * @return
     */
    public String getUserAgent() { return mUserAgent; }

    /**
     *
     * @return
     */
    public String getCurrentUrl() { return mActualUrl; }

    /**
     *
     * @param url
     */
    public void loadUrl(String url) {
        mLoadUrl = url;
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }

            if (mCustomHeaders != null && !mCustomHeaders.isEmpty()){
                mWebView.loadUrl(mLoadUrl, mCustomHeaders);
            }
            else{
                mWebView.loadUrl(mLoadUrl);
            }
        });
    }

    /**
     *
     * @param html
     * @param baseURL
     */
    public void loadHtml(final String html, final String baseURL) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.loadDataWithBaseURL(baseURL, html, "text/html", "UTF8", null);
        });
    }

    /**
     *
     */
    public void zoomIn() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.zoomIn();
        });
    }

    /**
     *
     */
    public void zoomOut() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.zoomOut();
        });
    }

    /**
     *
     * @return
     */
    public int getScrollX() {
        return mScrollX;
    }

    /**
     *
     * @return
     */
    public int getScrollY() {
        return mScrollY;
    }

    /**
     *
     * @param x
     * @param y
     */
    public void setScroll(int x, int y) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }

            mWebView.scrollTo(x, y);
        });
    }

    /**
     *
     * @param js
     */
    public void evaluateJS(String js) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.loadUrl("javascript:" + js);
        });
    }

    /**
     *
     *
     * @param textureWidth
     * @param textureHeight
     */
    public void resizeTex(int textureWidth, int textureHeight) {
        mTexWidth = textureWidth;
        mTexHeight = textureHeight;

        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mViewToHWBRenderer == null) {
                return;
            }
            mViewToHWBRenderer.SetTextureResolution(mTexWidth, mTexHeight);
            mViewToHWBRenderer.requestResize();
        });
    }

    public void resizeWeb(int webWidth, int webHeight) {
        mWebWidth = webWidth;
        mWebHeight = webHeight;

        UnityPlayer.currentActivity.runOnUiThread(() -> {
            mGlLayout.mRatioWidth = (float) mTexWidth / mWebWidth;
            mGlLayout.mRatioHeight = (float) mTexHeight / mWebHeight;

            ViewGroup.LayoutParams lp = mLayout.getLayoutParams();
            lp.width = mWebWidth;
            lp.height = mWebHeight;

            mLayout.setLayoutParams(lp);
        });
    }

    /**
     *
     * @param x
     * @param y
     * @param eventNum
     */
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

    /**
     *
     * @param key
     */
    public void keyEvent(char key) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            KeyEvent[] events = kcm.getEvents(new char[]{key});
            for (KeyEvent event : events) {
                mWebView.dispatchKeyEvent(event);
            }
        });
    }

    /**
     *
     */
    public void backSpaceKey() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) {
                return;
            }
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        });
    }

    /**
     *
     */
    public void goBack() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null || !mCanGoBack) {
                return;
            }
            mWebView.goBack();
        });
    }

    /**
     *
     */
    public void goForward() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null || !mCanGoForward) {
                return;
            }
            mWebView.goForward();
        });
    }

    /**
     *
     * @param includeDiskFiles
     */
    public void clearCash(boolean includeDiskFiles) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            mWebView.clearCache(includeDiskFiles);
        });
    }

    /**
     *
     */
    public void clearHistory() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            mWebView.clearHistory();
        });
    }

    /**
     *
     */
    public void clearCookie() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) {
                return;
            }
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        });
    }

    /**
     *
     * @param visible
     */
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

    /**
     *
     * @param dl_url_name
     * @param dl_uri_name
     * @param dl_id_name
     */
    public void setDownloadEventVariableName(
            String dl_url_name, String dl_uri_name, String dl_id_name) {
        mDlUrlName = dl_url_name;
        mDlUriName = dl_uri_name;
        mDlIdName = dl_id_name;
    }

    /**
     *
     * @param subDir
     */
    public void setSubDir(String subDir) {
        mSubDir = subDir;
    }

    /**
     *
     * @param dlOption
     */
    public void setDlOption(int dlOption) {
        mDlOption = dlOption;
    }

    /**
     *
     * @param onPageFinish
     */
    public void setOnPageFinish(String onPageFinish){
        mOnPageFinish = onPageFinish;
    }

    /**
     *
     * @param onDownloadStart
     */
    public void setOnDownloadStart(String onDownloadStart) {
        mOnDownloadStart = onDownloadStart;
    }

    /**
     *
     * @param onDownloadFinish
     */
    public void setOnDownloadFinish(String onDownloadFinish) {
        mOnDownloadFinish = onDownloadFinish;
    }

    /**
     *
     */
    public void requestCaptureDownloadProgress() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            DownloadManager dm = (DownloadManager) UnityPlayer.currentActivity.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = dm.query(new DownloadManager.Query());

            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                @SuppressLint("Range") int bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                mDownloadProgress = (float) bytesDownloadedSoFar / totalBytes;
            }
        });
    }

    /**
     *
     * @return
     */
    public float getDownloadProgress() {
        return this.mDownloadProgress;
    }

    // ---------------------------------------------------------------------------------------------------------
    //
    //

    /**
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
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

    /**
     *
     * @param headerKey
     * @param headerValue
     */
    private void AddCustomHeader(final String headerKey, final String headerValue) {
        if (mCustomHeaders == null) {
            return;
        }
        mCustomHeaders.put(headerKey, headerValue);
    }

    /**
     *
     * @param headerKey
     * @return
     */
    private String GetCustomHeaderValue(final String headerKey) {
        if (mCustomHeaders == null) {
            return null;
        }
        if (!mCustomHeaders.containsKey(headerKey)) {
            return null;
        }
        return mCustomHeaders.get(headerKey);
    }

    /**
     *
     * @param headerKey
     */
    private void RemoveCustomHeader(final String headerKey) {
        if (mCustomHeaders == null) {
            return;
        }
        mCustomHeaders.remove(headerKey);
    }

    /**
     *
     *
     */
    private void ClearCustomHeader() {
        if (mCustomHeaders == null) {
            return;
        }
        mCustomHeaders.clear();
    }

    /**
     *
     * @param handler
     * @param host
     * @param realm
     */
    private void showHttpAuthDialog(final HttpAuthHandler handler, final String host,
                                    final String realm)
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
