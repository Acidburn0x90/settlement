# AGENTS.md — Settlement Mod (`settlement`)

## Overview
A self-growing villager settlement simulation mod for **Minecraft 26.1+ (NeoForge, Java 25, unobfuscated)** grounded in Conzenian urban morphology, Malthusian/satisficing agrarian economics, and utility-scored villager AI.

Primary reference: [Minecraft Settlement Mod Research Roadmap.md](Minecraft%20Settlement%20Mod%20Research%20Roadmap.md).

## Locked-In Architectural Decisions
1. **Loader & Target:** NeoForge (`26.1.0.19-beta+`) on Minecraft `26.1` with Java `25` and ModDevGradle (`2.0.148+`).
2. **Hexagonal Package Split:** Single Gradle project divided into two strict boundaries:
   - `com.settlement.sim.core.*` — pure Java 25 domain logic. **Must never import `net.minecraft.*` or `net.neoforged.*`** so all simulation rules remain unit-testable in milliseconds via JUnit 5.
   - `com.settlement.mc.*` — thin Minecraft/NeoForge adapters, persistence (`SavedData` + `DataAttachments`), `Villager` `Brain` hooks, and voxel world primitives.
3. **Autonomy:** Autonomous AI town planner with optional player charter markers.
4. **Construction:** Physical builders placing blocks bottom-up, preceded by a foliage/liquid clear pass and protected by a stuck-recovery watchdog.
5. **Unloaded Chunks:** Elapsed-time offscreen catch-up math triggered on chunk reload (optionally paired with Town Hall chunk loading).
6. **Villager Entity Base:** Hook vanilla `Villager` (`Brain` + `PoiManager` + `BehaviorControl`) in Phases 2–3.
7. **Style & Scale:** Single biome-adaptive timber/stone palette and single self-sustaining town through Phase 4; multi-town trade graph in Phase 5.
8. **Player Loop:** Ambient survival integration via Minecraft 26.1 data-driven villager trades (`data/settlement/villager_trade/`) synced to town scarcity.

## Five-Tier Top-Down Package Layout
- **Tier 1 (Entry & Registration):** `com.settlement.mc.SettlementMod`
- **Tier 2 (Macro World Network):** `com.settlement.sim.core.world` + `com.settlement.mc.persistence`
- **Tier 3 (Town Subsystems):** `com.settlement.sim.core.town`
- **Tier 4 (Citizen AI Bridge):** `com.settlement.sim.core.agent` + `com.settlement.mc.ai`
- **Tier 5 (Engine & Voxel Primitives):** `com.settlement.mc.adapter`

## Critical Engine Safeguards
- Always guard world mutations with `if (level instanceof ServerLevel serverLevel)` and pass flag `3` (`Block.UPDATE_ALL`) to `Level#setBlock(BlockPos, BlockState, int)`.
- Always call `setDirty()` after mutating `SavedData` and `chunk.setUnsaved(true)` after mutating chunk `DataAttachments` in place.
- Enforce `sim.core` purity: never leak Minecraft classes into `com.settlement.sim.core`.

## Collaboration & Agent Role
- **User-Led Implementation:** The developer writes all domain and mod code themselves.
- **Agent Role:** Strictly **guidance, architecture review, performance/TPS analysis, and debugging insight**. Do not write non-boilerplate feature code unless the developer explicitly asks.
- **GitHub Workflow:** Work via feature branches (`feat/phase-0-...`), Pull Requests targeting `main`, and GitHub Issues/Milestones.

## Build & Verification Commands
- **Unit Tests (Pure Java):** `./gradlew test`
- **Full Build:** `./gradlew build`
- **Run Client:** `./gradlew runClient`
- **Run Dedicated Server:** `./gradlew runServer`
