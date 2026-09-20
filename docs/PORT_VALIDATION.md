# Validation: Minecraft 1.20.1 Fabric

Validated on **September 20, 2026** with Animated Inventory **1.1.1**, Fabric Loader **0.19.5** and **Java 17**. The loader is the stable version recommended by Fabric's metadata for this Minecraft release. The bundled resource loader is `0.11.12+fb82e9d777`, from Fabric API `0.92.12+1.20.1`. See the [build configuration](../build.gradle).

| Check | Result | Evidence |
| --- | --- | --- |
| Unit tests | 100 passed; no failures, errors or skips | [Suite counts](evidence/ports/unit-tests.json) |
| Production client | Passed with the built release jar | [Assertions](evidence/ports/fabric-1.20.1-production-client.txt) |
| Dedicated server | Passed | [Report](evidence/ports/fabric-1.20.1-server.txt) |
| Release artifact | Metadata, Java bytecode, nested resource loader and fixture exclusion checked | [SHA-256 and build details](evidence/ports/artifact.json) |

## In-game coverage

The production client loads the installable jar through Fabric's normal loader with development mode disabled, using Mojang's official client jar. A separate remapped validation mod creates a disposable integrated world. Animation durations are extended to one second to sample moving frames. Every inventory scenario seeds the server's inventory, waits for synchronization, performs screen input, and checks immediate client counts, synchronized client counts and the authoritative server menu.

| Behavior | Runtime checks |
| --- | --- |
| Pickup and placement | Left-click a stack, place the carried stack, and preserve exact quantities |
| Splitting and merging | Right-click pickup, single-item placement, partial merge into a nearly full stack, and swap unlike items |
| Quick move and hotbar swap | Shift-modified screen mouse input and the number-key screen input path |
| Crafting | Take the actual 2×2 oak-log recipe result; consume the ingredient and create a crafting transition |
| Drag preview | Render six items in each empty destination before release; commit six items per slot on release |
| Hover and hotbar | Observe native slot ownership, one hover highlight per frame, hovered-item scaling and the selector sprite's animated transform |
| Screen transitions | Capture the live inventory, retain its GPU image on close, render the exit image and reopen successfully |
| Disabled and reduced motion | Preserve normal inventory changes; disable visual copies or use stationary fades capped at 60 ms |
| Configuration | Open with F8; edit booleans, enums and numeric fields; reject invalid input; save, reload and restore defaults; disable the unsupported BNS control |
| Localization | Resolve all 57 setting labels, sections, tooltips and enum options; reload a selected language and verify English fallback |

The shift-click fixture supplies the shift key's polled state; it does not move or capture the operating-system mouse. The language-reload fixture supplies two German translations in the separate validation mod to prove resource-pack overrides and English fallback. Those test translations are excluded from the release jar; the mod ships English text.

Captured [inventory and drag-preview frames](evidence/ports/screenshots/12-drag-preview.png), [hover behavior](evidence/ports/screenshots/13-hover-highlight.png), [configuration controls](evidence/ports/screenshots/14-translated-config.png), [enum controls](evidence/ports/screenshots/18-translated-enum-controls.png), [language fallback](evidence/ports/screenshots/15-language-reload-fallback.png) and [unavailable integration](evidence/ports/screenshots/19-unavailable-integration.png) were inspected. Test clients ran hidden, with master volume muted and mouse capture disabled.

Fabric excluded the client-only production mod from the dedicated server. A fresh world reached `Done`, ticked and shut down cleanly. This checks server loading behavior, not remote multiplayer gameplay.

## Limits

These are automated game clients and inspected sample frames, not exhaustive manual gameplay. Third-party recipe viewers, Inventory Particles, custom renderers, full modpacks, alternate resource packs and other GPUs remain untested. The original NeoForge adapters do not establish compatibility here; see [integration limits](../README.md#optional-integrations). Only the real 2×2 crafting recipe path is exercised by this client fixture; unit tests also cover inferred crafting flows.

## Reproducing the checks

From this branch's repository root:

```powershell
.\gradlew.bat test build
.\gradlew.bat -PclientValidation -PproductionValidation runProductionClient
.\gradlew.bat -PserverValidation runServer
```

The production client writes assertions to `run-production-validation/validation.txt` and frames to `run-production-validation/screenshots/`. Its fixture jars are under `build/validation/`. `-PclientValidation runClient` runs the same fixture in Loom's development environment. The dedicated-server report is `run-validation-server/validation.txt`.

Validation source sets and the test language resource are excluded from release and source jars. Disposable worlds and temporary launch files stay in ignored build/run directories. The preserved original NeoForge 1.21.1 branch is unchanged.
