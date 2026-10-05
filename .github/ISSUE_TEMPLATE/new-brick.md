---
name: New brick (plugin)
about: Propose a new connector, format, stage, comparator, renderer or platform adapter
labels: brick
---

**Extension point:** <!-- e.g. format-reader -->
**Key:** <!-- e.g. iso20022 -->
**In-process or remote:** <!-- CORE/VERIFIED in-process, or REMOTE over gRPC -->
**Configuration it needs:** <!-- properties, secret references -->
**Which services carry it, which environments enable it:**
**Acceptance:** passes `PluginContractTck` + the extension point TCK; documented in HLD §7.
