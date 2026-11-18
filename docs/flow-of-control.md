# Flow of Control — Per-Phase Architecture

This document tracks the live execution flow of the Settlement Mod as each phase is implemented.

---

## Phase 0: Pure-Java Economic & Demographic Shift Loop

Every **6,000 Minecraft ticks** (1 shift, 4 shifts/day, 100 days/year), `MalthusianBalance.tickShift(...)` executes one deterministic step:

```mermaid
sequenceDiagram
    autonumber
    participant Clock as SimClock (100-day / 4-shift)
    participant Balance as MalthusianBalance
    participant HH as Household (per family)
    participant Store as Stockpile (Communal Granary)

    Balance->>Clock: getCurrentSeason()
    Clock-->>Balance: SPRING | SUMMER | AUTUMN | WINTER
    Note over Balance: seasonFactor * seeded +/-20% weatherFactor

    loop For each Household
        Note over Balance,HH: 1. Compute harvest from workers * season * weather
        Balance->>HH: allocateHarvest(harvest, seedTarget, subsistenceBuffer, 0.20)
        Note over HH: Priority: domesticSeed -> domesticCalories -> 20% surplus share
        HH-->>Balance: communalContribution
        Balance->>Store: depositCalories(communalContribution)

        Balance->>HH: consumeShift(10 cal/member)
        HH-->>Balance: unmetDeficit (never touches domesticSeed)
        opt unmetDeficit > 0
            Balance->>Store: withdrawCalories(unmetDeficit)
            Store-->>Balance: covered (never touches seedUnits)
        end
    end

    Balance->>Store: isAboveSubsistence(totalPop, 10, 12 shifts)
    Store-->>Balance: boolean surplusReady
    Balance->>Clock: advanceShift()
    Note over Balance: Returns BalanceOutcome(pop, +1 or 0 delta, stalled?, SettlementStage)
```

---

## Upcoming Phases (Planned)
- **Phase 1:** `ServerLevel` tick -> `WorldLedgerSavedData` (`SavedData`) + `ChunkSoilAttachment` (`DataAttachments`) + `VoxelWorldAdapter` slope/connected-area scan.
- **Phase 2:** `Villager` `Brain` -> `UtilityScorer` -> `TaskBoard.tryReserve(pos, uuid)` -> `RoomValidator` BFS.
- **Phase 3–5:** `BurgageCycle` plot infill -> Physical Builder -> `PheromoneGrid` ACO roads -> `SettlementNetwork` Gravity/XTENT trade.
