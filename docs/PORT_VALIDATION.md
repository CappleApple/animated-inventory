# Validation: Minecraft 1.21.1 Fabric

Validated on **September 19, 2026** with **Fabric 0.19.5** and **Java 21**. Loader and build versions are pinned; see the [README](../README.md).

| Check | Result | Evidence |
| --- | --- | --- |
| Unit tests | 92 passed; no failures, errors or skips | [Suite counts](evidence/ports/unit-tests.json) |
| Client fixture | Passed | [Report](evidence/ports/fabric-1.21.1-client.txt) |
| Dedicated server | Passed | [Report](evidence/ports/fabric-1.21.1-server.txt) |
| Release build | Passed | Loader metadata, bytecode target and fixture exclusion checked |

## Runtime coverage

A synthetic chest exercised native slot ownership, inferred movement, detached item opacity, the GPU compositor and the configuration editor. This fixture did not enter a world and does not establish gameplay or network synchronization behavior.

Fabric excluded the client-only production mod. A fresh dedicated server reached `Done`, ticked once and shut down cleanly. This checks server loading behavior, not multiplayer gameplay.

Test clients used hidden native windows, muted master volume and disabled mouse capture.

Unit coverage includes transaction inference, animation ownership and crafting flow.

Manual gameplay, a full modpack and all optional integrations remain untested. Automated optional class loading and packet tests do not establish complete third-party crafting or storage-menu behavior. See [integration limits](../README.md#optional-integrations).

## Reproducing the checks

Run from the repository root:

```powershell
.\gradlew.bat test build
.\gradlew.bat -PclientValidation runClient
.\gradlew.bat -PserverValidation runServer
```

Client reports are written to `run-validation/validation.txt`; dedicated-server reports are written to `run-validation-server/validation.txt`.

The runtime fixtures create disposable worlds in ignored run directories and exit automatically. Validation source sets are excluded from binary and source jars. The evidence above records the completed port validation before the project was placed at this branch's root.

The standalone branch-root layout was then rebuilt with `test build`: 92 tests passed, with no failures, errors or skips. Production and validation sources match the corresponding port sources apart from trailing blank-line cleanup; modern shared sources are included locally. The rebuilt jar was checked for bytecode level and fixture exclusion.
