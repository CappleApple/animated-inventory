# Animated Inventory

Animated Inventory animates item pickup, placement, quick move, merging, splitting and crafting while Minecraft's real menu state updates normally. It also animates hotbar selection, item emphasis and container screen transitions, with adjustable timing and reduced motion.

This branch builds **version 1.1 for Minecraft 1.20.1 Forge**. The original NeoForge 1.21.1 version remains on `main`.

## Installation

Minecraft **1.20.1**, **Forge 47.4.23**, and **Java 17**. Install `animatedinventory-forge-1.20.1-1.1.jar` in the client's `mods` directory. A server installation is not required.

## Configuration

Open **Mods → Animated Inventory → Config**, or edit `config/animatedinventory-client.toml`.

See the [configuration reference](docs/CONFIG.md) for setting names, defaults and ranges. Animations change presentation; click targets and inventory logic retain their normal positions.

Stack matching uses Minecraft 1.20.1 item tags. Item rendering uses the native GUI calls and item-count decorations. MixinExtras is included in the installable jar.

## Optional integrations

The Sophisticated adapter targets Sophisticated Core **1.20.1-1.5.1.2335**. Client startup and the inventory fixture passed with Core installed and both adapter mixins applied; actual Sophisticated storage-menu gameplay remains untested.

Bundled Not Siloed's NeoForge attachment adapter is unavailable on Forge; its configuration switch has no effect here. Recipe-viewer request observation and Inventory Particles detection remain guarded optional code and require version-specific gameplay testing.

The JEI 1.20.1 packet discriminator is unit-tested without consuming the outgoing buffer. This checks request recognition, not a complete JEI crafting workflow.

## Building

Use **JDK 17** to run the build. The wrapper pins **Gradle 8.8** and the build uses **ForgeGradle 6.0.54**. The Java toolchain compiles for Java 17.

```powershell
.\gradlew.bat test build
```

The installable jar is `build/libs/animatedinventory-forge-1.20.1-1.1.jar`. Source jars are for development. The `-slim.jar` omits the bundled MixinExtras dependency and is not the normal installation artifact.

## Validation

```powershell
.\gradlew.bat -PclientValidation runClient
.\gradlew.bat -PserverValidation runServer
```

The opt-in runtime fixtures run hidden, mute master volume, disable mouse capture and exit automatically. They are excluded from release and source jars. See [completed validation and limits](docs/PORT_VALIDATION.md).

## License

[CC BY-NC-SA 4.0 with a Modpack/Server Exception](LICENSE). The additional permission allows use in Minecraft modpacks and servers, including monetized ones, subject to the license terms.
