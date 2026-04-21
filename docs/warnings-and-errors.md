# Travel Cents — Build Warnings & Errors

Generated: 2026-04-16  
Source: `./gradlew clean compileDebugKotlin` + `./gradlew lintDebug`

---

## Summary

| Category | Count | Severity |
|---|---|---|
| Compose errors | 1 | Error |
| Kotlin compiler deprecations | 2 | Warning |
| Compose API misuse | 1 | Warning |
| Outdated dependencies (GradleDependency) | 15 | Warning |
| Newer versions available | 11 | Warning |
| Hardcoded deps (UseTomlInstead) | 12 | Warning |
| Unused resources | 2 | Warning |
| Launcher icon (monochrome missing) | 2 | Warning |
| ObsoleteSdkInt (folder config) | 1 | Warning |
| OldTargetApi | 1 | Warning |
| Typos (false positives — font cert data) | 35 | Warning |

**Total: 83 issues (1 error, 82 warnings)**

---

## Errors

### `[UnusedBoxWithConstraintsScope]` — Compose
**File:** `ui/main/current/overlays/FlightEventDetailsContent.kt:196`  
`BoxWithConstraints` is used but its `BoxWithConstraintsScope` is never referenced. Replace with a plain `Box` or use `maxWidth`/`maxHeight` from the scope.

---

## Kotlin Compiler Warnings

### `[Deprecated]` — FirestoreStartupConfig
**File:** `data/firebase/FirestoreStartupConfig.kt:8-9`  
- `FirebaseFirestoreSettings.Builder.setPersistenceEnabled()` — deprecated in Java
- `FirebaseFirestoreSettings.Builder.setCacheSizeBytes()` — deprecated in Java  

Migrate to `PersistentCacheSettings` / `MemoryCacheSettings` via `FirebaseFirestoreSettings.newBuilder().setLocalCacheSettings(...)`.

---

## Compose Warnings

### `[UseOfNonLambdaOffsetOverload]`
**File:** `ui/main/home/CurrencyConverterCard.kt:521`  
State-backed value passed to `Modifier.offset(x, y)` — should use the lambda overload `Modifier.offset { IntOffset(...) }` to avoid redundant recomposition.

---

## Unused Resources

| Resource | File |
|---|---|
| `R.drawable.landing_ai_chat` | `res/drawable-nodpi/landing_ai_chat.jpg` |
| `R.drawable.landing_planning` | `res/drawable-nodpi/landing_planning.jpg` |

These images are not referenced anywhere in code. Delete if no longer needed.

---

## Launcher Icon

**Files:** `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`  
Adaptive icons are missing a `<monochrome>` tag. Required for Android 13+ themed icons.

---

## Build Config

### `[OldTargetApi]`
**File:** `app/build.gradle.kts:21`  
`targetSdk = 35` — Android 36 is now available. Consider bumping after testing.

### `[ObsoleteSdkInt]`
**File:** `res/mipmap-anydpi-v26/`  
Folder qualifier `-v26` is redundant because `minSdkVersion` is already 26. Merge into `mipmap-anydpi/`.

---

## Dependency Updates

### `app/build.gradle.kts` — Hardcoded (should move to `libs.versions.toml`)

| Dependency | Current | Latest |
|---|---|---|
| `io.ktor:ktor-client-core` | 3.0.1 | 3.4.2 |
| `io.ktor:ktor-client-android` | 3.0.1 | 3.4.2 |
| `io.ktor:ktor-client-content-negotiation` | 3.0.1 | 3.4.2 |
| `io.ktor:ktor-serialization-kotlinx-json` | 3.0.1 | 3.4.2 |
| `sh.calvin.reorderable:reorderable` | 2.4.0 | 3.0.0 |
| `com.android.tools:desugar_jdk_libs` | 2.0.4 | 2.1.5 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.8.1 | 1.10.2 |
| `com.squareup.retrofit2:retrofit` | 2.11.0 | 3.0.0 |
| `com.squareup.retrofit2:converter-gson` | 2.11.0 | 3.0.0 |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | 5.3.2 |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | 5.3.2 |
| `compileSdk` | 35 | 36 |

> All of the above are also flagged `[UseTomlInstead]` — move them into `gradle/libs.versions.toml`.

### `gradle/libs.versions.toml` — Outdated Versions

| Dependency | Current | Latest |
|---|---|---|
| `androidx.core:core-ktx` | 1.10.1 | 1.18.0 |
| `androidx.credentials` | 1.2.0 | 1.6.0 |
| `androidx.credentials:credentials-play-services-auth` | 1.2.0 | 1.6.0 |
| `com.google.firebase:firebase-bom` | 34.9.0 | 34.12.0 |
| `com.google.firebase:firebase-firestore` | 26.1.1 | 26.2.0 |
| `com.google.android.libraries.identity.googleid:googleid` | 1.1.1 | 1.2.0 |
| `androidx.test.ext:junit` | 1.1.5 | 1.3.0 |
| `androidx.test.espresso:espresso-core` | 3.5.1 | 3.7.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.1 | 2.10.0 |
| `androidx.activity:activity-compose` | 1.8.0 | 1.13.0 |
| `org.jetbrains.kotlin.plugin.compose` | 2.2.10 | 2.3.20 |
| `androidx.compose:compose-bom` | 2024.09.00 | 2026.03.01 |
| `com.google.android.libraries.ads.mobile.sdk` | 0.24.0-beta01 | 1.0.0 |
| `androidx.compose.foundation:foundation-layout` | 1.10.3 | 1.10.6 |

---

## False Positives — Can Be Suppressed

### `[Typos]` — `res/values/font_certs.xml` (35 warnings)
Lint is misidentifying Base64-encoded certificate fingerprint data as misspelled English words (e.g., `GA1` → "Did you mean GA!"). These are not typos — they are cryptographic certificate strings.

**Fix:** Add to `lint.xml`:
```xml
<issue id="Typos">
    <ignore path="**/font_certs.xml" />
</issue>
```

---

## Recommended Fix Order

1. **Fix the error** — `FlightEventDetailsContent.kt:196` (`BoxWithConstraints` → `Box`)
2. **Suppress font cert false positives** — add `lint.xml` rule (eliminates 35 noisy warnings)
3. **Fix Compose offset** — `CurrencyConverterCard.kt:521` (lambda overload)
4. **Migrate deprecated Firestore config** — `FirestoreStartupConfig.kt`
5. **Move hardcoded deps to `libs.versions.toml`** — 12 entries in `build.gradle.kts`
6. **Delete unused drawables** — `landing_ai_chat.jpg`, `landing_planning.jpg`
7. **Dependency version bumps** — coordinate with testing (major bumps: Retrofit 3.x, OkHttp 5.x, Ktor 3.4.x)