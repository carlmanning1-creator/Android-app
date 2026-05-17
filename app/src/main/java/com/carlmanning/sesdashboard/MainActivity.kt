package com.carlmanning.sesdashboard

import android.content.Context
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.carlmanning.sesdashboard.ui.theme.SESUnitDashboardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.carlmanning.sesdashboard.BuildConfig

// ---- Sources --------------------------------------------------------------

data class Source(val name: String, val url: String, val profile: String = "")

val SOURCES = listOf(
    Source("myAvail", "https://myavailability.ses.nsw.gov.au"),
    Source("Outlook", "https://outlook.office.com"),
    Source("DBO Ops", "https://outlook.cloud.microsoft/mail/dbo.ops@ses.nsw.gov.au/", profile = "dbo_ops"),
    Source("Planner", "https://planner.cloud.microsoft")
)

const val DASHBOARD_TAB = "Dashboard"
const val SETTINGS_TAB = "Settings"
val ALL_TABS = listOf(DASHBOARD_TAB) + SOURCES.map { it.name } + listOf(SETTINGS_TAB)

// ---- Per-source state holders --------------------------------------------

data class MyAvailData(
    val operationalCount: Int = 0,
    val activityCount: Int = 0,
    val ooaaCount: Int = 0,
    val ooaaApprovalsCount: Int = 0,
    val unreadChannels: Int = 0,
    val totalUnread: Int = 0,
    val activityTitles: List<String> = emptyList(),
    val ooaaTitles: List<String> = emptyList(),
    val operationalTitles: List<String> = emptyList(),
    val time: String = ""
)

data class OutlookData(
    val unreadCount: Int = 0,
    val recentSenders: List<String> = emptyList(),
    val recentConversations: List<String> = emptyList(),
    val time: String = ""
)

data class PlannerData(
    val totalTasks: Int = 0,
    val openTasks: Int = 0,
    val openTitles: List<String> = emptyList(),
    val time: String = ""
)

data class ActionItem(
    val source: String,
    val subject: String,
    val action: String,
    val priority: String  // "high" | "medium" | "low"
)

val lastExtractedData = mutableStateOf("Tap Refresh on the Dashboard to populate.")
val myAvailState = mutableStateOf<MyAvailData?>(null)
val outlookPersonalState = mutableStateOf<OutlookData?>(null)
val outlookDboOpsState = mutableStateOf<OutlookData?>(null)
val plannerState = mutableStateOf<PlannerData?>(null)
val actionItemsState = mutableStateOf<List<ActionItem>?>(null)
val actionItemsLoading = mutableStateOf(false)
val actionItemsError = mutableStateOf<String?>(null)
val dismissedActionKeys = mutableStateOf<Set<String>>(emptySet())

fun ActionItem.dismissKey(): String = "$source|$subject|$action"
var globalWebView: WebView? = null

// ---- API key storage -----------------------------------------------------

private const val PREFS_NAME = "ses-dashboard-prefs"
private const val PREF_API_KEY = "anthropic_api_key"

fun getApiKey(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREF_API_KEY, "") ?: ""

fun setApiKey(context: Context, key: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(PREF_API_KEY, key).apply()
}

// ---- Bridge --------------------------------------------------------------

class ScrapeBridge {
    @JavascriptInterface
    fun reportData(json: String) {
        Handler(Looper.getMainLooper()).post {
            lastExtractedData.value = json
            try {
                val obj = JSONObject(json)
                when (obj.optString("type")) {
                    "myAvailability-summary" -> myAvailState.value = parseMyAvail(obj)
                    "outlook-summary" -> {
                        val host = obj.optString("host")
                        val data = parseOutlook(obj)
                        if (host.contains("cloud.microsoft")) {
                            outlookDboOpsState.value = data
                        } else {
                            outlookPersonalState.value = data
                        }
                    }
                    "planner-summary" -> plannerState.value = parsePlanner(obj)
                }
            } catch (e: Exception) {
                // Leave lastExtractedData as the raw JSON for debugging.
            }
        }
    }
}

private fun jsonStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).map { arr.optString(it) }
}

private fun parseMyAvail(obj: JSONObject): MyAvailData {
    fun count(key: String) = obj.optJSONObject(key)?.optInt("count") ?: 0
    fun titles(key: String) = jsonStringList(obj.optJSONObject(key)?.optJSONArray("items"))
    val msgs = obj.optJSONObject("messages")
    return MyAvailData(
        operationalCount = count("operational"),
        activityCount = count("activity"),
        ooaaCount = count("ooaa"),
        ooaaApprovalsCount = count("ooaaApprovals"),
        unreadChannels = msgs?.optInt("unreadChannels") ?: 0,
        totalUnread = msgs?.optInt("totalUnread") ?: 0,
        activityTitles = titles("activity"),
        ooaaTitles = titles("ooaa"),
        operationalTitles = titles("operational"),
        time = obj.optString("time")
    )
}

