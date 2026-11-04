Auto Clicker Nega: Master Blueprint v2

One consolidated specification · 22 October 2026, revised 3 November 2026 · Android app (minSdk 30), static website and web Studio · Kotlin + Jetpack Compose (no Material components)

> **Status.** This is the single source of truth. It replaces the earlier draft and all of its appendices. **Revised 3 November 2026:** Google Play is the only distribution channel (no direct-APK route), minSdk is 30, a "Start in X" wait of up to 12 hours replaces scheduled start, account-safety rules are added, and gate T8 is new. No code has been generated and nothing has been pushed. Every value the owner has not supplied is marked **OWNER INPUT** and is never guessed. Nothing is deferred to a later version: every feature listed here is built before release.

## 1. Owner inputs still open

None of these block the blueprint. Items 1 to 4 must be supplied before any code is generated.

| # | Item | Needed by | Notes |
| --- | --- | --- | --- |
| 1 | **Repo URL** | Before B00 | OWNER INPUT. Empty public repo, no README, no licence, no ignore file. |
| 2 | **Keystore alias** | Before B00, used at B22 | OWNER INPUT. Passwords are held by the owner and entered as repo secrets. They are never written into this document or the repo. |
| 3 | **Push route** | Before B00 | OWNER INPUT. GitHub connector on for the chat and sandbox network on, so the agent can push and read Actions logs. |
| 4 | **GitHub token** | When item 3 is ready | OWNER INPUT. Scopes: repo and workflow. Git author name and email come from the token's account. |
| 5 | **AdMob IDs:** app ID plus banner, rewarded and interstitial unit IDs | Before the release build | OWNER INPUT. Debug and test APKs use Google's sample IDs, so nothing is needed before then. |
| 6 | Play Console app entry and the in-app product `com.zdmgold.autoclickernega.pro` | Before the first store upload | OWNER INPUT. The Play account name was left blank by the owner. |
| 7 | Cloudflare Pages: two projects (site, studio) and their URLs | After B20 (site) and W04 (studio) | The owner connects each repo folder after its files exist, then supplies the URLs. |
| 8 | Inter font files | B01 | The agent fetches them if the sandbox has network. Otherwise the owner uploads four TTF files. |
| 9 | **Klick'r's Pro price** | Before B17 | The owner opens Klick'r's Pro purchase screen on a phone and reports the price. It sets ours. Default is $4.99. |
| 10 | Klick'r behaviour with no internet and with consent declined | Before B17 | Optional but useful. Airplane mode, start a scenario, note whether it starts, blocks or offers Pro. |
| 11 | Legal review of the declined-consent run cap (section 8.4) | Before release | OWNER INPUT. Tying free use to ad consent is a grey area, and this document is not legal advice. |

**Decisions already made (not open):** Kotlin + Compose. `zh` is Simplified Chinese. **Distribution is Google Play only. There is no direct-APK channel and no web checkout or license-key route.** **minSdk is 30 (Android 11+).** Capture uses the accessibility screenshot API only. A timed start is the **Start in X** wait, up to 12 hours. True clock-time scheduling is not built. Test-APK checkpoints are after B12 (engine and detection proof) and after B18 (finished UI), built from the debug workflow for the owner's own phone only. The launcher label is the full name. The agent proposes the icon concept at B19. The matcher is pure Kotlin first, and OpenCV is adopted only if the Detection lab shows it misses the latency target. The OCR engine is chosen by a lab bake-off. OCR language packs download on first use through Play Asset Delivery. Monetization is a rewarded video before each run, with Pro as a one-time purchase. Android intents and broadcast triggers are not built, by product decision. The web Studio is built after the app.

## 2. Product identity and tracking table

This table is updated each time an external identifier is created.

| Field | Value | Status |
| --- | --- | --- |
| Product name | Auto Clicker Nega | ✅ Chosen by owner. An exact-phrase web search found no existing app with this name. Play Store and trademark checks (WIPO, EUIPO, classes 9 and 42) are the owner's. |
| Brand wordmark in UI | 'Nega', shown once per screen | ✅ |
| Application ID | `com.zdmgold.autoclickernega` | ✅ Immutable after first release |
| Debug application ID | `com.zdmgold.autoclickernega.debug` | ✅ Installs beside release. The accessibility service is enabled separately for each. |
| Category | Tools | ✅ |
| Platforms | Android 11+ (minSdk 30), static website, web Studio. iOS is not feasible. | ✅ |
| Distribution | Google Play only. The manual release workflow builds a signed AAB. No direct-download channel. | ✅ |
| targetSdk and compileSdk | The level Google Play requires at B00, read from Play documentation and recorded in `libs.versions.toml` | ⬜ |
| Notifications | None. | ✅ |
| Backend | None. Offline-first. Static files only. | ✅ |
| Monetization | Free. Banner on Home. A rewarded video before each run. One-time Pro removes every ad. | ✅ |
| One-time product ID | `com.zdmgold.autoclickernega.pro` | ⬜ Created by owner in Play Console |
| Support email | stmakarios@gmail.com | ✅ |
| Static hosting | Cloudflare Pages, two projects from one repo: `website/` and `studio/`, each with its own build watch path. The website does not host language-pack files. | ⬜ Owner connects |
| Repository | OWNER INPUT. Public. Branches: `main` (always green) and `ci-analyser` (batches land here first). | ⬜ |
| Privacy and support URLs | Produced by B20 | ⬜ |
| Keystore alias and secrets | OWNER INPUT | ⬜ |
| AdMob IDs | OWNER INPUT | ⬜ |

## 3. Product definition and positioning

**What it is.** Auto Clicker Nega is an Android automation tool, with no root. It can tap, swipe and wait on a schedule, and it can watch the screen and act when it sees an image, text or colour you told it to find. A floating panel runs it over any app.

### 3.1 The pain points it is built around

| Pain in this category | What Nega builds |
| --- | --- |
| Scripts that tap fixed coordinates break whenever the UI shifts | Image, text, colour and number conditions are the primary mode. Coordinates remain as a fallback. |
| Ads interrupt the work, and some apps ask paying users to watch ads again after an update | No ad in the editor or the floating panel. The only ad is the run gate, which the user can skip with Pro. Purchase restore is hardened (section 8.6). |
| Hard to learn. Competitors lean on tutorials. | A Quick Scenario wizard, a Simple mode and a Smart mode, and a live detection HUD |
| Silent failure: users cannot tell why it did not act | The HUD shows the live match score and the reason for a miss. A shareable diagnostic report exists. |
| Battery drain | Adaptive search rate, search regions and a visible rate control |
| Heavy capture path with a foreground service and notification | None of that. Capture uses the accessibility screenshot API. |
| The service or its permission dies in the background | One-tap recovery and generic keep-alive guidance |
| Some games block injected taps | An honest-limits screen, never a silent failure |

### 3.2 Position against the closest competitor, Klick'r (Smart AutoClicker)

Klick'r facts below come from its Play listing mirror, F-Droid page, wiki and release notes. Klick'r is open source (GPL), actively maintained, and on its Play listing asks for a short ad before a scenario starts. Pro is a one-time purchase.

| Capability | Klick'r | Nega |
| --- | --- | --- |
| Image detection | Yes | Yes |
| Text detection (OCR) | Yes | Yes, with downloadable language packs including Arabic and Hebrew |
| Multiple conditions, AND/OR | Yes | Yes, up to 3 per event |
| Colour and number conditions | Yes (4.0 betas) | Yes |
| Counters and timer conditions | Yes | Yes |
| Event model with priorities | Yes | Yes (Smart mode) |
| Chaining scenarios | Yes | Yes (Run script action with a loop guard) |
| Android intents and broadcast triggers | Yes | **No**, by decision |
| Multi-finger and curve gestures | Not stated in what I found | Yes |
| Record and replay | Not stated | Yes (Record mode) |
| Screen capture | Foreground service plus notification | None. Uses the accessibility screenshot API. |
| Delayed start | Not stated in what I found | **Start in X**, up to 12 hours, with the screen kept on. Some competitors advertise clock-time scheduled start. Nega does not offer it. |
| Learning curve | Interactive tutorials | Quick Scenario wizard and live HUD |
| Ad at run start | Yes on Play, until Pro | A rewarded video per run, five free runs first, a 2-minute restart window |
| Open source and community | Yes | No |
| Maturity | 100K+ installs and years of fixes | New and unproven |

### 3.3 Honest weaknesses

Reliability is unproven until the acceptance gates in section 12 pass on a real phone. Klick'r has an established audience and may offer an ad-free build on F-Droid, so Nega competes on ease, speed to a first working automation, and reliability, not on price. Trust is earned through behaviour: honest limits and no surprise ads.

## 4. Platforms and feasibility

| Target | Verdict |
| --- | --- |
| **Android app** | Yes. `AccessibilityService.dispatchGesture` simulates taps and swipes over other apps without root. The app targets Android 11 and later (minSdk 30). |
| **iOS app** | No. iOS has no API for one app to inject touches into another. A fake version would be rejected as minimum-functionality and misleading. |
| **Website (static)** | Yes. Landing, privacy, support and accessibility-disclosure pages. |
| **Web Studio** | Yes, as an authoring tool. A browser cannot control the phone or any other app, so Studio never runs scripts. It builds, edits, tests and exports them. Detail in section 15. |

## 5. Complete feature set

**Gestures**

- Tap (with hold time and optional jitter), swipe, curve swipe (3 to 5 placed points), multi-finger strokes (2 to 3 simultaneous), wait
- Per-action interval, optional position jitter (pixels) and timing jitter (ms)

**Targeting and conditions**

- Coordinates, with rescaling when the screen size or density differs
- **Image:** a reference crop captured from the live screen. Confidence slider (default 0.85), search region, Safe or Fast match mode. Safe needs the same match on two consecutive frames and is the default.
- **Text (OCR):** contains or equals, with optional case sensitivity, in a search region
- **Colour:** a colour at a point or region, within a tolerance
- **Number:** a number read from a region, compared with a value
- **Timer:** time since the run began, or since an event last fired
- **Counter:** compare a counter with a value
- Every condition can be negated ('not found')

**Script models**

- **Simple mode:** an ordered list of actions with loops, a global timer, a start delay and a countdown. This is the classic auto clicker.
- **Start in X:** the start delay can be set from 1 minute to 12 hours. It is shown as a live countdown in the panel with a Cancel button, and the screen is kept on while waiting. The run gate decision happens when the user presses Start, before the wait. See section 7.13.
- **Smart mode:** a list of events. Each event has up to 3 conditions combined with AND or OR, a list of actions, a priority and an optional cooldown.
- Actions: tap, swipe, curve, multi-finger, wait, change a counter, **run another script** (with a loop guard), stop the run
- Counters: named, with a starting value, set, add and subtract

**Authoring**

- **Quick Scenario wizard:** 'Tap when this appears', 'Tap when this text appears' or 'Tap every N milliseconds'. Crop the target on the live screen and it is done.
- Build-in-place markers: numbered, draggable on the live screen. Circles are taps, arrows are swipes and curves, corner brackets are image targets.
- **Crop tool** to capture reference images and set search regions
- **Record mode:** a transparent layer logs your taps, swipes and long presses and turns them into steps. See section 7.9.
- Step and event editors with validation

**Floating panel and live feedback**

- Start, pause, stop, add tap, swipe, curve and image, collapse and close. Draggable, adjustable opacity, remembered position.
- **Detection HUD:** current score, the reason for a miss, and the active event
- Timer and counters shown live
- The panel and markers never block the target app during a run

**Capture**

- The accessibility screenshot API, with no notification and no consent dialog
- Frames are processed in memory and never stored or sent
- A secure screen produces a clear 'Can't see this screen' state and never a tap

**Text detection language packs**

- First use says 'Text detection needs a one-time language pack', with a choice of Wi-Fi or mobile data. No size is shown. After that it works offline.
- A missing pack shows 'Receive language pack'. Image detection is unaffected.
- Packs come through Play Asset Delivery. Every pack is checked against a hash built into the app.

**Scripts and sharing**

- A local library: create, rename, duplicate, delete with undo, search and sort
- Import and export through the file picker, the share sheet or a paste box. Plain JSON for coordinate-only scripts. A `.nega` bundle for scripts with images.
- Imports are validated and size-capped. Sharing warns that crops may show private on-screen content.

**Run gate and monetization (section 8)**

- A rewarded video before each run, five free runs first, a 2-minute restart window, a fallback ladder for no-fill, a run cap for users who decline consent
- A banner on Home only. A one-time Pro removes every ad.
- No ad in the editor, the overlay, the wizard or the Detection lab

**Product layer**

- Guided onboarding with the prominent accessibility disclosure, the settings deep link and live state detection
- One-tap recovery when the OS switches the service off
- Light, dark and system themes. 11 languages, RTL-safe, with an in-app language picker.
- Rating prompt after 3 successful runs, once. Feedback by email. Legal pages.
- **Delete all local data.** A shareable diagnostic report with no script content or pixels.
- **Detection lab (debug builds only):** capture, crop, run the matcher at chosen confidences, see score, box and timing, run the repeat test, compare OCR engines, export a report

**Not built, by product decision:** Android intents and broadcast triggers, clock-time scheduled start and background alarms, cloud sync, accounts, iOS, community script store, per-app profiles.

## 6. Capability matrix and permissions

Nothing is built unless a row below says so, and every permission traces to a row.

| Capability | Layer | Cost / network |
| --- | --- | --- |
| Tap, swipe, curve, multi-finger gestures | `service/engine` using `dispatchGesture` | On-device |
| Simple and Smart script engines | `core/run`, `service/engine` | On-device |
| Start in X wait | `core/run`, `service/engine`, `service/overlay` (keep-screen-on flag on the panel window) | On-device |
| Image, colour and number conditions | `core/vision`, `core/run` | On-device |
| Text detection with downloadable packs | `core/ocr`, `platform/ocr` | One-time data-file download |
| Screen capture | `service/capture` (accessibility screenshot API) | On-device |
| Floating panel, markers, crop tool, HUD | `service/overlay` using plain Views and `TYPE_ACCESSIBILITY_OVERLAY` | On-device |
| Record mode | `service/record` | On-device |
| Scripts, import and export | `core/script`, `data/` | On-device |
| Run gate, banner, rewarded video, interstitial | `core/reward`, `platform/ads` | Network |
| Pro purchase and restore | `platform/billing` | Store fee |
| Theme, localization (11 languages) | `ui/theme`, resources | On-device |
| Rating prompt, diagnostics, Detection lab (debug only) | `platform/review`, `platform/diag` | On-device |
| Web Studio | `studio/` (separate static project) | Static hosting |
| Not built | Android intents and broadcasts, clock-time scheduling, exact alarms, sync, accounts, backend, retention notifications, camera, microphone | None |

