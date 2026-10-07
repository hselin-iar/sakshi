# Pinned versions (Sakshi)

Never upgrade one. Changing a version is an Integration Owner decision.

`docs/research/versions.md` (the R1 research output) did not exist when T1.1 ran. These values were taken on 2026-10-07 from the package registries named in the Source column, not from memory. AGP, Kotlin and Gradle are the combination the Flutter 3.47.6 template generated, so they are the Flutter-tested set.

| Item | Pinned | Where it is set | Source |
|---|---|---|---|
| Flutter (stable) | 3.47.6 | local SDK, `~/dev/flutter` | storage.googleapis.com/flutter_infra_release/releases/releases_macos.json |
| Dart | 3.13.5 | bundled with Flutter; `pubspec.yaml` `sdk: ^3.13.5` | same |
| JDK | Temurin 17.0.20+1 | `JAVA_HOME` (Java 17 target in Gradle) | api.adoptium.net |
| Gradle | 9.3.1 | `android/gradle/wrapper/gradle-wrapper.properties` | Flutter template |
| Android Gradle Plugin | 9.1.0 | `android/settings.gradle.kts` | Flutter template |
| Kotlin (Gradle plugin) | 2.4.0 | `android/settings.gradle.kts`, `libs.versions.toml` `kotlin` | Flutter template |
| KSP | 2.3.12 | `libs.versions.toml` `ksp` | repo1.maven.org symbol-processing-gradle-plugin |
| Room | 2.8.5 | `libs.versions.toml` `room` | dl.google.com/dl/android/maven2 |
| WorkManager | 2.12.0 | `libs.versions.toml` `work` | dl.google.com/dl/android/maven2 |
| kotlinx-coroutines | 1.11.0 | `libs.versions.toml` `coroutines` | repo1.maven.org |
| kotlinx-serialization (json + plugin) | 1.11.0 (plugin = Kotlin version) | `libs.versions.toml` `serialization` | repo1.maven.org (stable only; 1.12.0-RC skipped) |
| Robolectric (test-only) | 4.17 | `libs.versions.toml` `robolectric`, `testImplementation` | repo1.maven.org. **Deliberate exception to the "no other libraries" rule, approved at T1.4**: Room cannot open a database in a plain JVM test, and DOC 4 T1.4 requires in-memory Room tests under `testDebugUnitTest`. Never on the app's classpath. |
| JUnit | 4.13.2 | `libs.versions.toml` `junit` | repo1.maven.org |
| Pigeon | 29.0.6 | `pubspec.yaml` dev_dependencies | pub.dev API |
| flutter_riverpod | 3.4.3 | `pubspec.yaml` | pub.dev API |
| go_router | 18.0.2 | `pubspec.yaml` | pub.dev API |
| minSdk | 29 | `android/app/build.gradle.kts` | AGENTS.md |
| Android SDK platform / build-tools | 36 / 36.0.0 | local SDK, `~/dev/android-sdk` | sdkmanager |

## Pigeon 29.0.6 facts verified by running it

- Options: `PigeonOptions(dartOut: ..., kotlinOut: ..., kotlinOptions: KotlinOptions(package: ...))`.
- `@async` on a host method generates `suspend fun` in the Kotlin interface.
- Generate with `dart run pigeon --input pigeons/sakshi_api.dart` (run from the repo root).

## T1.2: full LC-4 file against Pigeon 29.0.6

- The DOC 3 file was used verbatim (extracted by script, not retyped). No option-name or generic-nullability change was needed: `List<T>` generics, `@async`, `kotlinOptions(package:)` and enum/DTO classes all generate and compile as written.
- `@async void` and `@async T` both become `suspend fun` in Kotlin; `int` becomes `Long`.
- Gradle's `testDebugUnitTest` does not treat `src/main/assets/**` or `../../tools/qindex.json` as inputs. After editing `sayings.json` run `./gradlew testDebugUnitTest --rerun` or the integrity test can report up-to-date.

## Known notes

- The Flutter template sets `android.builtInKotlin=false` and `android.newDsl=false`; Flutter 3.47.6 warns that applying the Kotlin Gradle plugin in the app will fail in a future Flutter version. Left as generated. Do not migrate without an Integration Owner decision.
- The template's `ndkVersion = flutter.ndkVersion` makes the first build download an NDK (about 1 GB). Left as generated.
- Gradle build output is redirected by the template to `<repo>/build/`.
- WorkManager 2.12.0's library manifest adds `FOREGROUND_SERVICE`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED` and a `SystemForegroundService` declaration to the merge. T1.3 removes `FOREGROUND_SERVICE` and the service (DOC 2 §2.6.1: no foreground service). The other three remain, because WorkManager reschedules after reboot and takes wake locks while a job runs. Consequence for T1.7: do not use expedited work on Android 10-11 (it falls back to a foreground service); use a plain one-time request for the on-open job.
