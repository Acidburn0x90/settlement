package com.settlement.mc.persistence;

import com.mojang.serialization.DataResult;
import com.settlement.sim.core.town.Household;
import com.settlement.sim.core.town.Settlement;
import com.settlement.sim.core.town.SettlementStage;
import com.settlement.sim.core.town.Stockpile;
import com.settlement.sim.core.world.SimClock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    @DisplayName("empty CompoundTag parses to safe defaults with current schema version and no settlements")
    void emptyCompoundTagParsesToSafeDefaults() {
        DataResult<WorldLedgerSavedData> result = assertDoesNotThrow(
                () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, new CompoundTag())
        );

        assertFalse(result.isError());
        WorldLedgerSavedData ledger = result.getOrThrow();
        assertEquals(WorldLedgerSavedData.CURRENT_SCHEMA_VERSION, ledger.getSchemaVersion());
        assertTrue(ledger.getSettlements().isEmpty());
    }

    @Test
    @DisplayName("non-CompoundTag root NBT inputs return DataResult.isError() without throwing")
    void nonCompoundRootTagsReturnDataResultError() {
        List<Tag> malformedRoots = List.of(
                StringTag.valueOf("not_a_compound"),
                IntTag.valueOf(42),
                LongTag.valueOf(99L),
                new ListTag()
        );

        for (Tag tag : malformedRoots) {
            DataResult<WorldLedgerSavedData> result = assertDoesNotThrow(
                    () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, tag)
            );
            assertTrue(result.isError(), "Expected error DataResult for root tag: " + tag);
        }
    }

    @Test
    @DisplayName("wrong-type optional root fields return DataResult.isError() without throwing")
    void wrongTypeOptionalRootFieldsReturnDataResultError() {
        CompoundTag badSchema = new CompoundTag();
        badSchema.putString("schema_version", "v1");
        DataResult<WorldLedgerSavedData> badSchemaResult = assertDoesNotThrow(
                () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badSchema)
        );
        assertTrue(badSchemaResult.isError());

        CompoundTag badSettlementsType = new CompoundTag();
        badSettlementsType.putInt("settlements", 123);
        DataResult<WorldLedgerSavedData> badSettlementsResult = assertDoesNotThrow(
                () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badSettlementsType)
        );
        assertTrue(badSettlementsResult.isError());
    }

    @Test
    @DisplayName("missing required fields inside settlement, stockpile, or household return DataResult.isError()")
    void missingRequiredNestedFieldsReturnDataResultError() {
        CompoundTag validRoot = encodeSingleSettlementRoot();

        for (String settlementKey : List.of("id", "total_shifts", "stockpile", "households", "seed")) {
            CompoundTag copy = validRoot.copy();
            firstSettlement(copy).remove(settlementKey);
            DataResult<WorldLedgerSavedData> result = assertDoesNotThrow(
                    () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, copy)
            );
            assertTrue(result.isError(), "Expected error when missing settlement field: " + settlementKey);
        }

        for (String stockpileKey : List.of("capacity", "calorie_units", "seed_units")) {
            CompoundTag copy = validRoot.copy();
            firstSettlement(copy).getCompound("stockpile").orElseThrow().remove(stockpileKey);
            DataResult<WorldLedgerSavedData> result = assertDoesNotThrow(
                    () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, copy)
            );
            assertTrue(result.isError(), "Expected error when missing stockpile field: " + stockpileKey);
        }

        for (String householdKey : List.of("id", "workers", "dependents", "domestic_calories", "domestic_seed")) {
            CompoundTag copy = validRoot.copy();
            firstHousehold(copy).remove(householdKey);
            DataResult<WorldLedgerSavedData> result = assertDoesNotThrow(
                    () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, copy)
            );
            assertTrue(result.isError(), "Expected error when missing household field: " + householdKey);
        }
    }

    @Test
    @DisplayName("wrong-type nested fields return DataResult.isError() without throwing")
    void wrongTypeNestedFieldsReturnDataResultError() {
        CompoundTag validRoot = encodeSingleSettlementRoot();

        CompoundTag badUuidLen = validRoot.copy();
        firstSettlement(badUuidLen).putIntArray("id", new int[]{1, 2, 3});
        assertTrue(assertDoesNotThrow(() -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badUuidLen)).isError());

        CompoundTag badUuidType = validRoot.copy();
        firstSettlement(badUuidType).putString("id", "not-a-uuid");
        assertTrue(assertDoesNotThrow(() -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badUuidType)).isError());

        CompoundTag badTotalShifts = validRoot.copy();
        firstSettlement(badTotalShifts).putString("total_shifts", "ten");
        assertTrue(assertDoesNotThrow(() -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badTotalShifts)).isError());

        CompoundTag badHouseholdsElem = validRoot.copy();
        ListTag nonCompoundList = new ListTag();
        nonCompoundList.add(IntTag.valueOf(7));
        firstSettlement(badHouseholdsElem).put("households", nonCompoundList);
        assertTrue(assertDoesNotThrow(() -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badHouseholdsElem)).isError());

        CompoundTag badCapacity = validRoot.copy();
        firstSettlement(badCapacity).getCompound("stockpile").orElseThrow().putString("capacity", "huge");
        assertTrue(assertDoesNotThrow(() -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badCapacity)).isError());

        CompoundTag badWorkers = validRoot.copy();
        firstHousehold(badWorkers).putString("workers", "two");
        assertTrue(assertDoesNotThrow(() -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, badWorkers)).isError());
    }

    @Test
    @DisplayName("getSettlements returns an unmodifiable map and markUpdated sets the SavedData dirty flag")
    void getSettlementsIsUnmodifiableAndMarkUpdatedSetsDirtyFlag() {
        WorldLedgerSavedData ledger = new WorldLedgerSavedData();
        assertFalse(ledger.isDirty(), "Fresh ledger must start clean");

        ledger.markUpdated();
        assertTrue(ledger.isDirty(), "markUpdated() must mark SavedData dirty");

        UUID extraId = UUID.randomUUID();
        Settlement extra = new Settlement(
                extraId,
                new SimClock(),
                new Stockpile(100L),
                List.of(new Household(UUID.randomUUID(), 1, 0)),
                1L
        );
        ledger.putSettlement(extra);

        assertThrows(UnsupportedOperationException.class, () -> ledger.getSettlements().put(UUID.randomUUID(), extra));
        assertThrows(UnsupportedOperationException.class, () -> ledger.getSettlements().remove(extraId));
        assertThrows(UnsupportedOperationException.class, () -> ledger.getSettlements().clear());
    }

    @Test
    @DisplayName("negative domain values in valid-typed NBT must return DataResult.isError() without throwing")
    void negativeNumericNbtFieldsReturnDataResultErrorWithoutThrowing() {
        CompoundTag validRoot = encodeSingleSettlementRoot();

        CompoundTag negWorkers = validRoot.copy();
        firstHousehold(negWorkers).putInt("workers", -1);
        assertCodecParseReturnsError(negWorkers, "household.workers=-1");

        CompoundTag negDependents = validRoot.copy();
        firstHousehold(negDependents).putInt("dependents", -1);
        assertCodecParseReturnsError(negDependents, "household.dependents=-1");

        CompoundTag negDomCal = validRoot.copy();
        firstHousehold(negDomCal).putLong("domestic_calories", -1L);
        assertCodecParseReturnsError(negDomCal, "household.domestic_calories=-1");

        CompoundTag negDomSeed = validRoot.copy();
        firstHousehold(negDomSeed).putLong("domestic_seed", -1L);
        assertCodecParseReturnsError(negDomSeed, "household.domestic_seed=-1");

        CompoundTag negCap = validRoot.copy();
        firstSettlement(negCap).getCompound("stockpile").orElseThrow().putLong("capacity", -1L);
        assertCodecParseReturnsError(negCap, "stockpile.capacity=-1");

        CompoundTag negCal = validRoot.copy();
        firstSettlement(negCal).getCompound("stockpile").orElseThrow().putLong("calorie_units", -1L);
        assertCodecParseReturnsError(negCal, "stockpile.calorie_units=-1");

        CompoundTag negSeed = validRoot.copy();
        firstSettlement(negSeed).getCompound("stockpile").orElseThrow().putLong("seed_units", -1L);
        assertCodecParseReturnsError(negSeed, "stockpile.seed_units=-1");

        CompoundTag negShifts = validRoot.copy();
        firstSettlement(negShifts).putLong("total_shifts", -1L);
        assertCodecParseReturnsError(negShifts, "settlement.total_shifts=-1");
    }

    private static void assertCodecParseReturnsError(CompoundTag nbt, String caseName) {
        DataResult<WorldLedgerSavedData> result = assertDoesNotThrow(
                () -> WorldLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, nbt),
                "Negative NBT field '" + caseName + "' must return DataResult.error instead of throwing"
        );
        assertTrue(result.isError(), "Expected DataResult.error for negative NBT field: " + caseName);
    }

    private static CompoundTag encodeSingleSettlementRoot() {
        WorldLedgerSavedData ledger = new WorldLedgerSavedData();
        Stockpile stockpile = new Stockpile(1000L);
        stockpile.depositSeed(50L);
        stockpile.depositCalories(200L);
        Household hh = new Household(UUID.randomUUID(), 2, 1, 40L, 20L);
        Settlement settlement = new Settlement(UUID.randomUUID(), new SimClock(10L), stockpile, List.of(hh), 123L);
        ledger.putSettlement(settlement);
        return (CompoundTag) WorldLedgerSavedData.CODEC.encodeStart(NbtOps.INSTANCE, ledger).getOrThrow();
    }

    private static CompoundTag firstSettlement(CompoundTag root) {
        return root.getList("settlements").orElseThrow().getCompound(0).orElseThrow();
    }

    private static CompoundTag firstHousehold(CompoundTag root) {
        return firstSettlement(root).getList("households").orElseThrow().getCompound(0).orElseThrow();
    }
}
