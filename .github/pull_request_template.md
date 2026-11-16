## Summary
<!-- Briefly describe what this PR implements and link the related issue (e.g., Closes #1). -->

## Phase & Tier
- [ ] Phase 0: Pure-Java Simulation Sandbox
- [ ] Phase 1: World Persistence & Terrain Scanner
- [ ] Phase 2: Room Flood-Fill & Villager Brain Hook
- [ ] Phase 3: Burgage Plots & Safe Physical Builder
- [ ] Phase 4: ACO Roads, Fringe Belts & Demographics
- [ ] Phase 5: Performance Hardening & Multi-Town Trade

## Architecture & Engine Checklist
- [ ] `com.settlement.sim.core.*` contains **zero** `net.minecraft.*` or `net.neoforged.*` imports
- [ ] All world mutations are guarded with `if (level instanceof ServerLevel serverLevel)` and pass flag `3` to `setBlock`
- [ ] Mutated `SavedData` calls `setDirty()` and mutated chunk `DataAttachments` call `chunk.setUnsaved(true)`
- [ ] `./gradlew test` passes locally
