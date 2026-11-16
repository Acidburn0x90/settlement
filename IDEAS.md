# Settlement Mod — Idea Board

A living scratchpad for gameplay features, buildings, and mechanics, paired with historical, sociological, and game-theory precedents to keep the simulation grounded.

> **Core Invariants:**
> - **Resilience:** Villages never die or collapse to zero; deficits stall growth at a subsistence equilibrium.
> - **Seasons:** 100-day Minecraft year (`SPRING`, `SUMMER`, `AUTUMN`, `WINTER`) with seasonal crop cycles (preferring approved integration with existing seasons/crops mods).

---

## 1. Buildings & Spatial Hubs

### Communal Granary / Tithe Barn (Crop Reserve & Storage)
- **Idea:** Dedicated storage building for grain reserves, seed-corn, and town surpluses.
- **Historical / ABM Precedent:** Medieval **Tithe Barns & Communal Granaries** separated *seed-corn* (untouchable until Spring/Autumn sowing) from *consumable calories* and *trade surplus* ($N_{i,t}$ in the JASSS ABM).
- **Urban Morphology & Gameplay Fit:**
  - Acts as a primary **ACO road hub** (high-traffic desire lines converge here).
  - Pulls dense artisan workshops inward (central land value), while acting as the physical anchor for `Stockpile` in `sim.core.town`.
  - Prevents "villages dying" by enforcing a protected seed-corn floor.

### Dedicated Population Growth Building ("Villager Breeder" → Hearth / Midwifery / Parish Nursery)
- **Idea:** A dedicated building governing villager population growth / new household formation.
- **Historical / Demographic Precedent:** **European Marriage Pattern & Hearth-Gate** — historically, new families formed only when both a *physical dwelling/hearth* and a *reliable food surplus* existed. Pairing this with **Leslie demographic cohorts** (children → apprentices → adults) avoids immersion-breaking "infinite breeder boxes."
- **Performance & Balance Fit:**
  - Caps population cleanly to prevent TPS spikes from unbounded entity spawning.
  - Only triggers growth ticks when `Stockpile` exceeds the Malthusian subsistence threshold.

---

## 2. Jobs, Workshops & Seasonal Labor

### Seasonal Labor Shifting (No Idle Winters)
- **Idea:** Villagers shouldn't stand idle during `WINTER` when fields are fallow or frozen.
- **Historical Precedent:** Medieval agrarian calendars shifted farmhands in Winter to **threshing, tool repair, spinning/weaving, charcoal burning, and construction maintenance**.
- **AI Fit:** `UtilityScorer` weights seasonal tasks naturally (e.g., `TendCrop` peaks in `SPRING`/`SUMMER`/`AUTUMN`; `Build`, `Haul`, `Craft`, and `Repair` rise in `WINTER`).

### Candidate Job Buildings (Backlog)
- **Windmill / Watermill & Bakehouse:** Converts raw grain into bread/flour (calorie multiplier, rewards surplus).
- **Smithy / Toolwright:** Supplies tools that boost farm and builder throughput; central workshop plot.
- **Woodcutter's Yard / Charcoal Kiln:** Space-hungry / smoky heavy trade — natural **Conzenian fringe-belt** placement on cheap outer land.
- **Market Stall / Caravan Post (Phase 5):** Physical anchor for inter-town Gravity/XTENT trade merchants and scarcity-driven 26.1 villager trades.

---

## 3. Open Questions & Future Ideas
- *Add your new ideas below as bullet points or open a GitHub Issue with the `enhancement` label.*
