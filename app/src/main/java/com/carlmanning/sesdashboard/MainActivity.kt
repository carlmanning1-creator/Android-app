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
import androidx.compose.foundation.layout.padding
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
 * JavaScript via the bridge. Compose observes this and re-renders
 * automatically whenever it changes.
 */
val lastExtractedData = mutableStateOf("Waiting for first extraction...")

/**
 * Bridge object exposed to JavaScript inside the WebView.
 * JS can call window.ScrapeBridge.reportData(jsonString) and the string
 * lands in Kotlin in the function below.
 */
class ScrapeBridge {
    @JavascriptInterface
    fun reportData(json: String) {
        // JS callbacks arrive on a background thread. Hop to the main
        // (UI) thread before touching Compose state.
        Handler(Looper.getMainLooper()).post {
            lastExtractedData.value = json
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Make sure cookies are stored on disk so the login session survives app restarts.
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
 * A small status bar at the top showing what data the JS extractor has
 * most recently posted back via the bridge. Useful for development; we'll
 * replace this with the real dashboard in Phase 3.
 */
@Composable
fun ExtractionStatusBar() {
    val data by lastExtractedData
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = data,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * A single embedded WebView that loads one source URL and persists cookies
 * so the user only has to log in once. We will reuse this composable for
 * every source (Outlook x2, Planner, myAvailability) in later phases.
 */
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
                                        root.style.setProperty('display', 'flex', 'important');
                                        root.style.setProperty('flex-direction', 'column', 'important');
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

                        val testExtractorJs = """
                            (function() {
                                try {
                                    var payload = {
                                        url: location.pathname,
                                        title: document.title,
                                        timestamp: new Date().toISOString()
                                    };
                                    if (window.ScrapeBridge && window.ScrapeBridge.reportData) {
                                        window.ScrapeBridge.reportData(JSON.stringify(payload));
                                    }
                                } catch (e) {
                                    if (window.ScrapeBridge && window.ScrapeBridge.reportData) {
                                        window.ScrapeBridge.reportData('Extractor error: ' + e.message);
                                    }
                                }
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(testExtractorJs, null)
                    }
                }

                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                loadUrl(url)
            }
        }
    )
}