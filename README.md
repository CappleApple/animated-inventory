# Animated Inventory

Animated Inventory animates item pickup, placement, quick move, merging, splitting and crafting while Minecraft's real menu state updates normally. It also animates hotbar selection, item emphasis and container screen transitions, with adjustable timing and reduced motion.

This branch builds **version 1.1 for Minecraft 1.20.1 Fabric**. The original NeoForge 1.21.1 version remains on `main`.

## Installation

Minecraft **1.20.1**, **Fabric 0.19.5**, and **Java 17**. Fabric API is not required. Install `animatedinventory-fabric-1.20.1-1.1.jar` in the client's `mods` directory. A server installation is not required.

## Configuration

Press **F8** to open the configuration editor, or edit `config/animatedinventory-client.json`. The JSON file uses nested objects for the same sections and keys as the original configuration. The NeoForge TOML file is not imported.

See the [configuration reference](docs/CONFIG.md) for setting names, defaults and ranges. Animations change presentation; click targets and inventory logic retain their normal positions.

Native rendering preserves item decorations and vanilla drag-preview quantities. Stack matching uses Minecraft 1.20.1 item tags.

## Optional integrations

The original Bundled Not Siloed adapter requires NeoForge player attachments and is disabled on Fabric. Its configuration switch is retained but cannot enable that adapter. The Sophisticated NeoForge renderer mixins are excluded; no Fabric-specific adapter has been validated. Generic native slots remain eligible for animation.

Recipe-viewer request observation and Inventory Particles detection are retained as guarded optional code. Third-party viewer and storage gameplay has not been validated on this target.

The JEI 1.20.1 packet discriminator is unit-tested without consuming the outgoing buffer. This checks request recognition, not a complete JEI crafting workflow.

## Building

Use **JDK 21** to run the build. The wrapper pins **Gradle 8.14.3** and the build uses **Fabric Loom 1.11.8**. The Java toolchain compiles for Java 17.

```powershell
.\gradlew.bat test build
```

The installable jar is `build/libs/animatedinventory-fabric-1.20.1-1.1.jar`. Source jars are for development.

## Validation

```powershell
.\gradlew.bat -PclientValidation runClient
.\gradlew.bat -PserverValidation runServer
```

The opt-in runtime fixtures run hidden, mute master volume, disable mouse capture and exit automatically. They are excluded from release and source jars. See [completed validation and limits](docs/PORT_VALIDATION.md).

## License

[CC BY-NC-SA 4.0 with a Modpack/Server Exception](LICENSE). The additional permission allows use in Minecraft modpacks and servers, including monetized ones, subject to the license terms.
