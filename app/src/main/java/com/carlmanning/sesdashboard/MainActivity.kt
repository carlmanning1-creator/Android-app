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
import androidx.compose.material3.Button
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

val lastExtractedData = mutableStateOf("Tap Refresh after the page loads...")
var globalWebView: WebView? = null

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
                        RefreshButton()
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

@Composable
fun RefreshButton() {
    Button(
        onClick = {
            globalWebView?.evaluateJavascript(
                "if (window.refreshSesData) window.refreshSesData(); else 'not ready';",
                null
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("Refresh myAvailability data")
    }
}

@Composable
fun ExtractionStatusBar() {
    val data by lastExtractedData
    val scroll = rememberScrollState()
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp, max = 260.dp)
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
                globalWebView = this

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

                                function captureUserId(url) {
                                    if (window.__sesUserId) return;
                                    var m = (url || '').match(/[?&]user_id=([^&]+)/);
                                    if (m) window.__sesUserId = m[1];
                                }

                                // Stream Chat fetch interceptor: captures /channels response
                                // into window.__sesLastChannels and re-fires the refresh if
                                // we've already done one (so the messages tile self-heals).
                                var origFetch = window.fetch;
                                window.fetch = function(input) {
                                    var url = typeof input === 'string' ? input : (input && input.url) || '';
                                    var promise = origFetch.apply(this, arguments);
                                    if (url.indexOf('stream-io-api.com/channels') !== -1) {
                                        captureUserId(url);
                                        promise.then(function(response) {
                                            response.clone().json().then(function(data) {
                                                window.__sesLastChannels = data;
                                                if (window.__sesHasRefreshed && window.refreshSesData) {
                                                    window.refreshSesData();
                                                }
                                            }).catch(function() {});
                                        }).catch(function() {});
                                    }
                                    return promise;
                                };

                                function streamChannelUnread(c) {
                                    if (!Array.isArray(c.read) || !window.__sesUserId) return 0;
                                    for (var i = 0; i < c.read.length; i++) {
                                        var r = c.read[i];
                                        var uid = (r.user && r.user.id) || r.user_id;
                                        if (String(uid) === String(window.__sesUserId)) {
                                            return r.unread_messages || 0;
                                        }
                                    }
                                    return 0;
                                }

                                // Build a readable label for one item: title plus start datetime if available.
                                function itemSummary(it) {
                                    var inner = it.activity || it.activation || {};
                                    var title = it.title
                                        || inner.title
                                        || inner.name
                                        || it.name
                                        || it.id;
                                    var start = inner.start;
                                    if (start) {
                                        return title + ' — ' + String(start).replace('T', ' ').slice(0, 16);
                                    }
                                    return title;
                                }

                                window.refreshSesData = function() {
                                    window.__sesHasRefreshed = true;
                                    var token = localStorage.getItem('accessToken');
                                    if (!token) {
                                        postBridge({ error: 'No access token in localStorage. Are you logged in?' });
                                        return;
                                    }

                                    var headers = { 'Authorization': 'Bearer ' + token };
                                    var endpoints = [
                                        ['operational', 'https://api.ses-mams.net/activation-requests?skip=0&take=20&types%5B0%5D=Urgent&types%5B1%5D=NotUrgent&archived=false&query='],
                                        ['activity', 'https://api.ses-mams.net/activity-requests'],
                                        ['ooaa', 'https://api.ses-mams.net/out-of-area-activation-requests'],
                                        ['ooaaApprovals', 'https://api.ses-mams.net/out-of-area-activation-approvals']
                                    ];

                                    Promise.all(endpoints.map(function(pair) {
                                        return fetch(pair[1], { headers: headers })
                                            .then(function(r) {
                                                return r.json().then(function(d) {
                                                    return [pair[0], { status: r.status, data: d }];
                                                });
                                            })
                                            .catch(function(e) { return [pair[0], { error: e.message }]; });
                                    })).then(function(results) {
                                        var summary = {
                                            type: 'myAvailability-summary',
                                            time: new Date().toISOString().slice(11, 19)
                                        };

                                        results.forEach(function(pair) {
                                            var key = pair[0];
                                            var v = pair[1];
                                            if (v.error) {
                                                summary[key] = { error: v.error };
                                                return;
                                            }
                                            var data = v.data || {};
                                            var items = data.items || [];
                                            summary[key] = {
                                                count: items.length,
                                                totalCount: data.totalCount,
                                                items: items.slice(0, 8).map(itemSummary)
                                            };
                                        });

                                        if (window.__sesLastChannels && window.__sesUserId) {
                                            var d = window.__sesLastChannels;
                                            var totalUnread = 0;
                                            var unreadChannels = 0;
                                            d.channels.forEach(function(c) {
                                                var n = streamChannelUnread(c);
                                                if (n > 0) {
                                                    totalUnread += n;
                                                    unreadChannels++;
                                                }
                                            });
                                            summary.messages = {
                                                totalChannels: d.channels.length,
                                                unreadChannels: unreadChannels,
                                                totalUnread: totalUnread
                                            };
                                        } else {
                                            summary.messages = { note: 'Stream Chat data not yet captured. Tap Messages once and the tile will self-heal.' };
                                        }

                                        postBridge(summary);
                                    }).catch(function(e) {
                                        postBridge({ error: 'refresh failed: ' + e.message });
                                    });
                                };

                                postBridge({
                                    type: 'interceptor-installed',
                                    page: location.pathname,
                                    note: 'Auto-refreshing in 5 s. Tap Messages to populate the chat tile.'
                                });

                                setTimeout(function() {
                                    if (window.refreshSesData) window.refreshSesData();
                                }, 5000);
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