**Manifest permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`, `com.google.android.gms.permission.AD_ID`, and billing (added by the library). The accessibility service is declared with `BIND_ACCESSIBILITY_SERVICE`, `canPerformGestures` and `canTakeScreenshot`. There is **no** foreground-service permission, `SYSTEM_ALERT_WINDOW`, storage, camera, microphone, exact-alarm or `POST_NOTIFICATIONS` permission.

## 7. Architecture

### 7.1 Layers (single Gradle module `:app`, enforced as packages)

```
com.zdmgold.autoclickernega
├── core/      pure Kotlin, no android.* imports
│   ├── model/    Action, Condition, Event, Script, RunConfig, EngineState, Entitlement, ConsentState
│   ├── script/   codec, validator, migrator, scaler, .nega bundle, planner, quick-scenario factory, record-to-steps
│   ├── run/      condition evaluator, event scheduler, counters, cadence controller, loop guard, jitter
│   ├── vision/   frame, pyramid, NCC matcher, colour matcher, stabiliser, frame diff, detection log
│   ├── ocr/      text matcher, number parser, pack spec models
│   ├── reward/   run gate, run ledger, gate policy constants
│   ├── consent/  consent policy      billing/ entitlement policy
│   ├── port/     interfaces: stores, clock, frame source, gesture port, recogniser, ad, consent, billing, review
│   └── error/ result/ format/
├── data/      Room, DataStore, template files, import/export, ledger store, pack files
├── platform/  Android services behind ports: ads+UMP, billing, review, locale, accessibility state, diagnostics, OCR engine and pack manager
├── service/   the engine inside the AccessibilityService: runners, dispatcher, screenshot capture, overlay, record layer, bridge
├── ui/        Compose: theme, icons, components, screens, navigation, view models
└── (root)     NegaApp, MainActivity, AppGraph (manual DI, no Hilt)
```

`LayeringTest` fails the build if `core/` imports `android.*`, or if `ui/` reaches into `service/` other than through `EngineBridge`.

### 7.2 State

`ViewModel` with `StateFlow` and `collectAsStateWithLifecycle`. One ViewModel per screen, plus `AppViewModel` for cross-screen state: theme, entitlement, consent, locale. No business state in `remember`. Models are immutable data classes.

### 7.3 Script model (schema 1, versioned JSON)

Nothing has shipped, so the first schema is 1. `ScriptMigrator` exists from day one for future versions. A script is either **Simple** (a list of actions) or **Smart** (a list of events).

```kotlin
// core/model (excerpt)
sealed interface Action {
    data class Tap(val at: Target, val holdMs: Long = 40, val jitterPx: Int = 0, val intervalMs: Long = 100) : Action
    data class Swipe(val from: Point, val to: Point, val durationMs: Long, val intervalMs: Long) : Action
    data class Curve(val points: List<Point>, val durationMs: Long, val intervalMs: Long) : Action      // 3 to 5 points
    data class Multi(val strokes: List<Stroke>, val durationMs: Long, val intervalMs: Long) : Action    // 2 to 3 strokes
    data class Wait(val ms: Long) : Action
    data class CounterOp(val id: String, val op: CounterOpKind, val value: Int) : Action
    data class RunScript(val scriptId: String) : Action
    data object Stop : Action
}
sealed interface Target { data class At(val p: Point) : Target; data class AtMatch(val offset: Point) : Target }
sealed interface Condition { val negate: Boolean }   // Image, Text, Color, Number, Timer, Counter
data class Expr(val op: BoolOp, val items: List<Condition>)           // 1 to 3 items
data class Event(val id: String, val priority: Int, val cooldownMs: Long, val whenExpr: Expr, val actions: List<Action>)
```

```json
{
  "schema": 1, "name": "Collect reward", "mode": "smart",
  "screen": { "w": 1080, "h": 2400, "density": 420 },
  "run": { "globalTimerMs": 0, "startDelayMs": 3000, "countdownSec": 3 },
  "templates": { "t1": { "file": "t1.png", "w": 96, "h": 64, "refDensity": 420 } },
  "counters": [ { "id": "c1", "name": "Collected", "start": 0 } ],
  "events": [
    { "id": "e1", "name": "Reward button", "priority": 1, "cooldownMs": 500,
      "when": { "op": "AND", "items": [
        { "type": "image", "template": "t1", "confidence": 0.85, "mode": "safe", "region": [0, 1600, 1080, 2400] },
        { "type": "counter", "id": "c1", "cmp": "<", "value": 50 } ] },
      "do": [ { "type": "tap", "at": "match", "offset": [0, 0], "holdMs": 40 },
              { "type": "counter", "id": "c1", "op": "add", "value": 1 } ] },
    { "id": "e2", "name": "Done", "priority": 2,
      "when": { "op": "AND", "items": [ { "type": "counter", "id": "c1", "cmp": ">=", "value": 50 } ] },
      "do": [ { "type": "stop" } ] }
  ]
}
```

```json
{ "schema": 1, "name": "Tap loop", "mode": "simple",
  "screen": { "w": 1080, "h": 2400, "density": 420 },
  "run": { "loops": 0, "globalTimerMs": 600000, "startDelayMs": 3000, "countdownSec": 3 },
  "steps": [
    { "type": "tap", "x": 540, "y": 1200, "holdMs": 40, "intervalMs": 100, "jitterPx": 3 },
    { "type": "curve", "points": [[540,1800],[620,1400],[540,900]], "durationMs": 400, "intervalMs": 500 },
    { "type": "multi", "strokes": [ { "from": [400,1200], "to": [300,1200] }, { "from": [680,1200], "to": [780,1200] } ],
      "durationMs": 300, "intervalMs": 500 },
    { "type": "wait", "ms": 1000 } ] }
```

**Validation rules.** `loops: 0` is infinite. Up to 3 conditions per event. `at: match` is valid only in an event that has a positive image or text condition. A script that runs other scripts is checked for cycles and a depth of at most 3. `startDelayMs` is clamped to 0 through 43,200,000 (12 hours), which is the Start in X range. Limits: 256 KB of JSON, 500 steps or events, 40 templates. Ranges are clamped, and an unknown newer schema is rejected with a clear message.

**The `.nega` bundle** is a zip: `script.json`, `manifest.json` (schema, app version, SHA-256 of each template) and `templates/<id>.png`. The cap is 4 MB. Import verifies every hash. Studio reads and writes the same format. Golden fixtures in `contract/` are used by both the Kotlin tests and the Studio tests, so the two codebases cannot drift apart.

### 7.4 The engine

The engine lives inside `NegaAccessibilityService`, in the same process as the app. `EngineBridge` is an in-process singleton exposing a `StateFlow<EngineState>`, an event flow and command functions. The UI never drives individual taps, so UI jank cannot cause missed intervals. Two runners share one dispatcher and one capture pipeline.

```kotlin
// service/engine/SequenceRunner.kt (Simple mode, monotonic clock, no drift)
var nextAt = clock.nowMs() + startDelayMs
for (step in plan) {
    delayUntil(nextAt)
    dispatcher.dispatch(step)                 // suspends until completed or cancelled
    nextAt += step.intervalMs                 // start-to-start
    if (clock.nowMs() > nextAt + step.intervalMs) nextAt = clock.nowMs()   // never burst to catch up
}

// service/engine/EventRunner.kt (Smart mode, one pass per captured frame)
suspend fun onFrame(frame: Frame, now: Long) {
    val event = events.sortedBy { it.priority }
        .firstOrNull { it.enabled && now >= nextAllowed[it.id] && evaluator.passes(it.whenExpr, frame, ctx) }
    event?.let { execute(it.actions, anchor = ctx.lastMatch); nextAllowed[it.id] = now + it.cooldownMs }
}
```

**Semantics**

- Smart mode evaluates events by priority on each captured frame and runs the **first** event whose conditions pass. Others wait for the next frame. Per-event cooldowns prevent a fired event from re-firing on the same screen.
- Conditions are evaluated cheapest first with short-circuiting: counter and timer, then colour, then image, then text.
- Counters are per run. Each starts at its declared value.
- Interval means start-to-start. The floor is **50 ms** by default. Tap hold defaults to 40 ms. Gesture limits come from the platform's maximum duration and stroke count and are clamped.
- **Stop conditions:** user stop, global timer, loops finished (Simple), a `Stop` action, screen off, service unbind, the run cap in section 8, or 10 consecutive rejected gestures (state `Blocked`, with a message that some apps and secure screens refuse injected gestures). A Start in X wait is cancelled by the user's Cancel, screen off or service unbind (section 7.13).
- A `RunScript` action runs the child script under the same run, with no second gate. A guard enforces depth and cycles.

### 7.5 Detection pipeline and cadence

`DetectionPipeline` pulls frames from a `FrameSource`, converts them to grayscale, runs the evaluator and writes a `DetectionLog`. A `CadenceController` sets the search interval.

- Base interval is the larger of the OS capture limit and 200 ms.
- If five consecutive frames are essentially unchanged (`FrameDiff`), the interval backs off to 500 ms, then 1 s. Any change, or any action, resets it. A visible control offers Auto, Fast and Battery saver.
- Text recognition runs at most every 300 ms per region, and only for events that are otherwise eligible.
- Search regions are honoured everywhere, so the matcher scans only what it must.
- **False-tap defences:** a tap on a match needs the score to meet the confidence. **Safe** mode needs the same match, within 12 px, on two consecutive frames. Our own panel and markers are hidden or masked during capture. A capture failure never causes a tap. No match within a timeout leads to the next event or a visible stop, never a blind tap.
- **Evidence:** every detection logs the frame time, score, box, event, tap coordinates and outcome (completed, cancelled, rejected). The log never contains pixels. It feeds the HUD, the Detection lab and the diagnostic report.

### 7.6 Vision

Pure Kotlin: grayscale normalized cross-correlation on a downscaled image pyramid inside the search region, refined at full resolution. Templates are rescaled by the density ratio between the capture device and the reference device. A colour matcher compares a point or region within a tolerance. `NccMatcher` sits behind an interface. If the Detection lab shows it misses the latency target on the reference device, OpenCV replaces it without touching the runners, and its size cost is then accepted.

### 7.7 Text detection and language packs

```kotlin
interface TextRecognizer { suspend fun read(frame: Frame, region: Rect, lang: String): Outcome<List<TextBlock>> }
```

- **Engine:** a Tesseract-class engine with one downloadable pack per language, covering Arabic and Hebrew. Google's on-device text model, delivered through Play services, is the rival. The Detection lab compares both on short game-style text, and the winner is chosen on results. The choice is hidden behind `TextRecognizer`.
- **Number conditions** read text and parse digits through `NumberParser`, which normalises separators and locale digits.
- **First use:** the app says **'Text detection needs a one-time language pack'**, with a choice of Wi-Fi or mobile data. No size is shown. After the pack arrives it works offline.
- **Failure state:** a missing pack shows **'Receive language pack'**. Image and colour detection are unaffected.
- **Delivery:** packs come through Play Asset Delivery, with no server. Every pack is verified against a hash built into the app.
- **Privacy:** the download fetches a data file and sends no user data. The packs are data files, not executable code. The privacy policy says so.

### 7.8 Overlay, panel, markers, crop and HUD

- All windows are added through the service with `TYPE_ACCESSIBILITY_OVERLAY`, so `SYSTEM_ALERT_WINDOW` is not needed. They are plain Views, not Compose, to stay light on cheap devices.
- The **panel** window is small and touchable: start/pause, stop, add tap, swipe, curve and image, collapse, close, a timer, counters and the **HUD** strip. It is draggable, with adjustable opacity. During a Start in X wait it also shows the countdown and a Cancel button, and carries the keep-screen-on window flag only while the wait is active.
- The **marker** window is full-screen. Markers are numbered. It is `FLAG_NOT_TOUCHABLE` during runs, and markers are hidden during runs by default.
- The **crop** window dims the screen and lets the user drag a rectangle. The crop is taken from the latest frame and saved through `TemplateStore`.
- Before matching, the panel's rectangle is masked in the frame, so the panel cannot match itself.
- All colours and sizes come from the shared tokens through `OverlayTheme`.

### 7.9 Record mode

A transparent layer logs the user's taps, swipes and long presses, with timing, and turns them into steps. A layer cannot observe touches and also pass them through untouched, so the layer **consumes** the gesture, records it, then **re-dispatches** it through the accessibility gesture API so the target app still receives it. A paused recording can be edited into a Simple script.

**Risk, stated plainly:** how this feels on real devices (added latency, drags that follow the finger) is unproven. Gate **T6** in section 12 decides whether Record mode ships on by default.

### 7.10 Quick Scenario wizard

Three choices: 'Tap when this appears', 'Tap when this text appears' and 'Tap every N milliseconds'. The first two open the panel over the target app, let the user crop the target, and call `QuickScenarioFactory`, which builds a one-event Smart script (condition plus a tap at the match). The third builds a Simple script. The whole flow targets a working automation in under a minute.

### 7.11 Accessibility service configuration

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:canPerformGestures="true"
    android:canTakeScreenshot="true"
    android:canRetrieveWindowContent="false"
    android:description="@string/service_description"
    android:accessibilityFeedbackType="feedbackGeneric" />
```

`canTakeScreenshot` lets the service capture pixels for image targeting. `canRetrieveWindowContent` stays **false**, so the service never reads text, view trees or other apps' content. No event types are requested. `isAccessibilityTool` is **not** set, because it is reserved for disability-assistance tools. The Play permission declaration and demo video describe both gesture and screenshot use.

### 7.12 Honest limits (a first-class Help screen)

- The fastest reliable interval depends on the device. The default floor is 50 ms.
- Some apps block injected gestures. On secure screens (banking, DRM) capture returns an error, so detection shows 'Can't see this screen' and never taps.
- The OS rate-limits screenshots, so detection cadence has a ceiling, measured per device in the Detection lab.
- Image matching handles moderate scale and density change. It does not handle rotation, heavy animation or changed artwork. A low score is reported, never guessed.
- Some manufacturers stop accessibility services in the background. Help gives generic keep-alive guidance, and one-tap recovery detects the loss and re-enables.
- Rotation changes coordinates. Scripts record the screen size and are rescaled, with a warning if the orientation differs.
- **Start in X needs the screen on.** The panel keeps the screen awake while waiting, which drains the battery. The app warns above 30 minutes to plug in. If the phone's power manager stops the service during the wait, the wait is cancelled and one-tap recovery is offered.

### 7.13 Start in X wait

