# Validation: Minecraft 1.20.1 Forge

Validated on **September 20, 2026** with **Animated Inventory 1.1.1**, **Forge 47.4.10**, and **Java 17**. Forge 47.4.10 was the [official recommended 1.20.1 release](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) at validation time.

| Check | Result | Evidence |
| --- | --- | --- |
| Unit tests | 97 passed; no failures, errors or skips | [Suite counts](evidence/ports/unit-tests.json) |
| Packaged client | Passed against the installable release jar | [Client report](evidence/ports/forge-1.20.1-client.txt) |
| Packaged dedicated server | Fresh startup and restart passed | [Server report](evidence/ports/forge-1.20.1-server.txt) |
| Language resolution | 169 keys resolve in English and French fallback | [English](evidence/ports/forge-1.20.1/language-en_us.txt), [French fallback](evidence/ports/forge-1.20.1/language-fr_fr-fallback.txt) |
| Configuration rendering | All 15 pages captured and inspected at 960×600, GUI scale 2 | [Captures](evidence/ports/forge-1.20.1/) |
| Release packaging | Java 17 bytecode, Forge minimum, version, bundled MixinExtras and fixture exclusion checked | [Artifact checks](evidence/ports/forge-1.20.1-artifact.json) |

## Runtime coverage

The client runs through the official Forge `forgeclient` launch target. The installable jar supplies production classes; a separate validation mod supplies input, assertions and screenshots. The fixture verifies the loaded class source and Forge version. This is automated in-game testing, not manual gameplay.

A real integrated server opens native menus and synchronizes contents. The first pickup uses the screen's actual mouse press/release methods. Subsequent inventory actions enter the native screen click handler, including the production mixin and the normal client prediction and server packet paths. The suite checks client quantities immediately and authoritative server quantities after processing, waiting up to 100 server ticks for native packets to arrive.

| Area | Exercised behavior |
| --- | --- |
| Inventory transactions | Pickup, partial merge with cursor remainder, single/full placement, right-click split, dissimilar-stack swap, number-key slot swap, quick move, pickup-all, drag distribution, throw and creative clone |
| Crafting | Actual recipe-computed 2×2 log-to-planks output, normal and shift crafting, and 3×3 wheat-to-bread shift crafting; ingredient consumption and resulting server quantities |
| Equipment | Quick move into the native helmet slot and server equipment confirmation |
| Rendering | Initial synchronization without arrivals, cursor/slot and craft transitions, hover emphasis, highlight draw order, native hotbar selector movement, captured opening/closing render targets and release after closing |
| Lifecycle | Disable preserves normal menu input, re-enable, reduced-motion stationary fades and snapped hotbar, resize ownership reset, screen close and resource reload |
| Configuration | Every page, translated button text, invalid numeric input blocking save/navigation, valid input restoring them, Done persisting TOML, Forge reload reading disk edits, and Cancel discarding a draft |

All 57 setting labels and tooltips, section names, selectable enum choices, navigation and validation messages resolve through the actual client language manager. French selection resolves vanilla controls in French and uses the bundled English strings for mod-specific text. A French translation is not bundled. Captures include [general settings](evidence/ports/forge-1.20.1/config-00.png), [enum choices](evidence/ports/forge-1.20.1/config-01.png), [help text](evidence/ports/forge-1.20.1/config-tooltip.png), [unavailable adapter](evidence/ports/forge-1.20.1/config-13.png), and [French fallback](evidence/ports/forge-1.20.1/config-french-fallback.png).

The official dedicated server loads the same installable jar and reaches 40 ticks before saving all dimensions and exiting. Both a fresh world and a restart pass. The development server gate also passes. These checks establish server loading behavior, not remote multiplayer gameplay.

Clients use hidden native windows, muted master volume and disabled mouse capture. The fixture checks hidden-window and released-mouse state while running. Screenshots come from Minecraft's framebuffer.

## Limits

The current production suite uses vanilla inventories and recipes. Full modpacks, remote multiplayer, every item renderer and every optional integration combination have not been tested. The Sophisticated Core adapter previously passed a class-loading check; actual Sophisticated storage-menu workflows are not covered here. JEI's legacy packet discriminator is unit-tested without consuming its outgoing buffer, but this does not establish a complete recipe-viewer workflow. Bundled Not Siloed's NeoForge attachment API is unavailable on Forge. See [optional integrations](../README.md#optional-integrations).

## Reproducing the checks

Use JDK 17 from the repository root:

```powershell
.\gradlew.bat test build validationJar downloadAssets
.\gradlew.bat -PserverValidation runServer
```

The development client fixture is also available through `.\gradlew.bat -PclientValidation runClient`. The documented production evidence uses the separate launcher below instead.

On Windows, download the official Forge 1.20.1-47.4.10 installer and save it as `logs/forge-1.20.1-47.4.10-installer.jar`. Prepare an empty client installation:

```powershell
New-Item -ItemType Directory -Force run-production-client
'{"profiles":{}}' | Set-Content run-production-client/launcher_profiles.json
java -jar logs/forge-1.20.1-47.4.10-installer.jar --installClient run-production-client
python tools/launch-production-client.py
```

The Python launcher reads the official installed Forge and Mojang version metadata, verifies downloaded library hashes, uses assets from the default Gradle cache, copies the built release and validation jars, and checks the final report. Set `JAVA_HOME` to JDK 17. It creates a disposable integrated world and exits automatically. Reports and screenshots are written beneath `run-production-client`.

For packaged server validation:

```powershell
New-Item -ItemType Directory -Force run-production-server
java -jar logs/forge-1.20.1-47.4.10-installer.jar --installServer run-production-server
python tools/launch-production-server.py
python tools/launch-production-server.py
```

The second invocation tests a restart. The server fixture accepts the Minecraft EULA for this disposable test installation and binds to localhost port 25912. Its report is `run-production-server/server-validation.txt`.

Run directories and logs are ignored. Validation and production-validation source sets are excluded from release and source jars. The build also checks that neither fixture classes nor its refmap leak into either artifact. The `-validation.jar` is a local test fixture, not a release artifact.
