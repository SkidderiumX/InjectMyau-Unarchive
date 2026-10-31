# Project Instructions

- Do not run tests such as launching the game or etc. unless explicitly requested.

# Workflow

1. Change the code, then compile-check with `compileJava`.
2. Build the payload (`buildDev` for dev tests, `buildBeta` for beta, `buildLatest` for release).
3. Commit after building.
4. Write the changelog entry for the build (styles below), and commit it too.

The build task prints the build name and publish path in its output (e.g. `myau build: DevBuild+6` and `published to: Dev\261031\DevBuild+6`) - use that name for changelog headers, not a guessed one.

In `Dev/dev-logs.txt`, the `[commit-hash]` is the hash of the commit that contains the code built for that build (step 3).

Changelog files are also uploaded manually by the user to the Pages repo (github.io) - I only edit them here.

# Changelog Writing Style

General: newest entries at the top, one change per line, start lines with `Added` / `Fixed` / `Updated` / `Removed`, plain sentences, details in parentheses, no bullet markers.

- `Dev/dev-logs.txt` (dev builds):

```
261031
 -DevBuild+5 [af81634]
  +HUD: hide-[category] options to hide a category's modules from the arraylist
```

  One `YYMMDD` header per day, newest day at the top. Under it, ` -DevBuild+N [commit-hash]` lines in build order, each followed by one or more `  +change` lines.

- `Beta/beta-changelog.txt` (beta builds):

```
Beta-261030+3
Fixed Motion Blur smearing the HUD/hand (now blurs the world only, before the hand renders)
```

  One `Beta-YYMMDD+N` header line per build, newest build at the top, followed by its change lines.

- Release changelog (for `buildLatest` builds, uploaded to the Pages repo):

```
Release-YYMMDD+N
Added ...
Fixed ...
```

  Same style as beta, with `Release-YYMMDD+N` headers.

# Gradle Build Commands

Run from `OpenMyau-Inject/` (e.g. `.\gradlew.bat <task>`):

- `compileJava` - compile check only, no jar/native output
- `buildDll` - rebuilds `myau_native.dll` from the current jar without bumping the version
- `buildLatest` - Latest/release channel payload, bumps the version counter
- `buildBeta` - Beta channel payload (`myau_native_beta.dll`), bumps the beta counter
- `buildDev` - dev test payload (`DevBuild+N`) published to `Dev/<YYMMDD>/`, does not touch beta/release counters
- `Devbuild` - alias for `buildDev`

