# Contributing to Settlement Mod

Thank you for contributing to Settlement Mod! Please review the guidelines below before opening a pull request.

---

## 1. Branching Strategy & Git Workflow

- **Branch Naming:**
  - Features: `feat/phase-<X>-<short-description>` (e.g., `feat/phase-0-malthusian-balance`)
  - Fixes: `fix/<short-description>`
  - Refactors: `refactor/<short-description>`
- **Target Branch:** All feature and bugfix branches target `main`.
- **Commits:** Follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). Keep commits atomic and logically focused.

---

## 2. Hexagonal Architectural Boundary

The codebase enforces a strict separation between domain logic and game engine adapters:

1. **`com.settlement.sim.core.*` (Pure Java 25 Domain Logic):**
   - **MUST NEVER import `net.minecraft.*` or `net.neoforged.*`**.
   - Contains pure economic, demographic, geometric, and spatial domain models.
   - Must run and pass in milliseconds via JUnit 5.
   - Any external interaction must be abstracted via ports/interfaces (e.g., `SeasonPort`, `TerrainScannerPort`).

2. **`com.settlement.mc.*` (Minecraft & NeoForge Adapters):**
   - Implements ports using Minecraft engine APIs.
   - Handles persistence (`SavedData` and `DataAttachments`), rendering, networking, and entity brain hooks.

---

## 3. Critical Engine Safeguards

When writing adapter code in `com.settlement.mc.*`:
- **Server Guard:** Always guard world mutations with `if (level instanceof ServerLevel serverLevel)`.
- **Block Updates:** Pass flag `3` (`Block.UPDATE_ALL`) to `Level#setBlock(BlockPos, BlockState, int)`.
- **SavedData Persistence:** Always invoke `setDirty()` immediately after mutating `SavedData`.
- **Chunk Attachment Persistence:** Always call `chunk.setUnsaved(true)` after mutating chunk `DataAttachments` in place.
- **External Dependencies:** Do not add external mod or library dependencies without prior explicit approval. Prefer clean port abstractions.

---

## 4. Verification & Testing

Before submitting a Pull Request, verify that all checks pass:

```bash
# Execute unit tests (includes HexagonalBoundaryTest)
./gradlew test

# Verify compilation and build output
./gradlew build
```

---

## 5. Pull Request Checklist

Before submitting your PR, confirm:
- [ ] Code compiles on Java 25 without warnings or errors.
- [ ] `./gradlew test` passes cleanly.
- [ ] No `net.minecraft.*` or `net.neoforged.*` classes are imported in `com.settlement.sim.core.*`.
- [ ] World mutations and persistence updates respect engine safeguards (`ServerLevel`, `setDirty()`, `chunk.setUnsaved(true)`).
- [ ] No secrets, tokens, `.env` files, machine IPs, or absolute local filesystem paths are included in the diff.
- [ ] PR description clearly references the targeted GitHub Issue or Milestone.
