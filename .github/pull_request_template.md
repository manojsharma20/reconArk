## What and why

<!-- One concern per PR. Link the issue/ADR. -->

## Lego checklist
- [ ] New behaviour is a brick (plugin/stage/module), not a branch in the core
- [ ] Kernel and domain stay framework-free; plugins don't depend on each other
- [ ] New/changed plugins extend `PluginContractTck` + the extension point TCK
- [ ] Configuration declared in a `ConfigSpec`; secrets are reference names only
- [ ] Contracts (`contracts/`) updated with the code
- [ ] HLD/LLD/solution docs and diagrams updated; ADR added for new sockets, services or technology
- [ ] `./gradlew build`, frontend checks and Mermaid lint pass locally

## Security
- [ ] Endpoints have role checks; sensitive values masked; no secrets committed
