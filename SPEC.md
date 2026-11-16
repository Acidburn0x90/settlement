# Specification — Settlement Mod (`settlement`)

> **Target:** Minecraft `26.1+` · NeoForge `26.1.0.19-beta+` · Java `25` · Gradle `9.2.0`
> **Primary Research Blueprint:** [Minecraft Settlement Mod Research Roadmap.md](Minecraft%20Settlement%20Mod%20Research%20Roadmap.md)

---

## 1. Objective & Non-Goals

### Objective
Build an autonomous, self-growing villager settlement simulation mod grounded in **Conzenian urban morphology**, **Malthusian/Chayanovian agrarian economics**, **Christaller Central Place Theory**, and **utility-scored villager AI**. Custom settlements spawn as tiny **Founder Hamlets** alongside untouched vanilla villages and evolve organically through time, trade, and terrain constraints.

### Non-Goals (Out of Scope)
- **No Vanilla Village Overwrites:** Vanilla villages, structures, and villagers remain 100% untouched.
- **No Extinction / Total Collapse:** Settlements never die to zero population; deficits or player raiding of surplus stall growth at a subsistence equilibrium (`seedUnits` floor).
- **No Micromanagement Mayor Menus:** No RimWorld/MineColonies-style mandatory zoning UI; player influence happens through indirect economic/physical nudges (Granary deposits/withdrawals, trades, roads, optional Charter markers).
- **No Multi-Culture Packs (Phases 0–4):** Single biome-adaptive vanilla-plus timber/stone palette through Phase 4.
- **No Reinvented Seasons/Crops Mods:** Prefer integrating existing purpose-made mods via `SeasonPort` (only with explicit developer approval).

---

## 2. Locked-In Simulation & Game-Theory Mechanics

| Axis | Specification | Research / Game-Theory Grounding |
| :--- | :--- | :--- |
| **Time & Cadence** | **4 sub-daily shifts/day** (`6,000` MC ticks/shift), **100-day year** (`400` shifts/year, `25` days/season: `SPRING`, `SUMMER`, `AUTUMN`, `WINTER`). | Enables exact `elapsedShifts` offscreen catch-up and seasonal labor bottlenecks (Three-Field Rotation). |
| **Intra-Town Distribution** | **Hybrid Tithe-Pool:** `Household` retains subsistence + `seedUnits` first, contributing a surplus share (e.g., `20%`) to the Communal Granary `Stockpile`. | *JASSS* Egalitarian-Tithe pooling + Ostrom commons governance (prevents free-rider collapse). |
| **Resilience Floor** | Integer `calorieUnits` + ring-fenced `seedUnits` in `Stockpile` (`SavedData` authoritative). Player theft can drain surplus, never `seedUnits`. | Malthusian subsistence ceiling; guarantees settlements stall rather than die. |
| **Carrying Capacity & Variance** | Arable terrain, slope, river/biome junction (*Site vs. Situation*), and mild stochastic weather variance (`±20%` seasonal yield) cap natural town size. | Christaller Central Place Theory & Zipf rank-size distribution (most towns stay Hamlets/Villages). |
| **Hierarchy Stages** | `HAMLET` → `VILLAGE` → `TOWN` → `CITY` → `REGION` driven by food surplus, housing/hearth capacity, and regional trade centrality $C_D'(S_j)$. | Marshallian agglomeration economies: higher stages unlock cheaper scarcity trades, higher starting villager levels, and faster profession XP. |
| **Player Agency** | Indirect economic/physical nudges (Granary chest sync, data-driven trades, road building/land clearing) + optional Charter markers. | Incentive-aligned ambient survival integration without micromanagement. |

---

## 3. Architecture & Module Boundaries (5-Tier Hexagonal Split)

- **`com.settlement.sim.core.*` (Pure Java 25 — Zero `net.minecraft.*` / `net.neoforged.*` imports):**
  - `core.world` (Tier 2): `Season`, `SeasonPort`, `SimClock`, `SettlementNetwork` (Gravity/XTENT trade graph).
  - `core.town` (Tier 3): `Household`, `Stockpile`, `MalthusianBalance`, `SettlementStage`, `BurgageCycle`, `PheromoneGrid`, `TaskBoard`, `TerrainScannerPort`.
  - `core.agent` (Tier 4): `UtilityScorer` and atomic target reservation contracts (`Map<BlockPos, UUID>`).
- **`com.settlement.mc.*` (Thin Minecraft / NeoForge Adapters):**
  - `mc.SettlementMod` (Tier 1): Deferred registration, tick listeners, data-driven trade hooks.
  - `mc.persistence` (Tier 2): `WorldLedgerSavedData` (`setDirty()`), `ChunkSoilAttachment` (`chunk.setUnsaved(true)`).
  - `mc.ai` (Tier 4): Vanilla `Villager` `Brain`, `MemoryModuleType`, `PoiManager`, `BehaviorControl` with $x/n$ tick throttling.
  - `mc.adapter` (Tier 5): `VoxelWorldAdapter` (`ServerLevel` guard, `setBlock(..., 3)`), `RoomValidator` (bounded BFS).

---

## 4. Capability Map & Build Order

1. **Phase 0 — Pure-Java Sandbox:** `SimClock`, `Household`, `Stockpile`, `MalthusianBalance`, `SettlementStage`, deterministic RNG, and JUnit 5 subsistence/surplus tests.
2. **Phase 1 — Persistence & Terrain Scanner:** `WorldLedgerSavedData`, `ChunkSoilAttachment`, `scanColumnSlope`, 4-neighbor $\Delta y \le 1$ scan, 32×32 debug wand.
3. **Phase 2 — Room Flood-Fill & Brain Hook:** Bounded BFS `validateRoomFloodFill`, `TaskBoard` reservation lock, first `Villager` `Brain` + `UtilityScorer` loop.
4. **Phase 3 — Burgage Plots & Safe Physical Builder:** `BurgageCycle` (`INSTITUTIVE` → `REPLETIVE` → `CLIMAX` → `FALLOW`), dynamic soil yield, pre-clear + bottom-up builder + stuck-watchdog.
5. **Phase 4 — ACO Roads, Fringe Belts & Demographics:** `PheromoneGrid` ($\Delta y \le 1$), Space Syntax land values, contour farms, Leslie age/gender cohorts.
6. **Phase 5 — Scale, Catch-Up & Regional Trade:** $x/n$ throttling, multi-phase pathing, `lastSimulatedShift` offscreen catch-up, Gravity/XTENT merchant routing, and stage-scaled 26.1 villager trades.

---

## 5. Build, Test & Verification Commands

- **Unit Tests (Pure Java + Boundary Gate):** `./gradlew test`
- **Full Mod Build:** `./gradlew build`
- **Run Client / Dedicated Server:** `./gradlew runClient` / `./gradlew runServer`

---

## 6. Acceptance Criteria & Verification Strategy
- **Boundary Purity:** `HexagonalBoundaryTest` passes in CI (`Exit 0`) on every commit/PR, guaranteeing `sim.core` has zero Minecraft/NeoForge imports.
- **Determinism & Invariants:** Every Phase 0–5 pure-Java rule is verified by seeded JUnit 5 tests (e.g., below-subsistence stall, surplus growth, `seedUnits` preservation under deficit/theft).
- **In-World Verification:** Every `mc.*` adapter phase ends with a reproducible in-game check (`ServerLevel` mutations visible to client via flag `3`, `SavedData` surviving world reload, and zero TPS spikes).
