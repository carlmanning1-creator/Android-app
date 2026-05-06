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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.carlmanning.sesdashboard.ui.theme.SESUnitDashboardTheme

data class Source(val name: String, val url: String, val profile: String = "")

val SOURCES = listOf(
    Source("myAvail", "https://myavailability.ses.nsw.gov.au"),
    Source("Outlook", "https://outlook.office.com"),
    Source("DBO Ops", "https://outlook.cloud.microsoft/mail/dbo.ops@ses.nsw.gov.au/", profile = "dbo_ops"),
    Source("Planner", "https://planner.cloud.microsoft")
)

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
                    var currentSourceName by remember { mutableStateOf(SOURCES.first().name) }
                    val webViewMap = remember { mutableStateMapOf<String, WebView>() }

                    LaunchedEffect(currentSourceName, webViewMap[currentSourceName]) {
                        globalWebView = webViewMap[currentSourceName]
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SourceTabs(currentSourceName) { currentSourceName = it }
                        ActionButtons()
                        ExtractionStatusBar()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .fillMaxWidth()
                        ) {
                            SOURCES.forEach { source ->
                                key(source.name) {
                                    val isActive = currentSourceName == source.name
                                    SourceWebView(
                                        source = source,
                                        onWebViewReady = { wv -> webViewMap[source.name] = wv },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(if (isActive) 1f else 0f)
                                            .zIndex(if (isActive) 1f else 0f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceTabs(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SOURCES.forEach { source ->
            FilterChip(
                selected = current == source.name,
                onClick = { onSelect(source.name) },
                label = { Text(source.name) }
            )
        }
    }
}

@Composable
fun ActionButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                globalWebView?.evaluateJavascript(
                    "if (window.refresh) window.refresh();",
                    null
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Refresh")
        }
        Button(
            onClick = {
                globalWebView?.evaluateJavascript(
                    "if (window.dumpFetchLog) window.dumpFetchLog();",
                    null
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Fetch log")
        }
        Button(
            onClick = {
                globalWebView?.evaluateJavascript(
                    "if (window.dumpSource) window.dumpSource();",
                    null
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Dump")
        }
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
            .heightIn(min = 32.dp, max = 280.dp)
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

private fun assignProfileSafely(webView: WebView, profileName: String) {
    if (profileName.isEmpty()) return
    try {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            ProfileStore.getInstance().getOrCreateProfile(profileName)
            WebViewCompat.setProfile(webView, profileName)
        }
    } catch (e: Throwable) {
        android.util.Log.w("SourceWebView", "Profile assignment failed: ${e.message}")
    }
}

@Composable
fun SourceWebView(
    source: Source,
    onWebViewReady: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                assignProfileSafely(this, source.profile)

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
                        view?.evaluateJavascript(buildHeightFixJs(), null)
                        view?.evaluateJavascript(buildInterceptorJs(), null)
                    }
                }

                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                onWebViewReady(this)
                loadUrl(source.url)
            }
        }
    )
}

private fun buildHeightFixJs(): String = """
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

private fun buildInterceptorJs(): String = """
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

        function readAuthHeader(input, init) {
            try {
                var h = (init && init.headers) || (input && input.headers);
                if (!h) return '';
                if (h instanceof Headers) return h.get('Authorization') || '';
                if (typeof h === 'object') return h['Authorization'] || h['authorization'] || '';
                if (Array.isArray(h)) {
                    for (var i = 0; i < h.length; i++) {
                        if (h[i][0] && h[i][0].toLowerCase() === 'authorization') return h[i][1];
                    }
                }
            } catch (e) {}
            return '';
        }

        window.__sesFetchLog = window.__sesFetchLog || [];
        window.__sesOwaRequests = window.__sesOwaRequests || {};
        window.__sesOwaResponses = window.__sesOwaResponses || {};

        var origFetch = window.fetch;
        window.fetch = function(input, init) {
            var url = typeof input === 'string' ? input : (input && input.url) || '';

            if (url && url.indexOf('sentry') === -1) {
                window.__sesFetchLog.push({ url: url.slice(0, 250), t: Date.now() });
                if (window.__sesFetchLog.length > 50) window.__sesFetchLog.shift();
            }

            // OWA service.svc capture (Outlook).
            if (url.indexOf('owa/service.svc') !== -1 && init && init.body) {
                var aMatch = url.match(/[?&]action=([^&]+)/);
                var act = aMatch ? aMatch[1] : 'unknown';
                try {
                    window.__sesOwaRequests[act] = {
                        url: url,
                        method: (init && init.method) || 'POST',
                        body: typeof init.body === 'string' ? init.body : '[non-string]'
                    };
                } catch (e) {}
            }

            var promise = origFetch.apply(this, arguments);

            if (url.indexOf('owa/service.svc') !== -1) {
                var aMatch2 = url.match(/[?&]action=([^&]+)/);
                var act2 = aMatch2 ? aMatch2[1] : 'unknown';
                promise.then(function(response) {
                    response.clone().text().then(function(text) {
                        try {
                            window.__sesOwaResponses[act2] = JSON.parse(text);
                        } catch (e) {}
                    }).catch(function() {});
                }).catch(function() {});
            }

            // Stream Chat capture.
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

            // Planner capture: token from headers, and tasks response body —
            // but only cache responses that have actual data, so that empty
            // delta-sync responses don't wipe a previously-good cache.
            if (url.indexOf('api.planner.svc.cloud.microsoft') !== -1) {
                var auth = readAuthHeader(input, init);
                if (auth) window.__sesPlannerAuth = auth;
                if (url.indexOf('/tasks') !== -1) {
                    promise.then(function(response) {
                        response.clone().json().then(function(data) {
                            var v = (data && data.value) || data;
                            var isEmpty = Array.isArray(v) && v.length === 0;
                            if (!isEmpty || !window.__sesPlannerTasks) {
                                window.__sesPlannerTasks = data;
                            }
                        }).catch(function() {});
                    }).catch(function() {});
                }
            }

            return promise;
        };

        window.dumpFetchLog = function() {
            postBridge({
                type: 'fetch-log',
                host: location.hostname,
                count: window.__sesFetchLog.length,
                urls: window.__sesFetchLog.slice(-20).map(function(e) {
                    return e.url.replace(/^https?:\/\//, '').slice(0, 200);
                })
            });
        };

        window.dumpOwaData = function() {
            var actions = Object.keys(window.__sesOwaResponses || {});
            var summary = {
                type: 'owa-dump',
                host: location.hostname,
                actionsCaptured: actions
            };
            actions.forEach(function(a) {
                var resp = window.__sesOwaResponses[a];
                var req = window.__sesOwaRequests[a];
                summary[a] = {
                    requestBody: req ? String(req.body).slice(0, 400) : null,
                    responseTopKeys: (resp && typeof resp === 'object') ? Object.keys(resp).slice(0, 10) : null,
                    responsePreview: JSON.stringify(resp).slice(0, 500)
                };
            });
            postBridge(summary);
        };

        window.dumpPlannerData = function() {
            var d = window.__sesPlannerTasks;
            if (!d) {
                postBridge({ type: 'planner-dump', note: 'No tasks data captured yet — wait for Planner to load.' });
                return;
            }
            var tasks = Array.isArray(d) ? d : (d.value || d.tasks || []);
            var summary = {
                type: 'planner-dump',
                host: location.hostname,
                hasAuth: !!window.__sesPlannerAuth,
                rawTopKeys: typeof d === 'object' ? Object.keys(d).slice(0, 20) : null,
                tasksIsArray: Array.isArray(tasks),
                tasksCount: Array.isArray(tasks) ? tasks.length : 'n/a'
            };
            if (Array.isArray(tasks) && tasks[0]) {
                summary.firstTaskKeys = Object.keys(tasks[0]).slice(0, 30);
                summary.firstTaskPreview = JSON.stringify(tasks[0]).slice(0, 800);
            }
            postBridge(summary);
        };

        window.dumpSource = function() {
            var host = location.hostname;
            if (host.indexOf('outlook') !== -1) return window.dumpOwaData && window.dumpOwaData();
            if (host.indexOf('planner') !== -1) return window.dumpPlannerData && window.dumpPlannerData();
            if (host.indexOf('myavailability') !== -1) {
                return window.refreshSesData && window.refreshSesData();
            }
            postBridge({ error: 'No dump available for host: ' + host });
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

        function itemSummary(it) {
            var inner = it.activity || it.activation || {};
            var title = it.title || inner.title || inner.name || it.name || it.id;
            var start = inner.start;
            if (start) return title + ' — ' + String(start).replace('T', ' ').slice(0, 16);
            return title;
        }

        window.refreshSesData = function() {
            window.__sesHasRefreshed = true;
            var token = localStorage.getItem('accessToken');
            if (!token) {
                postBridge({ error: 'No SES access token. Are you on the myAvailability tab and logged in?' });
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
                    .then(function(r) { return r.json().then(function(d) { return [pair[0], { status: r.status, data: d }]; }); })
                    .catch(function(e) { return [pair[0], { error: e.message }]; });
            })).then(function(results) {
                var summary = {
                    type: 'myAvailability-summary',
                    time: new Date().toISOString().slice(11, 19)
                };

                results.forEach(function(pair) {
                    var key = pair[0];
                    var v = pair[1];
                    if (v.error) { summary[key] = { error: v.error }; return; }
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
                        if (n > 0) { totalUnread += n; unreadChannels++; }
                    });
                    summary.messages = {
                        totalChannels: d.channels.length,
                        unreadChannels: unreadChannels,
                        totalUnread: totalUnread
                    };
                } else {
                    summary.messages = { note: 'Stream Chat data not yet captured. Tap Messages once.' };
                }

                postBridge(summary);
            }).catch(function(e) {
                postBridge({ error: 'refresh failed: ' + e.message });
            });
        };

        window.extractOutlookData = function() {
            var resps = window.__sesOwaResponses || {};
            var fcd = resps['GetFolderChangeDigest'];
            var fc = resps['FindConversation'];

            var summary = {
                type: 'outlook-summary',
                host: location.hostname,
                pathHint: location.pathname,
                time: new Date().toISOString().slice(11, 19)
            };

            if (fcd && fcd.Folders && fcd.Folders[0]) {
                var f = fcd.Folders[0];
                summary.inbox = {
                    unreadCount: f.UnseenCount,
                    recentSenders: (f.RecentUniqueSenders || []).slice(0, 8)
                };
            } else {
                summary.inbox = { note: 'No GetFolderChangeDigest captured yet — wait for inbox to load.' };
            }

            if (fc && fc.Body && Array.isArray(fc.Body.Conversations)) {
                summary.recentConversations = fc.Body.Conversations.slice(0, 8).map(function(c) {
                    var when = (c.LastDeliveryTime || '').slice(0, 16).replace('T', ' ');
                    var senders = (c.UniqueSenders || []).slice(0, 2).join(', ');
                    var topic = (c.ConversationTopic || '').slice(0, 80);
                    return when + ' — ' + senders + ': ' + topic;
                });
            } else {
                summary.recentConversations = { note: 'No FindConversation captured yet.' };
            }

            postBridge(summary);
        };

        function buildPlannerSummary(d) {
            var summary = {
                type: 'planner-summary',
                host: location.hostname,
                time: new Date().toISOString().slice(11, 19)
            };
            if (!d) { summary.note = 'No Planner tasks data.'; return summary; }
            var tasks = Array.isArray(d) ? d : (d.value || d.tasks || []);
            if (!Array.isArray(tasks)) {
                summary.note = 'Unexpected Planner shape — tap Dump to inspect.';
                summary.topKeys = typeof d === 'object' ? Object.keys(d).slice(0, 10) : null;
                return summary;
            }
            summary.totalTasks = tasks.length;
            var openTasks = tasks.filter(function(t) {
                if (t.completedDateTime) return false;
                if (typeof t.percentComplete === 'number' && t.percentComplete >= 100) return false;
                return true;
            });
            summary.openTasks = openTasks.length;
            summary.openTitles = openTasks.slice(0, 10).map(function(t) {
                var title = t.displayName || t.title || t.name || t.id || '?';
                var due = '';
                if (t.dueDateTime) {
                    if (typeof t.dueDateTime === 'object') {
                        due = t.dueDateTime.date || t.dueDateTime.utcDate || '';
                    } else {
                        due = String(t.dueDateTime);
                    }
                }
                var prefix = String(title).slice(0, 70);
                if (due) return prefix + ' — due ' + String(due).slice(0, 10);
                return prefix;
            });
            return summary;
        }

        window.refreshPlannerFromAPI = function() {
            var token = window.__sesPlannerAuth;
            if (!token) {
                var fallback = buildPlannerSummary(window.__sesPlannerTasks);
                fallback.note = 'No Planner Bearer token captured yet — interact with Planner first.';
                postBridge(fallback);
                return;
            }
            var url = "https://api.planner.svc.cloud.microsoft/taskapi/v4.0/users('me')/tasks?%24expand=checklist,links,userAssignments";
            fetch(url, { headers: { 'Authorization': token } })
                .then(function(r) { return r.json().then(function(d) { return { status: r.status, data: d }; }); })
                .then(function(res) {
                    if (res.status >= 400) {
                        postBridge({ error: 'Planner API ' + res.status, body: JSON.stringify(res.data).slice(0, 300) });
                        return;
                    }
                    window.__sesPlannerTasks = res.data;
                    postBridge(buildPlannerSummary(res.data));
                })
                .catch(function(e) {
                    postBridge({ error: 'Planner fetch failed: ' + e.message });
                });
        };

        window.extractPlannerData = function() {
            postBridge(buildPlannerSummary(window.__sesPlannerTasks));
        };

        window.refresh = function() {
            var host = location.hostname;
            if (host.indexOf('myavailability') !== -1) {
                if (window.refreshSesData) window.refreshSesData();
                return;
            }
            if (host.indexOf('outlook') !== -1) {
                if (window.extractOutlookData) window.extractOutlookData();
                return;
            }
            if (host.indexOf('planner') !== -1) {
                if (window.refreshPlannerFromAPI) window.refreshPlannerFromAPI();
                return;
            }
            postBridge({ error: 'No extractor for host: ' + host });
        };

        postBridge({
            type: 'interceptor-installed',
            host: location.hostname,
            page: location.pathname
        });

        if (location.hostname.indexOf('myavailability') !== -1) {
            setTimeout(function() {
                if (window.refreshSesData) window.refreshSesData();
            }, 5000);
        }
    })();
""".trimIndent()