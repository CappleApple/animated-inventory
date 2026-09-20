# Animated Inventory

Animated Inventory animates item pickup, placement, quick move, merging, splitting and crafting while Minecraft's real menu state updates normally. It also animates hotbar selection, item emphasis and container screen transitions, with adjustable timing and reduced motion.

This branch builds **version 1.1.1 for Minecraft 26.3 Fabric**. The original NeoForge 1.21.1 version remains on `main`.

## Installation

Minecraft **26.3**, **Fabric 0.19.5**, and **Java 25**. The required Fabric resource-loader modules are bundled; a separate Fabric API installation is not required. Install `animatedinventory-fabric-26.3-1.1.1.jar` in the client's `mods` directory. A server installation is not required.

## Configuration

Press **F8** to open the localized configuration editor, or edit `config/animatedinventory-client.json`. The JSON file uses nested objects for the same sections and keys as the original configuration. The NeoForge TOML file is not imported.

See the [configuration reference](docs/CONFIG.md) for setting names, defaults and ranges. Animations change presentation; click targets and inventory logic retain their normal positions.

Minecraft's extracted GUI state carries the animations. Closing a container retains frozen presentation state; it does not retain the screen or menu. Hovered items and their decorations render above ordinary slots when `hover.slot_hover_raise_z` is positive.

## Optional integrations

The original Sophisticated 1.21.1 renderer mixins are excluded. Custom renderers that bypass native slot rendering do not acquire item-suppression ownership.

Bundled Not Siloed discovery remains guarded through its public attachment contract when available; no matching BNS installation has been validated on this target. Recipe-viewer request observation and Inventory Particles detection are also guarded optional code. Their presence does not establish complete third-party inventory or crafting compatibility.

## Building

Use **JDK 25** to run the build. The wrapper pins **Gradle 9.7.1** and the build uses **Fabric Loom 1.18.2**. The Java toolchain compiles for Java 25.

```powershell
.\gradlew.bat test build
```

The installable jar is `build/libs/animatedinventory-fabric-26.3-1.1.1.jar`. Source jars are for development.

## Validation

```powershell
.\gradlew.bat -PclientValidation runClient
.\gradlew.bat -PserverValidation runServer
```

The opt-in runtime fixtures run hidden, mute master volume, disable mouse capture and exit automatically. They are excluded from release and source jars. See [completed validation and limits](docs/PORT_VALIDATION.md).

## License

[CC BY-NC-SA 4.0 with a Modpack/Server Exception](LICENSE). The additional permission allows use in Minecraft modpacks and servers, including monetized ones, subject to the license terms.
