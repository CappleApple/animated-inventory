# Validation: Minecraft 26.3 NeoForge

Validated on **September 20, 2026** with **Animated Inventory 1.1.1**, **NeoForge 26.3.0.7-beta** and **Java 25**. The loader matches the [official NeoForge 26.3 ModDevGradle template](https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle/blob/main/gradle.properties) checked on that date.

| Check | Result | Evidence |
| --- | --- | --- |
| Unit tests | 102 passed; no failures, errors or skips | [Suite counts](evidence/ports/unit-tests.json) |
| Packaged-JAR client | Passed | [Client assertions](evidence/ports/neoforge-26.3-client.txt) |
| Dedicated server | Passed; 40 ticks with the mod present | [Server assertions](evidence/ports/neoforge-26.3-server.txt) |
| Release build | Passed | Java 25 bytecode, loader metadata and fixture exclusion checked |

NeoForge 26.3.0.7-beta is a beta loader. This validation uses the version pinned by the official template on the date above.

## Gameplay coverage

A disposable integrated world exercised the production JAR. The fixture confirms the loaded mod class comes from `build/libs/animatedinventory-neoforge-26.3-1.1.1.jar`, while validation code remains a separate development mod.

Real screen input exercised pickup, shift-click quick move, crafting-result pickup and shift crafting. The integrated server seeded and synchronized the quick-move inventory, calculated the crafting-table recipe from four planks, and confirmed the resulting transfers and ingredient consumption. Both crafting inputs created ingredient animations through the native result-slot path.

Additional deterministic client-menu cases covered odd-stack splitting, single-item placement, partial merging, cursor and hotbar-key swaps, quick-craft preview ownership and distribution, crafting, disabled animations, reduced motion and resizing. These cases call native menu operations with the animation transaction hooks inside a running world; they are separate from the server-confirmed input checks.

Rendering checks covered item movement and opacity, screen close/reopen, hover enlargement, raised and zero-depth hover layers, slot highlights, hotbar selection and reduced-motion snapping. Stack quantities remained immediate; preview rendering preserved real slot quantities and existing animation claims.

The client used OpenGL on an NVIDIA GeForce RTX 5070 Ti. Sampled frames were inspected for inventory placement, player models, moving stacks, hover layering, tooltips and closing transitions. Test clients used hidden native windows, muted master volume and disabled mouse capture.

## Configuration and localization

The native NeoForge editor rendered all 14 sections. Live language resources resolved all 57 setting labels and descriptions, section labels and descriptions, and every allowed enum choice. The fixture edited animation speed through the native text field, rejected an out-of-range value, saved on closing the screen, then verified that NeoForge's file watcher reloaded a subsequent TOML edit.

Selecting German and reloading resources exercised the English fallback for mod text alongside German Minecraft and NeoForge controls. English is the bundled mod translation; this does not establish a complete German translation. [A captured fallback screen](evidence/ports/configuration-fallback.png) shows the resulting labels and controls.

Unit regressions cover translation completeness and the native enum translation contract, alongside the existing transaction, ownership, crafting and renderer tests.

## Limits

The dedicated-server gate checks loading and clean shutdown after 40 ticks. The integrated-server input cases provide local client/server evidence; remote multiplayer, latency and full-modpack behavior were not tested.

Sampled screenshots and automated assertions do not cover every visual combination. Vulkan, other GPUs, shader mods, custom resource packs and manual gameplay were not tested. Optional integration discovery and packet tests do not establish complete third-party inventory or crafting behavior. See [integration limits](../README.md#optional-integrations).

## Reproducing the checks

Run from this branch's root:

```powershell
.\gradlew.bat test build
.\gradlew.bat -PclientValidation -PjarValidation -PcaptureValidation runClient
.\gradlew.bat -PserverValidation -PjarValidation runServer
```

The JAR validation mode excludes production source output directories from the runtime and loads the built release JAR with the separate fixture. Client assertions go to `run-validation/validation.txt`, screenshots to `run-validation/screenshots/`, and server assertions to `run-server/server-validation.txt`.

Fixtures create disposable worlds inside ignored run directories and exit automatically. They are excluded from binary and source JARs.