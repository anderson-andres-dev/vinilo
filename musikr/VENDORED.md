# Vendored module

This module (`:musikr`) is vendored from the [Auxio](https://github.com/OxygenCobalt/Auxio)
project, not written by Vinilo.

- Source: https://github.com/OxygenCobalt/Auxio
- Branch: `dev`
- Commit vendored: `d47d78664c7f74dc7be466d6569fba5158a600a4` (2026-07-08)
- Path: `musikr/`
- License: GNU General Public License v3.0 (see [`/LICENSE`](../LICENSE))

`src/main/cpp/taglib` is [TagLib](https://github.com/taglib/taglib), vendored at tag
`ee1931b` (the same revision Auxio pins via git submodule), copied without the `.git`
history.

## Changes made to adapt it into Vinilo

- `build.gradle` version references were switched from Auxio's root `ext {}` block
  (`kotlin_version`, `room_version`, `hilt_version`, `min_sdk`, `target_sdk`,
  `ndk_version`, ...) to literal versions matching Vinilo's own
  `gradle/libs.versions.toml`.
- Dropped the `com.diffplug.spotless` and `org.jetbrains.dokka` plugins — Vinilo
  doesn't run Auxio's formatting/doc pipeline for this module.
- Kotlin/Java package (`org.oxycblt.musikr`) and all source code are otherwise
  unmodified from upstream.

## Updating this vendored copy later

There is no submodule/subtree link back to Auxio — updates must be pulled manually:
re-fetch `musikr/` from a newer Auxio `dev` commit, diff against this copy, and
reapply the adaptations above.
