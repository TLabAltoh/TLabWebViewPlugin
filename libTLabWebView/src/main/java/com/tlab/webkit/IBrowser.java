package com.tlab.webkit;

public interface IBrowser {

    /**
     * Run javascript on the current web page.
     *
     * @param js javascript
     */
    void EvaluateJS(String js);

    /**
     * Request file download to Download Manager.
     *
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          The user agent to be used for the download
     * @param contentDisposition Content-disposition http header, if present
     * @param mimetype           The mimetype of the content reported by the server
     */
    void DownloadFromUrl(String url, String userAgent, String contentDisposition, String mimetype);

    /**
     * Update userAgent with the given userAgent string.
     *
     * @param ua     UserAgent string
     * @param reload If true, reload web page when userAgent is updated.
     */
    void SetUserAgent(final String ua, final boolean reload);

    /**
     * Capture current userAgent async.
     */
    int GetUserAgent();

    /**
     * Get current url that the WebView instance is loading
     *
     * @return Current url that the WebView instance is loading
     */
    String GetUrl();

    /**
     * Loads the given URL.
     *
     * @param url The URL of the resource to load.
     */
    void LoadUrl(String url);

    /**
     * Goes back in the history of this WebView.
     */
    void GoBack();

    /**
     * Goes forward in the history of this WebView.
     */
    void GoForward();
}
