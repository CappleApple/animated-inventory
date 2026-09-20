# Validation: Minecraft 26.2 Fabric

Validated on **September 20, 2026** with **Animated Inventory 1.1.1**, **Fabric Loader 0.19.5** and **Java 25**.

The loader matches the recommendation returned by the [official Fabric CLI](https://fabricmc.net/develop/cli/) for this Minecraft version. The bundled Fabric Resource Loader v1 **2.0.13+9edec1269e** and Fabric API Base **2.0.4+ece063239e** come from its recommended **Fabric API 0.161.0+26.2** release. A separate Fabric API installation is not required.

| Check | Result | Evidence |
| --- | --- | --- |
| Unit tests | 105 passed; no failures, errors or skips | [Suite counts](evidence/ports/unit-tests.json) |
| Packaged-JAR client | Passed | [Client assertions](evidence/ports/fabric-26.2-client.txt) |
| Dedicated server | Passed; client-only mod excluded | [Server assertions](evidence/ports/fabric-26.2-server.txt) |
| Release build | Passed | Java 25 bytecode, nested dependencies, language resources and fixture exclusion checked |

## Gameplay coverage

A disposable integrated world ran with Fabric's production loader, the official Minecraft client JAR, the installable mod JAR and a separate validation mod. The fixture confirmed that production classes came from `animatedinventory-fabric-26.2-1.1.1.jar`. The resource-loader modules were discovered inside the release JAR; no separate Fabric API was installed.

Real screen input exercised pickup, shift-click quick move, crafting-result pickup and shift crafting. The integrated server seeded and synchronized the quick-move inventory, computed the crafting-table recipe from four planks, and confirmed the resulting transfers and ingredient consumption. Both crafting inputs created ingredient animations through the native result-slot path.

Additional deterministic client-menu cases covered odd-stack splitting, single-item placement, partial merging, cursor and hotbar-key swaps, quick-craft preview ownership and distribution, crafting, disabled animations, reduced motion and resizing. These use native menu operations and animation hooks inside the running world; they are separate from the server-confirmed input checks.

Rendering checks covered item movement and opacity, screen close/reopen, hover enlargement, raised and zero-depth hover layers, slot highlights, hotbar selection and reduced-motion snapping. Preview rendering preserved real quantities and existing animation claims.

## Configuration and localization

F8 opened the actual editor. Live resources resolved all 57 setting labels, section labels, descriptions and enum choices. Every configuration page was rendered and checked for controls inside the viewport. The fixture clicked boolean and choice buttons, saved and reloaded numeric edits, rejected `NaN` without closing the editor, reset a page, and verified defaults after missing or invalid JSON entries.

Switching to French and reloading resources verified translated native controls alongside the mod's English fallback. English is the supplied mod translation; a complete French translation is not included. [A captured fallback screen](evidence/ports/configuration-fallback.png) shows the resulting labels and controls.

Unit regressions cover configuration persistence, invalid and missing values, declaration order and translation completeness, alongside the existing transaction, ownership, crafting and renderer tests.

## Limits

The client used OpenGL on an NVIDIA GeForce RTX 5070 Ti. Sampled inventory and configuration frames were visually inspected. Hidden test windows used muted audio and disabled mouse capture.

The dedicated-server gate reached 40 ticks and shut down cleanly with Fabric excluding the client-only mod. Integrated-server input cases provide local client/server evidence. Remote multiplayer, latency, full modpacks and optional third-party integrations were not tested. Vulkan, other GPUs, shader mods, custom resource packs, manual gameplay and every visual setting combination remain untested. See [integration limits](../README.md#optional-integrations).

## Reproducing the development checks

Run from this branch's root:

```powershell
.\gradlew.bat test build
.\gradlew.bat -PclientValidation -PcaptureValidation runClient
.\gradlew.bat -PserverValidation runServer
```

These commands run the same gameplay and configuration assertions using Loom's development launch. The packaged-JAR check above additionally used Fabric's normal `KnotClient` entry point with development mode disabled and the built JARs in a disposable `mods` directory.

Client reports are written to `run-validation/validation.txt`, screenshots to `run-validation/screenshots/`, and server reports to `run-server/server-validation.txt`. Fixtures create disposable worlds in ignored directories and exit automatically. They are excluded from binary and source JARs.
