package com.tlab.libwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.EGLExt;
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
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.self.viewtoglrendering.CustomGLSurfaceView;
import com.unity3d.player.UnityPlayer;

import com.self.viewtoglrendering.ViewToGLRenderer;
import com.self.viewtoglrendering.GLLinearLayout;

import java.util.Hashtable;
// opengl
import android.content.Context;
import android.util.AttributeSet;
import android.opengl.EGL14;
import android.opengl.EGL15;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

// Android Studio Collapse definitions and methods
// https://stackoverflow.com/questions/18445044/android-studio-collapse-definitions-and-methods

public class UnityConnect extends Fragment {
//public class UnityConnect {

    // ---------------------------------------------------------------------------------------------------------
    // Renderer
    //

    private ViewToGLRenderer mViewToGlRenderer;
    private CustomGLSurfaceView mGLSurfaceView;
    //private GLSurfaceView mGLSurfaceView;

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

    private static int mWebWidth;
    private static int mWebHeight;
    private static int mTextureWidth;
    private static int mTextureHeight;
    private static int mScreenWidth;
    private static int mScreenHeight;
    private static int mTexId;
    private static int mDlOption;
    private static String mSubDir;
    private static String mLoadUrl;
    private static String mActualUrl;
    private static String mHTMLCash;

    private boolean canGoBack;
    private boolean canGoForward;
    private boolean initialized = false;

    private String androiString;
    private String userAgent;
    private Hashtable<String, String> mCustomHeaders;

    private final static String TAG = "libwebview";

    // ---------------------------------------------------------------------------------------------------------
    // Initialize this class
    //

    public void initialize(int webWidth, int webHeight,
                           int textureWidth, int textureHeight,
                           int screenWidth, int screenHeight,
                           String url, int dlOption, String subDir, int texId)
    {
        if(webWidth <= 0 || webHeight <= 0) return;

        mWebWidth = webWidth;
        mWebHeight = webHeight;
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mTexId = texId;

        mLoadUrl = url;
        mSubDir = subDir;
        mDlOption = dlOption;

        initWebView();
    }

    public boolean IsInitialized() {
        return initialized;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Save eglcontext to share texture ptr
    //

    private static EGLContext mContext = EGL10.EGL_NO_CONTEXT;
    private static EGLDisplay mDisplay = EGL10.EGL_NO_DISPLAY;
    private static EGLSurface mDSurface = EGL10.EGL_NO_SURFACE;
    private static EGLSurface mRSurface = EGL10.EGL_NO_SURFACE;
    private static EGLConfig[] mConfig = new EGLConfig[1];

    public static void unityJNITest() {
        // Test code for calling Java methods in GL.IssuePluginEvent
        Log.i(TAG, "success !");
    }

    private static int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
        int[] value = new int[1];
        if(egl.eglGetConfigAttrib(display, config, attribute, value)) {
            return value[0];
        }

        return defaultValue;
    }

    private static void enumAvailableCongifs(){
        EGL10 egl = (EGL10)EGLContext.getEGL();
        EGLDisplay display = egl.eglGetCurrentDisplay();
        // Find out how many EGLConfigs exist
        final int[] num_config = new int[1];
        egl.eglGetConfigs(display, null, 0, num_config);
        // Allocate and retrieve the EGLConfigs/handles
        int configCount = num_config[0];
        final EGLConfig[] configs = new EGLConfig[configCount];
        egl.eglGetConfigs(display, configs, configCount, num_config);

        EGLConfig bestMatch = null;
        int bestR = Integer.MAX_VALUE, bestG = Integer.MAX_VALUE;
        int bestB = Integer.MAX_VALUE, bestA = Integer.MAX_VALUE;
        int bestD = Integer.MAX_VALUE, bestS = Integer.MAX_VALUE;
        for(int i = 0; i < num_config[0]; i++){
            EGLConfig config = configs[i];
            int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
            int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
            int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
            int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
            int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
            int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
            int renderable_type = findConfigAttrib(egl, display, config, EGL10.EGL_RENDERABLE_TYPE, 0);

            Log.i(TAG, "configs[" + i + "]" + configs[0]);
            Log.i(TAG, "configs[" + i + "] :" +
                    "r: " + r + " " +
                    "g: " + g + " " +
                    "b: " + b + " " +
                    "a: " + a + " " +
                    "d: " + d + " " +
                    "s: " + s + " " +
                    "renderable_type: " + renderable_type);
        }
    }

