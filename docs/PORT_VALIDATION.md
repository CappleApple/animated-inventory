# Validation: Minecraft 26.3 Fabric

Validated on **September 19, 2026** with **Fabric 0.19.5** and **Java 25**. Loader and build versions are pinned; see the [README](../README.md).

| Check | Result | Evidence |
| --- | --- | --- |
| Unit tests | 100 passed; no failures, errors or skips | [Suite counts](evidence/ports/unit-tests.json) |
| Client fixture | Passed | [Report](evidence/ports/fabric-26.3-client.txt) |
| Dedicated server | Passed | [Report](evidence/ports/fabric-26.3-server.txt) |
| Release build | Passed | Loader metadata, bytecode target and fixture exclusion checked |

## Runtime coverage

A disposable integrated world exercised a real screen pickup, immediate cursor counts, animation creation, native slot ownership, rendered movement, partial merge quantities, and screen close/reopen. The partial merge assertion directly mutates the local menu to avoid racing server reconciliation; it is not a network synchronization test.

Fabric excluded the client-only production mod. The dedicated server started, reached 40 ticks and shut down cleanly. This checks server loading behavior, not multiplayer gameplay.

Test clients used hidden native windows, muted master volume and disabled mouse capture.

Unit coverage includes transaction inference, animation ownership, crafting flow, premultiplied opacity, extracted render-state replay, native preview ownership, and cleanup after nested or failed elevated-slot extraction.

The client used OpenGL on an NVIDIA GeForce RTX 5070 Ti. Sampled frames from both modern Minecraft versions were inspected for inventory placement, player models, moving stacks, opacity and close-screen replay. This was not exhaustive visual testing. Vulkan, other GPUs, shader mods and custom resource packs were not tested.

Manual gameplay, a full modpack and all optional integrations remain untested. Automated optional class loading and packet tests do not establish complete third-party crafting or storage-menu behavior. See [integration limits](../README.md#optional-integrations).

## Reproducing the checks

Run from the repository root:

```powershell
.\gradlew.bat test build
.\gradlew.bat -PclientValidation runClient
.\gradlew.bat -PserverValidation runServer
```

Client reports are written to `run-validation/validation.txt`; dedicated-server reports are written to `run-server/server-validation.txt`. Add `-PcaptureValidation` to a client run to save frames in `run-validation/screenshots/`.

The runtime fixtures create disposable worlds in ignored run directories and exit automatically. Validation source sets are excluded from binary and source jars. The evidence above records the completed port validation before the project was placed at this branch's root.

The standalone branch-root layout was then rebuilt with `test build`: 100 tests passed, with no failures, errors or skips. Production and validation sources match the corresponding port sources apart from trailing blank-line cleanup; modern shared sources are included locally. The rebuilt jar was checked for bytecode level and fixture exclusion.
