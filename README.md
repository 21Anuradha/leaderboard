# Real-Time Leaderboard — Android Assignment

A modular real-time leaderboard system for a mobile gaming platform. Two independent modules simulate live score events and maintain ranked leaderboard state, exposed to a Jetpack Compose UI.

## How to Run

1. Open `leaderboard` in Android Studio (Ladybug or newer).
2. Sync Gradle and run the `app` configuration on an emulator or device (API 24+).
3. The leaderboard screen starts automatically and updates every 500–2000ms.

### Run Unit Tests

```bash
./gradlew test
```

## Module Responsibilities

The project uses **three Gradle modules** with clear boundaries:

| Module | Type | Responsibility |
|--------|------|----------------|
| `:score-engine` | Kotlin JVM library | Module 1 — score generation (`ScoreGeneratorModule`, `Player`, `ScoreUpdate`) |
| `:leaderboard-core` | Kotlin JVM library | Module 2 — ranking + state (`LeaderboardModule`, `RankingEngine`, `RankedPlayer`) |
| `:app` | Android application | UI layer only (`LeaderboardScreen`, `LeaderboardViewModel`, Hilt wiring) |

### Module 1 — `:score-engine` (`ScoreGeneratorModule`)

Simulates a game backend / match engine.

- Maintains a fixed roster of 20 players
- Emits `Flow<ScoreUpdate>` at random intervals (500–2000ms)
- Picks random players and adds random positive score deltas (10–300)
- Uses a seeded `Random` for deterministic sessions (reproducible for debugging)
- **UI-agnostic**, no Android dependencies beyond coroutines

### Module 2 — `:leaderboard-core` (`LeaderboardModule` + `RankingEngine`)

Consumes score events and owns leaderboard state.

- `LeaderboardModule`: collects the generator Flow, accumulates scores, exposes `StateFlow<List<RankedPlayer>>`
- `RankingEngine`: pure ranking logic (sort DESC, competition ranking, rank-change metadata)
- Does **not** generate scores

### Presentation — `:app` (`presentation/`)

- `LeaderboardViewModel`: thin bridge — starts the module, exposes state with lifecycle-aware `stateIn`
- `LeaderboardScreen`: Compose UI with rank, username, live score, and animations

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  :app — UI Layer (Compose)                              │
│  LeaderboardScreen → LeaderboardViewModel               │
└──────────────────────────┬──────────────────────────────┘
                           │ StateFlow
┌──────────────────────────▼──────────────────────────────┐
│  :leaderboard-core — Domain Layer                       │
│  LeaderboardModule ──► RankingEngine                    │
└──────────────────────────┬──────────────────────────────┘
                           │ Flow<ScoreUpdate>
