package com.tlab.webkit.chromium;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Message;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;

import com.tlab.webkit.Common;
import com.tlab.webkit.IBrowser;
import com.tlab.webkit.Common.*;
import com.tlab.widget.AlertDialog;
import com.unity3d.player.UnityPlayer;

import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnityConnect extends OffscreenBrowser implements IBrowser {

    private WebView mWebView;
    private View mVideoView;

    private ValueCallback<Uri[]> mFilePathCallback;

    private final Map<String, ByteBuffer> mJSPrivateBuffer = new Hashtable<>();
    private final Map<String, ByteBuffer> mJSPublicBuffer = new Hashtable<>();

    private final static String TAG = "TLabWebView (Chromium)";

    public class JSInterface {

        @JavascriptInterface
        public void postResult(final int id, final String result) {
            mAsyncResult.post(new AsyncResult(id, result), AsyncResult.Status.COMPLETE);
        }

        @JavascriptInterface
        public void postResult(final int id, final int result) {
            mAsyncResult.post(new AsyncResult(id, result), AsyncResult.Status.COMPLETE);
        }

        @JavascriptInterface
        public void postResult(final int id, final double result) {
            mAsyncResult.post(new AsyncResult(id, result), AsyncResult.Status.COMPLETE);
        }

        @JavascriptInterface
        public void postResult(final int id, final boolean result) {
            mAsyncResult.post(new AsyncResult(id, result), AsyncResult.Status.COMPLETE);
        }

        @JavascriptInterface
        public void unitySendMessage(String go, String method, String message) {
            UnityPlayer.UnitySendMessage(go, method, message);
        }

        @JavascriptInterface
        public void unityPostMessage(String message) {
            mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.Raw, message));
        }

        @JavascriptInterface
        public boolean malloc(String key, int bufferSize) {
            if (!mJSPublicBuffer.containsKey(key)) {
                ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                mJSPublicBuffer.put(key, buf);
                //Log.i(TAG, "[malloc] ok: " + bufferSize);
                return true;
            }
            return false;
        }

        @JavascriptInterface
        public void free(String key) {
            mJSPublicBuffer.remove(key);
            //Log.i(TAG, "[free] ok");
        }

        @JavascriptInterface
        public void write(String key, byte[] bytes) {
            if (mJSPublicBuffer.containsKey(key)) {
                ByteBuffer buf = mJSPublicBuffer.get(key);
                assert buf != null;
                buf.put(bytes, 0, bytes.length);
                //Log.i(TAG, "[write] ok: " + buf.position());
            }
        }

        @JavascriptInterface
        public boolean _malloc(String url, int bufferSize) {
            if (!mJSPrivateBuffer.containsKey(url)) {
                ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                mJSPrivateBuffer.put(url, buf);
                //Log.i(TAG, "[malloc] ok: " + bufferSize);
                return true;
            }
            return false;
        }

        @JavascriptInterface
        public void _free(String key) {
            mJSPrivateBuffer.remove(key);
            //Log.i(TAG, "[free] ok");
        }

        @JavascriptInterface
        public void _write(String key, byte[] bytes) {
            if (mJSPrivateBuffer.containsKey(key)) {
                ByteBuffer buf = mJSPrivateBuffer.get(key);
                assert buf != null;
                buf.put(bytes, 0, bytes.length);
                //Log.i(TAG, "[write] ok: " + buf.position());
            }
        }

        @JavascriptInterface
        public void fetchBlob(String url, String contentDisposition, String mimetype) {
            if (mJSPrivateBuffer.containsKey(url)) {
                ByteBuffer buf = mJSPrivateBuffer.get(url);
                assert buf != null;
                DownloadSupport support = new DownloadSupport(mDownloadOption, mDownloadProgress::put, mUnityPostMessageQueue);
                support.fetchFromDataUrl(new String(buf.array()), contentDisposition, mimetype);
                mJSPrivateBuffer.remove(url);
            }
        }
    }

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
        //                              | mCaptureLayout -----
        //                              |                    |
        //                              |                    |
        //                              |                    | mWebView
        //                              |
        //                              |
        //                              | mGlSurfaceView

        setRetainInstance(true);

        final Activity a = UnityPlayer.currentActivity;
        a.getFragmentManager().beginTransaction().add(0, self).commitAllowingStateLoss();

        a.runOnUiThread(() -> {

            initParam(webWidth, webHeight, texWidth, texHeight, screenWidth, screenHeight, isVulkan, OffscreenBrowser.CaptureMode.values()[captureMode]);

            init();

            if (mWebView == null) {
                mWebView = new WebView(a);
                mView = mWebView;
            }

            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler, final String host, final String realm) {
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
                 * @param view    The WebView that is initiating the callback.
                 * @param url     The url to be loaded.
                 * @param favicon The favicon for this page if it already exists in the
                 *                database.
                 */
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    if (mWebView != null)
                        mPageGoState.update(mWebView.canGoBack(), mWebView.canGoForward());
                }

                /**
                 * @param view The WebView that is initiating the callback.
                 * @param url  The url of the page.
                 */
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (mWebView != null)
                        mPageGoState.update(mWebView.canGoBack(), mWebView.canGoForward());
                    mSessionState.actualUrl = url;
                    EventCallback.Message message = new EventCallback.Message(EventCallback.Type.OnPageFinish, url);
                    mUnityPostMessageQueue.add(message);
                }

                /**
                 * @param view The WebView that is initiating the callback.
                 * @param url  The url of the resource the WebView will load.
                 */
                @Override
                public void onLoadResource(WebView view, String url) {
                    // Need to do null check
                    // Error AndroidRuntime java.lang.NullPointerException: Attempt to invoke virtual method 'boolean android.webkit.WebView.canGoBack()' on a null object reference
                    if (mWebView != null)
                        mPageGoState.update(mWebView.canGoBack(), mWebView.canGoForward());
                }

                /**
                 * @param view    The WebView that is initiating the callback.
                 * @param handler An {@link SslErrorHandler} that will handle the user's
                 *                response.
                 * @param error   The SSL error object.
                 */
                @SuppressLint("WebViewClientOnReceivedSslError")
                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    AlertDialog.Init dialog = new AlertDialog.Init(AlertDialog.Init.Reason.ERROR, "Ssl Error", "Your connection is not private");
                    dialog.setPositive("Enter", selected -> handler.proceed());
                    dialog.setNegative("Back to safety", selected -> handler.cancel());
                    if (mOnDialogResult != null) mOnDialogResult.dismiss();
                    mOnDialogResult = dialog.getOnResultListener();
                    mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDialog, dialog.marshall()));
                }

                /**
                 * @param view    The WebView that is initiating the callback.
                 * @param request Object containing the details of the request.
                 * @return True to cancel the current load, otherwise return false.
                 */
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    String url = uri.toString();

                    if (mWebView != null)
                        mPageGoState.update(mWebView.canGoBack(), mWebView.canGoForward());

                    if (mIntentFilters != null) {
                        for (String intentFilter : mIntentFilters) {
                            Pattern pattern = Pattern.compile(intentFilter);
                            Matcher matcher = pattern.matcher(url);
                            if (matcher.matches()) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                view.getContext().startActivity(intent);
                                return true;
                            }
                        }
                    }

                    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://") || url.startsWith("javascript:")) {
                        // Let webview handle the URL
                        return false;
                    } else if (url.startsWith("unity:")) {
                        String message = url.substring(6);
                        return true;
                    }

                    return true;
                }
            });
            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                    return false;
                }

                public boolean onConsoleMessage(ConsoleMessage cm) {
                    Log.d(TAG, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                    return true;
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
                    mVideoView = view;
                    mWebView.addView(mVideoView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }

                @Override
                public void onHideCustomView() {
                    super.onHideCustomView();
                    mWebView.removeView(mVideoView);
                    mVideoView = null;
                }

                /**
                 * @param webView           The WebView instance that is initiating the request.
                 * @param filePathCallback  Invoke this callback to supply the list of paths to files to upload,
                 *                          or {@code null} to cancel. Must only be called if the
                 *                          {@link #onShowFileChooser} implementation returns {@code true}.
                 * @param fileChooserParams Describes the mode of file chooser to be opened, and options to be
                 *                          used with it.
                 */
                @Override
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                    if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
                    mFilePathCallback = filePathCallback;

                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    contentSelectionIntent.setType("*/*");

                    Intent[] intentArray = new Intent[0];

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                    startActivityForResult(Intent.createChooser(chooserIntent, "Select content"), REQUEST_FILE_PICKER);

                    return true;
                }

                private Intent getChooserIntent() {
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("*/*");

                    return contentSelectionIntent;
                }

                /**
                 * @param request the PermissionRequest from current web content.
                 */
                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    final String[] requestedResources = request.getResources();
                    for (String r : requestedResources) {
                        if ((r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) || (r.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) || r.equals(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
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
                    Download.Request request = new Download.Request(url, userAgent, contentDisposition, mimetype);
                    EventCallback.Message message = new EventCallback.Message(EventCallback.Type.OnDownload, request.marshall());
                    mUnityPostMessageQueue.add(message);
                }
            });
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
                    mScrollState.update(x, y);
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

                    mUnityPostMessageQueue.add(new EventCallback.Message(EventCallback.Type.OnDownloadFinish, new Download.EventInfo(downloadedID, uri.toString()).marshall()));
                }
            };
            a.registerReceiver(mOnDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);

            mWebView.setInitialScale(100);
            mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            // --------- drawing cache setting
            mWebView.clearCache(true);
            // ---------
            mWebView.setLongClickable(false);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.setVerticalScrollBarEnabled(true);
            mWebView.setBackgroundColor(0x00000000);
            mWebView.addJavascriptInterface(new JSInterface(), "tlab");
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
            // --------- // fix file access
            webSettings.setAllowContentAccess(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            // ---------
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            if (mSessionState.userAgent != null && !mSessionState.userAgent.isEmpty()) {
                webSettings.setUserAgentString(mSessionState.userAgent);
            }
            webSettings.setDefaultTextEncodingName("utf-8");
            webSettings.setDatabaseEnabled(true);
            webSettings.setDomStorageEnabled(true);

            mCaptureLayout.addView(mWebView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            if (mSessionState.loadUrl != null) LoadUrl(mSessionState.loadUrl);

            mInitialized = true;
        });
    }

    private void showHttpAuthDialog(final HttpAuthHandler handler, final String host, final String realm) {
        final Activity a = UnityPlayer.currentActivity;
        final android.app.AlertDialog.Builder mHttpAuthDialog = new android.app.AlertDialog.Builder(a);
        LinearLayout layout = new LinearLayout(a);

        mHttpAuthDialog.setTitle("Enter the password").setCancelable(false);
        final EditText etUserName = new EditText(a);
        etUserName.setWidth(100);
        layout.addView(etUserName);
        final EditText etUserPass = new EditText(a);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_FILE_PICKER || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        if (resultCode == Activity.RESULT_OK) {
            String dataString = data.getDataString();
            if (dataString != null) {
                results = new Uri[]{Uri.parse(dataString)};
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    /**
     * I need to call this function on unity's render thread because
     * releaseSharedTexture() call GLES or Vulkan function and it
     * needs to be called on render thread.
     */
    @Override
    public void Dispose() {
        ReleaseSharedTexture();

        final Activity a = UnityPlayer.currentActivity;
        final UnityConnect self = this;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            abortCaptureThread();

            mWebView.stopLoading();
            if (mVideoView != null)
                mWebView.removeView(mVideoView);
            mWebView.destroy();
            mWebView = null;
            mView = null;

            a.unregisterReceiver(mOnDownloadComplete);
            a.getFragmentManager().beginTransaction().remove(self).commitAllowingStateLoss();

            mDisposed = true;
        });
    }

    public void EvaluateJS(String js) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null || !mWebView.getSettings().getJavaScriptEnabled()) return;
            mWebView.loadUrl("javascript:(function(){" + js + "})();");
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
            // get base64 from blob url and write to file stream
            //@formatter:off
            String js = JSUtil.toVariable("url", url) + JSUtil.toVariable("mimetype", mimetype) + JSUtil.toVariable("contentDisposition", contentDisposition) +
                    "function writeBuffer(buffer, bufferId, segmentSize, offset)\n" +
                    "{\n" +
                    "    if (segmentSize === 0) return;\n" +
                    "    var i = offset;\n" +
                    "    while(i + segmentSize <= buffer.length)\n" +
                    "    {\n" +
                    "       window.tlab._write(bufferId, buffer.slice(i, i + segmentSize));\n" +
                    "       i += segmentSize\n" +
                    "    }\n" +
                    "    writeBuffer(buffer, bufferId, parseInt(segmentSize / 2), i);\n" +
                    "}\n" +
                    "var xhr = new XMLHttpRequest();\n" +
                    "xhr.open(\"GET\", url, true);\n" +
                    "xhr.setRequestHeader(\"Content-type\", mimetype + \";charset=UTF-8\");\n" +
                    "xhr.responseType = \"blob\";\n" +
                    "xhr.onload = function(e) {\n" +
                    "    if (this.status == 200) {\n" +
                    "        var blobFile = this.response;\n" +
                    "        var reader = new FileReader();\n" +
                    "        reader.readAsDataURL(blobFile);\n" +
                    "        reader.onloadend = function() {\n" +
                    "            base64data = reader.result;\n" +
                    "            bufferId = url;\n" +
                    "            buffer = new TextEncoder().encode(base64data);\n" +
                    "            window.tlab._malloc(bufferId, buffer.length);\n" +
                    "            writeBuffer(buffer, bufferId, 500000, 0);\n" +
                    "            window.tlab.fetchBlob(url, contentDisposition, mimetype);\n" +
                    "        }\n" +
                    "    }\n" +
                    "};\n" +
                    "xhr.send();";
            //@formatter:on

            EvaluateJS(js);
        }
    }

    public void SetUserAgent(final String ua, final boolean reload) {
        // https://developer.mozilla.org/ja/docs/Web/HTTP/Headers/User-Agent/Firefox
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            try {
                mWebView.getSettings().setUserAgentString(ua);
                if (reload) mWebView.reload();
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
            mSessionState.userAgent = mWebView.getSettings().getUserAgentString();
            mAsyncResult.post(new AsyncResult(id, mSessionState.userAgent), AsyncResult.Status.COMPLETE);
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

            mWebView.loadUrl(mSessionState.loadUrl);
        });
    }

    public void GoBack() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null || !mPageGoState.canGoBack) return;
            mWebView.goBack();
        });
    }

    public void GoForward() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null || !mPageGoState.canGoForward) return;
            mWebView.canGoForward();
        });
    }

    /**
     * Retrieve buffers that allocated in order to map javascript buffer to java.
     *
     * @param id Name of buffers that allocated in order to map javascript buffer to java
     * @return current buffer value
     */
    public byte[] GetJSBuffer(String id) {
        if (mJSPublicBuffer.containsKey(id)) {
            ByteBuffer buf = mJSPublicBuffer.get(id);
            assert buf != null;
            return buf.array();
        }
        return null;
    }

    public int EvaluateJSForResult(String varNameOfResultId, String js) {
        int id = mAsyncResult.request();
        if (id == -1) return -1;
        EvaluateJS(Common.JSUtil.toVariable(varNameOfResultId, id) + js);
        return id;
    }

    public void LoadHtml(final String html, final String baseURL) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.loadDataWithBaseURL(baseURL, html, "text/html", "UTF8", null);
        });
    }

    /**
     * Scrolls the contents of this WebView up by half the view size.
     *
     * @param top True to jump to the top of the page
     */
    public void PageUp(boolean top) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.pageUp(top);
        });
    }

    /**
     * Scrolls the contents of this WebView down by half the page size.
     *
     * @param bottom True to jump to bottom of page
     */
    public void PageDown(boolean bottom) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.pageDown(bottom);
        });
    }

    /**
     * Clear WebView Cache.
     *
     * @param includeDiskFiles If false, only the RAM cache will be cleared.
     */
    public void ClearCash(boolean includeDiskFiles) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.clearCache(includeDiskFiles);
        });
    }

    /**
     * Clear WebView History.
     */
    public void ClearHistory() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.clearHistory();
        });
    }

    /**
     * Performs zoom in in this WebView.
     */
    public void ZoomIn() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.zoomIn();
        });
    }

    /**
     * Performs zoom out in this WebView.
     */
    public void zoomOut() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            mWebView.zoomOut();
        });
    }

    /**
     * Clear WebView Cookie.
     */
    public void ClearCookie() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (mWebView == null) return;
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        });
    }
}
