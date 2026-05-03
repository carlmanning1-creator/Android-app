package com.carlmanning.sesdashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.carlmanning.sesdashboard.ui.theme.SESUnitDashboardTheme

/**
 * Top-level mutable state holding the most recent payload posted from
 * JavaScript via the bridge. Compose observes this and re-renders.
 */
val lastExtractedData = mutableStateOf("Waiting for first API response...")

/**
 * Bridge object exposed to JavaScript inside the WebView.
 * JS calls window.ScrapeBridge.reportData(jsonString); the string lands
 * here on a background thread, and we hop to the UI thread to update state.
 */
class ScrapeBridge {
    @JavascriptInterface
    fun reportData(json: String) {
        Handler(Looper.getMainLooper()).post {
            lastExtractedData.value = json
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CookieManager.getInstance().setAcceptCookie(true)
        setContent {
            SESUnitDashboardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ExtractionStatusBar()
                        SourceWebView(
                            url = "https://myavailability.ses.nsw.gov.au",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status bar showing the most recent JSON payload posted by the JS extractor.
 * Scrollable up to ~140 dp so larger payloads remain readable.
 */
@Composable
fun ExtractionStatusBar() {
    val data by lastExtractedData
    val scroll = rememberScrollState()
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp, max = 140.dp)
    ) {
        Text(
            text = data,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(scroll)
        )
    }
}

@Composable
fun SourceWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true

                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                WebView.setWebContentsDebuggingEnabled(true)

                addJavascriptInterface(ScrapeBridge(), "ScrapeBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        // 1) Best-effort layout fix (kept in case it ever takes).
                        val heightFixJs = """
                            (function() {
                                function applyFix() {
                                    document.documentElement.style.setProperty('height', '100%', 'important');
                                    document.body.style.setProperty('height', '100%', 'important');
                                    document.body.style.setProperty('min-height', '100vh', 'important');
                                    document.body.style.setProperty('margin', '0', 'important');
                                    var root = document.getElementById('root');
                                    if (root) {
                                        root.style.setProperty('height', '100%', 'important');
                                        root.style.setProperty('min-height', '100vh', 'important');
                                    }
                                }
                                applyFix();
                                var count = 0;
                                var interval = setInterval(function() {
                                    applyFix();
                                    if (++count > 20) clearInterval(interval);
                                }, 500);
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(heightFixJs, null)

                        // 2) API interceptor — wraps fetch and XHR so we can
                        //    grab JSON responses from interesting endpoints
                        //    as soon as they hit the wire. Idempotent.
                        val interceptorJs = """
                            (function() {
                                if (window.__sesInterceptorInstalled) return;
                                window.__sesInterceptorInstalled = true;

                                function postBridge(payload) {
                                    try {
                                        if (window.ScrapeBridge && window.ScrapeBridge.reportData) {
                                            window.ScrapeBridge.reportData(JSON.stringify(payload, null, 2));
                                        }
                                        console.log('[SES Interceptor]', payload);
                                    } catch (e) {}
                                }

                                function classify(url) {
                                    if (!url) return null;
                                    if (url.indexOf('stream-io-api.com/channels') !== -1) return 'channels';
                                    if (url.indexOf('stream-io-api.com/threads') !== -1) return 'threads';
                                    if (url.indexOf('broadcast-messages/unacknowledged-count') !== -1) return 'broadcast-unread';
                                    if (url.indexOf('api.ses-mams.net') !== -1) return 'sesmams';
                                    return null;
                                }

                                function summarize(type, url, data) {
                                    var summary = {
                                        type: type,
                                        endpoint: url.split('?')[0].split('/').slice(-3).join('/'),
                                        time: new Date().toISOString().slice(11, 19)
                                    };
                                    if (type === 'channels' && data && data.channels) {
                                        summary.totalChannels = data.channels.length;
                                        summary.unreadChannels = data.channels.filter(function(c) {
                                            return (c.unread_count || 0) > 0;
                                        }).map(function(c) {
                                            return {
                                                name: c.channel ? (c.channel.name || c.channel.id) : '?',
                                                unread: c.unread_count || 0
                                            };
                                        });
                                        summary.totalUnread = summary.unreadChannels.reduce(function(s, c) { return s + c.unread; }, 0);
                                    } else if (type === 'broadcast-unread') {
                                        summary.value = data && data.count !== undefined ? data.count : data;
                                    } else {
                                        summary.preview = JSON.stringify(data).slice(0, 400);
                                    }
                                    return summary;
                                }

                                // Wrap fetch
                                var origFetch = window.fetch;
                                window.fetch = function(input) {
                                    var url = typeof input === 'string' ? input : (input && input.url) || '';
                                    var type = classify(url);
                                    var promise = origFetch.apply(this, arguments);
                                    if (type) {
                                        promise.then(function(response) {
                                            response.clone().json().then(function(data) {
                                                postBridge(summarize(type, url, data));
                                            }).catch(function() {});
                                        }).catch(function() {});
                                    }
                                    return promise;
                                };

                                // Wrap XHR
                                var origOpen = XMLHttpRequest.prototype.open;
                                var origSend = XMLHttpRequest.prototype.send;
                                XMLHttpRequest.prototype.open = function(method, url) {
                                    this._sesUrl = url;
                                    return origOpen.apply(this, arguments);
                                };
                                XMLHttpRequest.prototype.send = function() {
                                    var xhr = this;
                                    var type = classify(xhr._sesUrl);
                                    if (type) {
                                        xhr.addEventListener('load', function() {
                                            try {
                                                var data = JSON.parse(xhr.responseText);
                                                postBridge(summarize(type, xhr._sesUrl, data));
                                            } catch (e) {}
                                        });
                                    }
                                    return origSend.apply(this, arguments);
                                };

                                postBridge({
                                    type: 'interceptor-installed',
                                    time: new Date().toISOString().slice(11, 19),
                                    page: location.pathname
                                });
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(interceptorJs, null)
                    }
                }

                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                loadUrl(url)
            }
        }
    )
}