private fun parseOutlook(obj: JSONObject): OutlookData {
    val inbox = obj.optJSONObject("inbox")
    return OutlookData(
        unreadCount = inbox?.optInt("unreadCount") ?: 0,
        recentSenders = jsonStringList(inbox?.optJSONArray("recentSenders")),
        recentConversations = jsonStringList(obj.optJSONArray("recentConversations")),
        time = obj.optString("time")
    )
}

private fun parsePlanner(obj: JSONObject): PlannerData {
    return PlannerData(
        totalTasks = obj.optInt("totalTasks"),
        openTasks = obj.optInt("openTasks"),
        openTitles = jsonStringList(obj.optJSONArray("openTitles")),
        time = obj.optString("time")
    )
}

// ---- Claude API call -----------------------------------------------------

private const val CLAUDE_MODEL = "claude-haiku-4-5-20251001"
private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"

private val ACTION_SYSTEM_PROMPT = """
You are an assistant for Carl Manning, a deputy of the NSW SES (State Emergency Service) Dubbo Unit. You triage his unread emails, requests, and tasks to identify items that may require his action.

Carl wants to see ALL items that have any action attached, even minor ones. Rate each item:
- "high": clearly requires action, often time-sensitive (deadlines, urgent requests, decisions awaiting Carl, urgent operational requests)
- "medium": probably requires a response or action, but no strict deadline
- "low": might warrant a glance — could be relevant context, an FYI he should be aware of, an open task that's not urgent

When in doubt, prefer "low" over skipping. The only items you should skip entirely are:
- Pure marketing or advertising (e.g. conferences, training providers selling courses)
- Generic newsletters with no action implied
- Automated security alerts ("password changed") with no action required

Output ONLY a valid JSON array. No markdown, no preamble, no explanation, no code fences. Each element of the array must be an object with these fields:
- "source": one of "email-personal", "email-dbo-ops", "myavail-operational", "myavail-activity", "myavail-ooaa", "myavail-ooaa-approvals", "planner", "chat".
- "subject": a brief 1-line description of the item (under 80 chars).
- "action": what Carl needs to do (under 100 chars).
- "priority": one of "high", "medium", "low".

If there are genuinely no action items at all, return [].
""".trimIndent()

private fun buildActionsPrompt(
    myAvail: MyAvailData?,
    outlookPersonal: OutlookData?,
    outlookDboOps: OutlookData?,
    planner: PlannerData?
): String {
    val sb = StringBuilder()
    sb.appendLine("Today's unread items follow. Each section lists up to ~10 items.")
    sb.appendLine()

    if (outlookPersonal != null && outlookPersonal.recentConversations.isNotEmpty()) {
        sb.appendLine("=== Personal SES Outlook (${outlookPersonal.unreadCount} total unread) ===")
        outlookPersonal.recentConversations.forEach { sb.appendLine("- $it") }
        sb.appendLine()
    }
    if (outlookDboOps != null && outlookDboOps.recentConversations.isNotEmpty()) {
        sb.appendLine("=== DBO Ops shared mailbox (${outlookDboOps.unreadCount} total unread) ===")
        outlookDboOps.recentConversations.forEach { sb.appendLine("- $it") }
        sb.appendLine()
    }
    if (myAvail != null) {
        if (myAvail.operationalTitles.isNotEmpty()) {
            sb.appendLine("=== myAvailability — Operational requests (${myAvail.operationalCount}) ===")
            myAvail.operationalTitles.forEach { sb.appendLine("- $it") }
            sb.appendLine()
        }
        if (myAvail.activityTitles.isNotEmpty()) {
            sb.appendLine("=== myAvailability — Activity requests (${myAvail.activityCount}) ===")
            myAvail.activityTitles.forEach { sb.appendLine("- $it") }
            sb.appendLine()
        }
        if (myAvail.ooaaTitles.isNotEmpty()) {
            sb.appendLine("=== myAvailability — OOAA requests (${myAvail.ooaaCount}) ===")
            myAvail.ooaaTitles.forEach { sb.appendLine("- $it") }
            sb.appendLine()
        }
        if (myAvail.ooaaApprovalsCount > 0) {
            sb.appendLine("=== myAvailability — OOAA approvals pending Carl's action: ${myAvail.ooaaApprovalsCount} ===")
            sb.appendLine()
        }
        if (myAvail.unreadChannels > 0) {
            sb.appendLine("=== myAvailability chat ===")
            sb.appendLine("${myAvail.unreadChannels} channels with ${myAvail.totalUnread} unread messages (channel topics not visible — flag as low priority general check unless context shows urgency).")
            sb.appendLine()
        }
    }
    if (planner != null && planner.openTitles.isNotEmpty()) {
        sb.appendLine("=== Planner tasks (${planner.openTasks} open of ${planner.totalTasks} total) ===")
        planner.openTitles.forEach { sb.appendLine("- $it") }
        sb.appendLine()
    }

    return sb.toString().trim()
}