- Range 1 minute to 12 hours. The cap is a constant `MaxStartWaitMs = 43_200_000` in `GatePolicy`, so it can be lowered after gate T8.
- The wait runs inside `RunSession` in the accessibility service. The panel window carries the keep-screen-on flag only while a wait is active.
- The panel shows a live countdown and a Cancel button. Above 30 minutes, the start sheet shows: 'Plug in your phone. The screen stays on while waiting.'
- Presets: 1, 5, 15 and 30 minutes, and 1, 2, 4, 8 and 12 hours, plus a custom entry up to the cap.
- The run gate (section 8.2) is decided when the user presses Start, before the wait begins. The wait is part of the same run and is not gated again.
- Cancelled by the user's Cancel, screen off or service loss. The message says why.

## 8. Monetization and the run gate

### 8.1 Principles

- **Every feature is free and unlimited for everyone:** scripts, images, events, conditions, loops, text detection, record mode.
- The only ads are: a **banner on Home**, a **rewarded video before a run**, and an **interstitial used only as a no-fill fallback at run start**. There is **no ad** in the editor, the floating panel, the wizard or the Detection lab.
- **Pro** is a one-time purchase that removes every ad and every gate.
- Every ad request goes through one consent gate (section 8.5).

### 8.2 The run gate, exactly

The gate is checked only at the moment the user presses Start. Editing, saving, importing, exporting and the Detection lab are never gated. A run that has started is never interrupted by an ad, and a run lasts as long as its script says, including overnight. A Start in X wait (section 7.13) belongs to the run that was gated when Start was pressed, and is not gated again.

| Situation when the user presses Start | What happens |
| --- | --- |
| Pro | The run starts. No ad. |
| One of the first 5 runs ever | The run starts. No ad, so the product proves itself first. |
| The same script is restarted within 2 minutes of its stop | The run starts. No new video. |
| Pause and resume | Not a new run, so no gate |
| Consent allows ads and a rewarded video is ready | A sheet: 'Watch a short video to start this run', beside 'Get Pro'. After the reward the run starts. |
| Consent allows ads, but no rewarded video loads after 2 tries | The app tries a full-screen interstitial. If it loads and is shown, the run starts. |
| Nothing loads | A **fallback run**, capped at 15 minutes, up to 3 per day. After that: 'No ads available right now. Try again later or get Pro.' |
| **Consent declined** (EEA, UK, Switzerland, or by choice) | No ad of any kind can be requested. **Capped runs: 3 per day, each up to 15 minutes.** The sheet offers Pro, or opens Privacy choices. |

```kotlin
// core/reward/RunGate.kt (pure, unit-tested)
sealed interface GateDecision {
    data object StartFree : GateDecision                   // Pro, first 5 runs, restart window, pause/resume
    data object NeedRewarded : GateDecision
    data object TryInterstitial : GateDecision
    data class StartCapped(val capMs: Long, val reason: CapReason) : GateDecision   // fallback or declined
    data class Blocked(val reason: BlockReason) : GateDecision
}
fun decide(e: Entitlement, c: ConsentState, ads: AdAvailability, l: RunLedger, now: Long, script: String): GateDecision
```

All numbers (5 free runs, 2-minute window, 15-minute cap, 3 per day, 2 load tries, and the 12-hour `MaxStartWaitMs`) are constants in `GatePolicy`.

### 8.3 The run ledger

`RunLedger` lives in DataStore: runs started, free runs left, fallback and capped runs today (with the local date), the last stop (script and time), and the last clock reading seen. A clock moved backwards never resets a daily counter. Capped runs are enforced by `RunSession`, which stops cleanly at the cap with a message and the Pro offer.

### 8.4 Declined consent and no-fill

- **Declined consent:** a developer reply in an ad-consent library discussion states that Google serves no ads at all if a user declined consent, in any format. So there is nothing to show instead, and the only levers are a limit or Pro. The capped runs above are that limit.
- **No-fill** means the network has no ad for that request. Fill varies by format and region, which is why the ladder tries a rewarded video, then an interstitial, then a short fallback run, so a no-fill market is never locked out.
- **Legal note:** tying free use to ad consent is a grey area. A real paid alternative (Pro) is what makes the design defensible. This document is not legal advice, and the owner should have the cap reviewed before release (section 1, item 11).
- **No ads over other apps:** AdMob's banner rules say ads should not float or hover over content. The floating panel sits over another app's screen, so no ad is ever placed in it.

### 8.5 Consent (Google UMP, one `ConsentManager`, one gated `AdService`)

1. On every launch call `requestConsentInfoUpdate`, then `loadAndShowConsentFormIfRequired`. UMP decides the region. The form appears only where required.
2. No ad SDK initialisation and no ad request, of any format, until `canRequestAds()` is true.
3. A failed or offline refresh never overrides a saved permit. If the refresh errors, use `canRequestAds()` or the last persisted result, whichever allows. Only an explicit decline, a withdrawal or no prior answer disables ads. A successful refresh overwrites the cache.
4. Settings shows **Privacy choices** when UMP reports it is required.
5. An ad-SDK failure never crashes or blocks. It collapses the slot or moves down the ladder, and logs locally.

**Testing.** Debug builds use Google's sample IDs and `DebugGeography` with a registered test device hash. A debug-only Diagnostics panel shows the consent state, a reset button and the device hash, because a phone-only developer cannot read logcat. Production ad unit IDs are used only on a registered test device. Release QA covers the EEA, UK, Switzerland and one unregulated region, each online, offline with consent saved, and offline with no consent. The consent message itself is created in the AdMob console by the owner.

### 8.6 Pro: purchase, restore, price

- Product `com.zdmgold.autoclickernega.pro`, one-time. **Default price $4.99**, adjusted after the owner reports Klick'r's Pro price. Play's per-country pricing adapts it locally. For reference, one-time unlocks on competitor iOS listings ranged from about $4.99 to $49.99.
- Google Play Billing handles purchase, restore, acknowledgement and pending purchases. A pending purchase unlocks nothing until Play confirms it.
- **Restore must be reliable.** Some competitor users report being asked to watch ads or buy again after an update. So: purchases are re-queried on every launch and when the app resumes after 24 hours. The last verified Pro state is cached with a timestamp. **A failed or offline check never downgrades Pro.** Only a successful check that returns no purchase, after one retry, clears it, which also covers refunds. 'Restore purchase' is always visible in Settings.
- Wording in the listing and the app promises only what the build does.

### 8.7 Banner slot spec

An anchored adaptive banner on **Home only**. Reserved height is at most 90 dp, with a 16 dp gap above and 0 below (safe area). Nothing is drawn until an ad has loaded. The whole slot (gap, border, background, height) collapses to zero when consent is absent, Pro is active, the load fails, the keyboard is visible, or the screen is not Home.

### 8.8 Revenue, honestly

Rewarded video pays more per view than a banner, and an ad per run in a market of heavy users is the main income. Rates are much lower in lower-income markets, so volume and Pro conversion carry the model. Third-party revenue estimates for this category are rough. The model is built to be fair first and profitable at scale, not to guarantee a figure.

## 9. Design system: iOS-like premium, neither Material nor Cupertino

The feel is iOS-like and built from our own tokens: grouped inset lists, large titles, hairline borders, flat tinted surfaces, opacity-only press feedback and soft motion with no bounce. Tokens are frozen in B01, before any widget exists. They live in `ui/theme/Tokens.kt`, and `service/overlay/OverlayTheme.kt` reads the same constants so the floating panel matches the app. A value that is not in this section does not appear in code.

### 9.1 Colour tokens (light is the default; dark is first-class)

| Token | Light | Dark | Usage |
| --- | --- | --- | --- |
| `bgPrimary` | #F5F6F8 | #0B0D10 | Scaffold |
| `bgSecondary` | #FFFFFF | #15181D | Cards, sheets, dialogs |
| `bgTertiary` | #ECEEF2 | #1E2229 | Inputs, empty and disabled states |
| `accent` | #12C79C | #2EE6B6 | Primary fills, active run state |
| `accentDim` | #0FA683 | #1FB892 | Pressed fills |
| `accentText` | #007A62 | #2EE6B6 | Accent as text or icon on surfaces |
| `onAccent` | #052B23 | #052B23 | Label on accent fills |
| `textPrimary` | #0E1116 | #F2F4F7 | Headlines, body |
| `textSecondary` | #4A5260 | #A4ABB6 | Subtitles |
| `textTertiary` | #636B78 | #8A929E | Placeholders, timestamps |
| `borderSubtle` | #E1E4EA | #262B33 | Card hairlines, dividers |
| `borderFocus` | #007A62 | #2EE6B6 | Active input border |
| `error` | #D92D20 | #FF6B61 | Validation, destructive actions |
| `success` | #1A7F4B | #4ADE9A | Saved, completed |
| `warning` | #B45309 | #F5B04A | Attention needed |
| `markerTap` | #12C79C | #2EE6B6 | Tap marker (circle) |
| `markerSwipe` | #4F6DF5 | #8FA8FF | Swipe and curve marker (arrow) |
| `markerImage` | #9B45D6 | #D49BFF | Image target marker (corner brackets) |
| `scrim` | #000000 at 40% | #000000 at 60% | Sheet and dialog backdrop |

Markers differ by **shape and number**, never colour alone. `ContrastTest` (B01) computes WCAG contrast for every text-on-surface pair and fails the build below 4.5:1, forcing any failing token to be adjusted before it ships.

### 9.2 Typography, spacing, shape, motion

One bundled family, **Inter** (open licence, four static weights 400 to 700), plus the system monospace. Arabic, Hebrew, Devanagari and CJK fall back to the system font. SF Pro cannot ship on Android.

| Role | Size / weight / line height |
| --- | --- |
| Brand (Display) | 34 / 700 / 1.1 |
| Screen title (Headline) | 28 / 600 / 1.2 |
| Group title (Title 1) | 22 / 600 / 1.3 |
| Chrome: buttons, labels (Title 2) | 17 / 600 / 1.3 |
| Reading (Body) | 17 / 400 / 1.5 |
| Secondary chrome (Callout) | 16 / 500 / 1.4 |
| Metadata (Footnote) | 13 / 400 / 1.4 |
| Fine print (Caption) | 12 / 400 / 1.3 |
| Section header | 12 / 600, uppercase, 0.8 sp tracking |
| Verbatim JSON (Mono) | 14 / 400 / 1.4 |

- Spacing scale: 2, 4, 8, 16, 24, 32, 48, 64 dp. Every gap is a decision, and unrelated groups never share the same gap.
- Radii: card 16, hero 20, chip 12, button 12, input 12, sheet 20 (top only), dialog 16, pill = half height.
- Cards: `bgSecondary` fill, 0.5 dp `borderSubtle` hairline, no elevation, no ripple. Press feedback is opacity 0.6 only.
- The only shadow in the product is on the floating panel: #000 at 18% alpha, 16 dp blur, 4 dp Y offset, drawn inside the panel window's own padding.
- Touch targets 48 x 48 dp minimum. Insets honoured at the root of every screen.
- Icon sizes: nav 22, row leading 22, trailing chevron 16, hero 28, panel 20. Stroke is 2.0 everywhere.
- Motion: screen push 350 ms ease-in-out (fade plus 20 px slide), pop 280 ms, sheet 400 ms ease-out, button press 150 ms, success reveal 300 ms, theme toggle 200 ms linear. **Reduce motion** (system animator scale 0) turns every transition into an instant opacity change. No spring or bounce anywhere. The runner path has no animation.

### 9.3 Icons

One source for Compose and the overlay: **vector drawables** at 24 dp, stroke 2.0, round caps and joins, with no fonts and no icon packages, so tofu boxes cannot occur. One `AppIcon` composable fixes size and tint. One `AppIcons` alias object maps semantic names to drawables. The set (36): play, pause, stop, add, close, chevron right, chevron left, settings, trash, copy, edit, import, export, paste, tap, swipe, curve, multi-finger, image target, text target, colour target, branch, timer, counter, repeat, shield, check, info, warning, help, drag handle, crosshair, search, record, hud, download.

### 9.4 Accessibility (non-negotiable)

Every interactive element has a semantic label, and reading order is verified in TalkBack. Text scales with the system setting and is tested at 200%. Colour is never the only indicator. Contrast is at least 4.5:1, enforced by `ContrastTest`. Reduce motion is honoured everywhere. RTL layouts flip through start/end, never by manual mirroring.

### 9.5 Components (built in B10, used everywhere, never reimplemented per screen)

| Component | Purpose and rule |
| --- | --- |
| `NegaCard`, `NegaRow` | Filled card, and a two-line row with a leading icon and trailing chevron. Opacity or tint press. |
| `NegaChip`, `ChipIconButton` | Metadata pill, and a 40 x 40 container around an icon. No bare floating icons. |
| `PrimaryButton`, `SecondaryButton` | Filled accent, and filled neutral. Never a 1 px outline for a primary action. |
| `SectionHeader`, `EmptyState` | Uppercase header, centred empty state that collapses if there is nothing to say |
| `NavBar` | Brand or back left, title in the chrome face, action chips right |
| `NegaSheet`, `NegaDialog` | Custom, with `scrim` |
| `NegaTextField`, `Stepper`, `NegaToggle`, `Segmented` | Filled inputs and controls |
| `ConfidenceSlider`, `RegionPicker` | Value shown, range 0.60 to 0.98 with a Safe/Fast hint, and a rectangle picker for search regions |
| `ConditionChip` | A readable summary of one condition, with its icon |
| `UndoBanner`, `ErrorView` | Delete undo, and a full-screen fallback with Retry and Report |
| `AdSlot` | Section 8.7 spec |
| `pressable()` | Modifier: no indication, opacity 0.6 while pressed |

### 9.6 Screens

Every screen follows one shape: nav bar, primary content, secondary actions as cards or rows, then the bottom slot. Only lists scroll, and only when they need to.

| Screen | Layout and behaviour |
| --- | --- |
| **Onboarding** | Welcome, the **prominent disclosure** with 'Agree and continue' or 'Not now', the settings deep link, then live service-state detection |
| **Home** | The 'Nega' wordmark once, a settings chip, a status card (service ready or off, with a one-tap fix), a primary 'Open floating panel' card, a 'Quick scenario' card, the script list as two-line rows, an import row, and the banner slot. Search and sort appear above 8 scripts. |
| **Quick Scenario wizard** | Three choices, then the panel opens over the target app for the crop. See section 7.10. |
| **Simple editor** | Name, run settings (including Start in X), the step list with up and down controls, a step sheet for tap, swipe, curve, multi-finger and wait, and a fixed Save. No ads. |
| **Smart editor** | The event list with priorities. An event sheet holds up to 3 conditions with AND/OR, the actions, and a cooldown. Counter manager. Image and text target pickers. No ads. |
| **Run gate sheet** | Section 8.2. Always shows Get Pro beside the video. |
| **Language pack sheet** | 'Text detection needs a one-time language pack', Wi-Fi or mobile data, then 'Receive language pack' if it is missing |
| **Settings** | Appearance, language, defaults (interval, jitter, countdown, start wait, search rate), panel opacity, Privacy choices (when required), Pro and Restore, rate, feedback, share diagnostic report, delete all local data, About |
| **Help** | The honest limits from section 7.12, keep-alive guidance, how matching works |
| **About** | Version, open-source licences (including the Inter licence), privacy and support links |
| **Paywall** | Optional, reached from Settings or the run gate. Never a blocking modal. States plainly that Pro removes every ad. |
| **Detection lab (debug only)** | Capture, crop, run the matcher at chosen confidences, see score, box and timing, run the repeat test, compare OCR engines, export a report |

