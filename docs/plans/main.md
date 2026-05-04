# shutterboy — build plan

## Status: TBD — to be populated

The user dictates the phase breakdown next. This file is a placeholder so the
repo wiring (CLAUDE.md, README, subagents) has a target path that exists.

Per the global CLAUDE.md rule, when phases land here they must:

- Use numbered phases with typeable letters (`Phase A`, `Phase B`, …) — no `§`.
- Carry sub-step checkboxes (`- [ ] **A.1** …`, `- [ ] **A.2** …`).
- Tick `- [x]` AND add `shipped in commit <id>` on the phase header in the same
  commit that lands the work.
- Mark the whole plan `## Status: ✅ DONE` once every phase is ticked.

The first phase will almost certainly be **Phase 0 — bootstrap**: scaffold the
Android project (`android create`), wire the package as `com.eight87.shutterboy`,
move `docs/artwork/easter_egg_tiger.png` into `app/src/main/res/drawable-nodpi/`,
land the launcher icon (tiger cutout + Pentax SLR sitting on the right per the
agreed concept), and verify a hello-world build runs on `emulator-5554`.
