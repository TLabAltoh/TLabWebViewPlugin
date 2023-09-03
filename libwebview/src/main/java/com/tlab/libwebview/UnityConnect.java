package com.tlab.libwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
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
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.unity3d.player.UnityPlayer;
import com.self.viewtoglrendering.ViewToGLRenderer;
import com.self.viewtoglrendering.GLLinearLayout;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class UnityConnect  extends Fragment {

    // ---------------------------------------------------------------------------------------------------------
    // Instance.
    //

    private static UnityConnect m_Instance;

    // ---------------------------------------------------------------------------------------------------------
    // Renderer
    //

    private ViewToGLRenderer mViewToGlRenderer;
    private GLSurfaceView mGLSurfaceView;

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

    private static int mWebWidth;
    private static int mWebHeight;
    private static int mTextureWidth;
    private static int mTextureHeight;
    private static int mScreenWidth;
    private static int mScreenHeight;
    private boolean canGoBack;
    private boolean canGoForward;
    private static String mLoadUrl;
    private String gameObject;
    private String userAgent;
    private Hashtable<String, String> mCustomHeaders;

    // ---------------------------------------------------------------------------------------------------------
    // Initialize this class
    //

    public static void initialize(int webWidth, int webHeight,
                                  int textureWidth, int textureHeight,
                                  int screenWidth, int screenHeight, String url)
    {
        if(webWidth <= 0 || webHeight <= 0) {
            Log.i("libwebview", "initialize: web resolution unsuitable");
            return;
        }
        mWebWidth = webWidth;
        mWebHeight = webHeight;
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mLoadUrl = url;

        if (m_Instance != null) return;
        m_Instance = new UnityConnect();
        m_Instance.initWebView();
    }

    public boolean IsInitialized() {
        return mWebView != null;
    }

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

        mViewToGlRenderer = new ViewToGLRenderer();
        mViewToGlRenderer.SetTextureResolution(mTextureWidth, mTextureHeight);
        mViewToGlRenderer.SetWebResolution(mWebWidth, mWebHeight);

        Log.i("tlabwebview", "texture resolution finished");

        mViewToGlRenderer.createTexture();

        Log.i("tlabwebview", "createTexture() finished");

        mViewToGlRenderer.createTextureCapture(
                UnityPlayer.currentActivity,
                R.raw.vertex,
                R.raw.fragment_oes
        );

        Log.i("tlabwebview", "createTextureCapture() finished");
        Log.i("tlabwebview", "mViewToGLRenderer created");

        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void run() {
                // mLayout settings
                mLayout = new RelativeLayout(UnityPlayer.currentActivity);
                mLayout.setGravity(Gravity.TOP);
                // set view to out of display.
                mLayout.setX(mScreenWidth);
                mLayout.setY(mScreenHeight);
                mLayout.setBackgroundColor(0x00000000);

                Log.i("tlabwebview", "mLayout created");

                // mGLSurfaceView settings
                mGLSurfaceView = new GLSurfaceView(UnityPlayer.currentActivity);
                mGLSurfaceView.setEGLContextClientVersion(2);
                mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
                mGLSurfaceView.setPreserveEGLContextOnPause(true);
                mGLSurfaceView.setRenderer(mViewToGlRenderer);
                mGLSurfaceView.setBackgroundColor(0x00000000);

                Log.i("tlabwebview", "mGLSurfaceView created");

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

                Log.i("tlabwebview", "mGlLayout created");

                if (mWebView == null) mWebView = new BitmapWebView(UnityPlayer.currentActivity);

                // --------------------------------------------------------------------------------------------------------
                // Settings each View and View Group
                //

                // mWebView settings
                mWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        mWebView.loadUrl("about:blank");
                        canGoBack = mWebView.canGoBack();
                        canGoForward = mWebView.canGoForward();
                    }

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
                    }

                    @Override
                    public void onLoadResource(WebView view, String url) {
                        canGoBack = mWebView.canGoBack();
                        canGoForward = mWebView.canGoForward();
                    }

                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                        if (mCustomHeaders == null || mCustomHeaders.isEmpty()) {
                            return super.shouldInterceptRequest(view, url);
                        }

                        try {
                            HttpURLConnection urlCon = (HttpURLConnection) (new URL(url)).openConnection();
                            // The following should make HttpURLConnection have a same user-agent of webView)
                            // cf. http://d.hatena.ne.jp/faw/20070903/1188796959 (in Japanese)
                            urlCon.setRequestProperty("User-Agent", userAgent);

                            for (HashMap.Entry<String, String> entry : mCustomHeaders.entrySet()) {
                                urlCon.setRequestProperty(entry.getKey(), entry.getValue());
                            }

                            urlCon.connect();

                            return new WebResourceResponse(
                                    urlCon.getContentType().split(";", 2)[0],
                                    urlCon.getContentEncoding(),
                                    urlCon.getInputStream()
                            );

                        } catch (Exception e) {
                            return super.shouldInterceptRequest(view, url);
                        }
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

                mWebView.setWebChromeClient(new WebChromeClient(){
                    @Override
                    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                        /*
                        newWebView.add(new BitmapWebView(UnityPlayer.currentActivity));
                        WebSettings webSettings = newWebView.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        webSettings.setSupportMultipleWindows(true);
                        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                        webSettings.setDomStorageEnabled(true);
                        webSettings.setAllowFileAccess(true);
                        webSettings.setUserAgentString(dbHelper.getUserConfig().get("userAgent"));
                        newWebView.setFocusable(true);
                        newWebView.setFocusableInTouchMode(true);

                        newWebView.setWebViewClient(new WebViewClient());
                        newWebView.setWebChromeClient(new WebChromeClient(){
                            @Override
                            public void onCloseWindow(WebView window) {
                                window.setVisibility(View.GONE);
                                myWebView.removeView(window);
                            }
                        });
                        myWebView.addView(newWebView);

                        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                        transport.setWebView(newWebView);
                        resultMsg.sendToTarget();
                        return true;
                         */
                        return false;
                    }
                });
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.setInitialScale(100);
                mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                mWebView.clearCache(true);
                mWebView.setDrawingCacheEnabled(true);
                mWebView.setLongClickable(false);
                mWebView.setVisibility(View.VISIBLE);
                mWebView.setVerticalScrollBarEnabled(true);
                mWebView.setBackgroundColor(0x00000000);
                mWebView.zoomIn();
                WebSettings webSettings = mWebView.getSettings();
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setSupportZoom(true);
                webSettings.setSupportMultipleWindows(true);    // add
                webSettings.setBuiltInZoomControls(false);
                webSettings.setDisplayZoomControls(true);
                webSettings.setJavaScriptEnabled(true);
                webSettings.setAllowUniversalAccessFromFileURLs(true);
                webSettings.setMediaPlaybackRequiresUserGesture(false);
                if (userAgent != null && userAgent.length() > 0){
                    Log.i("tlabwebview", "setUserAgentString(" + userAgent.toString() + ")");
                    webSettings.setUserAgentString(userAgent);
                }
                webSettings.setDefaultTextEncodingName("utf-8");
                webSettings.setDatabaseEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setDatabasePath(
                        mWebView.getContext().getDir(
                                "databases",
                                Context.MODE_PRIVATE
                        ).getPath()
                );

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

                if (mLoadUrl != null) LoadURL(mLoadUrl);
            }
        });

        Log.i("tlabwebview", "webView initialized");
    }

    public void Destroy() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {
            public void run() {
                if (mWebView == null) return;
                mWebView.stopLoading();
                mGlLayout.removeView(mWebView);
                mWebView.destroy();
                mWebView = null;
            }
        });
        Log.i("tlabwebview", "libwebview---Destroy: destroy webview");
    }

    // ---------------------------------------------------------------------------------------------------------
    // java's unity interface.
    //

    public static byte[] getPixel() {
        if (m_Instance == null) {
            Log.i("tlabwebview: ", "texture data does not exists");
            return new byte[0];
        }

        byte[] data = m_Instance.mViewToGlRenderer.getTexturePixels();
        m_Instance.mGlLayout.postInvalidate();
        // Log.i("tlabwebview", "texture data exists");
        return data;
    }

    public static void setUserAgent(String ua) {
        if (m_Instance == null) return;
        m_Instance.userAgent = ua;
    }

    public static void setGameObjectString(String gos) {
        if (m_Instance == null) return;
        m_Instance.gameObject = gos;
    }

    public static void loadUrl(String url) {
        if (m_Instance == null) return;
        m_Instance.LoadURL(url);
    }

    public static void zoomIn(){
        if(m_Instance == null) return;
        m_Instance.ZoomIn();
    }

    public static void zoomOut(){
        if(m_Instance == null) return;
        m_Instance.ZoomOut();
    }

    public static void evaluateJS(String js){
        if(m_Instance == null) return;
        m_Instance.EvaluateJS(js);
    }

    public static void touchEvent(int x, int y, int event) {
        if(m_Instance == null) return;
        m_Instance.TouchEvent(x, y, event);
    }

    public static void keyEvent(char key){
        if(m_Instance == null)return;
        m_Instance.KeyEvent(key);
    }

    public static void backSpaceKey(){
        if(m_Instance == null)return;
        m_Instance.BackSpaceKey();
    }

    public static void goBack() {
        if (m_Instance == null) return;
        m_Instance.GoBack();
    }

    public static void goForward() {
        if (m_Instance == null) return;
        m_Instance.GoForward();
    }

    public static void setVisible(boolean visible) {
        if (m_Instance == null) return;
        m_Instance.SetVisibility(visible);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Browser manipulation functions
    //

    public void LoadURL(String url) {
        mLoadUrl = url;
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            if (mCustomHeaders != null && !mCustomHeaders.isEmpty())
                mWebView.loadUrl(mLoadUrl, mCustomHeaders);
            else
                mWebView.loadUrl(mLoadUrl);
        }});

        //Log.i("tlabwebview", "libwebview---LoadURL: url: " + url.toString() + " loaded");
    }

    public void LoadHTML(final String html, final String baseURL) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            mWebView.loadDataWithBaseURL(baseURL, html, "text/html", "UTF8", null);
        }});
        //Log.i("TlabBrowser", "libwebview---LoadHTML: html: " + baseURL.toString() + "loaded");
    }

    public void ZoomIn(){
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            mWebView.zoomIn();
        }});
        //Log.i("TlabBrowser", "libwebview---ZoomIn");
    }

    public void ZoomOut(){
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            mWebView.zoomOut();
        }});
        //Log.i("TlabBrowser", "libwebview---ZoomOut");
    }

    public void EvaluateJS(final String js) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            mWebView.loadUrl("javascript:" + js);
        }});
    }

    public void TouchEvent(int x, int y, int eventNum) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            // Obtain MotionEvent object
            // https://banbara-studio.hatenablog.com/entry/2018/04/02/130902
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis() + 50;
            int source = InputDevice.SOURCE_CLASS_POINTER;

            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
            int metaState = 0;
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
        }});
        // Log.i("tlabwebview", "touch event dispatched: " + Integer.valueOf(x).toString() + ", " + Integer.valueOf(y).toString());
    }

    public static char toLower(char c) {
        if (c >= 'A' && c <= 'Z') {
            c += 0x20;/*  www. jav a  2 s. c  o m*/
        }
        return c;
    }

    public  void KeyEvent(char key){
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            KeyEvent[] events = kcm.getEvents(new char[]{key});
            for (int i = 0; i < events.length; i++)
                mWebView.dispatchKeyEvent(events[i]);
        }});
        //Log.i("tlabwebview", "key event dispatched");
    }

    public void BackSpaceKey() {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        }});
    }

    public void GoBack() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null || !canGoBack) return;
            mWebView.goBack();
        }});
        //Log.i("tlabwebview", "page backed out");
    }

    public void GoForward() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null || !canGoForward) return;
            mWebView.goForward();
        }});
        //Log.i("tlabwebview", "page forwarded");
    }

    public void SetMargins(int left, int top, int right, int bottom) {
        final FrameLayout.LayoutParams params
                = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.NO_GRAVITY);
        params.setMargins(left, top, right, bottom);
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            mWebView.setLayoutParams(params);
        }});
    }

    public void SetVisibility(final boolean visibility) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(new Runnable() {public void run() {
            if (mWebView == null) return;

            if (visibility) {
                mWebView.setVisibility(View.VISIBLE);
                mWebView.requestFocus();
            } else
                mWebView.setVisibility(View.INVISIBLE);
        }});
    }

    public void AddCustomHeader(final String headerKey, final String headerValue) {
        if (mCustomHeaders == null) return;
        mCustomHeaders.put(headerKey, headerValue);
    }

    public String GetCustomHeaderValue(final String headerKey) {
        if (mCustomHeaders == null) return null;

        if (!mCustomHeaders.containsKey(headerKey)) return null;

        return this.mCustomHeaders.get(headerKey);
    }

    public void RemoveCustomHeader(final String headerKey) {
        if (mCustomHeaders == null) return;

        if (this.mCustomHeaders.containsKey(headerKey))
            this.mCustomHeaders.remove(headerKey);
    }

    public void ClearCustomHeader() {
        if (mCustomHeaders == null) return;
        this.mCustomHeaders.clear();
    }

    public void ClearCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            final Activity a = UnityPlayer.currentActivity;
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(a);
            cookieSyncManager.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncManager.stopSync();
            cookieSyncManager.sync();
        }
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

        mHttpAuthDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String userName = etUserName.getText().toString();
                String userPass = etUserPass.getText().toString();
                mWebView.setHttpAuthUsernamePassword(host, realm, userName, userPass);
                handler.proceed(userName, userPass);
                //mHttpAuthDialog = null;
            }
        });
        mHttpAuthDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                handler.cancel();
                //mHttpAuthDialog = null;
            }
        });
        mHttpAuthDialog.create().show();
    }
}
