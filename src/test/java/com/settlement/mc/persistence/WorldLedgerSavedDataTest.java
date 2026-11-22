package com.settlement.mc.persistence;

import com.mojang.serialization.DataResult;
import com.settlement.sim.core.town.Household;
import com.settlement.sim.core.town.Settlement;
import com.settlement.sim.core.town.SettlementStage;
import com.settlement.sim.core.town.Stockpile;
import com.settlement.sim.core.world.SimClock;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldLedgerSavedDataTest {

    @Test
    @DisplayName("WorldLedgerSavedData round-trips settlements through Mojang Codec and NbtOps")
    void codecRoundTripPreservesSettlementState() {
        WorldLedgerSavedData original = new WorldLedgerSavedData();
        UUID townId = UUID.randomUUID();
        UUID hhId = UUID.randomUUID();

        SimClock clock = new SimClock(42L);
        Stockpile stockpile = new Stockpile(2500L);
        stockpile.depositSeed(120L);
        stockpile.depositCalories(640L);
        Household household = new Household(hhId, 10, 6, 300L, 80L);

        Settlement settlement = new Settlement(townId, clock, stockpile, List.of(household), 987654321L);
        original.putSettlement(settlement);
        assertTrue(original.isDirty(), "putSettlement must call setDirty()");

        DataResult<Tag> encoded = WorldLedgerSavedData.CODEC.encodeStart(NbtOps.INSTANCE, original);
        Tag nbt = encoded.getOrThrow();

        DataResult<WorldLedgerSavedData> decoded = WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, nbt);
        WorldLedgerSavedData restored = decoded.getOrThrow();

        assertEquals(WorldLedgerSavedData.CURRENT_SCHEMA_VERSION, restored.getSchemaVersion());
        assertEquals(1, restored.getSettlements().size());

        Settlement restoredTown = restored.getSettlements().get(townId);
        assertEquals(townId, restoredTown.getId());
        assertEquals(42L, restoredTown.getClock().getTotalShifts());
        assertEquals(987654321L, restoredTown.getSeed());
        assertEquals(16, restoredTown.getTotalPopulation());
        assertEquals(SettlementStage.VILLAGE, restoredTown.getStage());
        assertEquals(2500L, restoredTown.getStockpile().getCapacity());
        assertEquals(640L, restoredTown.getStockpile().getCalorieUnits());
        assertEquals(120L, restoredTown.getStockpile().getSeedUnits());

        Household restoredHh = restoredTown.getHouseholds().get(0);
        assertEquals(hhId, restoredHh.getId());
        assertEquals(10, restoredHh.getWorkers());
        assertEquals(6, restoredHh.getDependents());
        assertEquals(300L, restoredHh.getDomesticCalories());
        assertEquals(80L, restoredHh.getDomesticSeed());
    }

    @Test
    @DisplayName("putSettlement and getOrCreate reject null arguments")
    void nullArgumentsRejected() {
        WorldLedgerSavedData ledger = new WorldLedgerSavedData();
        assertThrows(IllegalArgumentException.class, () -> ledger.putSettlement(null));
        assertThrows(IllegalArgumentException.class, () -> WorldLedgerSavedData.getOrCreate(null));
    }
}
