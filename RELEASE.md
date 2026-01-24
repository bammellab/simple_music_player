# Release Build Instructions

## Prerequisites

1. **Signing Configuration**: The app uses signing properties from `local.properties`:
   ```properties
   RELEASE_STORE_PASSWORD=your_store_password
   RELEASE_KEY_ALIAS=your_key_alias
   RELEASE_KEY_PASSWORD=your_key_password
   ```

   The keystore file is configured in `app/build.gradle.kts`.

## Build Commands

### Debug APK
```bash
./gradlew.bat :app:assembleDebug
```

### Release APK
```bash
./gradlew.bat :app:assembleRelease
```

### Release AAB (for Google Play)
```bash
./gradlew.bat :app:bundleRelease
```

## Output Locations

| Build Type   | Output Path                                     |
|--------------|-------------------------------------------------|
| Debug APK    | `app/build/outputs/apk/debug/`                  |
| Release APK  | `app/build/outputs/apk/release/`                |
| Release AAB  | `app/build/outputs/bundle/release/`             |
| Mapping File | `app/build/outputs/mapping/release/mapping.txt` |

## R8 Obfuscation

Release builds use R8 for code shrinking and obfuscation:
- `isMinifyEnabled = true` - Enables R8 code shrinking and obfuscation
- `isShrinkResources = true` - Removes unused resources
- ProGuard rules: `app/proguard-rules.pro`

## Crash Report Deobfuscation

With AGP 8.13.2+, the mapping file is automatically embedded in the AAB under `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`.

When uploading to Google Play:
- Google Play extracts the mapping file automatically
- Crash reports in Play Console are deobfuscated automatically
- No manual mapping file upload required

For local debugging of obfuscated crash logs, use the mapping file at:
`app/build/outputs/mapping/release/mapping.txt`

## Verifying Release Build Configuration

After building a release AAB, run the release build verification tests:

```bash
./gradlew.bat :app:bundleRelease
./gradlew.bat :app:testDebugUnitTest --tests "com.bammellab.musicplayer.release.ReleaseBuildTest"
```

These tests verify:
- The mapping file exists in the AAB at the correct location
- The mapping file contains valid R8 obfuscation data
- The mapping file format is compatible with Google Play Console
- The bundle contains minified DEX files

## Version Management

Version numbers are configured in `app/build.gradle.kts`:
```kotlin
versionCode = 1
versionName = "1.0"
```

Update these values before creating a new release.