┌──────────────────────────▼──────────────────────────────┐
│  :score-engine — Data / Engine Layer                    │
│  ScoreGeneratorModule                                   │
└─────────────────────────────────────────────────────────┘
```

**Pattern:** MVVM + Clean-ish layering. Ranking is domain logic, not in ViewModel or UI.

**Why Jetpack Compose over XML?**
- Declarative UI maps naturally to reactive `StateFlow` updates
- Built-in animation APIs (`animateIntAsState`, `animateColorAsState`) for score/rank effects without custom `ValueAnimator` wiring
- `LazyColumn` with stable `key = player.id` avoids full-list recomposition
- Less boilerplate for a single-screen assignment; XML would be fine for a larger legacy codebase

## Business Rules

| Rule | Implementation |
|------|----------------|
| Sort by score DESC | `sortedByDescending` in `RankingEngine` |
| Same score → same rank | Competition ranking: 1, 1, 3, 4 |
| Real-time updates | Cold `Flow` collected on `Dispatchers.Default` |
| No flicker | `StateFlow` + stable LazyColumn keys + animated score transitions |
| Scores only increase | Delta applied as `score + delta` in `RankingEngine` |

## UI Effects

- **Score counter animation** — `animateIntAsState` smoothly counts up
- **Highlight on update** — green flash background + avatar border for ~1.2s
- **Rank change indicator** — ▲ / ▼ / ● text when rank moves
- **Live pulse dot** — indicates active match

## Performance & Lifecycle

### Avoiding UI thread blocking
- Score generation and ranking run on `Dispatchers.Default` inside `LeaderboardModule`'s scope
- ViewModel only forwards `StateFlow`; no heavy work in `init`

### Avoiding unnecessary recompositions
- `LazyColumn` items keyed by `player.id`
- `collectAsStateWithLifecycle()` stops collection when screen is not active
- `StateFlow` deduplicates identical emissions

### Avoiding memory leaks
- `viewModelScope` cancelled with ViewModel destruction
- `LeaderboardModule` uses `SupervisorJob`; animation cleanup coroutines are children
- `stop()` cancels the collection job when a match ends

### Screen rotation
- `LeaderboardModule` is `@Singleton` — scores survive rotation
- `ViewModel` + `StateFlow` restore UI instantly; no reload flicker
- `SharingStarted.WhileSubscribed(5_000)` tolerates brief subscriber gaps during config changes

### App backgrounded
- `collectAsStateWithLifecycle` pauses collection when lifecycle is below `STARTED`
- Generator Flow pauses when not collected (cold flow semantics)
- State is retained in `StateFlow` until process death

### Scaling

| Scale | Approach |
|-------|----------|
| **1K users** | Current O(n log n) sort per update is fine (~1ms). Paginate UI with `LazyColumn`. |
| **100K users** | Server-side ranking with incremental updates. Client receives top-N + self rank via WebSocket. Replace full sort with sorted map / segment tree. Batch UI updates (e.g. 100ms debounce). Virtualize list. |

## Trade-offs

| Decision | Trade-off |
|----------|-----------|
| Singleton `LeaderboardModule` | Survives rotation but holds memory for app lifetime. Production: scope to `MatchComponent`. |
| In-process Gradle modules | `:score-engine` and `:leaderboard-core` are JVM libraries with zero Android UI deps; `:app` is the only Android module. |
| Full re-sort on each update | Simple and correct for 20 players; doesn't scale to 100K without redesign. |
| Animation state in domain model (`isRecentlyUpdated`) | Couples presentation hint to domain; acceptable for assignment, would use UI-side diffing in production. |

## Code Review Simulation

Assume a mid-level dev wrote this system:

### Must Fix

1. **`LeaderboardModule.start()` called without guard** — Each `ViewModel` init could spawn duplicate collectors on the same cold Flow. **Fix:** Track `collectionJob` and return early if active.

2. **Duplicate Hilt bindings** — `@Inject` constructors *and* manual `@Provides` in an `AppModule` cause compile errors. **Fix:** Use constructor injection only (current codebase has no manual `@Provides`).

3. **Missing `android:name` on Application** — Hilt requires `@HiltAndroidApp` registered in the manifest. **Fix:** Add `LeaderboardApplication` to manifest.

### Improvement

4. **Animation flag in domain model** — `isRecentlyUpdated` leaks UI concern into `RankedPlayer`. Better: emit `LeaderboardDiff` events or track updated IDs in ViewModel.

5. **`RankingEngine` as `@Singleton`** — Stateless class doesn't need singleton scope; `@Inject` without `@Singleton` reduces misuse.

6. **Hardcoded player roster** — Extract to a `PlayerRepository` interface so the generator can be swapped for a real WebSocket source.

### Tech Debt

7. **No match session lifecycle** — `stop()` exists but isn't wired from UI. Production needs `MatchScope` with explicit start/end.

8. **Session seed uses `System.currentTimeMillis()`** — Non-reproducible across app restarts. Pass explicit match ID as seed.

9. **No error handling on Flow collection** — Add `catch` operator and expose error state to UI for production resilience.

## 7-Day Ship Plan

### Non-negotiable (Days 1–4)
- Correct real-time ranking with competition rules
- Module separation (generator ≠ leaderboard ≠ UI)
- Unit tests for ranking logic
- Lifecycle-safe state (rotation, background)

### Defer / Cut (Days 5–7 if needed)
- Podium UI polish
- Anti-cheat
- Multi-match history
- CI/detekt setup

### Work Division

| Person | Tasks |
|--------|-------|
| **Junior** | Compose list items, theme/colors, avatar component, loading state |
| **Mid-level** | `ScoreGeneratorModule`, `LeaderboardModule` wiring, Hilt setup, instrumented smoke test |
| **Lead (me)** | `RankingEngine` design, architecture review, performance/lifecycle strategy, README, test strategy, ship/no-ship decisions |

## What I'd Improve With More Time

- WebSocket-backed `ScoreUpdate` source with Turbine integration tests
- `MatchScope` Hilt component with proper cleanup
- ktlint + detekt in CI
- Anti-cheat: server-authoritative scores, rate limiting, delta validation
- UI diffing with `LazyListState.animateScrollToItem` for rank movement

## Project Structure

```
leaderboard/
├── score-engine/          # Module 1 — JVM library, no Android deps
│   └── src/main/kotlin/com/example/leaderboard/data/
│       ├── Player.kt
│       ├── ScoreUpdate.kt
│       └── ScoreGeneratorModule.kt
├── leaderboard-core/      # Module 2 — JVM library, consumes score-engine
│   └── src/main/kotlin/com/example/leaderboard/domain/
│       ├── RankedPlayer.kt
│       ├── RankingEngine.kt
│       └── LeaderboardModule.kt
└── app/                   # Android UI + Hilt
    └── src/main/java/com/example/leaderboard/
        ├── presentation/
        │   ├── LeaderboardViewModel.kt
        │   └── LeaderboardScreen.kt
        ├── ui/theme/
        ├── MainActivity.kt
        └── LeaderboardApplication.kt
```
