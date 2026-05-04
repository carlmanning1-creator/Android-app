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
import androidx.compose.foundation.text.selection.SelectionContainer
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

val lastExtractedData = mutableStateOf("Waiting for first API response...")

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
 * Status bar showing the most recent JSON payload posted by the extractor.
 * Wrapped in SelectionContainer so you can long-press to select + copy.
 */
@Composable
fun ExtractionStatusBar() {
    val data by lastExtractedData
    val scroll = rememberScrollState()
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp, max = 160.dp)
    ) {
        SelectionContainer {
            Text(
                text = data,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(scroll)
            )
        }
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

                        // Best-effort layout fix.
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

                        // API interceptor.
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
 
                                function captureUserId(url) {
                                    if (window.__sesUserId) return;
                                    var m = (url || '').match(/[?&]user_id=([^&]+)/);
                                    if (m) window.__sesUserId = m[1];
                                }
 
                                function readEntryUserId(r) {
                                    // Stream Chat nests the user under r.user.id, not r.user_id.
                                    if (r.user && r.user.id) return r.user.id;
                                    if (r.user_id) return r.user_id;
                                    return null;
                                }
 
                                function getUnreadForChannel(c) {
                                    if (typeof c.unread_count === 'number') return c.unread_count;
                                    if (typeof c.unread_messages === 'number') return c.unread_messages;
                                    if (c.membership && typeof c.membership.unread_messages === 'number') return c.membership.unread_messages;
                                    if (Array.isArray(c.read) && window.__sesUserId) {
                                        for (var i = 0; i < c.read.length; i++) {
                                            var r = c.read[i];
                                            if (String(readEntryUserId(r)) === String(window.__sesUserId)) {
                                                if (typeof r.unread_messages === 'number') return r.unread_messages;
                                                if (typeof r.unread_count === 'number') return r.unread_count;
                                            }
                                        }
                                    }
                                    return 0;
                                }
 
                                function summarize(type, url, data) {
                                    var summary = {
                                        type: type,
                                        endpoint: url.split('?')[0].split('/').slice(-3).join('/'),
                                        time: new Date().toISOString().slice(11, 19)
                                    };
                                    if (type === 'channels' && data && data.channels) {
                                        window.__sesLastChannels = data;
                                        summary.userId = window.__sesUserId || '(not captured)';
                                        summary.totalChannels = data.channels.length;
                                        var unreadList = [];
                                        var totalUnread = 0;
                                        for (var i = 0; i < data.channels.length; i++) {
                                            var c = data.channels[i];
                                            var u = getUnreadForChannel(c);
                                            if (u > 0) {
                                                var name = c.channel ? (c.channel.name || c.channel.id) : '?';
                                                unreadList.push({ name: String(name).slice(0, 60), unread: u });
                                                totalUnread += u;
                                            }
                                        }
                                        summary.totalUnread = totalUnread;
                                        summary.unreadChannels = unreadList.slice(0, 20);
                                    } else if (type === 'broadcast-unread') {
                                        summary.value = (data && data.count !== undefined) ? data.count : data;
                                    } else {
                                        summary.preview = JSON.stringify(data).slice(0, 400);
                                    }
                                    return summary;
                                }
 
                                var origFetch = window.fetch;
                                window.fetch = function(input) {
                                    var url = typeof input === 'string' ? input : (input && input.url) || '';
                                    var type = classify(url);
                                    if (type) captureUserId(url);
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
 
                                var origOpen = XMLHttpRequest.prototype.open;
                                var origSend = XMLHttpRequest.prototype.send;
                                XMLHttpRequest.prototype.open = function(method, url) {
                                    this._sesUrl = url;
                                    return origOpen.apply(this, arguments);
                                };
                                XMLHttpRequest.prototype.send = function() {
                                    var xhr = this;
                                    var type = classify(xhr._sesUrl);
                                    if (type) captureUserId(xhr._sesUrl);
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