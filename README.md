# IVR — Minecraft 1.20.1 / Forge / MTR 3 port

This branch is a port of [IVR](https://github.com/Hulan1234/IVR-Mod) to
**Minecraft 1.20.1 + Forge 47.x + Minecraft Transit Railway 3**.

The upstream project targets Minecraft 1.16.5–1.19.4 (Fabric + Forge, MTR 3.2.2).
This branch keeps the shared `common/` and `forge/` sources, ports the 1.19.4 API
calls to 1.20.1, and adds a standalone ForgeGradle project in `forge-1.20.1/`.

## Download

See the [Releases](../../releases) page. The current build is
`IVR-1.2-pre-release-r7-1.20.1-forge-mtr3.jar` (all-in-one JarJar build, includes
MixinExtras 0.5.5).

Requirements:

| Dependency | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x (tested 47.4.0 / 47.4.10 / 47.4.15) |
| Minecraft Transit Railway | MTR 3: official `1.20.1-3.2.2-hotfix-2` or Yomi's `1.20.1-3.6.3` |
| Architectury API | 9.2.14+forge |

MTR 4 is not supported (the dependency range rejects it). Install the jar on both
client and server; back up worlds before installing MTR addons.

## Build

```bash
cd forge-1.20.1
./gradlew build --no-daemon
# jar: build/libs/IVR-FORGE-mc1.20.1-mtr3-<version>-all.jar
```

Java 17 toolchain is required (Gradle can run on JDK 21). The project pulls MTR
from the Modrinth maven and MixinExtras from Maven Central.

## Port fixes included

- 1.19.4 → 1.20.1 API migration (GuiGraphics screens, block state methods,
  `level()`, item tag helpers, font providers, models/textures).
- MTR 3.2.2 Forge package moves (`mtr.forge.mappings.ForgeUtilities`), PSD/APG
  door rendering without `TEMP`, refmap for MTR's overrides of vanilla methods.
- Infinite `RailwayData.load → DataCache.sync → getInstance → load` recursion on
  world load (singleplayer hang): thread-local load guard plus a deferred
  `dataCache.sync()` after the instance is registered.
- MTR 3.6.x compatibility: `DashboardScreen` fields/method removed in 3.6.x are
  read reflectively/optionally; `removeSavedRailS2C` hooks no longer depend on
  compiler-generated lambda names.
- Other MTR addons (Shanghai/Tianjin sign GUIs, YJCM, PIDS) can read station data:
  `RailwayData.getStation` no longer returns null/unnamed KSD copies when the KSD
  client mirror is empty or late; `ClientCache` falls back to MTR's mapping.
- IVR itself can read KSD stations: `KSDStation.fromMTRStation` handles MTR
  stations with null corners instead of aborting the whole sync with an NPE, and
  the initial KSD snapshot is retried while MTR data is still loading.
- MTR 3.6.x ticket barriers: `entityInside` is no longer cancelled for normal
  tickets, so MTR and addon barriers (including Metropolis/Fare-Pro) keep working;
  IVR only intercepts when the player holds an IVR/KSD ticket.
- `PlatformScreen` mixin targets the production `onClose` mapping instead of
  looking for a literal `onClose` (fixes the 3.6.x mixin apply failure).

## License

MIT, following upstream IVR. See `LICENSE`.