## 10. Store compliance map (Google Play)

The biggest approval risk is the Accessibility API, so it is designed in from the start.

| Policy | Mitigation |
| --- | --- |
| Accessibility API use | A prominent in-app disclosure with an explicit 'Agree and continue' before the settings deep link. It states that the service performs taps and swipes and captures the screen area needed to find what the user chose, reads no text from other apps, and sends nothing off the device. A Permissions Declaration form and a demo video cover gestures and screenshots. `canRetrieveWindowContent=false`. `isAccessibilityTool` not set. |
| Data safety form | Matches behaviour exactly. AdMob means advertising ID and related ad data are declared. Frames are processed in memory only. Reference crops stay on the device. Language packs are data downloads that send no user data. 'No data collected' is **not** claimed. |
| Ads policy | A banner on Home, a rewarded video the user opts into, and an interstitial only as a no-fill fallback. No ads in the editor or the overlay. Ad behaviour declared accurately. |
| Metadata policy | Original listing text, not copied from competitors. No game names or trademarks. No cheating, 'bot' or performance-guarantee claims. |
| In-app purchase | Play Billing only. One product, created before submission. Restore always available. |
| Privacy policy | A live URL describing accessibility use, screenshots, ads, packs and purchases |
| Target API and 16 KB page size | Target the level Play requires at B00. CI verifies that bundled native libraries (ads SDK, and OCR if native) are 16 KB aligned. |
| Audience | Not child-directed. Age rating answered honestly. Ads configured accordingly. |
| Regional and data-subject rights | UMP in the EEA, UK and Switzerland. No server-side user data exists. **Delete all local data** covers erasure. |
| Account safety | One dedicated developer account for Nega. No devices, payment details or contact details shared with other developer accounts. The accessibility disclosure, declaration form and demo video are kept matching the app's behaviour exactly, and the form is resubmitted if that behaviour changes. Ad placements are checked against AdMob rules before each release. A rejection does not affect account standing, so the response to a rejection is to fix and resubmit. |

## 11. Error taxonomy

Every error has a class and a defined treatment. 'Show an error' without a class is not allowed.

| Class | Examples | Recoverable | Retriable | UI treatment |
| --- | --- | --- | --- | --- |
| Service not enabled | Accessibility off | Yes | After user action | Status card with guided fix |
| Service killed by OS | Vendor background kill | Yes | Auto-detected | Banner with one-tap re-enable |
| Start wait cancelled | Screen off, service lost, user Cancel | Yes | Yes | The panel and Home say why the wait ended, with a restart option |
| Gesture rejected | Gesture cancelled | Yes | Yes | Per-step skip and counter. After 10 consecutive: **Blocked** state with a plain message. |
| Capture unavailable | Secure screen, too-frequent request | Yes | Yes | 'Can't see this screen'. Next event or stop. **Never a tap.** |
| Nothing found | Timeout, low score | Yes | By design | HUD shows the score and reason |
| Language pack | Missing, failed download, hash mismatch | Yes | Yes | 'Receive language pack'. Image and colour unaffected. A hash mismatch discards the file. |
| Run gate | No ad, blocked fallback | Yes | Yes | The gate sheet with the Pro offer |
| Storage failure | DB, DataStore or file error | Yes | Yes | In-memory fallback plus a warning banner. Never silent loss. |
| Malformed or oversized import | Bad JSON, bad bundle hash, too many steps | No, for that file | No | A message naming the reason |
| Schema too new | File from a later app version | No | No | 'Made by a newer version' |
| SDK failure | Ads, consent, billing | Yes | Yes | Silent slot collapse, ladder step, or a message in Settings |
| Unexpected exception | Any uncaught throwable | No | Restart | Full-screen `ErrorView`. The crash is saved to a local log. |

A global handler is installed in `NegaApp`. The last 5 crashes are kept in app storage. The shareable report contains the version, device model, Android version, error classes and the detection log, and never script content or images. Every async step checks lifecycle before touching UI, and every save, delete and share shows an explicit success or failure message.

## 12. Performance targets and acceptance gates

The cheapest supported device is the target device. The reference device is named at B12 from the owner's phone, plus an API 30 emulator profile with 1.5 GB RAM in CI.

| Metric | Target |
| --- | --- |
| Cold start to interactive Home | < 2 s |
| UI frame rate | 60 fps, no dropped frames scrolling 100+ scripts |
| Database reads | < 50 ms |
| Scheduler lateness at 100 ms interval | p95 <= 10 ms, p99 <= 25 ms |
| Match time per frame inside the search region | <= 150 ms on the reference device |
| Capture to tap | median <= 500 ms, p95 <= 900 ms |
| Detection cadence | >= 2 searches per second |
| Release APK size | <= 12 MB without OpenCV or native OCR, raised only after the lab decides |

**Acceptance gates.** The thresholds are our targets, not industry benchmarks. Real-app frames are captured on the owner's phone and never committed to the repo.

| Test | Setup | Pass |
| --- | --- | --- |
| T1 Repeat-run stability | One static screen, one image, 100 consecutive runs without restart | At least 98 detections, **0 false taps**, drift at most 15%, 0 runs that report 'found' without searching |
| T2 False positives | 500 negative frames: scrolling, loading, dialogs, dark mode, other aspect ratios, target absent | At most 5 (1%) at default confidence, at most 15 (3%) at the minimum slider value, dark vs light no worse than 2x |
| T3 Cross-profile | Five profiles: 1080x2400 at 420 dpi, 1440x3200 at 560, 720x1600 at 320, dark mode, large display size. 30 detections each | At least 95% on each, at most 1 false positive in 30, drop from baseline at most 5 points |
| T4 Latency | 50 detections on the reference device | Median <= 500 ms, p95 <= 900 ms, >= 2 searches per second |
| T5 Recovery | Screen off/on, background/foreground, secure screen. 10 runs each | 10 of 10 resume. A secure screen yields the 'can't see' state and **never** a tap. |
| T6 Record fidelity | Record and replay a 20-gesture sequence on the reference device | Each replayed gesture within 12 px and 50 ms of the recording, and the target app still receives every gesture |
| T7 Text detection | 200 short game-style strings per shipped pack, plus a download with a corrupted file | At least 90% exact reads per pack, reported per language, and a corrupted pack is always rejected |
| T8 Start in X | Waits of 1 hour, 4 hours and 12 hours plugged in, and 1 hour unplugged, on the owner's phone | The countdown ends, the run starts on time, and the service is still connected |

**Fail actions.** A failed T1 to T5 demotes image targeting to a secondary mode until fixed, and tuning a threshold alone is not a fix. A failed T6 leaves Record mode off by default. A pack that fails T7 is not offered for that language, and Help says so. A failed T8 lowers `MaxStartWaitMs` to the longest passing wait, and Help says so.

**Where each runs.** JVM tests on synthetic frames cover matcher logic and part of T2. The emulator workflow covers T1, T3 and part of T5. The Detection lab on the owner's phone covers T4, T6, T7, the secure-screen case and real-frame T2. T8 runs on the owner's phone.

## 13. Localization

| Language | Folder | Direction |
| --- | --- | --- |
| English (default) | `values` | LTR |
| Spanish, French, German, Portuguese | `values-es`, `-fr`, `-de`, `-pt` | LTR |
| Arabic | `values-ar` | **RTL** |
| Hindi | `values-hi` | LTR |
| Japanese, Korean | `values-ja`, `-ko` | LTR |
| Chinese (Simplified) | `values-zh` | LTR |
| Hebrew | `values-iw` (the legacy code Android resolves for Hebrew), tested at B18 | **RTL** |

- `locales_config.xml` enables per-app language. An in-app picker uses AppCompat per-app locales.
- All UI text lives in `strings.xml`. `NoHardcodedStringsTest` fails the build on literals in `ui/`. A parity test fails if any key is missing from any of the 11 files.
- RTL uses start/end everywhere. Android pseudo-locales run in lint to catch truncation. Numbers and durations use locale-aware formatters.
- Translations are written by the agent. A native review before store release is recommended and is not claimed to have happened.
- The store listing, the website landing pages and Studio are localized into the same 11 languages. The privacy policy stays in English.

## 14. Data, privacy and storage

- **Room** stores scripts as one JSON column plus indexed name, updated time and step count. The schema version is exported and migrations are tested.
- **DataStore** holds settings, the consent cache, the entitlement cache, the run ledger and rating state.
- **Files:** reference crops are PNGs under app storage per script. Language packs live in a separate pack folder. Frames are held in memory only and never written or sent.
- Every storage call is wrapped in try/catch with an in-memory fallback and a visible warning. A corrupted database is renamed aside, a fresh one is created, and the user is told.
- `allowBackup` is false in v1. Users move scripts by export.
- No analytics SDK. Crash visibility comes from Play Console vitals plus the local crash log, which is shared only by the user.
- **Delete all local data** wipes the database, DataStore, templates, packs and crash log.
- Secrets (keystore, passwords, ad IDs) live only in GitHub secrets and are never written to the repo or logs.

## 15. The web: site and Studio

### 15.1 Is a web version possible?

**Partly, and honestly.** A browser cannot tap, swipe or read another app, so no web page can be the auto clicker. What the web can do well is **author** scripts on a large screen and **market** the app. Both are built.

### 15.2 The static website (`website/`, Cloudflare Pages project 1)

- Pages: `index.html` (landing), `privacy.html`, `support.html`, `accessibility.html` (the plain-language disclosure matching the in-app text), `404.html`, plus localized landing pages `website/<lang>/index.html` for the 10 other languages.
- `app-ads.txt` for AdMob (OWNER INPUT: publisher ID). `robots.txt`, `sitemap.xml`, `_headers`. The website hosts no language-pack files, because packs come through Play Asset Delivery.
- Hand-written HTML and CSS with tokens as CSS variables, light and dark through `prefers-color-scheme`. No framework, no build step, no shared code with the app.
- Cloudflare settings: build command empty, output directory `website`, build watch path `website/`.

### 15.3 Nega Studio (`studio/`, Cloudflare Pages project 2)

A static web app for building scripts on a big screen and sending them to the phone.

