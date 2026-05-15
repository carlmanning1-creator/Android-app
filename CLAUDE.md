# CLAUDE.md — Project Context

## About Carl Manning
- **Role**: Deputy, NSW SES (State Emergency Service) — Dubbo Unit
- **Primary device**: Android phone
- **Google account**: Google One Premium with 5 TB storage plan (use this for any cloud storage/backend needs — no need for additional paid services)

## Projects

### 1. SES Unit Dashboard (`com.carlmanning.sesdashboard`)
Native Android app (Kotlin + Jetpack Compose) that aggregates:
- myAvailability (SES) — operational/activity/OOAA requests
- Outlook personal SES inbox
- Outlook DBO Ops shared mailbox
- Microsoft Planner tasks
Uses Claude Haiku via Anthropic API (key stored in SharedPreferences) to triage action items.
Data is extracted by injecting JavaScript into WebViews.

### 2. Second Brain / ADHD Memory System (planned — separate app)
See architecture below.

## Second Brain — Architecture Decision Record

### Core concept
A dedicated Android app as Carl's external memory and ADHD support tool. Claude is the intelligent layer on top — not just a feature, but the operating system of the whole thing.

### Tech stack
- **Platform**: Native Android (Kotlin + Jetpack Compose)
- **Auth + Storage**: Google Drive API (OAuth 2.0 — same Google account as Calendar)
- **Calendar**: Google Calendar API (read + write)
- **AI**: Anthropic Claude API (Haiku for quick ops, escalate to Sonnet for planning/analysis)
- **Voice**: Android SpeechRecognizer (fast, on-device) + optional Claude cleanup pass
- **Notifications**: Android WorkManager + NotificationManager

### Google Drive storage structure
```
/SecondBrain/
  memory.md              ← Claude's permanent context file (prepended to every Claude call)
  /notes/
    /ses/
    /family/
    /work/
    /personal/
    /kink/
    /other/
    /[user-created-buckets]/
  todos.json             ← structured: title, bucket, priority, due, done, created
  /audio/                ← optional voice recordings
```

### Life buckets (initial set)
SES, Family, Work, Personal, Kink, Other
User can create additional custom buckets and transfer items between buckets.
Sensitive buckets (e.g. Kink) should offer biometric/PIN access protection.

### Screens
1. **Dashboard** — morning digest, upcoming GCal events, priority todos, recent notes summary
2. **Quick Capture** — text field + voice button → Claude auto-tags and sorts into bucket
3. **Notes** — filterable by bucket, searchable, full markdown view
4. **Todos** — by bucket/priority/due date, checkable, Claude prioritisation on demand
5. **Chat** — full Claude conversation with `memory.md` as system context
6. **Calendar** — upcoming events, create events from notes/todos via natural language

### Claude integration scope
- Auto-sort new notes/todos into the correct bucket
- Transcribe + clean voice notes
- Summarise notes on demand
- Conversational chat interface (captures, retrieves, manages everything)
- Suggest solutions and prioritise tasks
- Build daily/weekly/life plans using calendar + todos + memory
- Update `memory.md` with important facts from interactions over time

### Reminders
- Android push notifications for time-sensitive todos/deadlines
- Morning digest notification (configurable time) with day summary
- In-app digest on open (today's priorities + upcoming events)

### Phase 2 (after core is complete)
- Quick capture home screen widget
- Dashboard home screen widget

### Cost estimate
- Google Drive API: Free (well within personal-use quota)
- Google Calendar API: Free
- Anthropic API: ~$0.25–$2/month with Haiku for most calls
- Total ongoing cost: API calls only, no server costs