private suspend fun callClaude(apiKey: String, userPrompt: String): String =
    withContext(Dispatchers.IO) {
        val url = URL(CLAUDE_API_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("x-api-key", apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.setRequestProperty("content-type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 60000

        val body = JSONObject().apply {
            put("model", CLAUDE_MODEL)
            put("max_tokens", 2048)
            put("system", ACTION_SYSTEM_PROMPT)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                throw Exception("Claude API HTTP $code: ${err.take(200)}")
            }
            val resp = JSONObject(text)
            val content = resp.optJSONArray("content") ?: throw Exception("No 'content' in response")
            if (content.length() == 0) throw Exception("Empty content array")
            content.getJSONObject(0).optString("text")
                .ifEmpty { throw Exception("Empty text in response") }
        } finally {
            conn.disconnect()
        }
    }

private fun parseActionItems(claudeText: String): List<ActionItem> {
    // Claude should return only a JSON array, but be defensive:
    // strip leading/trailing whitespace and any markdown fences if present.
    val clean = claudeText.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```")
        .trim()
    val start = clean.indexOf('[')
    val end = clean.lastIndexOf(']')
    if (start == -1 || end == -1 || end < start) {
        throw Exception("No JSON array in Claude response: ${clean.take(200)}")
    }
    val arr = JSONArray(clean.substring(start, end + 1))
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ActionItem(
            source = o.optString("source", ""),
            subject = o.optString("subject", ""),
            action = o.optString("action", ""),
            priority = o.optString("priority", "low")
        )
    }
}

// ---- Activity / Compose root ---------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CookieManager.getInstance().setAcceptCookie(true)
        setContent {
            SESUnitDashboardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentTab by remember { mutableStateOf(DASHBOARD_TAB) }
                    val webViewMap = remember { mutableStateMapOf<String, WebView>() }

                    LaunchedEffect(currentTab, webViewMap[currentTab]) {
                        globalWebView = webViewMap[currentTab]
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        TabRow(currentTab) { currentTab = it }
                        ActionButtons(currentTab, webViewMap)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .fillMaxWidth()
                        ) {
                            DashboardScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if (currentTab == DASHBOARD_TAB) 1f else 0f)
                                    .zIndex(if (currentTab == DASHBOARD_TAB) 1f else 0f)
                            )
                            SettingsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if (currentTab == SETTINGS_TAB) 1f else 0f)
                                    .zIndex(if (currentTab == SETTINGS_TAB) 1f else 0f)
                            )
                            SOURCES.forEach { source ->
                                key(source.name) {
                                    val isActive = currentTab == source.name
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
fun TabRow(current: String, onSelect: (String) -> Unit) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ALL_TABS.forEach { name ->
            FilterChip(
                selected = current == name,
                onClick = { onSelect(name) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
fun ActionButtons(currentTab: String, webViewMap: Map<String, WebView>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (currentTab == DASHBOARD_TAB) {
                    SOURCES.forEach { src ->
                        webViewMap[src.name]?.evaluateJavascript(
                            "if (window.refresh) window.refresh();", null
                        )
                    }
                    // After triggering refresh of all sources, generate action items.
                    scope.launch {
                        // Give the WebViews ~3s to populate before we send to Claude.
                        kotlinx.coroutines.delay(3000)
                        triggerActionItemsGeneration(context)
                    }
                } else {
                    globalWebView?.evaluateJavascript(
                        "if (window.refresh) window.refresh();", null
                    )
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(if (currentTab == DASHBOARD_TAB) "Refresh All" else "Refresh")
        }
    }
}

private suspend fun triggerActionItemsGeneration(context: Context) {
    val key = getApiKey(context)
    if (key.isBlank()) {
        actionItemsError.value = "Set your Anthropic API key in the Settings tab."
        return
    }
    actionItemsLoading.value = true
    actionItemsError.value = null
    try {
        val prompt = buildActionsPrompt(
            myAvailState.value,
            outlookPersonalState.value,
            outlookDboOpsState.value,
            plannerState.value
        )
        if (prompt.isBlank()) {
            actionItemsError.value = "No data to analyse yet — refresh sources first."
            return
        }
        val text = callClaude(key, prompt)
        actionItemsState.value = parseActionItems(text)
        dismissedActionKeys.value = emptySet()  // reset dismissals on regenerate
    } catch (e: Exception) {
        actionItemsError.value = e.message ?: "Unknown error"
    } finally {
        actionItemsLoading.value = false
    }
}

// ---- Dashboard ------------------------------------------------------------

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val myAvail by myAvailState
    val outlookPersonal by outlookPersonalState
    val outlookDboOps by outlookDboOpsState
    val planner by plannerState
    val items by actionItemsState
    val loading by actionItemsLoading
    val error by actionItemsError
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionItemsTile(
            items = items,
            loading = loading,
            error = error,
            onGenerate = { scope.launch { triggerActionItemsGeneration(context) } }
        )
        InboxTile(personal = outlookPersonal, dboOps = outlookDboOps)
        ActivitiesTile(myAvail)
        PlannerTile(planner)
        MessagesTile(myAvail)
    }
}

@Composable
private fun TileCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun NotLoaded(hint: String) {
    Text(
        text = hint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun ActionItemsTile(
    items: List<ActionItem>?,
    loading: Boolean,
    error: String?,
    onGenerate: () -> Unit
) {
    TileCard(title = "Action Items") {
        when {
            loading -> {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Asking Claude to triage your unread items…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            error != null -> {
                Text("Couldn't generate action items.", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
                Text(error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onGenerate) { Text("Retry") }
            }
            items == null -> {
                NotLoaded("Tap below to ask Claude to identify what needs your action today.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onGenerate) { Text("Generate action items") }
            }
            items.isEmpty() -> {
                Text("Nothing requires action right now. ✓")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onGenerate) { Text("Regenerate") }
            }
            else -> {
                val dismissed by dismissedActionKeys
                val visible = items.filter { it.dismissKey() !in dismissed }
                val dismissCount = items.size - visible.size

                fun onDismiss(item: ActionItem) {
                    dismissedActionKeys.value = dismissedActionKeys.value + item.dismissKey()
                }

                val high = visible.filter { it.priority.equals("high", true) }
                val medium = visible.filter { it.priority.equals("medium", true) }
                val low = visible.filter { it.priority.equals("low", true) }
                if (high.isNotEmpty()) {
                    PrioritySection("High priority", high, MaterialTheme.colorScheme.error, defaultExpanded = true, onDismiss = ::onDismiss)
                }
                if (medium.isNotEmpty()) {
                    if (high.isNotEmpty()) Spacer(Modifier.height(6.dp))
                    PrioritySection("Medium priority", medium, MaterialTheme.colorScheme.tertiary, defaultExpanded = true, onDismiss = ::onDismiss)
                }
                if (low.isNotEmpty()) {
                    if (high.isNotEmpty() || medium.isNotEmpty()) Spacer(Modifier.height(6.dp))
                    PrioritySection("Low priority", low, MaterialTheme.colorScheme.onSurfaceVariant, defaultExpanded = false, onDismiss = ::onDismiss)
                }
                if (visible.isEmpty()) {
                    Text("All items dismissed. ✓", style = MaterialTheme.typography.bodyMedium)
                }
                if (dismissCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$dismissCount dismissed — Regenerate to clear",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onGenerate) { Text("Regenerate") }
            }
        }
    }
}

@Composable
private fun PrioritySection(
    title: String,
    items: List<ActionItem>,
    color: androidx.compose.ui.graphics.Color,
    defaultExpanded: Boolean,
    onDismiss: (ActionItem) -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Column {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp)
        ) {
            Text(
                if (expanded) "▼" else "▶",
                color = color,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "$title (${items.size})",
                color = color,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                items.forEach { item ->
                    ActionItemRow(item = item, onDismiss = { onDismiss(item) })
                }
            }
        }
    }
}

@Composable
private fun ActionItemRow(item: ActionItem, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.Top,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = false,
            onCheckedChange = { isChecked -> if (isChecked) onDismiss() }
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(item.action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "[${item.source}] ${item.subject}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InboxTile(personal: OutlookData?, dboOps: OutlookData?) {
    TileCard(title = "Inbox") {
        if (personal != null) {
            Text("Personal SES: ${personal.unreadCount} unread")
        } else {
            NotLoaded("Personal SES — open Outlook tab once to populate.")
        }
        if (dboOps != null) {
            Text("DBO Ops: ${dboOps.unreadCount} unread")
        } else {
            NotLoaded("DBO Ops — open DBO Ops tab once to populate.")
        }
        if (personal != null && personal.recentConversations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Recent (Personal):", style = MaterialTheme.typography.labelMedium)
            personal.recentConversations.take(4).forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ActivitiesTile(data: MyAvailData?) {
    TileCard(title = "myAvailability — Activities") {
        if (data == null) {
            NotLoaded("Open myAvail tab once to populate.")
            return@TileCard
        }
        Text("Operational: ${data.operationalCount}")
        Text("Activity: ${data.activityCount}")
        Text("OOAA: ${data.ooaaCount}")
        Text("OOAA approvals pending: ${data.ooaaApprovalsCount}")
        if (data.activityTitles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Activities:", style = MaterialTheme.typography.labelMedium)
            data.activityTitles.take(5).forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (data.ooaaTitles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("OOAA:", style = MaterialTheme.typography.labelMedium)
            data.ooaaTitles.take(5).forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PlannerTile(data: PlannerData?) {
    TileCard(title = "Planner") {
        if (data == null) {
            NotLoaded("Open Planner tab once to populate.")
            return@TileCard
        }
        Text("${data.openTasks} open / ${data.totalTasks} total")
        if (data.openTitles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            data.openTitles.take(8).forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MessagesTile(data: MyAvailData?) {
    TileCard(title = "myAvailability — Messages") {
        if (data == null) {
            NotLoaded("Open myAvail tab once to populate.")
            return@TileCard
        }
        Text("${data.unreadChannels} channels with unread")
        Text("${data.totalUnread} unread messages total")
    }
}

// ---- Settings -------------------------------------------------------------

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(getApiKey(context)) }
    var savedConfirmation by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Anthropic API key",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Used to call Claude (Haiku 4.5) for the Action Items tile. Stored locally on this device only — never uploaded to any server other than api.anthropic.com.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; savedConfirmation = false },
                    placeholder = { Text("sk-ant-...") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            setApiKey(context, apiKey.trim())
                            savedConfirmation = true
                        }
                    ) {
                        Text(if (savedConfirmation) "Saved ✓" else "Save")
                    }
                    Button(
                        onClick = {
                            apiKey = ""
                            setApiKey(context, "")
                            savedConfirmation = true
                        }
                    ) {
                        Text("Clear")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Get a key at console.anthropic.com → Settings → API keys. Cost is roughly \$0.0005 per dashboard refresh — about a dollar per month with daily use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("SES Unit Dashboard — built for personal use by Carl Manning.", style = MaterialTheme.typography.bodySmall)
                Text("Aggregates myAvailability + Outlook (×2) + Planner via WebView profiles.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---- WebView --------------------------------------------------------------

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
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript(buildInterceptorJs(), null)
                    }

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

// ---- Injected JS (unchanged) ---------------------------------------------

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
                        try { window.__sesOwaResponses[act2] = JSON.parse(text); } catch (e) {}
                    }).catch(function() {});
                }).catch(function() {});
            }
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

        // Stream Chat uses axios which uses XMLHttpRequest, not fetch.
        var origOpen = XMLHttpRequest.prototype.open;
        var origSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.open = function(method, url) {
            this.__sesUrl = url;
            return origOpen.apply(this, arguments);
        };
        XMLHttpRequest.prototype.send = function() {
            var xhr = this;
            var url = xhr.__sesUrl || '';
            if (url.indexOf('stream-io-api.com/channels') !== -1) {
                captureUserId(url);
                xhr.addEventListener('load', function() {
                    try {
                        var data = JSON.parse(xhr.responseText);
                        window.__sesLastChannels = data;
                        if (window.__sesHasRefreshed && window.refreshSesData) {
                            window.refreshSesData();
                        }
                    } catch (e) {}
                });
            }
            return origSend.apply(this, arguments);
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
            var summary = { type: 'owa-dump', host: location.hostname, actionsCaptured: actions };
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
            if (!d) { postBridge({ type: 'planner-dump', note: 'No tasks data captured yet.' }); return; }
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
                var summary = { type: 'myAvailability-summary', time: new Date().toISOString().slice(11, 19) };
                results.forEach(function(pair) {
                    var key = pair[0]; var v = pair[1];
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
                    var totalUnread = 0; var unreadChannels = 0;
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
                summary.inbox = { note: 'No GetFolderChangeDigest captured yet.' };
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
            var summary = { type: 'planner-summary', host: location.hostname, time: new Date().toISOString().slice(11, 19) };
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
                fallback.note = 'No Planner Bearer token captured yet.';
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
                .catch(function(e) { postBridge({ error: 'Planner fetch failed: ' + e.message }); });
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