- **Open and edit** `.nega` bundles and script JSON, validated against the same schema as the app.
- **Screenshot stage:** load phone screenshots (they stay in the browser's memory). Place tap, swipe, curve and multi-finger points. Draw search regions. Drag a rectangle to crop an image target. Sample a colour point.
- **Event and condition editor** mirroring the app: up to 3 conditions with AND/OR, actions, priorities, counters.
- **Match lab:** run a JavaScript normalized cross-correlation of a crop against other screenshots and see the best match box and score, to help tune confidence before the phone is involved. Results are indicative, because a real phone capture differs. Text and number conditions can be authored but not tested in Studio.
- **Export** a `.nega` bundle or JSON. The user moves the file to the phone with any file transfer and imports it through the file picker. Small coordinate-only scripts can be pasted.
- **Privacy:** nothing is uploaded. There are no accounts and no analytics. It works offline as a PWA.
- **Architecture:** vanilla ES modules, no framework, no build step, no third-party CDN scripts, a strict content-security policy. Modules: `schema/` (types, validator, migrator), `bundle/` (zip reader and writer, with `DecompressionStream` for deflate and `SubtleCrypto` for SHA-256), `vision/` (grayscale, pyramid, correlation, colour), `editor/` (canvas stage, markers, regions, crop), `ui/` (tokens as CSS variables, components), `i18n/` (11 JSON files), `sw.js` and `manifest.webmanifest`.
- **Drift protection:** this is a separate JavaScript implementation, not shared code. Golden fixtures in `contract/` (scripts, bundles, expected validation results) are run by both the Kotlin tests and the Studio tests, so any divergence fails a build.
- Cloudflare settings: output directory `studio`, build watch path `studio/`. A separate `studio_check.yml` runs the Studio tests (Node's built-in test runner) when `studio/` or `contract/` changes.
- **Feasibility:** yes. About 40 files, built after the app (batches W01 to W04).

## 16. CI/CD

| Workflow | Trigger | Does |
| --- | --- | --- |
| `check.yml` | push to `ci-analyser` | JDK 17, cached Gradle, then `ktlintCheck`, `detekt`, `lintDebug`, `testDebugUnitTest`, `assembleDebug`. No upload, no signing. From B22 it also runs `minifyReleaseWithR8` and the native-library alignment check. |
| `studio_check.yml` | push to `ci-analyser` touching `studio/` or `contract/` | Runs the Studio tests with Node's built-in test runner against the shared fixtures |
| `debug_apk.yml` | `workflow_dispatch` | Builds the debug APK and uploads it as an artifact, for the owner's own phone only |
| `emulator_stress.yml` | `workflow_dispatch` | Hardware-accelerated emulator, installs the debug APK, enables the service over adb, runs the stress scenarios, uploads a report |
| `build_and_deploy.yml` | `workflow_dispatch` | Decodes the keystore from secrets, builds a signed AAB for Play with R8, and uploads it plus the mapping file. Any APK it produces is for internal testing only and is never distributed. |
| `dependabot.yml` | weekly | Gradle and Actions updates |

**Documented exception to the original manual-only rule:** `check.yml` and `studio_check.yml` run on pushes to `ci-analyser`, because the owner asked for a check after every push and a public repo has free minutes. The rule's purpose, no wasted builds and no auto-published artifacts, is kept: build, deploy and release workflows stay manual only.

**Branch flow.** Every batch lands on `ci-analyser` first. When the check is green it is fast-forwarded to `main`, so `main` only ever contains green batches and is what the owner inspects on the web. Cloudflare Pages watches `main`: project 1 with build watch path `website/`, project 2 with `studio/`.

**Versions.** At B00 the agent reads current stable versions from the Google and Maven repositories, pins them exactly in `gradle/libs.versions.toml` and records them. Nothing floats. The Gradle wrapper is generated in B00. If the sandbox cannot produce the wrapper binary, workflows pin Gradle through the setup action until it lands.

**Secrets (the owner enters these in the repo settings).**

| Name | Content |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Single-line base64 of the keystore. The agent generates the keystore in its sandbox once the alias is supplied and hands it over as one copy-pasteable line. The owner keeps the `.jks` too. |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD` | Supplied by the owner |
| `ANDROID_KEY_ALIAS` | OWNER INPUT |
| `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_REWARDED_ID`, `ADMOB_INTERSTITIAL_ID` | OWNER INPUT |

Play App Signing is recommended, which makes this key an upload key that can be reset.

## 17. Build loop and contract

**Per batch, run by the agent in its sandbox:**

1. Write the batch's files.
2. Commit as `<batch-id>: <purpose>`, for example `B08: accessibility service and overlay`.
3. Push to `ci-analyser`.
4. Poll the Actions run through the GitHub API. On failure, read the logs, patch, commit and push again. Main is never force-pushed.
5. On green, fast-forward `main`.
6. Report: the batch, the files that landed, what to look for on the repo web page, and the check result.
7. Continue to the next batch without waiting, except at a checkpoint or when an owner input is missing.

**Checkpoints.** **APK-1 after B12** proves the engine and detection on the owner's phone. **APK-2 after B18** is the finished UI before the brand and website work. At each, the owner runs `debug_apk.yml` on the web Actions tab, downloads the artifact and installs it. The signed release run happens only after APK-2 has been tested.

**Termux (read-only extras).** The agent gives the exact clone command once the repo URL is supplied, for example `git clone <url> ~/<repo>`, plus `git log --oneline -5` and `git status` for light checks.

**Contract block, emitted before B00:**

```
REPO URL:      <supplied by owner>
BRANCH FLOW:   batch -> ci-analyser -> check green -> fast-forward main
BATCH PLAN:    B00 to B22, then S01 to S03, then W01 to W04
PER-BATCH:     write -> commit -> push -> check -> fix -> main -> report
CHECKPOINTS:   APK-1 after B12, APK-2 after B18
FINAL GATE:    stress workflow, then manual release workflow
```

## 18. Test pyramid

- **JVM unit tests (the bulk):** codec, validator, migrator, scaler, planners, evaluator, event scheduler, both runners with a fake dispatcher, matcher on synthetic frames, stabiliser, text matcher, number parser, run gate, ledger, consent and entitlement policies, `ContrastTest`, `LayeringTest`, `NoHardcodedStringsTest`, locale parity, and the shared `contract/` fixtures.
- **Lint, ktlint, detekt:** style and a first line of correctness.
- **Emulator scenarios (S01 to S03):** a tap-counting test activity, rate accuracy, global timer, screen-off stop, rotation rescale, service loss and recovery, malformed imports, size and density profiles, and gates T1, T3 and part of T5.
- **Device lab:** the owner's phone is the truth for timing and detection quality, through the Detection lab (T4, T6, T7), and for the Start in X wait (T8).
- **Studio tests:** Node's built-in runner, against the same fixtures.
- **Manual matrix:** `docs/TESTING.md`, covering each Android version bucket, each language, RTL, large font, dark mode and the consent regions.

## 19. Phases and batches in dependency order

A phase is a logical stage. A batch is the smallest set of files that can be checked, committed and reviewed together without leaving the repo broken. A later batch never starts before the batches it depends on have landed green.

| Phase | Batch | Contents | Depends on |
| --- | --- | --- | --- |
| **P1 Foundation** | B00 | Repo scaffold, CI, docs | nothing |
|  | B01 | Design tokens, theme, fonts | B00 |
|  | B02 | Core models, errors, formatters | B00 |
|  | B03 | Script logic: codec, validator, bundle, planners, quick-scenario factory, record conversion, shared fixtures | B02 |
| **P2 Data** | B04 | Ports, Room, DataStore, template and pack files, import/export gateway | B02, B03 |
| **P3 Detection and engine core** | B05 | Vision: frames, matcher, colour, stabiliser, frame diff | B02 |
|  | B06 | Text core: matcher, number parser, pack models | B02, B05 |
|  | B07 | Engine core: evaluator, scheduler, counters, cadence, both runners, pipeline, run session (including the Start in X wait), bridge | B03, B05, B06 |
| **P4 Android service** | B08 | Accessibility service, gestures, screenshot capture, overlay, panel, HUD, markers, crop | B07, B04 |
| **P5 UI foundation** | B09 | Icon set | B01 |
|  | B10 | Component library | B01, B09 |
|  | B11 | App shell, navigation, platform services | B04, B07, B10 |
| **P6 Screens and features** | B12 | Onboarding, Home, Detection lab. **APK-1** | B08, B10, B11 |
|  | B13 | Simple and Smart editors, import/export UI | B04, B10, B12 |
|  | B14 | Quick Scenario wizard and Record mode | B08, B13 |
|  | B15 | Settings, Help, About, Paywall screen | B10, B11, B13 |
|  | B16 | OCR engine, language packs and their UI | B06, B13 |
|  | B17 | Run gate, ledger, ads, consent, billing, review | B13, B15 |
|  | B18 | Localization. **APK-2** | B17 |
| **P7 Ship** | B19 | Brand: icon and splash | B18 |
|  | B20 | Website | B19 |
|  | B21 | Store docs and README | B20 |
|  | B22 | Release hardening | B21 |
| **P8 Verification** | S01 to S03 | Emulator harness, stress scenarios, detection gates | B22 |
| **P9 Studio** | W01 to W04 | Scaffold and schema, screenshot stage, match lab, PWA and localization | B03 fixtures, B22 |

## 20. File manifest

Path shorthand: `K/` = `app/src/main/kotlin/com/zdmgold/autoclickernega/`, `R/` = `app/src/main/res/`, `T/` = `app/src/test/kotlin/com/zdmgold/autoclickernega/`. Each row gives the file, what it does and how, a key snippet, and what it connects to. Auto-generated files (Gradle wrapper jar, Room schema JSON) are not counted. A batch may exceed 15 files only when it is a family of near-identical files (icons, strings, website pages).

### B00. Scaffold, CI and docs (depends on: nothing)

After this batch the owner sees `app/`, `.github/`, `gradle/` and `docs/` on the repo page.

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `.gitignore` | Keeps build output, keystores and local config out of git. Committed before any build runs. | `*.jks` | Everything |
| `.editorconfig` | ktlint settings | `max_line_length = 120` | ktlint in `check.yml` |
| `settings.gradle.kts` | Project name, repositories, module include | `include(":app")` | Gradle |
| `build.gradle.kts` | Declares plugins once, applied in `app` | `alias(libs.plugins.ksp) apply false` | `libs.versions.toml` |
| `gradle.properties` | JVM memory, AndroidX, caching | `android.useAndroidX=true` | Gradle |
| `gradle/libs.versions.toml` | Every dependency and plugin at an exact version read from the repos at B00 | `[versions]` table | `app/build.gradle.kts`, Dependabot |
| `gradle/wrapper/*`, `gradlew`, `gradlew.bat` | Reproducible Gradle | generated | CI |
| `config/detekt/detekt.yml` | Complexity limits. Forbids `TODO`, `FIXME` and `STOPSHIP` comments. | `ForbiddenComment` | `check.yml` |
| `app/build.gradle.kts` | Namespace, minSdk 30, targetSdk, debug suffix, R8 for release, Compose, Room schema export | `applicationIdSuffix = ".debug"` | All source |
| `app/proguard-rules.pro` | Minimal keep rules | `-keep class com.zdmgold.autoclickernega.service.** { *; }` | R8 |
| `app/src/main/AndroidManifest.xml` | Minimal: application and launcher activity | `<activity android:name=".MainActivity">` | `NegaApp`, `MainActivity` |
| `K/NegaApp.kt`, `K/MainActivity.kt` | Compile skeleton so the first check is green | `class NegaApp : Application()` | Manifest |
| `.github/workflows/check.yml` | Light check on `ci-analyser` pushes | `on: push: branches: [ci-analyser]` | The whole build loop |
| `.github/workflows/debug_apk.yml` | Manual debug APK | `on: workflow_dispatch` | Checkpoints |
| `.github/dependabot.yml` | Weekly dependency updates | `interval: weekly` | `libs.versions.toml` |
| `docs/BLUEPRINT.md` | This blueprint exported as the build's source of truth |  | All |
| `docs/TESTING.md` | Manual QA matrix |  | Checkpoints |
| `README.md` | Stub, finalised in B21 |  |  |

### B01. Design tokens, theme, fonts (depends on: B00)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/ui/theme/Tokens.kt` | Colours (light and dark), spacing, radii, icon sizes as constants. The only place raw values exist. | `val accent = Color(0xFF12C79C)` | Every UI file, `OverlayTheme` |
| `K/ui/theme/Typography.kt` | Eight roles plus mono, built on Inter | `val title2 = TextStyle(fontSize = 17.sp, fontWeight = SemiBold)` | `NegaTheme` |
| `K/ui/theme/Theme.kt` | `NegaTheme` provides tokens through a CompositionLocal. Light default, system, manual. | `CompositionLocalProvider(LocalTokens provides tokens)` | `MainActivity`, all screens |
| `K/ui/theme/Motion.kt` | Durations and easing. Reads the system animator scale for reduce motion. | `fun duration(ms: Int) = if (reduced) 0 else ms` | Navigation, components |
| `R/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf` | Bundled weights 400 to 700 |  | `Typography.kt` |
| `app/src/main/assets/licenses/inter-OFL.txt` | Font licence text |  | About screen |
| `R/values/themes.xml`, `R/values/colors.xml` | Splash theme with the window background set so there is no blank flash | `Theme.SplashScreen` parent | Manifest |
| `T/ui/theme/ContrastTest.kt` | WCAG contrast for every text pair, light and dark. Fails below 4.5:1. | `assertTrue(ratio >= 4.5)` | `Tokens.kt` |
| `T/ui/theme/TokensTest.kt` | Spacing scale and radii stay on the allowed lists |  | `Tokens.kt` |

### B02. Core models, errors, formatters (depends on: B00)

Pure Kotlin. Nothing here imports Android.

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/core/model/Geometry.kt` | `Point`, `Rect`, `Stroke` value types | `data class Rect(val l: Int, val t: Int, val r: Int, val b: Int)` | Every model |
| `K/core/model/Action.kt`, `Target.kt` | The sealed actions of section 7.3 and the `At` or `AtMatch` target | `sealed interface Action` | Codec, planners, runners |
| `K/core/model/Condition.kt` | Image, Text, Color, Number, Timer and Counter conditions, each with `negate` | `data class ImageCond(val template: String, val confidence: Float, val mode: MatchMode, val region: Rect?, override val negate: Boolean)` | Evaluator, editor |
| `K/core/model/Expr.kt`, `Event.kt` | Up to 3 conditions with AND/OR, and an event with priority, cooldown and actions | `data class Event(val id: String, val priority: Int, ...)` | Scheduler, Smart editor |
| `K/core/model/Script.kt`, `RunConfig.kt`, `ScreenSpec.kt`, `TemplateRef.kt`, `CounterDef.kt` | Immutable script (Simple or Smart), run options (including the Start in X delay), reference screen, template metadata, counter definitions | `sealed interface ScriptBody { data class Simple(...); data class Smart(...) }` | Codec, store, editors |
| `K/core/model/EngineState.kt` | Idle, Countdown, Running, Paused, Blocked, ServiceLost, plus events and `StopReason` | `sealed interface EngineState` | Bridge, Home, panel, HUD |
| `K/core/model/Entitlement.kt`, `ConsentState.kt`, `AppSettings.kt` | Pro state, ads-permitted state, settings with `ThemeMode` and `SearchRate` | `enum class SearchRate { Auto, Fast, BatterySaver }` | Providers, stores |
| `K/core/error/AppError.kt` | The section 11 taxonomy as a sealed type with class, recoverable and retriable flags | `sealed class AppError(val recoverable: Boolean)` | Every layer, `ErrorView` |
| `K/core/result/Outcome.kt` | Success or typed failure | `sealed interface Outcome<out T>` | Stores, gateways |
| `K/core/format/Formatters.kt` | Durations and counts, locale aware, including hour-long waits | `fun formatDuration(ms: Long, locale: Locale)` | Home rows, panel |
| `T/LayeringTest.kt` | Scans imports: `core/` has no `android.*`, `ui/` touches `service/` only through the bridge |  | Whole tree |
| `T/core/model/ModelsTest.kt` | Equality, copy and range clamps |  | Models |

### B03. Script logic (depends on: B02)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/core/script/ScriptCodec.kt` | JSON to and from `Script` for both modes, schema 1 | `fun decode(json: String): Outcome<Script>` | Store, import, export, bundle |
| `K/core/script/ScriptValidator.kt` | Limits of section 7.3: size, counts, 1 to 3 conditions, `at: match` rule, script cycles, clamped ranges (including `startDelayMs` to 12 hours) | `require(items.size in 1..3)` | Import, editor save |
| `K/core/script/ScriptMigrator.kt` | Identity at schema 1 from day one. Rejects a newer schema with a clear error. | `fun migrate(raw: JsonObject): Outcome<JsonObject>` | Codec |
| `K/core/script/CoordinateScaler.kt` | Rescales points, regions and templates by screen and density ratio | `fun scale(p: Point, from: ScreenSpec, to: ScreenSpec)` | Planner, matcher |
| `K/core/script/NegaBundle.kt` | `.nega` zip: `script.json`, `manifest.json`, `templates/`. Verifies hashes. 4 MB cap. | `fun unpack(bytes: ByteArray): Outcome<Bundle>` | Import, export |
| `K/core/script/ActionPlanner.kt` | Simple mode: builds the planned step list with scaling and seeded jitter | `fun plan(script: Script, screen: ScreenSpec): List<PlannedStep>` | `SequenceRunner` |
| `K/core/script/EventCompiler.kt` | Smart mode: validates events, orders by priority, resolves match anchors | `fun compile(script: Script): Outcome<EventPlan>` | `EventRunner` |
| `K/core/script/QuickScenarioFactory.kt` | Builds the one-event script of the wizard, or the timed-tap Simple script | `fun tapWhenImage(t: TemplateRef, region: Rect?): Script` | Wizard (B14) |
| `K/core/script/RecordToSteps.kt` | Turns recorded gestures and gaps into steps | `fun toSteps(rec: List<RecordedGesture>): List<Action>` | Record mode (B14) |
| `K/core/run/Jitter.kt` | Seeded position and timing jitter so runs are reproducible in tests | `fun offset(seed: Long, px: Int)` | `ActionPlanner` |
| `contract/README.md`, `contract/fixtures/valid/*.json`, `contract/fixtures/invalid/*.json` | Golden scripts, bundle manifests and expected validation results shared with Studio |  | Kotlin and Studio tests |
| `T/core/script/ScriptCodecTest.kt`, `ScriptValidatorTest.kt`, `ScriptMigratorTest.kt`, `CoordinateScalerTest.kt`, `NegaBundleTest.kt`, `ContractFixturesTest.kt` | Round trips, malformed input, oversize, nesting, cycles, newer schema, fixture agreement |  | The files above |
| `T/core/script/ActionPlannerTest.kt`, `EventCompilerTest.kt`, `QuickScenarioFactoryTest.kt`, `RecordToStepsTest.kt` | Order, loops, priority, anchors, wizard output, recording conversion |  | Planners |

### B04. Ports and data layer (depends on: B02, B03)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/core/port/ScriptStore.kt`, `SettingsStore.kt`, `TemplateStore.kt`, `PackStore.kt`, `Clock.kt` | Interfaces the rest of the app depends on | `interface ScriptStore { val scripts: Flow<List<ScriptSummary>> }` | `data/`, view models, engine |
| `K/data/db/ScriptEntity.kt` | One row per script: id, name, JSON, step count, updated time | `@Entity(tableName = "scripts")` | DAO |
| `K/data/db/ScriptDao.kt` | Reads, upsert, delete, undo-restore | `@Query("SELECT id,name FROM scripts ORDER BY updatedAt DESC")` | `RoomScriptStore` |
| `K/data/db/NegaDatabase.kt`, `Migrations.kt` | Version 1 with exported schema. Corruption triggers rename and recreate. | `@Database(version = 1, exportSchema = true)` | Store |
| `K/data/RoomScriptStore.kt` | Implements `ScriptStore` through the codec, wraps errors as `AppError.Storage` |  | View models |
| `K/data/DataStoreSettings.kt` | Settings, consent cache, entitlement cache, rating state | `preferencesDataStore("settings")` | Settings, `AppViewModel` |
| `K/data/FileTemplateStore.kt` | PNG crops under app storage per script | `filesDir/templates/<scriptId>/<id>.png` | Editors, runners |
| `K/data/FilePackStore.kt` | Language packs in their own folder, with hash and installed-version records | `filesDir/packs/<lang>/` | Pack manager (B16) |
| `K/data/ScriptFileGateway.kt` | Reads picked files with size caps, writes exports, builds share intents | `contentResolver.openInputStream(uri)` | Home, editors |
| `K/data/DataWiper.kt` | Deletes database, DataStore, templates, packs and crash log | `suspend fun wipeAll()` | Settings |
| `R/xml/file_paths.xml` | FileProvider paths for exports |  | Manifest |
| `T/data/FileTemplateStoreTest.kt`, `FilePackStoreTest.kt`, `SettingsMappingTest.kt`, `ScriptEntityMappingTest.kt` | Temp-directory and mapping tests |  | Data layer |

### B05. Vision core (depends on: B02)

Pure Kotlin, fully unit-tested on synthetic frames, so the wedge is proven before any UI depends on it.

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/core/port/FrameSource.kt` | Capture contract: the next frame or a typed failure | `suspend fun next(): Outcome<Frame>` | Pipeline, the screenshot capture source |
| `K/core/vision/Frame.kt` | Grayscale byte array with width and height, plus the colour sample needed by the colour matcher | `class Frame(val w: Int, val h: Int, val px: ByteArray)` | Matchers |
| `K/core/vision/FrameDiff.kt` | Cheap change measure between frames, for adaptive cadence | `fun changed(a: Frame, b: Frame): Boolean` | `CadenceController` |
| `K/core/vision/Pyramid.kt` | Downscaled levels for a coarse-to-fine search | `fun levels(f: Frame, n: Int): List<Frame>` | Matcher |
| `K/core/vision/NccMatcher.kt` | Normalized cross-correlation. Coarse search in the region, refinement at full size. | `fun find(f: Frame, t: Frame, spec: ImageCond): Match?` | Evaluator, lab |
| `K/core/vision/ColorMatcher.kt` | Colour at a point or region within a tolerance | `fun matches(f: Frame, c: ColorCond): Boolean` | Evaluator |
| `K/core/vision/TemplateScaler.kt` | Rescales a template by the density ratio |  | Matcher |
| `K/core/vision/MatchStabilizer.kt` | Safe mode: same match within 12 px on two consecutive frames | `fun accept(m: Match?): Match?` | Evaluator |
| `K/core/vision/Match.kt`, `DetectionLog.kt` | Score, box, centre. Log entries with no pixels. | `data class Match(val score: Float, val box: Rect)` | HUD, lab, diagnostics |
| `T/core/vision/SyntheticFrames.kt` | Generates frames with and without targets, noise, scale, brightness |  | All vision tests |
| `T/core/vision/NccMatcherTest.kt`, `ColorMatcherTest.kt`, `PyramidTest.kt`, `MatchStabilizerTest.kt`, `FrameDiffTest.kt`, `TemplateScalerTest.kt` | Hit, miss, false-positive rate on synthetic negatives, scale tolerance, stability |  | Vision core |

### B06. Text core (depends on: B02, B05)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/core/port/TextRecognizer.kt` | The recogniser contract of section 7.7 | `suspend fun read(frame: Frame, region: Rect, lang: String): Outcome<List<TextBlock>>` | Evaluator, OCR engine (B16) |
| `K/core/ocr/TextBlock.kt` | Recognised text with its box and confidence |  | Matcher |
| `K/core/ocr/TextMatcher.kt` | Contains or equals, optional case sensitivity, Unicode normalisation | `fun matches(blocks: List<TextBlock>, c: TextCond): Match?` | Evaluator |
| `K/core/ocr/NumberParser.kt` | Reads digits from text, handling separators and locale digits | `fun parse(s: String): Double?` | Number conditions |
| `K/core/ocr/PackSpec.kt`, `PackManifest.kt` | A pack's language and hash, and the app's built-in list of them | `data class PackSpec(val lang: String, val sha256: String)` | Pack manager, `scripts/pack_hashes.sh` |
| `T/core/ocr/TextMatcherTest.kt`, `NumberParserTest.kt`, `PackManifestTest.kt`, `FakeTextRecognizer.kt` | Matching rules, number edge cases, built-in pack list parsing |  | Text core |

### B07. Engine core (depends on: B03, B05, B06)

Written against ports, so it runs on the JVM with a fake dispatcher, fake clock and fake frames, with no emulator.

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/core/port/GesturePort.kt`, `ServiceBridge.kt` | The dispatch contract, and the UI-facing engine contract (commands in, state out) | `interface GesturePort { suspend fun dispatch(g: Action): Boolean }` | Runners, view models |
| `K/core/run/ConditionEvaluator.kt` | Evaluates an expression cheapest first with short-circuiting and returns the match anchor | `fun passes(e: Expr, f: Frame, ctx: RunContext): Boolean` | `EventRunner` |
| `K/core/run/EventScheduler.kt` | Priority order, cooldowns, first-match policy | `fun pick(events: List<Event>, now: Long): Event?` | `EventRunner` |
| `K/core/run/CounterStore.kt` | Per-run counters with set, add and subtract | `fun apply(op: CounterOp)` | Evaluator, runners |
| `K/core/run/CadenceController.kt` | Adaptive search interval with Auto, Fast and Battery saver | `fun nextIntervalMs(changed: Boolean): Long` | Pipeline |
| `K/core/run/LoopGuard.kt` | Depth and cycle guard for `RunScript` | `fun enter(scriptId: String): Boolean` | Runners |
| `K/core/run/StartWaitPolicy.kt` | The Start in X range, presets and `MaxStartWaitMs = 43_200_000`. `GatePolicy` (B17) re-exports it so every run constant is listed in one place. | `const val MaxStartWaitMs = 43_200_000L` | `RunSession`, editor, validator |
| `K/service/engine/MonotonicClock.kt` | Implements `Clock` with the monotonic clock | `System.nanoTime() / 1_000_000` | Runners |
| `K/service/engine/SequenceRunner.kt` | Simple mode with start-to-start timing and no catch-up bursts | `nextAt += step.intervalMs` | Planner, dispatcher |
| `K/service/engine/EventRunner.kt` | Smart mode: one pass per frame, first passing event runs | `suspend fun onFrame(frame: Frame, now: Long)` | Scheduler, evaluator |
| `K/service/engine/DetectionPipeline.kt` | Pulls frames, converts, masks the panel, runs the evaluator, writes the log | `suspend fun frames(): Flow<Frame>` | `FrameSource`, HUD |
| `K/service/engine/RunSession.kt` | State machine: start wait, countdown, run, pause, stop. Handles the Start in X wait with its cancel reasons, loops, global timer, caps, screen-off and the 10-rejection rule. | `enum class StopReason { User, Timer, Done, Cap, ScreenOff, ServiceLost, Blocked, WaitCancelled }` | Bridge, panel |
| `K/service/bridge/EngineBridge.kt` | In-process singleton. The service registers on connect. Exposes state and events. Commands return a typed error if no service is bound. | `val state: StateFlow<EngineState>` | Service, `ServiceBridge` users |
| `T/core/run/ConditionEvaluatorTest.kt`, `EventSchedulerTest.kt`, `CounterStoreTest.kt`, `CadenceControllerTest.kt`, `LoopGuardTest.kt`, `StartWaitPolicyTest.kt` | Short-circuit order, priority, cooldown, counters, backoff and reset, cycles, wait range and presets |  | Core run |
| `T/service/engine/SequenceRunnerTest.kt`, `EventRunnerTest.kt`, `RunSessionTest.kt`, `DetectionPipelineTest.kt`, `FakeGesturePort.kt`, `FakeClock.kt`, `FakeFrameSource.kt` | Timing without drift, event choice, caps, stop reasons, wait then run, wait cancelled by Cancel, screen off and service loss, no tap on capture failure |  | Engine |

### B08. Accessibility service, gestures, capture, overlay (depends on: B07, B04)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `R/xml/accessibility_service_config.xml` | Section 7.11 configuration | `android:canTakeScreenshot="true"` | Manifest |
| `K/service/NegaAccessibilityService.kt` | On connect: registers with the bridge, builds the overlay, selects the frame source. On unbind: stops runs and unregisters. | `override fun onServiceConnected()` | Bridge, overlay |
| `K/service/ScreenStateReceiver.kt` | Stops runs, and cancels a Start in X wait, when the screen turns off | `Intent.ACTION_SCREEN_OFF` | `RunSession` |
| `K/service/engine/GestureDispatcher.kt` | Builds `GestureDescription` for tap, swipe, curve and multi-finger. Clamps to platform limits. Suspends until completed or cancelled. | `service.dispatchGesture(desc, callback, null)` | Runners |
| `K/service/capture/ScreenshotFrameSource.kt` | Capture through the accessibility screenshot API. Maps secure-window, interval and internal errors to typed failures. | `takeScreenshot(Display.DEFAULT_DISPLAY, executor, callback)` | Pipeline |
| `K/service/capture/BitmapToFrame.kt` | Hardware buffer to grayscale `Frame`, downscaled, recycled immediately |  | Capture source |
| `K/service/overlay/OverlayController.kt` | Adds and removes the panel, marker and crop windows. Hides them during capture. Sets the keep-screen-on flag on the panel window only while a Start in X wait is active. | `TYPE_ACCESSIBILITY_OVERLAY` | Service |
| `K/service/overlay/PanelView.kt` | Native panel: start/pause, stop, add tap, swipe, curve, image, timer, counters, collapse, close, and the Start in X countdown with Cancel. Draggable, with opacity. |  | Controller, bridge |
| `K/service/overlay/HudView.kt` | The detection strip: score, reason for a miss, active event | `fun show(score: Float?, reason: HudReason)` | Panel, detection log |
| `K/service/overlay/MarkerLayer.kt`, `MarkerView.kt` | Numbered draggable markers. Circle (tap), arrow (swipe, curve), corner brackets (image). `FLAG_NOT_TOUCHABLE` during runs. |  | Panel, session |
| `K/service/overlay/CropOverlay.kt` | Dims the screen, drag a rectangle, confirm, crop from the latest frame and save through `TemplateStore` |  | Panel, `TemplateStore` |
| `K/service/overlay/OverlayTheme.kt` | Reads `Tokens` as colours and pixel sizes for plain Views |  | Overlay views |
| `K/service/overlay/ScreenMetrics.kt` | Current size, density and rotation |  | Scaler, overlay |
| `K/service/ScriptSession.kt` | Shared live draft that overlay edits write into and the editors read back | `val draft: StateFlow<Script>` | Editors, overlay |
| `R/values/strings.xml` | English strings. Every later batch appends its own keys. |  | Everything |

### B09. Icon set (depends on: B01)

Thirty-six vector drawables at 24 dp, stroke 2.0, round caps and joins.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
  <path android:pathData="M9 6l6 6-6 6" android:strokeColor="#FF000000" android:strokeWidth="2"
      android:strokeLineCap="round" android:strokeLineJoin="round"/>
</vector>
```

| File | Role and how it works | Connects to |
| --- | --- | --- |
| `R/drawable/ic_play.xml`, `ic_pause`, `ic_stop`, `ic_add`, `ic_close`, `ic_chevron_right`, `ic_chevron_left`, `ic_settings`, `ic_trash`, `ic_copy`, `ic_edit`, `ic_import`, `ic_export`, `ic_paste`, `ic_tap`, `ic_swipe`, `ic_curve`, `ic_multi`, `ic_image_target`, `ic_text_target`, `ic_color_target`, `ic_branch`, `ic_timer`, `ic_counter`, `ic_repeat`, `ic_shield`, `ic_check`, `ic_info`, `ic_warning`, `ic_help`, `ic_drag`, `ic_crosshair`, `ic_search`, `ic_record`, `ic_hud`, `ic_download` | One stroke path each. The tint is applied by the consumer, so one drawable serves both Compose and the overlay. | `AppIcons`, `PanelView` |
| `K/ui/icons/AppIcon.kt` | The only icon composable. Fixes size from tokens and applies the tint. `fun AppIcon(icon: Int, size: Dp = Tokens.iconRow)` | Components, screens |
| `K/ui/icons/AppIcons.kt` | Semantic aliases. The drawable name appears here and nowhere else in Compose code. `val Play = R.drawable.ic_play` | `AppIcon` |

### B10. Component library (depends on: B01, B09)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/ui/components/Pressable.kt` | Modifier: no indication, opacity 0.6 while pressed | `Modifier.pressable(onClick)` | Every tappable |
| `NegaCard.kt`, `NegaRow.kt` | Card and two-line row per section 9.5 | `NegaRow(title, subtitle, onClick)` | Home, Settings |
| `NegaChip.kt`, `ChipIconButton.kt` | Metadata pill, and a 40 dp icon container |  | NavBar, rows |
| `PrimaryButton.kt`, `SecondaryButton.kt` | Filled accent and filled neutral buttons | `PrimaryButton(label, enabled, onClick)` | Screens |
| `SectionHeader.kt`, `EmptyState.kt` | Uppercase header, centred empty state |  | Lists |
| `NavBar.kt` | Brand or back left, title, action chips right | `NavBar(title, leading, actions)` | Every screen |
| `NegaSheet.kt`, `NegaDialog.kt` | Custom sheet and dialog with `scrim` |  | Editors, Settings, gate |
| `NegaTextField.kt`, `Stepper.kt`, `NegaToggle.kt`, `Segmented.kt` | Filled inputs and controls |  | Editors, Settings |
| `ConfidenceSlider.kt`, `RegionPicker.kt` | Slider 0.60 to 0.98 with a Safe/Fast hint, and a rectangle picker for search regions |  | Condition sheets |
| `ConditionChip.kt` | A readable summary of one condition with its icon | `ConditionChip(cond)` | Smart editor |
| `UndoBanner.kt`, `ErrorView.kt` | Delete undo, full-screen error fallback |  | Home, shell |
| `AdSlot.kt` | Section 8.7 spec: zero height until an ad exists | `if (!visible) return` | Home |
| `T/ui/components/ConfidenceSliderTest.kt` | Value mapping and range clamps |  | Slider |

Each component ships with a preview.

### B11. App shell and platform services (depends on: B04, B07, B10)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/AppGraph.kt` | Manual DI. Builds stores, bridge access and managers once. | `class AppGraph(app: Application)` | App, view models |
| `K/NegaApp.kt`, `K/MainActivity.kt` | Updated: install the error handler, edge-to-edge, theme, locale, nav host |  | Everything |
| `K/ui/nav/Routes.kt`, `NegaNavHost.kt` | Typed routes. Transitions use `Motion`. Back-stack rules for cold start and onboarding gating. |  | Screens |
| `K/ui/AppViewModel.kt` | Cross-screen state: theme, entitlement, consent, locale |  | Shell |
| `K/platform/diag/GlobalErrorHandler.kt`, `CrashLogStore.kt`, `DiagnosticReport.kt` | Catches uncaught errors, keeps 5 crashes, builds the shareable report without scripts or pixels |  | `ErrorView`, Settings |
| `K/platform/access/AccessibilityState.kt` | Detects whether the service is enabled and connected, as a flow | `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` | Onboarding, Home |
| `K/platform/access/SettingsIntents.kt` | The accessibility settings deep link |  | Onboarding |
| `K/platform/locale/LocaleManager.kt` | Per-app language through AppCompat |  | Settings |
| `app/src/main/AndroidManifest.xml` | Updated: the accessibility service entry, permissions, FileProvider, locale config |  | Platform |
| `T/platform/CrashLogStoreTest.kt`, `DiagnosticReportTest.kt` | Five-crash rotation, the report contains no scripts or pixels |  | Platform |

### B12. Onboarding, Home, Detection lab (depends on: B08, B10, B11) and **APK-1**

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/ui/onboarding/OnboardingScreen.kt`, `OnboardingViewModel.kt` | The steps of section 9.6 with live service-state detection |  | `AccessibilityState`, nav |
| `K/ui/onboarding/DisclosureContent.kt` | The prominent disclosure text and the Agree and Not now actions. One file, reused by Help. |  | Onboarding, Help |
| `K/ui/home/HomeScreen.kt`, `HomeViewModel.kt` | Status card, panel card, quick-scenario card, script rows, import row, run action through the bridge |  | `ScriptStore`, bridge, `AdSlot` |
| `app/src/debug/kotlin/.../lab/DetectionLabScreen.kt`, `DetectionLabViewModel.kt` | Capture, crop, run the matcher at chosen confidences, see score, box and timing, run the 100-repeat test |  | Pipeline, matchers |
| `app/src/debug/kotlin/.../lab/LabReport.kt` | Exports the lab results as a report to share |  | Share sheet |
| `app/src/debug/kotlin/.../lab/LabEntry.kt` and `app/src/release/kotlin/.../lab/LabEntry.kt` | Debug entry point, and a no-op stub so release builds contain no lab |  | Nav |
| `T/ui/home/HomeViewModelTest.kt`, `T/ui/onboarding/OnboardingViewModelTest.kt` | State logic |  | View models |

**APK-1 checkpoint.** The owner runs `debug_apk.yml` and installs the APK. Tests on the phone: onboarding and enabling the service. Opening the panel and placing a tap. Running a coordinate script. In the Detection lab: capturing a screen, cropping a target, reading the score, running the repeat test on a real app screen and measuring latency. Then exporting the report and sending it back. This proves the wedge on a real device before the editors, ads and localization are built on top of it.

### B13. Editors and import/export (depends on: B04, B10, B12)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/ui/editor/EditorScreen.kt`, `EditorViewModel.kt` | Hosts the Simple and Smart editors. Draft state, validation through `ScriptValidator`, save, delete with undo. Save is fixed at the bottom. **No ads.** | `val draft: StateFlow<Script>` | `ScriptStore`, `ScriptSession` |
| `K/ui/editor/RunSettingsCard.kt` | Loops, global timer, countdown, search rate, and the Start in X control: presets from 1 minute to 12 hours, a custom entry up to `MaxStartWaitMs`, and the 'Plug in your phone' warning above 30 minutes |  | Editor |
| `K/ui/editor/StepList.kt`, `StepSheet.kt` | Simple mode: ordered steps with up and down controls. One sheet per type: tap, swipe, curve, multi-finger, wait. |  | `Action`, components |
| `K/ui/editor/EventList.kt`, `EventSheet.kt` | Smart mode: events with priorities. A sheet holds up to 3 conditions with AND/OR, actions and a cooldown. |  | `Event`, `Expr` |
| `K/ui/editor/ConditionSheet.kt`, `ActionSheet.kt`, `CounterSheet.kt` | Per-condition and per-action forms with live validation, and the counter manager |  | Event sheet |
| `K/ui/editor/ImageTargetPicker.kt` | Lists saved crops. 'Capture new' hands off to the overlay crop tool and returns the crop. |  | `TemplateStore`, overlay |
| `K/ui/editor/TextTargetPicker.kt`, `ColorTargetPicker.kt` | Text region and string, and colour sample and tolerance. The text picker checks the language pack (B16). |  | Condition sheet |
| `K/ui/home/ImportSheet.kt` | File picker, paste box and bundle import, with the validation messages from section 11 |  | `ScriptFileGateway` |
| `K/ui/home/ExportActions.kt` | Export JSON or `.nega` through the share sheet, with the private-content warning |  | Gateway |
| `T/ui/editor/EditorViewModelTest.kt`, `EventEditorTest.kt`, `T/ui/home/ImportFlowTest.kt` | Validation, the 3-condition limit, the 12-hour wait clamp, undo, malformed import, bundle hash failure |  | View models |

### B14. Quick Scenario wizard and Record mode (depends on: B08, B13)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/ui/wizard/QuickScenarioScreen.kt`, `QuickScenarioViewModel.kt` | Three choices. Opens the panel for the crop, then saves the factory's script and offers to run it. |  | `QuickScenarioFactory`, overlay |
| `K/service/record/RecordLayer.kt` | Full-screen transparent layer that consumes a gesture, logs it, then re-dispatches it | `onTouchEvent(e)` | Panel, dispatcher |
| `K/service/record/GestureRecorder.kt` | Turns raw touch streams into tap, swipe and long-press records with timing | `fun finish(): List<RecordedGesture>` | `RecordToSteps` |
| `K/ui/editor/RecordReview.kt` | Review, trim and delete recorded steps before saving as a Simple script |  | Editor |
| `app/src/debug/kotlin/.../lab/RecordLab.kt` | Runs gate T6: record and replay 20 gestures and report position and timing error |  | Lab |
| `T/ui/wizard/QuickScenarioViewModelTest.kt`, `T/service/record/GestureRecorderTest.kt` | Wizard output, gesture classification |  | Wizard, recorder |

### B15. Settings, Help, About, Paywall screen (depends on: B10, B11, B13)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt` | Section 9.6 list. Privacy choices appears only when consent says so. Delete all local data asks for confirmation, then calls `DataWiper`. |  | `SettingsStore`, `DataWiper`, `LocaleManager` |
| `K/ui/help/HelpScreen.kt` | The honest limits as a first-class screen, plus keep-alive guidance and how matching works |  | `DisclosureContent` |
| `K/ui/about/AboutScreen.kt` | Version, licences (Inter, the OCR engine and data), privacy and support links |  | Assets |
| `K/ui/paywall/PaywallScreen.kt` | The optional upgrade page, reached from Settings or the run gate. States that Pro removes every ad. |  | `EntitlementProvider` (B17) |
| `T/ui/settings/SettingsViewModelTest.kt` | Setting persistence and the wipe flow |  | Settings |

### B16. OCR engine and language packs (depends on: B06, B13)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/platform/ocr/OcrEngine.kt` | The winning engine behind `TextRecognizer`, chosen by the lab bake-off | `class OcrEngine(...) : TextRecognizer` | Evaluator |
| `K/platform/ocr/LanguagePackManager.kt` | Tracks installed packs, starts a download, verifies it, reports state as a flow | `suspend fun receive(lang: String, net: Network): Outcome<Unit>` | Pack sheet, `FilePackStore` |
| `K/platform/ocr/PlayDeliverySource.kt` | The single pack source: Play Asset Delivery | `suspend fun fetch(spec: PackSpec): Outcome<File>` | Manager |
| `K/platform/ocr/PackVerifier.kt` | Checks the SHA-256 against the hash built into the app and discards a mismatch | `fun verify(file: File, spec: PackSpec): Boolean` | Manager |
| `K/ui/ocr/LanguagePackSheet.kt`, `LanguagePackViewModel.kt` | 'Text detection needs a one-time language pack', Wi-Fi or mobile data, and 'Receive language pack' on failure |  | Text picker, Help |
| `asset-packs/ocr_<lang>/build.gradle.kts` (one per language, 11) and `settings.gradle.kts` includes | Play Asset Delivery pack modules, one per language | `assetPack { packName.set("ocr_eng") }` | Play source |
| `scripts/pack_hashes.sh` | Computes pack hashes for the app's built-in list |  | Verifier |
| `app/src/debug/kotlin/.../lab/OcrBakeOff.kt` | Runs both candidate engines on game-style strings and reports accuracy and time. The loser stays in the debug source set only. |  | Lab |
| `T/platform/PackVerifierTest.kt`, `LanguagePackManagerTest.kt` | Hash match, corrupted file rejected, retry, failure state, with a fake source |  | Pack code |

OCR data files cannot be authored as text, so this batch needs sandbox network, and the licences of the engine and its data go into About.

### B17. Run gate, ads, consent, billing, review (depends on: B13, B15)

| File | Role and how it works | Key snippet | Connects to |
| --- | --- | --- | --- |
| `K/core/reward/RunGate.kt`, `GatePolicy.kt`, `RunLedger.kt`, `AdAvailability.kt` | The pure decision of section 8.2, its constants (including the re-exported `MaxStartWaitMs`), the ledger model and ad availability | `fun decide(...): GateDecision` | `RunSession`, gate sheet |
| `K/core/consent/ConsentPolicy.kt` | Pure decision: refresh result plus cache give the final permit | `fun canRequestAds(refresh: Outcome<Boolean>, cached: Boolean?): Boolean` | `ConsentManager` |
| `K/core/billing/EntitlementPolicy.kt` | Pure cache and verification rules, including 'a failed check never downgrades Pro' |  | `EntitlementProvider` |
| `K/core/port/AdGateway.kt`, `ConsentGateway.kt`, `BillingGateway.kt`, `ReviewGateway.kt` | Contracts for the SDK-backed services |  | Providers |
| `K/data/LedgerStore.kt` | Persists the ledger in DataStore |  | `RunGate` |
| `K/platform/ads/ConsentManager.kt` | UMP request and form on every launch. Persists the last result. | `loadAndShowConsentFormIfRequired` | `AdService` |
| `K/platform/ads/AdService.kt` | The only place the ad SDK is initialised. Waits for the permit. Pro skips it. Failure steps down the ladder. | `if (!consent.canRequestAds) return` | Services below |
| `K/platform/ads/RewardedAdService.kt`, `InterstitialAdService.kt`, `BannerAd.kt` | Preload and show a rewarded video, the fallback interstitial and the Home banner |  | `AdSlot`, gate sheet |
| `K/platform/billing/BillingManager.kt`, `EntitlementProvider.kt` | Purchase, restore, acknowledge, re-query on launch and resume, cached entitlement as a flow |  | Paywall, `AppViewModel` |
| `K/platform/review/ReviewManager.kt` | Asks for review after 3 successful runs, once |  | Run events |
| `K/ui/rungate/RunGateSheet.kt`, `RunGateViewModel.kt` | The sheet of section 8.2, always showing Get Pro beside the video |  | Home, panel, `RunGate` |
| `app/src/debug/kotlin/.../DebugDiagnostics.kt` | Consent state, reset button, test-region toggle and device hash for the phone-only developer |  | Consent |
| `app/build.gradle.kts`, `AndroidManifest.xml` | Updated: ad and billing dependencies, the AdMob app ID as a manifest placeholder defaulting to Google's sample ID |  | Release flow |
| `T/core/reward/RunGateTest.kt`, `RunLedgerTest.kt` | Every row of the gate table, the daily reset, the clock-backwards rule, the restart window |  | Gate |
| `T/core/consent/ConsentPolicyTest.kt`, `T/core/billing/EntitlementPolicyTest.kt`, `T/platform/GateIntegrationTest.kt` | Offline never overrides a saved permit, decline disables all formats, restore never downgrades, the gate with fake ads |  | Policies |

### B18. Localization (depends on: B17) and **APK-2**

| File | Role and how it works | Connects to |
| --- | --- | --- |
| `R/values-es/strings.xml`, `values-fr`, `values-de`, `values-pt`, `values-ar`, `values-hi`, `values-ja`, `values-ko`, `values-zh`, `values-iw` | Ten translations of every English key, including plurals and the Start in X strings | Every screen |
| `R/xml/locales_config.xml` | Declares the 11 supported languages for per-app language | Manifest, `LocaleManager` |
| `app/lint.xml` | Pseudo-locale and truncation checks | `check.yml` |
| `T/l10n/LocaleParityTest.kt` | Every key exists in all 11 files, with matching placeholders | Resources |
| `T/l10n/NoHardcodedStringsTest.kt` | Fails on literals in `ui/` | `ui/` |

This batch also carries the RTL pass (Arabic and Hebrew) and the large-font pass over every screen.

**APK-2 checkpoint.** The owner installs the finished UI and tests it in dark mode, at 200% font size, in Arabic or Hebrew, with a Smart script that uses image targeting, an OR condition and a counter, with a run gate flow using test ads, with a Start in X wait, and with a Record-mode capture. Screenshots are compared with a reference-tier app before the brand and website work begins. This is the second gate from the premium bar: static analysis and tests, plus a visual check.

### B19. Brand: icon and splash (depends on: B18)

| File | Role and how it works | Connects to |
| --- | --- | --- |
| `R/drawable/ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` | Adaptive and themed icon artwork, drawn as vectors | Manifest |
| `R/values/ic_launcher_background.xml` | Background colour from tokens | Adaptive icon |
| `R/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | Adaptive icon definitions | Manifest |
| `R/drawable/ic_splash.xml`, `R/values-v31/themes.xml` | Splash artwork and Android 12 splash attributes | Theme |

The agent proposes one icon concept built from the touch-point idea, and the owner approves it.

### B20. Website (depends on: B19)

Static only. The owner connects the repo to Cloudflare Pages after this batch lands: build command empty, output directory `website`, build watch path `website/`.

| File | Role and how it works | Connects to |
| --- | --- | --- |
| `website/index.html` | Landing page with real search-friendly copy in original wording, light and dark through `prefers-color-scheme` | `style.css` |
| `website/es/index.html`, `fr`, `de`, `pt`, `ar`, `hi`, `ja`, `ko`, `zh`, `he` | Localized landing pages with `hreflang` links. Arabic and Hebrew use `dir="rtl"`. | `index.html` |
| `website/privacy.html` | Privacy policy: accessibility use, screenshots processed on-device, ads and consent, purchases, language-pack downloads, no server data | Play listing |
| `website/support.html` | Support page with the support email and FAQ | Play listing |
| `website/accessibility.html` | Plain-language disclosure of the accessibility use, matching the in-app text | Play declaration |
| `website/404.html`, `robots.txt`, `sitemap.xml`, `_headers` | Crawl and security basics (Cloudflare headers file) | Cloudflare |
| `website/app-ads.txt` | AdMob verification file. OWNER INPUT: publisher ID. | AdMob |
| `website/style.css` | Tokens mirrored as CSS variables | All pages |
| `website/images/*` | SVG icon and optimised images | Pages |

### B21. Store submission docs and README (depends on: B20)

| File | Role and how it works |
| --- | --- |
| `store_submission/ASO_METADATA.md` | Title (30 characters or fewer), short description (80 or fewer), full description, keywords. Original wording, localized for the 11 languages. |
| `store_submission/DATA_BACKING.md` | Data safety answers and the justification of every permission |
| `store_submission/PLAY_DECLARATIONS.md` | Accessibility declaration text, rewarded-ad wording check, demo-video script, content-rating answers |
| `store_submission/SCREENSHOT_STRATEGY.md` | Which screens, in which order, per language |
| `docs/RELEASE_RUNBOOK.md` | Release steps, staged rollout and halt procedure, previous-version rollback, and the account-safety rules of section 10 |
| `README.md` | Final: build, test, release and the branch flow |

### B22. Release hardening (depends on: B21)

| File | Role and how it works | Key snippet |
| --- | --- | --- |
| `app/build.gradle.kts` | Release signing reads environment variables only. R8 and resource shrinking on. | `System.getenv("ANDROID_KEYSTORE_PASSWORD")` |
| `app/proguard-rules.pro` | Final keep rules, verified by the R8 step |  |
| `.github/workflows/build_and_deploy.yml` | Manual signed AAB for Play, plus the mapping file | `on: workflow_dispatch` |
| `.github/workflows/check.yml` | Adds `minifyReleaseWithR8` and the alignment check |  |
| `scripts/check_alignment.sh` | Verifies native libraries are 16 KB aligned |  |

The keystore is generated in the agent's sandbox once the alias is supplied, delivered to the owner as a single copy-pasteable base64 line, and never committed.

### S01 to S03. Verification batches (depend on: B22, run after APK-2)

| Batch | Files | What it does |
| --- | --- | --- |
| S01 Harness | `app/src/androidTest/.../TapSinkActivity.kt`, `StressHarness.kt`, `app/src/debug/.../DebugControlReceiver.kt`, `.github/workflows/emulator_stress.yml` | A test activity that counts taps, swipes and multi-finger gestures with timestamps and shows a fixed or moving image target. A debug-only receiver lets adb start and stop scripts. The workflow boots an emulator, enables the service over adb and collects a report. |
| S02 Scenarios | `StressScenarios.kt` | Tap rate accuracy at 50, 100 and 250 ms. Swipe, curve and multi-finger accuracy. Global timer and loop counts. Smart-mode event priority and cooldown. Counters. Screen-off stop. Start in X countdown, cancel and screen-off cancel. Rotation rescale. Service loss and recovery. Malformed and oversized imports. Size and density profiles. An API 30 low-memory profile. The run gate with fake ads. |
| S03 Detection | `DetectionBenchmark.kt`, `docs/DETECTION_GATES.md` | Runs T1, T3 and the synthetic part of T2 on the emulator and records results against section 12. The owner's lab report supplies T4, T5, T6, T7 and real-frame T2, and the owner's phone supplies T8. |

Emulator timing is not phone timing, so the owner's phone remains the truth for latency.

### W01 to W04. Nega Studio (depends on: B03 fixtures, B22)

Separate Cloudflare Pages project with build watch path `studio/`. No build step.

| Batch | File | Role and how it works | Connects to |
| --- | --- | --- | --- |
| W01 | `studio/index.html`, `studio/_headers`, `studio/manifest.webmanifest` | Shell with a strict content-security policy and the PWA manifest | Cloudflare, `app.js` |
| W01 | `studio/css/tokens.css`, `studio/css/app.css` | The app's tokens as CSS variables, light and dark | All UI |
| W01 | `studio/js/schema/types.js`, `validator.js`, `migrator.js` | The schema 1 shape, the same validation rules as `ScriptValidator`, and the newer-schema rejection | Editor, bundle, fixtures |
| W01 | `studio/js/bundle/zip.js`, `crc32.js`, `bundle.js` | Reads and writes the `.nega` zip, using `DecompressionStream` for deflate and `SubtleCrypto` for SHA-256 | Open and export |
| W01 | `studio/js/app.js`, `state.js` | Bootstrap, simple view routing and the in-memory script state | All modules |
| W01 | `.github/workflows/studio_check.yml`, `studio/test/schema.test.mjs`, `bundle.test.mjs`, `contract.test.mjs` | Runs the Studio tests against the shared `contract/fixtures` | CI |
| W02 | `studio/js/editor/stage.js`, `markers.js`, `regions.js`, `crop.js`, `sampler.js` | The canvas: zoom and pan, tap, swipe, curve and multi-finger markers, search regions, image crop, colour sample | Screenshots |
| W02 | `studio/js/editor/eventEditor.js`, `conditionForm.js`, `actionForm.js`, `counters.js` | The Smart editor: up to 3 conditions with AND/OR, actions, priorities, counters | Schema |
| W02 | `studio/js/editor/scriptList.js`, `studio/js/ui/components.js` | Open, save and list scripts, and the shared UI components | App shell |
| W02 | `studio/test/stage.test.mjs`, `eventEditor.test.mjs` | Coordinate transforms, editor validation | Editor |
| W03 | `studio/js/vision/gray.js`, `pyramid.js`, `ncc.js`, `color.js` | A JavaScript correlation matcher, written separately from the Kotlin one | Match lab |
| W03 | `studio/js/vision/matchLab.js` | Runs a crop against other screenshots and shows the best box and score, with an 'indicative only' note | Stage |
| W03 | `studio/test/ncc.test.mjs`, `color.test.mjs` | Matching on synthetic arrays | Vision |
| W04 | `studio/sw.js`, `studio/js/i18n/index.js`, `studio/js/i18n/<lang>.json` (11) | Offline caching and the 11 languages, RTL-safe | Whole app |
| W04 | `studio/help.html`, `studio/README.md`, `studio/test/i18n.test.mjs` | A help page, deployment notes, and a key-parity test | Docs, CI |

## 21. How the pieces connect: four end-to-end flows

**A. Starting a run (Smart script).** `HomeScreen` calls `HomeViewModel.run`, which asks `RunGateViewModel`. That calls `RunGate.decide` with the `RunLedger` (from `LedgerStore`), `EntitlementProvider`, `ConsentManager` and ad availability from `AdService`. If a video is needed, `RunGateSheet` shows it through `RewardedAdService`. Then `EngineBridge.start` reaches `NegaAccessibilityService`, which creates a `RunSession`. If the script has a Start in X wait, `RunSession` shows the countdown in `PanelView` while `OverlayController` holds the keep-screen-on flag, then runs the countdown, global timer and cap. `EventCompiler.compile` prepares the events and `EventRunner.onFrame` runs once per frame supplied by `DetectionPipeline`. The pipeline reads `ScreenshotFrameSource`. `ConditionEvaluator` calls `NccMatcher`, `ColorMatcher` and `OcrEngine`. A passing event goes to `GestureDispatcher`. `EngineState` and `DetectionLog` flow back to `PanelView` and `HudView`.

**B. Quick Scenario wizard.** `QuickScenarioScreen` opens the panel through `EngineBridge`. `CropOverlay` takes the crop from the latest frame and saves it with `TemplateStore`. `QuickScenarioFactory` builds the script, `ScriptValidator` checks it, `ScriptStore` saves it, and Home offers to run it through flow A.

**C. Text condition and the language pack.** `TextTargetPicker` asks `LanguagePackManager` whether the pack is installed. If not, `LanguagePackSheet` says 'Text detection needs a one-time language pack'. `PlayDeliverySource` fetches it, `PackVerifier` checks the hash, and `FilePackStore` keeps it. The condition then works offline through `OcrEngine`.

**D. Moving a script between devices.** In Studio, `bundle.js` exports a `.nega` file. The user moves it to the phone. `ImportSheet` passes it to `ScriptFileGateway`, `NegaBundle.unpack` verifies every hash, `ScriptValidator` checks the limits, `TemplateStore` saves the crops, `ScriptStore` upserts the script, and it appears on Home.

## 22. Size of the build

Counted from the manifest above, with auto-generated files excluded: **about 395 files** for the app, website and verification harness (of which about 90 are tests, 36 are icons, 11 are string files and 23 are website files), plus **about 50 files** for Studio. That is **about 445 files** in total, in 23 app batches (B00 to B22), 3 verification batches and 4 Studio batches, 30 batches in all.

## 23. Final checklist before the first line of code

- [ ] Repo URL, keystore alias and push route supplied
- [ ] Dependency versions pinned exactly at B00
- [ ] `LayeringTest`, `ContrastTest`, `NoHardcodedStringsTest`, `LocaleParityTest` and `ContractFixturesTest` in place
- [ ] Every permission traces to a row in section 6
- [ ] No `TODO`, `FIXME` or `throw UnimplementedError()` anywhere in generated output
- [ ] Gates T1 to T8 defined and measured, or the stated fail action applied
- [ ] The run gate tested for every row of section 8.2, including offline and declined consent
- [ ] Consent tested in the EEA, UK, Switzerland and one unregulated region, online and offline
- [ ] Data safety form matches behaviour, including ad data and pack downloads
- [ ] Rewarded-ad wording in the Play policy read at B17, and the declined-consent cap reviewed by the owner's lawyer
- [ ] Listing text is original, with no game names or cheating claims
- [ ] `main` contains only green batches. Build and release workflows run manually only.
- [ ] Signed release is built only after APK-2 has been tested on a phone
- [ ] Developer account is dedicated to Nega, with no shared devices or payment details
- [ ] `MaxStartWaitMs` set from the T8 result
- [ ] Accessibility declaration, in-app disclosure and demo video match the shipped behaviour exactly

## 24. Gap closure

| Area | Decision |
| --- | --- |
| Navigation | No exported deep links. Home is the root. Onboarding gates first launch. Predictive back enabled. |
| Connectivity | Everything works offline except ads, consent refresh, billing and the one-time pack download, and each degrades as described |
| Background work | None. Runs stop when the screen turns off. A Start in X wait keeps the screen on instead of using a background alarm. |
| Feature flags and kill switch | Not built, because there is no backend. Play staged rollout and halt are the rollback tools. |
| Dependency policy | Exact pins, weekly Dependabot, licence list in About |
| Observability | Play Console vitals plus the local crash log and user-initiated report. No analytics SDK. |
| Update prompt, bulk operations, accounts | Not part of the product |
| Tablets, foldables, landscape | Layouts use width classes. Landscape is tested at APK-2, and scripts warn on orientation mismatch. |

## 25. Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Play rejects or restricts the accessibility declaration | Strict minimal service config, prominent disclosure, demo video, honest listing. A rejection does not affect account standing, so fix and resubmit. |
| Play is the only distribution channel | The declaration, disclosure and demo video are the critical path. Allow time for resubmission before any launch date. |
| Account termination | The account-safety row in section 10: a dedicated account, declarations that match behaviour, and ad placements checked against AdMob rules. |
| Image detection is unreliable on real apps | Gates T1 to T5, and the Detection lab reaches the owner's phone at APK-1 before most UI is built on top. Demotion is the defined fail action. |
| The pure-Kotlin matcher is too slow on cheap phones | `NccMatcher` sits behind an interface. OpenCV can replace it without touching the runners. |
| OCR is heavy or inaccurate | Engine chosen by a bake-off, per-pack accuracy reported (T7), and a pack that fails is not offered |
| OCR data and engine need sandbox network and carry licences | B16 depends on the network being on. Licences go into About. |
| The OS rate-limits screenshots more tightly on some devices | Adaptive cadence, measured per device in the lab, stated in Help |
| Record mode feels laggy | Gate T6. It stays off by default if it fails. |
| Vendors kill the accessibility service | One-tap recovery and generic keep-alive guidance |
| Long Start in X waits are killed by vendor power managers, or drain the battery | Gate T8, a lower cap if needed, one-tap recovery, and the plug-in warning above 30 minutes |
| Restore or entitlement bugs annoy paying users | Re-query on launch and resume, cached state, never downgraded by a failed check, and a dedicated test |
| Consent-declined cap is legally contested | Pro is a genuine alternative, and the owner's lawyer reviews it before release |
| Ad fill is low in some markets | The ladder of rewarded, interstitial and fallback runs, so users are never locked out |
| Revenue is modest | Third-party estimates for this category are rough, and a small share of a large audience is a side income, not a business. The model is fair first and profitable at scale. |
| Competitor code licensing | Smart AutoClicker is open source under a copyleft licence. Nothing is copied. The matcher, runners and Studio are written from scratch. |
| Studio and the app drift apart | Shared fixtures in `contract/` are run by both test suites |
| Machine translations contain errors | Native review recommended before store release, not claimed to have happened |
| Play policy changes on accessibility or rewarded ads | Reviewed before each release. The disclosure text lives in one file and one web page. |
