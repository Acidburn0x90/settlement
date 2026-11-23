# Settlement Mod — Task Checklist

## Phase 0: Java 25 Toolchain & Pure-Java Simulation Sandbox
- [x] Scaffold repository (`.gitignore`, `AGENTS.md`, `tasks/todo.md`, Gradle 9.2 wrapper, NeoForge 26.1 ModDevGradle config, hexagonal package shells)
- [x] Scaffold contributor documentation (`README.md`, `CONTRIBUTING.md`, `SECURITY.md`), architectural invariants (100-day seasonal calendar, resilience equilibrium), and Phase 0-1 Java skeletons
- [x] Implement `Household`, `Stockpile`, and Malthusian grain balance in `com.settlement.sim.core.town` (resilience invariant: settlements stall at subsistence rather than extinguishing)
- [x] Model 100-day seasonal calendar and crop cycle port (`Season`, `SeasonPort`) in `com.settlement.sim.core.world`
- [x] Write JUnit 5 tests in `src/test/java/com/settlement/sim/core/` verifying population stalls below subsistence and grows above surplus threshold, plus HexagonalBoundaryTest

## Phase 1: World Persistence, Chunk Attachments & Terrain Scanner
- [x] Implement `WorldLedgerSavedData` (`SavedDataType` + `Codec`) in `com.settlement.mc.persistence`
- [x] Attach per-chunk soil fertility and experience counters via `DataAttachments` (`chunk.markUnsaved()`)
- [ ] Implement `scanColumnSlope(x, z)` and 4-neighbor von Neumann connected-area scan ($\Delta y \le 1$)
- [ ] Create debug wand item to evaluate and mark a 32x32 build footprint (`setBlock(..., 3)`)

## Phase 2: Room Flood-Fill, Task Reservation & First Villager Brain Hook
- [ ] Implement bounded BFS `validateRoomFloodFill(doorPos)` (max 512 air blocks)
- [ ] Implement `TaskBoard` with atomic target reservation (`Map<BlockPos, UUID>`)
- [ ] Register custom `MemoryModuleType`, `PoiManager` POI, and `BehaviorControl` injected into vanilla `Villager` `Brain`
- [ ] Connect `UtilityScorer` (`Eat` vs `TendCrop` vs `StoreHarvest`)

## Phase 3: Burgage Plots, Backyard Economy & Safe Physical Builder
- [ ] Model `Plot` (`DwellingZone` + `ExtensionYard`) with `BurgageCycle` (`INSTITUTIVE`, `REPLETIVE`, `CLIMAX`, `FALLOW`)
- [ ] Tie plot geometry to household output rules and dynamic soil yield curves
- [ ] Implement physical builder `BehaviorControl` (pre-clear foliage/liquid, bottom-up layers, stuck-watchdog)

## Phase 4: ACO Desire-Line Roads, Fringe Belts & Demographic Growth
- [ ] Implement 2D `PheromoneGrid` and virtual ACO pathwalkers ($\beta$, $\gamma$, $s_R$)
- [ ] Upgrade high-pheromone tiles to dirt/gravel/cobble paths with pathfinding malus tuning and walk speed bonus
- [ ] Compute Space Syntax street connectivity for central land values and fringe-belt placement
- [ ] Add contour-hugging farm flood-fill and Leslie demographic cohorts

## Phase 5: Performance Hardening, Offscreen Catch-Up & Multi-Town Trade
- [ ] Add $x/n$ population-scaled `UtilityScorer` tick throttling and multi-phase pathfinding
- [ ] Implement `lastSimulatedTick` elapsed-time offscreen catch-up math on chunk reload
- [ ] Activate `SettlementNetwork` Gravity/XTENT trade flows, merchant routing, and scarcity-driven 26.1 villager trades
