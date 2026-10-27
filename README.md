# Spendio

[![CI](https://github.com/sourabhk166/spendio-android/actions/workflows/ci.yml/badge.svg)](https://github.com/sourabhk166/spendio-android/actions/workflows/ci.yml)

A privacy-first Android expense tracker that records spending automatically from bank, card and UPI
notifications — parsed, categorized and stored entirely on the device.

## Features

- **Automatic capture** — a `NotificationListenerService` picks up transaction alerts from banking
  and UPI apps. No SMS permission and no bank credentials are ever required.
- **Tiered parsing** — a transaction detector filters out OTPs, promotions and reminders; known formats
  are handled by regex templates and unseen ones by a heuristic fallback, all behind one strategy
  interface.
- **Exactly-once ingestion** — deterministic dedupe keys and a unique database index absorb duplicate
  notification deliveries.
- **Learning categorization** — merchant rules and keywords assign categories, and every user
  correction trains future categorization. Self-transfers are detected and excluded from spend.
- **Insights** — spending stats, budgets, search, and a review queue for low-confidence captures.
- **Encrypted at rest** — SQLCipher database whose passphrase is sealed by the Android Keystore. Only a
  hash of each raw notification is kept, never the text itself.
- **Encrypted backups** — portable backup files protected with AES-256-GCM, keyed by PBKDF2-HMAC-SHA256
  from the user's passphrase.
- **Biometric app lock** and **CSV export**.
- **Reliable background capture** — handles Doze and OEM battery restrictions through a
  battery-optimization exemption flow.

## How capture works

```
Bank / UPI / card notification
  └─ NotificationListenerService      off the main thread, one failure never stops the listener
       └─ Source filter → transaction detector
            ├─ Template parser        known bank formats
            └─ Heuristic fallback     unseen formats, flagged for review
                 └─ Deduplicator → Categorizer → encrypted Room database
```

## Tech stack

Kotlin · Jetpack Compose · Coroutines & Flow · Hilt · Room + SQLCipher · DataStore ·
Jetpack Navigation · BiometricPrompt · Android Keystore

## Project structure

```
app/src/main/java/com/spendio/app/
├── capture/   notification listener, capture pipeline, background reliability
├── parser/    detector, template + heuristic parsers, deduplication, categorization
├── data/      Room database, encryption keys, backup, CSV export, repositories
├── domain/    core models (Money, Transaction, categories)
├── ui/        Compose screens and ViewModels
├── ai/        integration point for an on-device AI parsing tier
└── di/        Hilt modules
```

## Building and testing

Requires Android Studio with JDK 17 (minSdk 26, targetSdk 34).

```bash
./gradlew assembleDebug        # build a debug APK
./gradlew testDebugUnitTest    # run the unit test suite
```

The suite has 137 JVM unit tests covering the capture pipeline, parsers, deduplication,
categorization, backup encryption, export and ViewModels. Parser regression tests are modelled on
real-world Indian bank and UPI notification formats; all account numbers, references and names in
them are fictional. CI builds, tests and lints every push.

Release builds read signing credentials from a local `keystore.properties`, which is not part of the
repository; debug builds need no extra setup.

## Roadmap

- An on-device AI parsing tier (ML Kit GenAI) for formats no template covers. The integration point
  and template-learning path are already in place.
