package com.tlab.libwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.robot9.shared.SharedTexture;
import com.tlab.dialog.AlertDialog;
import com.tlab.dialog.ColorPicker;
import com.tlab.dialog.DataTimePickerDialog;
import com.tlab.dialog.Dialog;
import com.tlab.dialog.OptionSelector;
import com.tlab.eventcallback.CatchDownloadUrlCallback;
import com.tlab.eventcallback.DownloadEventCallback;
import com.tlab.viewtobuffer.CustomGLSurfaceView;
import com.tlab.viewtobuffer.ViewToBufferLayout;
import com.tlab.viewtobuffer.ViewToBufferRenderer;
import com.tlab.viewtobuffer.ViewToHWBRenderer;
import com.tlab.viewtobuffer.ViewToPBORenderer;
import com.tlab.viewtobuffer.ViewToSurfaceLayout;
import com.unity3d.player.UnityPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnityConnect extends Fragment {

    // ---------------------------------------------------------------------------------------------------------
    // Renderer
    //

    private ViewToBufferRenderer m_viewToBufferRenderer;
    private CustomGLSurfaceView m_glSurfaceView;

    // ---------------------------------------------------------------------------------------------------------
    // View
    //

    private BitmapWebView m_webview;
    private View m_videoView;

    // ---------------------------------------------------------------------------------------------------------
    // Layout
    //

    private RelativeLayout m_rootLayout;
    private LinearLayout m_captureLayout;
    private FrameLayout m_frameLayout;

    // ---------------------------------------------------------------------------------------------------------
    //
    //

    private boolean m_captureLayoutLoopKeepAlive = true;
    private final Object m_captureLayoutLoopMutex = new Object();

    // ---------------------------------------------------------------------------------------------------------
    //
    //

    private enum DownloadOption {
        applicationFolder, downloadFolder
    }

    private enum CaptureMode {
        hardwareBuffer, byteBuffer, surface
    }

    private int m_webWidth;
    private int m_webHeight;
    private int m_texWidth;
    private int m_texHeight;
    private int m_screenWidth;
    private int m_screenHeight;

    private int m_scrollX;
    private int m_scrollY;

    private static final int INPUT_FILE_REQUEST_CODE = 1;

    private ValueCallback<Uri[]> m_filePathCallback;

    private DownloadOption m_downloadOption;
    private float m_downloadProgress = 0.0f;
    private String m_downloadSubDirectory;
    private String m_onPageFinish;
    private String m_varTmp = "__tmp";
    private BroadcastReceiver m_onDownloadComplete;
    private final Map<String, ByteBuffer> m_blobDlBuffer = new Hashtable<>();
    private final DownloadEventCallback m_downloadEventCallback = new DownloadEventCallback();
    private final CatchDownloadUrlCallback m_catchDownloadUrlCallback = new CatchDownloadUrlCallback();

    private final Map<String, ByteBuffer> m_webBuffer = new Hashtable<>();

    private String m_loadUrl;
    private String m_actualUrl;

    private String m_htmlCash;

    private String[] m_intentFilters;

    private boolean m_canGoBack;
    private boolean m_canGoForward;

    private String m_userAgent;
    private Hashtable<String, String> m_customHeaders;

    private CaptureMode m_captureMode = CaptureMode.hardwareBuffer;

    private boolean m_isSharedBufferExchanged = true;
    private SharedTexture m_sharedTexture;
    private HardwareBuffer m_sharedBuffer;

    private long[] m_hwbTexID;
    private boolean m_isVulkan;

    private boolean m_initialized = false;

    private int m_fps = 30;

    private boolean m_useCustomWidget = false;

    private final static String TAG = "libwebview";

    // ---------------------------------------------------------------------------------------------------------
    // Initialize this class
    //

    /**
     * Initialize WebView if it is not initialized yet.
     *
     * @param webWidth        WebView width
     * @param webHeight       WebView height
     * @param textureWidth    Texture width
     * @param textureHeight   Texture height
     * @param screenWidth     Screen width
     * @param screenHeight    Screen height
     * @param url             init url
     * @param isVulkan        is this app vulkan api
     * @param useCustomWidget Disable html5's native widget and use custom one
     */
    //@formatter:off
    public void initialize(int webWidth, int webHeight,
                           int textureWidth, int textureHeight,
                           int screenWidth, int screenHeight,
                           String url, boolean isVulkan, int renderMode, boolean useCustomWidget) {
    //@formatter:on
        if (webWidth <= 0 || webHeight <= 0) {
            return;
        }

        m_webWidth = webWidth;
        m_webHeight = webHeight;
        m_texWidth = textureWidth;
        m_texHeight = textureHeight;
        m_screenWidth = screenWidth;
        m_screenHeight = screenHeight;
        m_loadUrl = url;
        m_isVulkan = isVulkan;
        m_captureMode = CaptureMode.values()[renderMode];
        m_useCustomWidget = useCustomWidget;

        initWebView();
    }

    /**
     * If it returns true, this WebView component is already initialized.
     *
     * @return Whether or not this WebView component is initialized.
     */
    public boolean IsInitialized() {
        return m_initialized;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Shared Texture Utility
    //

    public void releaseSharedTexture() {
        Log.i(TAG, "[sharedtex] release shared texture pass 0 (start)");
        m_hwbTexID = null;
        if (m_sharedTexture != null) {
            m_sharedTexture.release();
            m_sharedTexture = null;
        }
        Log.i(TAG, "[sharedtex] release shared texture pass 1 (end)");
    }

    public boolean contentExists() {
        return m_viewToBufferRenderer.contentExists();
    }

    /**
     *
     */
    public void updateSurface() {
        if (m_viewToBufferRenderer instanceof ViewToHWBRenderer) {
            HardwareBuffer sharedBuffer = ((ViewToHWBRenderer) m_viewToBufferRenderer).getHardwareBuffer();

            if (sharedBuffer == null) {
                return;
            }

            if (m_sharedBuffer == sharedBuffer) {
                m_sharedTexture.updateUnityTexture();

                return;
            }

            //Log.i(TAG, "[sharedtex] [updateSharedTexture] pass 0 (start)");

            //Log.i(TAG, "[sharedtex] [updateSharedTexture] pass 1");

            releaseSharedTexture();

            //Log.i(TAG, "[sharedtex] [updateSharedTexture] pass 2 (release shared texture)");

            SharedTexture sharedTexture = new SharedTexture(sharedBuffer, m_isVulkan);

            m_hwbTexID = new long[1];
            m_hwbTexID[0] = sharedTexture.getPlatformTexture();

            m_sharedTexture = sharedTexture;
            m_sharedBuffer = sharedBuffer;

            m_isSharedBufferExchanged = false;

            //Log.i(TAG, "[sharedtex] [updateSharedTexture] pass 3 (end)");
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // File download
    //

    public void writeDataUrlToStorage(String url, String dataUrl, String mimetype) {

        Context context = UnityPlayer.currentActivity.getApplicationContext();

        String extension = MimeTypes.getDefaultExt(mimetype);

        String base64 = dataUrl.replaceFirst("^data:.+;base64,", "");

        byte[] data = Base64.decode(base64, 0);

        boolean downloaded = false;

        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String filename = dateFormat.format(new Date()) + "." + extension;

        String downloadDir = "";

        if (m_downloadOption == DownloadOption.applicationFolder) {
            downloadDir = Objects.requireNonNull(context.getExternalFilesDir(null)).getPath();
        } else if (m_downloadOption == DownloadOption.downloadFolder) {
            downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        }

        if (!m_downloadSubDirectory.isEmpty()) {
            downloadDir = downloadDir + "/" + m_downloadSubDirectory;
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
                if (!m_downloadEventCallback.isOnStartEmpty()) {
                    //@formatter:off
                    String argument =
                            "var " +  m_downloadEventCallback.varUrl  + " = '" + url + "'; " +
                            "var " +  m_downloadEventCallback.varId   + " = -1; ";
                    //@formatter:on
                    evaluateJS(argument + m_downloadEventCallback.onStart);
                }

                if (!m_downloadEventCallback.isOnFinishEmpty()) {
                    //@formatter:off
                    String argument =
                            "var " +  m_downloadEventCallback.varUri  + " = 'none'; " +
                            "var " +  m_downloadEventCallback.varId   + " = -1; ";
                    //@formatter:on
                    evaluateJS(argument + m_downloadEventCallback.onFinish);
                }
            }, 500);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Initialize webview
    //

    /**
     *
     */
    @SuppressLint({"SetJavaScriptEnabled", "CommitTransaction"})
    private void initWebView() {
        final UnityConnect self = this;

        // -----------------------------------------------------------
        // Hierarchical structure.
        // parent -----
        //            |
        //            |
        //            | m_rootLayout -----
        //                          |
        //                          |
        //                          | m_captureableLayout -----
        //                          |               |
        //                          |               |
        //                          |               | m_webview
        //                          |
        //                          |
        //                          | m_glSurfaceView

        setRetainInstance(true);

        final Activity a = UnityPlayer.currentActivity;
        a.getFragmentManager()
                .beginTransaction()
                .add(0, self)
                .commitAllowingStateLoss();

        a.runOnUiThread(() -> {

            m_rootLayout = new RelativeLayout(a);
            m_rootLayout.setGravity(Gravity.TOP);
            m_rootLayout.setX(m_screenWidth);
            m_rootLayout.setY(m_screenHeight);
            m_rootLayout.setBackgroundColor(0xFFFFFFFF);

            if ((m_captureMode == CaptureMode.hardwareBuffer) || (m_captureMode == CaptureMode.byteBuffer)) {
                switch (m_captureMode) {
                    case hardwareBuffer:
                        m_viewToBufferRenderer = new ViewToHWBRenderer();
                        break;
                    case byteBuffer:
                        m_viewToBufferRenderer = new ViewToPBORenderer();
                        break;
                }
                m_viewToBufferRenderer.SetTextureResolution(m_texWidth, m_texHeight);

                m_glSurfaceView = new CustomGLSurfaceView(a);
                m_glSurfaceView.setEGLContextClientVersion(3);
                m_glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
                m_glSurfaceView.setPreserveEGLContextOnPause(true);
                m_glSurfaceView.setRenderer(m_viewToBufferRenderer);
                m_glSurfaceView.setBackgroundColor(0x00000000);

                m_captureLayout = new ViewToBufferLayout(a, m_viewToBufferRenderer);
            } else if (m_captureMode == CaptureMode.surface) {
                m_captureLayout = new ViewToSurfaceLayout(a);
            }

            m_captureLayout.setOrientation(ViewToBufferLayout.VERTICAL);
            m_captureLayout.setGravity(Gravity.START);
            m_captureLayout.setBackgroundColor(Color.WHITE);

            m_frameLayout = new FrameLayout(a);

            if (m_webview == null) {
                m_webview = new BitmapWebView(a);
            }

            // --------------------------------------------------------------------------------------------------------
            // Settings each View and View Group
            //

            m_webview.setWebViewClient(new WebViewClient() {
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
                    m_canGoBack = m_webview.canGoBack();
                    m_canGoForward = m_webview.canGoForward();
                }

                /**
                 * @param view The WebView that is initiating the callback.
                 * @param url  The url of the page.
                 */
                @Override
                public void onPageFinished(WebView view, String url) {
                    m_canGoBack = m_webview.canGoBack();
                    m_canGoForward = m_webview.canGoForward();
                    m_actualUrl = url;

                    if (m_onPageFinish != null && !m_onPageFinish.isEmpty() || m_useCustomWidget) {
                        //@formatter:off
                        String js =
                                m_varTmp + " = document.getElementsByTagName(\"input\");" +
                                        m_varTmp + " = Array.from(" + m_varTmp + ");\n" +
                                        m_varTmp + ".forEach(input => {\n" +
                                        "    switch (input.getAttribute(\"type\")){\n" +
                                        "        case \"time\":\n" +
                                        "            input[\"onclick\"] = function(e){\n" +
                                        "                e = e || event;\n" +
                                        "                e.preventDefault();\n" +
                                        "                if (typeof " + m_varTmp + " == \"undefined\") {\n" +
                                        "                   window." + m_varTmp + " = input;" +
                                        "                   window.TLabWebViewActivity.showDateTimePicker(false, true);\n" +
                                        "                }\n" +
                                        "                return false;\n" +
                                        "            };" +
                                        "            break;\n" +
                                        "        case \"date\":\n" +
                                        "            input[\"onclick\"] = function(e){\n" +
                                        "                e = e || event;\n" +
                                        "                e.preventDefault();\n" +
                                        "                if (typeof " + m_varTmp + " == \"undefined\") {\n" +
                                        "                   window." + m_varTmp + " = input;" +
                                        "                   window.TLabWebViewActivity.showDateTimePicker(true, false);\n" +
                                        "                }\n" +
                                        "                return false;\n" +
                                        "            };" +
                                        "            break;\n" +
                                        "        case \"datetime-local\":\n" +
                                        "            input[\"onclick\"] = function(e){\n" +
                                        "                e = e || event;\n" +
                                        "                e.preventDefault();\n" +
                                        "                if (typeof " + m_varTmp + " == \"undefined\") {\n" +
                                        "                   window." + m_varTmp + " = input;" +
                                        "                   window.TLabWebViewActivity.showDateTimePicker(true, true);\n" +
                                        "                }\n" +
                                        "                return false;\n" +
                                        "            };" +
                                        "            break;\n" +
                                        "        case \"color\":\n" +
                                        "            input[\"onclick\"] = function(e){\n" +
                                        "                e = e || event;\n" +
                                        "                e.preventDefault();\n" +
                                        "                if (typeof " + m_varTmp + " == \"undefined\") {\n" +
                                        "                   window." + m_varTmp + " = input;" +
                                        "                   window.TLabWebViewActivity.showColorPicker();\n" +
                                        "                }\n" +
                                        "                return false;\n" +
                                        "            };" +
                                        "            break;\n" +
                                        "    }\n" +
                                        "});" +
                                        m_varTmp + " = document.getElementsByTagName(\"select\");\n" +
                                        m_varTmp + " = Array.from(" + m_varTmp + ");\n" +
                                        m_varTmp + ".forEach(select => {\n" +
                                        "    select[\"onmousedown\"] = function(e) {\n" +
                                        "        e = e || event;\n" +
                                        "        e.preventDefault();\n" +
                                        "        if (typeof " + m_varTmp + " == \"undefined\") {\n" +
                                        "           var texts = [];\n" +
                                        "           var flags = [];\n" +
                                        "           for (var i = 0; i < select.options.length; i++) {\n" +
                                        "               texts.push(select.options[i].text);\n" +
                                        "               flags.push(select.options[i].selected);\n" +
                                        "           }\n" +
                                        "           window." + m_varTmp + " = select;" +
                                        "           window.TLabWebViewActivity.showOptionSelector(Array.from(texts), Array.from(flags), select.multiple);\n" +
                                        "        }\n" +
                                        "        return false;\n" +
                                        "    }\n" +
                                        "});"+
                                        m_varTmp + " = undefined;";
                        //@formatter:on
                        evaluateJS(js + m_onPageFinish);
                    }
                }

                /**
                 * @param view The WebView that is initiating the callback.
                 * @param url  The url of the resource the WebView will load.
                 */
                @Override
                public void onLoadResource(WebView view, String url) {
                    m_canGoBack = m_webview.canGoBack();
                    m_canGoForward = m_webview.canGoForward();
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
                    AlertDialog alertDialog = new AlertDialog(a);
                    alertDialog.setMessage("ssl error", "your connection is not private");
                    alertDialog.setOptions("enter", selected -> {
                        handler.proceed();
                        m_frameLayout.removeView(alertDialog);
                    });
                    alertDialog.setOptions("back to safety", selected -> {
                        handler.cancel();
                        m_frameLayout.removeView(alertDialog);
                    });
                    int minSize = Math.min(m_webWidth, m_webHeight);
                    alertDialog.setScale(minSize);
                    m_frameLayout.addView(alertDialog);
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

                    m_canGoBack = m_webview.canGoBack();
                    m_canGoForward = m_webview.canGoForward();

                    if (m_intentFilters != null) {
                        for (String intentFilter : m_intentFilters) {
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
            m_webview.setWebChromeClient(new WebChromeClient() {
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
                    m_videoView = view;
                    m_webview.addView(m_videoView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }

                /**
                 *
                 */
                @Override
                public void onHideCustomView() {
                    super.onHideCustomView();
                    m_webview.removeView(m_videoView);
                }

                @Override
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                    if (m_filePathCallback != null) {
                        m_filePathCallback.onReceiveValue(null);
                    }
                    m_filePathCallback = filePathCallback;

                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    contentSelectionIntent.setType("*/*");

                    Intent[] intentArray = new Intent[0];

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                    startActivityForResult(Intent.createChooser(chooserIntent, "Select content"), INPUT_FILE_REQUEST_CODE);

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
            m_webview.setDownloadListener(new DownloadListener() {
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
                    if (!m_catchDownloadUrlCallback.isEmpty()) {
                        UnityPlayer.UnitySendMessage(m_catchDownloadUrlCallback.go, m_catchDownloadUrlCallback.func, url + "\n" + userAgent + "\n" + contentDisposition + "\n" + mimetype);

                        return;
                    }

                    downloadFromUrl(url, userAgent, contentDisposition, mimetype);
                }
            });
            m_webview.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                /**
                 * @param v    The view whose scroll position has changed.
                 * @param x    Current horizontal scroll origin.
                 * @param y    Current vertical scroll origin.
                 * @param oldX Previous horizontal scroll origin.
                 * @param oldY Previous vertical scroll origin.
                 */
                @Override
                public void onScrollChange(View v, int x, int y, int oldX, int oldY) {
                    m_scrollX = x;
                    m_scrollY = y;
                }
            });

            // create download complete event receiver.
            m_onDownloadComplete = new BroadcastReceiver() {
                /**
                 * @param context The Context in which the receiver is running.
                 * @param intent  The Intent being received.
                 */
                @Override
                public void onReceive(Context context, Intent intent) {
                    long downloadedID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri uri = dm.getUriForDownloadedFile(downloadedID);

                    if (!m_downloadEventCallback.isOnFinishEmpty()) {
                        //@formatter:off
                        String argument =
                                "var " +  m_downloadEventCallback.varUri  + " = '"    + uri           + "'; "   +
                                "var " +  m_downloadEventCallback.varId   + " = "     + downloadedID  + "; ";
                        //@formatter:on
                        evaluateJS(argument + m_downloadEventCallback.onFinish);
                    }
                }
            };
            a.registerReceiver(m_onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);

            m_webview.setInitialScale(100);
            m_webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            // --------- drawing cache setting
            m_webview.clearCache(true);
            // ---------
            m_webview.setLongClickable(false);
            m_webview.setVisibility(View.VISIBLE);
            m_webview.setVerticalScrollBarEnabled(true);
            m_webview.setBackgroundColor(0x00000000);
            m_webview.addJavascriptInterface(new TLabWebViewJSInterface(), "TLabWebViewActivity");
            WebSettings webSettings = m_webview.getSettings();
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
            if (m_userAgent != null && !m_userAgent.isEmpty()) {
                webSettings.setUserAgentString(m_userAgent);
            }
            webSettings.setDefaultTextEncodingName("utf-8");
            webSettings.setDatabaseEnabled(true);
            webSettings.setDomStorageEnabled(true);

            a.addContentView(m_rootLayout, new RelativeLayout.LayoutParams(m_webWidth, m_webHeight));
            m_frameLayout.addView(m_webview, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            if ((m_captureMode == CaptureMode.hardwareBuffer) || (m_captureMode == CaptureMode.byteBuffer)) {
                m_captureLayout.addView(m_frameLayout, new ViewToBufferLayout.LayoutParams(ViewToBufferLayout.LayoutParams.MATCH_PARENT, ViewToBufferLayout.LayoutParams.MATCH_PARENT));
                m_rootLayout.addView(m_glSurfaceView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                m_rootLayout.addView(m_captureLayout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            } else if (m_captureMode == CaptureMode.surface) {
                m_captureLayout.addView(m_frameLayout, new ViewToSurfaceLayout.LayoutParams(ViewToSurfaceLayout.LayoutParams.MATCH_PARENT, ViewToSurfaceLayout.LayoutParams.MATCH_PARENT));
                m_rootLayout.addView(m_captureLayout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            }

            if (m_loadUrl != null) {
                loadUrl(m_loadUrl);
            }

            new Thread(() -> {
                while (m_captureLayoutLoopKeepAlive) {
                    synchronized (m_captureLayoutLoopMutex) {
                        m_captureLayout.postInvalidate();
                    }
                    try {
                        Thread.sleep(1000 / m_fps);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

            m_initialized = true;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || m_filePathCallback == null) {
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

        m_filePathCallback.onReceiveValue(results);
        m_filePathCallback = null;
        return;
    }

    /**
     * I need to call this function on unity's render thread because
     * releaseSharedTexture() call GLES or Vulkan function and it
     * needs to be called on render thread.
     */
    public void Destroy() {

        releaseSharedTexture();

        final Activity a = UnityPlayer.currentActivity;
        final UnityConnect self = this;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }

            synchronized (m_captureLayoutLoopMutex) {
                m_captureLayoutLoopKeepAlive = false;
            }

            m_frameLayout.removeAllViews();
            m_frameLayout = null;

            m_webview.post(() -> {
                m_webview.stopLoading();
                m_webview.destroy();
                m_webview = null;
            });

            a.unregisterReceiver(m_onDownloadComplete);
            a
                    .getFragmentManager()
                    .beginTransaction()
                    .remove(self)
                    .commitAllowingStateLoss();
        });
    }

    // ---------------------------------------------------------------------------------------------------------
    // javascript interface
    //

    public class TLabWebViewJSInterface {

        @JavascriptInterface
        public void viewSource(final String src) {
            m_htmlCash = src;
        }

        @JavascriptInterface
        public void unitySendMessage(String go, String method, String message) {
            UnityPlayer.UnitySendMessage(go, method, message);
        }

        @JavascriptInterface
        public void onBlobMapBufferFinish(String url, String mimetype) {
            if (m_blobDlBuffer.containsKey(url)) {
                ByteBuffer buf = m_blobDlBuffer.get(url);
                assert buf != null;
                writeDataUrlToStorage(url, new String(buf.array()), mimetype);
                m_blobDlBuffer.remove(url);
            }
        }

        @JavascriptInterface
        public boolean malloc(String key, int bufferSize) {
            if (!m_webBuffer.containsKey(key)) {
                ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                m_webBuffer.put(key, buf);
                //Log.i(TAG, "[malloc] ok: " + bufferSize);
                return true;
            }
            return false;
        }

        @JavascriptInterface
        public void free(String key) {
            m_webBuffer.remove(key);
            //Log.i(TAG, "[free] ok");
        }

        @JavascriptInterface
        public void write(String key, byte[] bytes) {
            if (m_webBuffer.containsKey(key)) {
                ByteBuffer buf = m_webBuffer.get(key);
                assert buf != null;
                buf.put(bytes, 0, bytes.length);
                //Log.i(TAG, "[write] ok: " + buf.position());
            }
        }

        @JavascriptInterface
        public boolean mallocForBlobDlEvent(String url, int bufferSize) {
            if (!m_blobDlBuffer.containsKey(url)) {
                ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                m_blobDlBuffer.put(url, buf);
                //Log.i(TAG, "[malloc] ok: " + bufferSize);
                return true;
            }
            return false;
        }

        @JavascriptInterface
        public void freeForBlobDlEvent(String key) {
            m_blobDlBuffer.remove(key);
            //Log.i(TAG, "[free] ok");
        }

        @JavascriptInterface
        public void writeForBlobDlEvent(String key, byte[] bytes) {
            if (m_blobDlBuffer.containsKey(key)) {
                ByteBuffer buf = m_blobDlBuffer.get(key);
                assert buf != null;
                buf.put(bytes, 0, bytes.length);
                //Log.i(TAG, "[write] ok: " + buf.position());
            }
        }

        @JavascriptInterface
        public void showDateTimePicker(boolean date, boolean time) {
            final Activity a = UnityPlayer.currentActivity;
            a.runOnUiThread(() -> {
                DataTimePickerDialog dataTimePickerDialog = new DataTimePickerDialog(a, date, time);
                dataTimePickerDialog.setOptions("cancel", selected -> {
                    //@formatter:off
                    String js = m_varTmp + " = undefined;";
                    //@formatter:on
                    evaluateJS(js);
                    m_frameLayout.removeView(dataTimePickerDialog);
                });
                dataTimePickerDialog.setOptions("ok", selected -> {
                    //@formatter:off
                    String js =
                            m_varTmp + ".value = " + "\"" + dataTimePickerDialog.getValue() + "\";" +
                            m_varTmp + " = undefined;";
                    //@formatter:on
                    evaluateJS(js);
                    m_frameLayout.removeView(dataTimePickerDialog);
                });
                int minSize = Math.min(m_webWidth, m_webHeight);
                dataTimePickerDialog.setScale(minSize);
                m_frameLayout.addView(dataTimePickerDialog);
            });
        }

        @JavascriptInterface
        public void showColorPicker() {
            final Activity a = UnityPlayer.currentActivity;
            a.runOnUiThread(() -> {
                ColorPicker colorPicker = new ColorPicker(a);
                colorPicker.setOptions("cancel", selected -> {
                    //@formatter:off
                    String js = m_varTmp + " = undefined;";
                    //@formatter:on
                    evaluateJS(js);
                    m_frameLayout.removeView(colorPicker);
                });
                colorPicker.setOptions("ok", selected -> {
                    //@formatter:off
                    String js =
                            m_varTmp + ".value = " + "\"" + colorPicker.getValue() + "\";" +
                            m_varTmp + " = undefined;";
                    //@formatter:on
                    evaluateJS(js);
                    m_frameLayout.removeView(colorPicker);
                });
                int minSize = Math.min(m_webWidth, m_webHeight);
                colorPicker.setScale(minSize);
                m_frameLayout.addView(colorPicker);
            });
        }

        @JavascriptInterface
        public void showOptionSelector(String[] texts, boolean[] flags, boolean multiple) {
            final Activity a = UnityPlayer.currentActivity;
            a.runOnUiThread(() -> {
                OptionSelector optionSelector = new OptionSelector(a, texts, flags, multiple);
                optionSelector.setOptions("cancel", selected -> {
                    //@formatter:off
                    String js = m_varTmp + " = undefined;";
                    //@formatter:on
                    evaluateJS(js);
                    m_frameLayout.removeView(optionSelector);
                });
                optionSelector.setOptions("ok", selected -> {
                    //@formatter:off
                    String js = "";
                    boolean[] values = optionSelector.getValue();
                    for (int j = 0; j < values.length; j++) {
                        js += (m_varTmp + ".options[" + j + "].selected = " + flags[j] + ";");
                    }
                    js += (m_varTmp + " = undefined;");
                    //@formatter:on
                    evaluateJS(js);
                    m_frameLayout.removeView(optionSelector);
                });
                int minSize = Math.min(m_webWidth, m_webHeight);
                optionSelector.setScale(minSize);
                m_frameLayout.addView(optionSelector);
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // java's unity interface.
    //

    //
    // javascript
    //

    /**
     * Retrieve buffers that allocated in order to map javascript buffer to java.
     *
     * @param key Name of buffers that allocated in order to map javascript buffer to java
     * @return current buffer value
     */
    public byte[] getWebBuffer(String key) {
        if (m_webBuffer.containsKey(key)) {
            ByteBuffer buf = m_webBuffer.get(key);
            assert buf != null;
            return buf.array();
        }

        return null;
    }

    /**
     * Run javascript on the current web page.
     *
     * @param js javascript
     */
    public void evaluateJS(String js) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null || !m_webview.getSettings().getJavaScriptEnabled()) {
                return;
            }
            m_webview.loadUrl("javascript:" + js);
        });
    }

    //
    // view to hardware buffer
    //

    /**
     * Return the texture pointer of the WebView frame
     * (NOTE: In Vulkan, the VkImage pointer returned by this function could not be used for UpdateExternalTexture. This issue has not been fixed).
     *
     * @return texture pointer of the webview frame (Vulkan: VkImage, OpenGLES: TexID)
     */
    public long getPlatformTextureID() {
        if (m_hwbTexID == null) {
            return 0;
        }

        return m_hwbTexID[0];
    }

    public void setUnityTextureID(long unityTexID) {
        if (m_sharedTexture != null) {
            m_sharedTexture.setUnityTexture(unityTexID);
        }
    }

    //
    // view to byte buffer
    //

    public byte[] getByteBuffer() {
        if (m_viewToBufferRenderer instanceof ViewToPBORenderer) {
            return ((ViewToPBORenderer) m_viewToBufferRenderer).getPixelBuffer();
        }

        return new byte[0];
    }

    //
    // composition layers
    //

    public void setSurface(Object surfaceObj) {
        Surface surface = (Surface) surfaceObj;
        if (m_captureLayout instanceof ViewToSurfaceLayout) {
            ((ViewToSurfaceLayout) m_captureLayout).setSurface(surface);
        }
    }

    public void removeSurface() {
        if (m_captureLayout instanceof ViewToSurfaceLayout) {
            ((ViewToSurfaceLayout) m_captureLayout).removeSurface();
        }
    }

    //
    // download event
    //

    /**
     * Register the callback that will be called before the download event starts.
     * If this parameter is not empty, the download event is not started automatically, you must call the download event manually.
     *
     * @param go   Name of the game object to which the function is attached
     * @param func C# function name that is called before the download event starts
     */
    public void setOnCatchDownloadUrl(String go, String func) {
        m_catchDownloadUrlCallback.go = go;
        m_catchDownloadUrlCallback.func = func;
    }

    /**
     * Request file download to Download Manager.
     *
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          The user agent to be used for the download
     * @param contentDisposition Content-disposition http header, if present
     * @param mimetype           The mimetype of the content reported by the server
     */
    public void downloadFromUrl(String url, String userAgent, String contentDisposition, String mimetype) {
        if (url.startsWith("https://") || url.startsWith("http://")) {
            final Activity a = UnityPlayer.currentActivity;
            final Context context = a.getApplicationContext();

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

            if (m_downloadOption == DownloadOption.applicationFolder) {
                request.setDestinationInExternalFilesDir(context, m_downloadSubDirectory, filename);
            } else if (m_downloadOption == DownloadOption.downloadFolder) {
                if (!m_downloadSubDirectory.isEmpty()) {
                    filename = m_downloadSubDirectory + "/" + filename;
                }
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            }

            DownloadManager dm = (DownloadManager) a.getSystemService(Context.DOWNLOAD_SERVICE);
            id = dm.enqueue(request);

            if (!m_downloadEventCallback.isOnStartEmpty()) {
                //@formatter:off
                String argument =
                        "var " +  m_downloadEventCallback.varUrl  + " = '"    + url   + "'; " +
                        "var " +  m_downloadEventCallback.varId   + " = "     + id    + "; ";
                //@formatter:on
                evaluateJS(argument + m_downloadEventCallback.onStart);
            }
        } else if (url.startsWith("data:")) { // data url scheme
            // write base64 data to file stream
            writeDataUrlToStorage(url, url, mimetype);
        } else if (url.startsWith("blob:")) { // blob url scheme
            // get base64 from blob url and write to file stream
            //@formatter:off
            String js =
                    "function writeBuffer(buffer, bufferName, segmentSize, offset)" +
                    "{" +
                    "    if (segmentSize === 0) return;" +
                    "" +
                    "    var i = offset;" +
                    "    while(i + segmentSize <= buffer.length)" +
                    "    {" +
                    "       window.TLabWebViewActivity.writeForBlobDlEvent(bufferName, buffer.slice(i, i + segmentSize));" +
                    "       i += segmentSize" +
                    "    }" +
                    "" +
                    "" +
                    "    writeBuffer(buffer, bufferName, parseInt(segmentSize / 2), i);" +
                    "}" +
                    "var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + url + "', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimetype + ";charset=UTF-8');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobFile = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobFile);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            bufferName = '" + url + "';" +
                    "            buffer = new TextEncoder().encode(base64data);" +
                    "            window.TLabWebViewActivity.mallocForBlobDlEvent(bufferName, buffer.length);" +
                    "            writeBuffer(buffer, bufferName, 500000, 0);" +
                    "            window.TLabWebViewActivity.onBlobMapBufferFinish('" + url + "','" + mimetype + "');" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
            //@formatter:on

            evaluateJS(js);
        }
    }

    /**
     * Defines the download event parameter's name. it can be accessed from javascript when a download event occurs.
     *
     * @param varUrl Variable name for url (URL of the file to be downloaded) to use in javascript's download event callback
     * @param varUri Variable name for uri (The destination for the downloaded file) to use in javascript's download event callback
     * @param varId  Variable name for download id (The ID of the download event) to use in javascript's download event callback
     */
    public void setDownloadEventVariableName(String varUrl, String varUri, String varId) {
        m_downloadEventCallback.varUrl = varUrl;
        m_downloadEventCallback.varUri = varUri;
        m_downloadEventCallback.varId = varId;
    }

    /**
     * Specifies the sub directory from which the files are to be downloaded.
     *
     * @param downloadSubDirectory The sub directory from which the files are downloaded. This directory is created under the directory specified in DownloadOption.
     */
    public void setDownloadSubDirectory(String downloadSubDirectory) {
        m_downloadSubDirectory = downloadSubDirectory;
    }

    public void setFps(int fps) {
        m_fps = fps;
    }

    /**
     * @param varTmp temporary variable name used in javascript
     */
    public void setVarTmp(String varTmp) {
        m_varTmp = varTmp;
    }

    /**
     * Set the directory in which the file will be downloaded.
     *
     * @param dlOption Download location for the files
     */
    public void setDownloadOption(int dlOption) {
        m_downloadOption = DownloadOption.values()[dlOption];
    }

    /**
     * Register Javascript to run when download event starts.
     *
     * @param onDownloadStart javascript
     */
    public void setOnDownloadStart(String onDownloadStart) {
        m_downloadEventCallback.onStart = onDownloadStart;
    }

    /**
     * Register Javascript to run when download event finishes.
     *
     * @param onDownloadFinish javascript
     */
    public void setOnDownloadFinish(String onDownloadFinish) {
        m_downloadEventCallback.onFinish = onDownloadFinish;
    }

    /**
     * Asynchronous capture of download event progress.
     */
    public void requestCaptureDownloadProgress() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            DownloadManager dm = (DownloadManager) a.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = dm.query(new DownloadManager.Query());

            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                @SuppressLint("Range") int bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                m_downloadProgress = (float) bytesDownloadedSoFar / totalBytes;
            }
        });
    }

    /**
     * Get the progress of the download event currently being recorded.
     *
     * @return Current download progress (0 ~ 1)
     */
    public float getDownloadProgress() {
        return this.m_downloadProgress;
    }

    //
    // html
    //

    /**
     * Gets the HTML value currently captured.
     *
     * @return HTML currently captured
     */
    public String getCaptured() {
        return m_htmlCash;
    }

    /**
     * Capture specific HTML elements currently displayed async.
     *
     * @param id Target HTML element tag
     */
    public void captureElementById(String id) {
        loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.getElementById('" + id + "').outerHTML)");
    }

    /**
     *
     */
    public void capturePage() {
        loadUrl("javascript:window.TLabWebViewActivity.viewSource(document.documentElement.outerHTML)");
    }

    /**
     * Loads the given HTML.
     *
     * @param html    The HTML of the resource to load
     * @param baseURL baseURL
     */
    public void loadHtml(final String html, final String baseURL) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.loadDataWithBaseURL(baseURL, html, "text/html", "UTF8", null);
        });
    }

    //
    // user agent
    //

    /**
     * Update userAgent with the given userAgent string.
     *
     * @param ua     UserAgent string
     * @param reload If true, reload web page when userAgent is updated.
     */
    public void setUserAgent(final String ua, final boolean reload) {
        // https://developer.mozilla.org/ja/docs/Web/HTTP/Headers/User-Agent/Firefox
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) return;

            try {
                m_webview.getSettings().setUserAgentString(ua);

                if (reload) {
                    m_webview.reload();
                }

            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        });

        m_userAgent = ua;
    }

    /**
     * Capture current userAgent async.
     */
    public void captureUserAgent() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_userAgent = m_webview.getSettings().getUserAgentString();
        });
    }

    /**
     * Gets the currently captured userAgent string.
     *
     * @return UserAgent String that is currently being captured.
     */
    public String getUserAgent() {
        return m_userAgent;
    }

    //
    // url
    //

    /**
     * Get current url that the webview instance is loading
     *
     * @return Current url that the webview instance is loading
     */
    public String getCurrentUrl() {
        return m_actualUrl;
    }

    /**
     * Loads the given URL.
     *
     * @param url The URL of the resource to load.
     */
    public void loadUrl(String url) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }

            if (m_intentFilters != null) {
                for (String intentFilter : m_intentFilters) {
                    Pattern pattern = Pattern.compile(intentFilter);
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.matches()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        m_webview.getContext().startActivity(intent);
                        m_loadUrl = url;
                        return;
                    }
                }
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                m_loadUrl = "http://" + url;
            } else {
                m_loadUrl = url;
            }

            if (m_customHeaders != null && !m_customHeaders.isEmpty()) {
                m_webview.loadUrl(m_loadUrl, m_customHeaders);
            } else {
                m_webview.loadUrl(m_loadUrl);
            }
        });
    }

    /**
     * Register Javascript to run when the page is finished loading.
     *
     * @param onPageFinish javascript
     */
    public void setOnPageFinish(String onPageFinish) {
        m_onPageFinish = onPageFinish;
    }

    /**
     * Register url patterns to treat as deep links
     *
     * @param filters Url patterns that are treated as deep links (regular expression)
     */
    public void setIntentFilters(String[] filters) {
        m_intentFilters = filters;
    }

    //
    // zoom in/out
    //

    /**
     * Performs zoom in in this WebView.
     */
    public void zoomIn() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.zoomIn();
        });
    }

    /**
     * Performs zoom out in this WebView.
     */
    public void zoomOut() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.zoomOut();
        });
    }

    //
    // scroll
    //

    /**
     * Get content's scroll position x
     *
     * @return Page content's current scroll position x
     */
    public int getScrollX() {
        return m_scrollX;
    }

    /**
     * Get content's scroll position y
     *
     * @return Page content's current scroll position y
     */
    public int getScrollY() {
        return m_scrollY;
    }

    /**
     * Set content's scroll position.
     *
     * @param x Scroll position x of the destination
     * @param y Scroll position y of the destination
     */
    public void scrollTo(int x, int y) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.scrollTo(x, y);
        });
    }

    /**
     * Move the scrolled position of webview
     *
     * @param x The amount of pixels to scroll by horizontally
     * @param y The amount of pixels to scroll by vertically
     */
    public void scrollBy(int x, int y) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.scrollBy(x, y);
        });
    }

    /**
     * Scrolls the contents of this WebView up by half the view size.
     *
     * @param top True to jump to the top of the page
     */
    public void pageUp(boolean top) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.pageUp(top);
        });
    }

    /**
     * Scrolls the contents of this WebView down by half the page size.
     *
     * @param bottom True to jump to bottom of page
     */
    public void pageDown(boolean bottom) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.pageDown(bottom);
        });
    }

    //
    // resize
    //

    /**
     * Update WebView texture resolution.
     *
     * @param textureWidth  Texture new width
     * @param textureHeight Texture new height
     */
    public void resizeTex(int textureWidth, int textureHeight) {
        m_texWidth = textureWidth;
        m_texHeight = textureHeight;

        if (m_viewToBufferRenderer != null) {
            m_viewToBufferRenderer.SetTextureResolution(m_texWidth, m_texHeight);
            m_viewToBufferRenderer.requestResizeTex();
        }
    }

    /**
     * Update WebView resolution.
     *
     * @param webWidth  WebView new width
     * @param webHeight WebView new height
     */
    public void resizeWeb(int webWidth, int webHeight) {
        m_webWidth = webWidth;
        m_webHeight = webHeight;

        if (m_viewToBufferRenderer != null) {
            m_viewToBufferRenderer.SetTextureResolution(m_texWidth, m_texHeight);
            m_viewToBufferRenderer.disable();
        }

        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            ViewGroup.LayoutParams lp = m_rootLayout.getLayoutParams();
            lp.width = m_webWidth;
            lp.height = m_webHeight;

            ArrayList<Dialog> dialogs = Dialog.getViewsByType(m_rootLayout, Dialog.class);
            int minSize = Math.min(m_webWidth, m_webHeight);
            dialogs.forEach(dialog -> {
                dialog.setScale(minSize);
            });

            m_rootLayout.setLayoutParams(lp);
        });
    }

    /**
     * Update resolution for both WebView and Texture.
     *
     * @param textureWidth  Texture new width
     * @param textureHeight Texture new height
     * @param webWidth      WebView new width
     * @param webHeight     WebView new height
     */
    public void resize(int textureWidth, int textureHeight, int webWidth, int webHeight) {
        m_texWidth = textureWidth;
        m_texHeight = textureHeight;
        m_webWidth = webWidth;
        m_webHeight = webHeight;

        if (m_viewToBufferRenderer != null) {
            m_viewToBufferRenderer.SetTextureResolution(m_texWidth, m_texHeight);
            m_viewToBufferRenderer.disable();
        }

        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            ViewGroup.LayoutParams lp = m_rootLayout.getLayoutParams();
            lp.width = m_webWidth;
            lp.height = m_webHeight;

            ArrayList<Dialog> dialogs = Dialog.getViewsByType(m_rootLayout, Dialog.class);
            int minSize = Math.min(m_webWidth, m_webHeight);
            dialogs.forEach(dialog -> {
                dialog.setScale(minSize);
            });

            m_rootLayout.setLayoutParams(lp);
        });
    }

    //
    // touch event
    //

    /**
     * Dispatch of a touch event.
     *
     * @param x        Touch position x
     * @param y        Touch position y
     * @param eventNum Touch event type (TOUCH_DOWN: 0, TOUCH_UP: 1, TOUCH_MOVE: 2)
     */
    public void touchEvent(int x, int y, int eventNum) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_frameLayout == null) {
                return;
            }

            // Obtain MotionEvent object
            // https://banbara-studio.hatenablog.com/entry/2018/04/02/130902
            final long downTime = SystemClock.uptimeMillis();
            final long eventTime = SystemClock.uptimeMillis() + 50;
            final int source = InputDevice.SOURCE_CLASS_POINTER;

            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
            final int metaState = 0;
            MotionEvent event = MotionEvent.obtain(downTime, eventTime, eventNum, x, y, metaState);
            event.setSource(source);

            // Dispatch touch event to view
            m_frameLayout.dispatchTouchEvent(event);
        });
    }

    //
    // key event
    //

    /**
     * Dispatch of a basic keycode event.
     *
     * @param key 'a', 'b', 'A' ....
     */
    public void keyEvent(char key) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_frameLayout == null) {
                return;
            }
            KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            KeyEvent[] events = kcm.getEvents(new char[]{key});
            for (KeyEvent event : events) {
                m_frameLayout.dispatchKeyEvent(event);
            }
        });
    }

    /**
     * Dispatch of a backspace key event.
     */
    public void backSpaceKey() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_frameLayout == null) {
                return;
            }
            m_frameLayout.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            m_frameLayout.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        });
    }

    //
    // page back/forward
    //

    /**
     * Goes back in the history of this WebView.
     */
    public void goBack() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null || !m_canGoBack) {
                return;
            }
            m_webview.goBack();
        });
    }

    /**
     * Goes forward in the history of this WebView.
     */
    public void goForward() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null || !m_canGoForward) {
                return;
            }
            m_webview.goForward();
        });
    }

    //
    // cache
    //

    /**
     * Clear WebView Cache.
     *
     * @param includeDiskFiles If false, only the RAM cache will be cleared.
     */
    public void clearCash(boolean includeDiskFiles) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.clearCache(includeDiskFiles);
        });
    }

    /**
     * Clear WebView History.
     */
    public void clearHistory() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.clearHistory();
        });
    }

    /**
     * Clear WebView Cookie.
     */
    public void clearCookie() {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        });
    }

    //
    // others
    //

    /**
     * Set the visibility state of this view.
     *
     * @param visibility int: One of VISIBLE, INVISIBLE, or GONE. Value is VISIBLE, INVISIBLE, or GONE
     */
    public void setVisible(int visibility) {
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }

            switch (visibility) {
                case 0:
                    m_webview.setVisibility(View.VISIBLE);
                    m_webview.requestFocus();
                    break;
                case 1:
                    m_webview.setVisibility(View.INVISIBLE);
                    break;
                case 2:
                    m_webview.setVisibility(View.GONE);
                    break;
            }
        });
    }

    private void SetMargins(int left, int top, int right, int bottom) {
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.NO_GRAVITY);
        params.setMargins(left, top, right, bottom);
        final Activity a = UnityPlayer.currentActivity;
        a.runOnUiThread(() -> {
            if (m_webview == null) {
                return;
            }
            m_webview.setLayoutParams(params);
        });
    }

    private void AddCustomHeader(final String headerKey, final String headerValue) {
        if (m_customHeaders == null) {
            return;
        }
        m_customHeaders.put(headerKey, headerValue);
    }

    private String GetCustomHeaderValue(final String headerKey) {
        if (m_customHeaders == null) {
            return null;
        }
        if (!m_customHeaders.containsKey(headerKey)) {
            return null;
        }
        return m_customHeaders.get(headerKey);
    }

    private void RemoveCustomHeader(final String headerKey) {
        if (m_customHeaders == null) {
            return;
        }
        m_customHeaders.remove(headerKey);
    }

    private void ClearCustomHeader() {
        if (m_customHeaders == null) {
            return;
        }
        m_customHeaders.clear();
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
            m_webview.setHttpAuthUsernamePassword(host, realm, userName, userPass);
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
