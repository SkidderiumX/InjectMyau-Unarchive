# Project Instructions

- Do not run tests such as launching the game or etc. unless explicitly requested.

# Gradle Build Commands

Run from `OpenMyau-Inject/` (e.g. `.\gradlew.bat <task>`):

- `compileJava` - compile check only, no jar/native output
- `buildDll` - rebuilds `myau_native.dll` from the current jar without bumping the version
- `buildLatest` - Latest/release channel payload, bumps the version counter
- `buildBeta` - Beta channel payload (`myau_native_beta.dll`), bumps the beta counter
- `buildDev` - dev test payload (`DevBuild+N`) published to `Dev/<YYMMDD>/`, does not touch beta/release counters
- `Devbuild` - alias for `buildDev`
