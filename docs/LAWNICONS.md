# Native Lawnicons

The existing Android app entries use the installed `app.lawnchair.lawnicons` pack.
Install Lawnicons in the launcher's Android profile, then open **settings → style →
Lawnicons → enabled**. The same toggle disables it globally. Default: disabled.
The corresponding style.toml setting is `icons.lawnicons = true` (under `[icons]`,
`lawnicons = true`). No widgets, app entries, shortcuts or artwork are added.

`AndroidAppCatalog` previously loaded `LauncherActivityInfo.getIcon(0)` and rendered
its bitmap and monochrome mask once. Home, folder previews, recents and drawer/search
all consume those shared maps. The resolver now substitutes Lawnicons artwork at
that loading point, before the existing profile badges are applied. It reads the
pack's appfilter activity mappings and grayscale package mappings; exact activities
win. It uses the foreground artwork without the adaptive background, tinted with
`Palette.accent`. Theme and toggle changes rebuild the shared icons. Pack install,
update, removal and availability callbacks invalidate the cache. Missing packs,
missing mappings, missing drawables and rendering errors use the original loading
path. The existing monochrome style still applies to original fallback icons.
Android deep shortcuts keep their own artwork (a contact, chat or website is not
its parent app); their labels and actions are untouched, as are Linux and OWL apps.
Calendar artwork uses the static mapping, rather than a new daily icon feature.

## Build and test

Branch: `feature/native-lawnicons`.

```
./scripts/fetch-proot.sh
./gradlew -PlawniconsTest :owl:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Requires JDK 25 for Gradle, JDK 17 for the owl toolchain, Android SDK 37 and the
NDK pinned by scripts/fetch-proot.sh. Android native build tools need a supported
host (the phone's ARM64 Linux environment is not an official Android NDK host).
The **Lawnicons test APK** GitHub Actions workflow installs build tools, builds
proot, runs tests and lint, and uploads the debug APK plus reports. Push this branch
to your fork or run the workflow manually. Download `omalauncher-lawnicons-test`
from that run's artifacts and extract the APK.

## Install safely

The `-PlawniconsTest` build has application ID
`dev.omalauncher.lawnicons.test` and label **Omalauncher Lawnicons Test**. It installs
alongside the existing launcher with separate app storage and task affinities.
It does not reuse the original configuration or Linux/Codex environment. Leave the
original app installed, do not clear its storage, and do not remove Linux from it.
Start the test app directly before considering it as the default Home app. Choose
**without Linux** during test setup: icon testing does not require a second rootfs.
Install Lawnicons, enable its toggle in the test launcher, and pin a few existing
apps normally to exercise the home layout. No automatic duplicate entries are made.

Verify a covered app on home, folder previews, recents, the drawer and typed search;
switch themes; disable/re-enable Lawnicons; test an uncovered app; update/uninstall
Lawnicons; check tapping, long-press menus and work/clone profile badges. Also check
the old monochrome icon setting both ways. Return to the original launcher through
Android Settings → Apps → Default apps → Home app. Uninstall only the test app when
done. Your original launcher and its private Linux data remain installed.

To update the original package in place requires the original signing key and an
acceptable version code. A different signing key cannot update that installation.
Never uninstall the original app to get around that error: doing so can delete its
private launcher and Linux data. This branch does not perform any installation.

## Validation in this session

- Six mapping tests passed using Kotlin 2.4.20 and JUnit 4.13.2 on an isolated JDK 25.
- `Lawnicons.kt` and `LawniconMappings.kt` compiled against Android API 37.0.
- The new workflow passed actionlint 1.7.12; `git diff --check` passed.
- [GitHub Actions run 39016171536](https://github.com/tigertrails/omalauncher/actions/runs/39016171536)
  built commit `5ab556a`: 121 app tests and 106 owl tests passed, none skipped;
  Android lint completed with 0 errors (54 warnings and 3 hints); APK assembly passed.
- Downloaded APK: signature verified (v2), isolated package ID and task affinities
  verified, and all arm64 native runtime libraries present. SHA-256:
  `e0f5ab852bc51d011c00b4500cf582226c8f41c02eb3229b7adfdbb3e8eaa41a`.
- Test APK saved to `/root/Downloads/omalauncher-lawnicons-test.apk`.
  [GitHub artifact](https://github.com/tigertrails/omalauncher/actions/runs/39016171536/artifacts/12094753023)
  contains the same APK (inside a ZIP). On-device rendering still needs a manual test.

## Manual assignments

Long-press an Android app in the drawer, search results or recents and choose
**choose Lawnicon**. On home, long-press its tile and tap **Lawnicon**. This opens
Lawnicons' own searchable picker, including its generic icons. Selecting an icon
enables Lawnicons and saves the resource name for that app/activity and Android
profile. The existing app icon changes on every surface and still follows the
current theme accent. Cancel leaves the previous icon unchanged. **reset Lawnicon**
in the drawer menu (or **reset icon** on home) restores automatic pack mapping,
then the original icon if unmapped. Turning the global toggle off keeps assignments
saved for next time. If an assigned resource disappears in a pack update, automatic
mapping and original-icon fallback still work. Nothing creates a new app entry.

Test APK builds now restore one stable debug signing key from this fork's
`LAWNICONS_TEST_KEYSTORE` Actions secret. Forks running this workflow must supply
their own debug keystore (JKS, alias `androiddebugkey`, password `android`) as this
base64-encoded secret. Do not commit the private key. The initial test build used
an ephemeral key; its first upgrade may require removing **only Omalauncher
Lawnicons Test**, which resets that test app's data. Leave the original Omalauncher
installed and its data intact. Subsequent builds using the stable key can update
the test app normally. Test versionCode is 16; the original package stays at 15.
