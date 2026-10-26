# Forge 1.20.1 build

Standalone ForgeGradle 6 project for the IVR 1.20.1 / MTR 3 port. It compiles the
shared sources from the repository root:

- `../common/main/java` and `../common/main/resources`
- `../forge/src/main/java` and `../forge/src/main/resources`
- `src/main/java` (1.20.1 platform implementations: NPC platform/renderer,
  `Utilities_1_20_1`, `RenderUtilities_1_20_1`)

## Requirements

- JDK 17 (toolchain; Gradle itself may run on JDK 21)
- Network access to Maven Central, Modrinth maven, Forge maven and Sponge maven

## Build

```bash
./gradlew build --no-daemon
```

Outputs:

- `build/libs/IVR-FORGE-mc1.20.1-mtr3-<version>-all.jar` — install this one
  (JarJar includes MixinExtras 0.5.5)
- `build/libs/IVR-FORGE-mc1.20.1-mtr3-<version>.jar` — plain jar without JarJar

## Dev runs

Optional runtime dependencies (MTR and Architectury) are enabled with:

```bash
# official MTR 3.2.2
./gradlew runClient -PivrRuntimeTest

# Yomi's MTR 3.6.3
./gradlew runClient -PivrRuntimeTest -PivrMtr363
```

`-PquickplayWorld=<save folder>` passes `--quickPlaySingleplayer` for automated
client tests.

## MSD stub

`stubs/msd/java` contains compile-time-only shapes of the optional MSD catenary
classes used by `MSDRigidCatenaryRenderMixin`. The stub source set is never
packaged; the optional mixin only applies when the real MSD mod is installed.