    private static int getContextAttribute(EGL10 egl, EGLContext context, EGLDisplay display, int attribute){
        final int[] values = new int[1];
        egl.eglQueryContext(display, context, attribute, values);
        return values[0];
    }

    private static void enumContextAttrinute(){
        EGL10 egl = (EGL10)EGLContext.getEGL();
        EGLContext context = egl.eglGetCurrentContext();
        EGLDisplay display = egl.eglGetCurrentDisplay();

        int value;
        value = getContextAttribute(egl, context, display, EGL14.EGL_CONTEXT_CLIENT_VERSION);
        Log.i(TAG, "EGL14.EGL_CONTEXT_CLIENT_VERSION: " + value);
        value = getContextAttribute(egl, context, display, EGL14.EGL_CONTEXT_CLIENT_TYPE);
        Log.i(TAG, "EGL14.EGL_CONTEXT_CLIENT_TYPE: " + value);
        value = getContextAttribute(egl, context, display, EGL14.EGL_CONTEXT_LOST);
        Log.i(TAG, "EGL14.EGL_CONTEXT_LOST: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_MAJOR_VERSION);
        Log.i(TAG, "EGL15.EGL_CONTEXT_MAJOR_VERSION: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_MINOR_VERSION);
        Log.i(TAG, "EGL15.EGL_CONTEXT_MINOR_VERSION: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT);
        Log.i(TAG, "EGL15.EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT);
        Log.i(TAG, "EGL15.EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_OPENGL_DEBUG);
        Log.i(TAG, "EGL15.EGL_CONTEXT_OPENGL_DEBUG: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_OPENGL_FORWARD_COMPATIBLE);
        Log.i(TAG, "EGL15.EGL_CONTEXT_OPENGL_FORWARD_COMPATIBLE: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_OPENGL_PROFILE_MASK);
        Log.i(TAG, "EGL15.EGL_CONTEXT_OPENGL_PROFILE_MASK: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_OPENGL_RESET_NOTIFICATION_STRATEGY);
        Log.i(TAG, "EGL15.EGL_CONTEXT_OPENGL_RESET_NOTIFICATION_STRATEGY: " + value);
        value = getContextAttribute(egl, context, display, EGL15.EGL_CONTEXT_OPENGL_ROBUST_ACCESS);
        Log.i(TAG, "EGL15.EGL_CONTEXT_OPENGL_ROBUST_ACCESS: " + value);
        value = getContextAttribute(egl, context, display, EGLExt.EGL_CONTEXT_FLAGS_KHR);
        Log.i(TAG, "EGLExt.EGL_CONTEXT_FLAGS_KHR: " + value);
        value = getContextAttribute(egl, context, display, EGLExt.EGL_CONTEXT_MAJOR_VERSION_KHR);
        Log.i(TAG, "EGLExt.EGL_CONTEXT_MAJOR_VERSION_KHR: " + value);
        value = getContextAttribute(egl, context, display, EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR);
        Log.i(TAG, "EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR: " + value);
        value = getContextAttribute(egl, context, display, EGLExt.EGL_RECORDABLE_ANDROID);
        Log.i(TAG, "EGLExt.EGL_RECORDABLE_ANDROID: " + value);
        value = getContextAttribute(egl, context, display, EGLExt.EGL_OPENGL_ES3_BIT_KHR);
        Log.i(TAG, "EGLExt.EGL_OPENGL_ES3_BIT_KHR: " + value);
    }

    private static void chooseEGLConfig(){
        EGL10 egl = (EGL10)EGLContext.getEGL();
        EGLDisplay display = egl.eglGetCurrentDisplay();
        int[] attribList = new int[]{
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE
        };
        // No error checking performed, minimum required code to elucidate logic
        // Expand on this logic to be more selective in choosing a configuration
        int[] numConfig = new int[1];
        egl.eglChooseConfig(display, attribList, null, 0, numConfig);
        int configSize = numConfig[0];
        EGLConfig[] eglConfigs = new EGLConfig[configSize];
        egl.eglChooseConfig(display, attribList, eglConfigs, configSize, numConfig);

        mConfig[0] = eglConfigs[0];
    }

    public static void checkEGLContextExist(){
        if(mContext != EGL10.EGL_NO_CONTEXT){
            Log.i(TAG, "egl has already been acquired. Skip processing");
            return;
        }

        EGL10 egl = (EGL10)EGLContext.getEGL();
        mContext = egl.eglGetCurrentContext();
        mDisplay = egl.eglGetCurrentDisplay();
        mDSurface = egl.eglGetCurrentSurface(EGL10.EGL_DRAW);
        mRSurface = egl.eglGetCurrentSurface(EGL10.EGL_READ);

        enumAvailableCongifs();
        enumContextAttrinute();
        chooseEGLConfig();

        Log.i(TAG, "thread name: " + Thread.currentThread().getName());
        Log.i(TAG, "context class name: " + mContext.getClass().getName());

        if(mContext == EGL10.EGL_NO_CONTEXT)
            Log.i(TAG, "check exist but egl context is not created");
        else
            Log.i(TAG, "check exist and egl context created !!");

        if(mDisplay == EGL10.EGL_NO_DISPLAY)
            Log.i(TAG, "check exist but egl display is not created");
        else
            Log.i(TAG, "check exist and egl display created !!");

        if(mDSurface == EGL10.EGL_NO_SURFACE)
            Log.i(TAG, "check exist but egl draw surface is not created");
        else
            Log.i(TAG, "check exist and egl draw surface created !!");

        if(mRSurface == EGL10.EGL_NO_SURFACE)
            Log.i(TAG, "check exist but egl read surface is not created");
        else
            Log.i(TAG, "check exist and egl read surface created !!");

        //egl.eglMakeCurrent(mDisplay, mDSurface, mRSurface, mContext);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Test function (not included in the release)
    //

    public void createContextTest() { }

    // ---------------------------------------------------------------------------------------------------------
    // Initialize webview
    //

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

            mViewToGlRenderer = new ViewToGLRenderer();
            mViewToGlRenderer.SetTextureResolution(mTextureWidth, mTextureHeight);
            mViewToGlRenderer.SetWebResolution(mWebWidth, mWebHeight);
            mViewToGlRenderer.saveEGL(mContext, mDisplay, mDSurface, mRSurface);
            mViewToGlRenderer.createTextureCapture(UnityPlayer.currentActivity, mTexId, R.raw.vertex, R.raw.fragment_oes);

            Log.i(TAG, "unity texture id: " + mTexId);
            Log.i(TAG, "mViewToGLRenderer created");

            // mLayout settings
            mLayout = new RelativeLayout(UnityPlayer.currentActivity);
            mLayout.setGravity(Gravity.TOP);
            // set view to out of display.
            mLayout.setX(mScreenWidth);
            mLayout.setY(mScreenHeight);
            mLayout.setBackgroundColor(0x00000000);

            Log.i(TAG, "mLayout created");

            // mGLSurfaceView settings
            mGLSurfaceView = new CustomGLSurfaceView(UnityPlayer.currentActivity);
            //mGLSurfaceView = new GLSurfaceView(UnityPlayer.currentActivity);
            mGLSurfaceView.setEGLContextClientVersion(3);
            mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
            mGLSurfaceView.setPreserveEGLContextOnPause(true);
            mGLSurfaceView.setSharedContext(mContext, mDisplay, mConfig);
            mGLSurfaceView.setRenderer(mViewToGlRenderer);
            mGLSurfaceView.setBackgroundColor(0x00000000);
            Log.i(TAG, "mGLSurfaceView created");

            // mGlLayout settings
            mGlLayout = new GLLinearLayout(
                    UnityPlayer.currentActivity,
                    (float)mTextureWidth / mWebWidth,
                    (float)mTextureHeight / mWebHeight
            );
            mGlLayout.setOrientation(GLLinearLayout.VERTICAL);
            mGlLayout.setGravity(Gravity.START);
            mGlLayout.setViewToGLRenderer(mViewToGlRenderer);
            mGlLayout.setBackgroundColor(0x00000000);
//            new Thread(new Runnable() {
//                public void run() {
//                    while(true){
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                        mGlLayout.postInvalidate();
//                    }
//                }
//            }).start();

            Log.i(TAG, "mGlLayout created");

            if (mWebView == null) mWebView = new BitmapWebView(UnityPlayer.currentActivity);

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
                    mActualUrl = url;
                }

                @Override
                public void onLoadResource(WebView view, String url) {
                    canGoBack = mWebView.canGoBack();
                    canGoForward = mWebView.canGoForward();
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
//                    newWebView.add(new BitmapWebView(UnityPlayer.currentActivity));
//                    WebSettings webSettings = newWebView.getSettings();
//                    webSettings.setJavaScriptEnabled(true);
//                    webSettings.setSupportMultipleWindows(true);
//                    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//                    webSettings.setDomStorageEnabled(true);
//                    webSettings.setAllowFileAccess(true);
//                    webSettings.setUserAgentString(dbHelper.getUserConfig().get("userAgent"));
//                    newWebView.setFocusable(true);
//                    newWebView.setFocusableInTouchMode(true);
//
//                    newWebView.setWebViewClient(new WebViewClient());
//                    newWebView.setWebChromeClient(new WebChromeClient(){
//                        @Override
//                        public void onCloseWindow(WebView window) {
//                            window.setVisibility(View.GONE);
//                            myWebView.removeView(window);
//                        }
//                    });
//                    myWebView.addView(newWebView);
//
//                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
//                    transport.setWebView(newWebView);
//                    resultMsg.sendToTarget();
//                    return true;
                    return false;
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

                    if(mDlOption == DlOption.applicationFolder.ordinal()) {
                        request.setDestinationInExternalFilesDir(context, mSubDir, filename);
                    }else if(mDlOption == DlOption.downloadFolder.ordinal()) {
                        String downloadDir = Environment.DIRECTORY_DOWNLOADS;
                        request.setDestinationInExternalPublicDir(downloadDir, filename);
                    }

                    DownloadManager dm = (DownloadManager) UnityPlayer.currentActivity.getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);

                    Toast.makeText(context, filename + "　download is completed..", Toast.LENGTH_LONG).show();
                }
            });
            //mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setInitialScale(100);
            mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            // --------- drawing cache setting
            mWebView.clearCache(true);
            //mWebView.setDrawingCacheEnabled(true);
            // ---------
            mWebView.setLongClickable(false);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.setVerticalScrollBarEnabled(true);
            mWebView.setBackgroundColor(0x00000000);
            mWebView.addJavascriptInterface(new TLabJavascriptInterface(), "TLabWebViewActivity");
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setLoadWithOverviewMode(true);
            // --------- enable cache
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            //webSettings.setAppCacheEnabled(true); // deprecated in API level 33.
            // ---------
            webSettings.setUseWideViewPort(true);
            webSettings.setSupportZoom(true);
            webSettings.setSupportMultipleWindows(true);    // add
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(true);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            if (userAgent != null && userAgent.length() > 0){
                Log.i(TAG, "setUserAgentString(" + userAgent.toString() + ")");
                webSettings.setUserAgentString(userAgent);
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

            if (mLoadUrl != null) loadUrl(mLoadUrl);

            initialized = true;

            Log.i(TAG, "webView initialized");
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
        });
        Log.i(TAG, "destroy webview");
    }

    // ---------------------------------------------------------------------------------------------------------
    // javascript interface
    //

    public class TLabJavascriptInterface
    {
        @JavascriptInterface
        public void viewSource(final String src) {
            mHTMLCash = src;
            Log.i(TAG, "capture HTML source success");
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // java's unity interface.
    //

    public void updateSurface() {
        //mGLSurfaceView.postInvalidate();
        mGlLayout.postInvalidate();
        //mLayout.invalidate();
    }

    public byte[] getPixel() {
        byte[] data = mViewToGlRenderer.getTexturePixels();
        updateSurface();
        // Log.i("tlabwebview", "texture data exists");
        return data;
    }

    public int getTexPtr() { return mViewToGlRenderer.getTexturePtr(); }

    public String getCaptured(){
        return mHTMLCash;
    }

    public void captureElementById(String id){ loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.getElementById('" + id + "').outerHTML)"); }

    public void capturePage(){ loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.documentElement.outerHTML)"); }

    public void setUserAgent(final String ua, final boolean reload) {
        // https://developer.mozilla.org/ja/docs/Web/HTTP/Headers/User-Agent/Firefox
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) return;

            try {
                //String androidString = mWebView.getSettings().getUserAgentString().substring(userAgent.indexOf("("), userAgent.indexOf(")") + 1);
                //userAgent = mWebView.getSettings().getUserAgentString().replace(androidString,"X11; Ubuntu; Linux x86_64");
                mWebView.getSettings().setUserAgentString(ua);

                if(reload) mWebView.reload();

            }catch (Exception e){
                e.printStackTrace();
            }
        });

        userAgent = ua;
    }

    public void captureUserAgent(){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) return;
            userAgent = mWebView.getSettings().getUserAgentString();
        });
    }

    public String getUserAgent() { return userAgent; }

    public String getCurrentUrl() { return mActualUrl; }

    public void loadUrl(String url) {
        mLoadUrl = url;
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;

            if (mCustomHeaders != null && !mCustomHeaders.isEmpty())
                mWebView.loadUrl(mLoadUrl, mCustomHeaders);
            else
                mWebView.loadUrl(mLoadUrl);
        });
        //Log.i("tlabwebview", "url loaded: " + url.toString());
    }

    public void loadHtml(final String html, final String baseURL){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.loadDataWithBaseURL(baseURL, html, "text/html", "UTF8", null);
        });
        //Log.i("tlabwebview", "html loaded: " + baseURL.toString());
    }

    public void zoomIn(){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.zoomIn();
        });
        //Log.i("tlabwebview", "zoom in");
    }

    public void zoomOut(){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.zoomOut();
        });
        //Log.i("tlabwebview", "zoom out");
    }

    public void evaluateJS(String js){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.loadUrl("javascript:" + js);
        });
        //Log.i("tlabwebview", "evaluate javascript: " + js);
    }

    public void touchEvent(int x, int y, int eventNum) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;

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
        // Log.i("tlabwebview", "touch event dispatched: " + Integer.valueOf(x).toString() + ", " + Integer.valueOf(y).toString());
    }

    public void keyEvent(char key){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;
            KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            KeyEvent[] events = kcm.getEvents(new char[]{key});
            for (KeyEvent event : events) mWebView.dispatchKeyEvent(event);
        });
        //Log.i("tlabwebview", "key event dispatched: " + key);
    }

    public void backSpaceKey(){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        });
        //Log.i("tlabwebview", "back space key dispatched");
    }

    public void goBack() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null || !canGoBack) return;
            mWebView.goBack();
        });
        //Log.i("tlabwebview", "page backed out");
    }

    public void goForward() {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null || !canGoForward) return;
            mWebView.goForward();
        });
        //Log.i("tlabwebview", "page forwarded");
    }

    public void clearCash(boolean includeDiskFiles){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) return;
            mWebView.clearCache(includeDiskFiles);
        });
        //Log.i("tlabwebview", "web cache cleared");
    }

    public void clearHistory(){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) return;
            mWebView.clearHistory();
        });
        //Log.i("tlabwebview", "clear history");
    }

    public void clearCookie(){
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if(mWebView == null) return;
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        });
        //Log.i("tlabwebview", "cookie cleared");
    }

    public void setVisible(boolean visible) {
        UnityPlayer.currentActivity.runOnUiThread(() -> {
            if (mWebView == null) return;

            if (visible) {
                mWebView.setVisibility(View.VISIBLE);
                mWebView.requestFocus();
            } else
                mWebView.setVisibility(View.INVISIBLE);
        });
        //Log.i("tlabwebview", "set visibility: " + visibility);
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
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;
            mWebView.setLayoutParams(params);
        }});
    }

    private void AddCustomHeader(final String headerKey, final String headerValue) {
        if (mCustomHeaders == null) return;
        mCustomHeaders.put(headerKey, headerValue);
    }

    private String GetCustomHeaderValue(final String headerKey) {
        if (mCustomHeaders == null) return null;
        if (!mCustomHeaders.containsKey(headerKey)) return null;
        return this.mCustomHeaders.get(headerKey);
    }

    private void RemoveCustomHeader(final String headerKey) {
        if (mCustomHeaders == null) return;
        this.mCustomHeaders.remove(headerKey);
    }

    private void ClearCustomHeader() {
        if (mCustomHeaders == null) return;
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
