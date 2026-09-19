# Validation: Minecraft 1.20.1 Forge

Validated on **September 19, 2026** with **Forge 47.4.23** and **Java 17**. Loader and build versions are pinned; see the [README](../README.md).

| Check | Result | Evidence |
| --- | --- | --- |
| Unit tests | 95 passed; no failures, errors or skips | [Suite counts](evidence/ports/unit-tests.json) |
| Client fixture | Passed | [Report](evidence/ports/forge-1.20.1-client.txt) |
| Dedicated server | Passed | [Report](evidence/ports/forge-1.20.1-server.txt) |
| Release build | Passed | Loader metadata, bytecode target and fixture exclusion checked |

## Runtime coverage

A disposable integrated world exercised initial synchronization, real pickup, partial merge quantities, quick move, native rendering, resize ownership and the configuration screen. It passed both with and without Sophisticated Core 1.20.1-1.5.1.2335 installed; both optional adapter mixins applied.

Fresh dedicated-server startup and restart reached 40 ticks with the mod present and saved all dimensions before clean shutdown. This checks server loading behavior, not multiplayer gameplay.

Test clients used hidden native windows, muted master volume and disabled mouse capture.

Unit coverage includes transaction inference, animation ownership, crafting flow and JEI's legacy packet discriminator. The release jar includes Java 17 classes, its refmap, both mixin configurations and MixinExtras. Sophisticated Core is excluded.

Manual gameplay, a full modpack and all optional integrations remain untested. Automated optional class loading and packet tests do not establish complete third-party crafting or storage-menu behavior. See [integration limits](../README.md#optional-integrations).

## Reproducing the checks

Run from the repository root:

```powershell
.\gradlew.bat test build
.\gradlew.bat -PclientValidation runClient
.\gradlew.bat -PserverValidation runServer
```

Client reports are written to `run-validation-client/client-validation.txt`; dedicated-server reports are written to `run-validation-server/server-validation.txt`. Add `-PsophisticatedValidation` to a client run to load Sophisticated Core 1.20.1-1.5.1.2335 and assert that both adapter mixins apply.

The runtime fixtures create disposable worlds in ignored run directories and exit automatically. Validation source sets are excluded from binary and source jars. The evidence above records the completed port validation before the project was placed at this branch's root.

The standalone branch-root layout was then rebuilt with `test build`: 95 tests passed, with no failures, errors or skips. Production and validation sources match the corresponding port sources apart from trailing blank-line cleanup; modern shared sources are included locally. The rebuilt jar was checked for bytecode level and fixture exclusion.
