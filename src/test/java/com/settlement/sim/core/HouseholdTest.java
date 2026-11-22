package com.settlement.sim.core;

import com.settlement.sim.core.town.Household;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HouseholdTest {

    @Test
    @DisplayName("dependencyRatio divides total members by active workers")
    void dependencyRatioDividesTotalMembersByWorkers() {
        Household hh = new Household(UUID.randomUUID(), 2, 1);

        assertEquals(2, hh.getWorkers());
        assertEquals(1, hh.getDependents());
        assertEquals(3, hh.getTotalMembers());
        assertEquals(1.5, hh.dependencyRatio(), 1e-6);
    }

    @Test
    @DisplayName("dependencyRatio clamps zero workers to 1 to avoid divide-by-zero")
    void dependencyRatioGuardsZeroWorkers() {
        Household hh = new Household(UUID.randomUUID(), 0, 3);

        assertEquals(3, hh.getTotalMembers());
        assertEquals(3.0, hh.dependencyRatio(), 1e-6);
    }

    @Test
    @DisplayName("dependencyRatio returns 0.0 for an empty household")
    void zeroMembersYieldsZeroRatio() {
        Household hh = new Household(UUID.randomUUID(), 0, 0);

        assertEquals(0, hh.getTotalMembers());
        assertEquals(0.0, hh.dependencyRatio(), 1e-6);
    }

    @Test
    @DisplayName("constructor rejects null id and negative member counts")
    void constructorRejectsNullIdAndNegativeCounts() {
        UUID validId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new Household(null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Household(validId, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Household(validId, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new Household(validId, 1, 0, -1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new Household(validId, 1, 0, 0L, -1L));
    }

    @Test
    @DisplayName("consumeShift deducts from domesticCalories and returns 0 deficit when covered")
    void consumeShiftCoversFullNeedFromDomesticCalories() {
        Household hh = new Household(UUID.randomUUID(), 2, 1, 50L, 15L);

        // 3 members * 10 cal/shift = 30 needed
        long deficit = hh.consumeShift(10);

        assertEquals(0L, deficit);
        assertEquals(20L, hh.getDomesticCalories());
        assertEquals(15L, hh.getDomesticSeed());
    }

    @Test
    @DisplayName("consumeShift returns unmet deficit without touching ring-fenced domesticSeed")
    void consumeShiftReturnsUnmetDeficitWithoutTouchingSeed() {
        Household hh = new Household(UUID.randomUUID(), 1, 1, 12L, 25L);

        // 2 members * 10 cal/shift = 20 needed, only 12 available -> 8 deficit
        long deficit = hh.consumeShift(10);

        assertEquals(8L, deficit);
        assertEquals(0L, hh.getDomesticCalories());
        assertEquals(25L, hh.getDomesticSeed());
    }

    @Test
    @DisplayName("allocateHarvest fills seed target then subsistence buffer before sharing surplus")
    void allocateHarvestPrioritizesSeedAndBufferBeforeSharingSurplus() {
        Household hh = new Household(UUID.randomUUID(), 2, 0, 10L, 5L);

        // Need +15 seed (to reach 20), +30 calories (to reach 40). Harvest of 45 leaves 0 surplus -> 0 communal share.
        long communalShare = hh.allocateHarvest(45L, 20L, 40L, 0.20);

        assertEquals(0L, communalShare);
        assertEquals(20L, hh.getDomesticSeed());
        assertEquals(40L, hh.getDomesticCalories());
    }

    @Test
    @DisplayName("allocateHarvest contributes 20% communal share of surplus above seed and subsistence targets")
    void allocateHarvestSharesSurplusAboveTargets() {
        Household hh = new Household(UUID.randomUUID(), 2, 0, 40L, 20L);

        // Seed and buffer already full -> all 100 is surplus -> 20% communal share = 20, household keeps 80 (total 120)
        long communalShare = hh.allocateHarvest(100L, 20L, 40L, 0.20);

        assertEquals(20L, communalShare);
        assertEquals(20L, hh.getDomesticSeed());
        assertEquals(120L, hh.getDomesticCalories());
    }

    @Test
    @DisplayName("consumeShift and allocateHarvest reject invalid negative or out-of-range arguments")
    void invalidConsumptionAndHarvestArgsRejected() {
        Household hh = new Household(UUID.randomUUID(), 1, 0);

        assertThrows(IllegalArgumentException.class, () -> hh.consumeShift(-1));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(-1L, 0L, 0L, 0.20));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(10L, -1L, 0L, 0.20));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(10L, 0L, -1L, 0.20));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(10L, 0L, 0L, -0.1));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(10L, 0L, 0L, 1.1));
    }

    @Test
    @DisplayName("addDependent increments dependents and totalMembers")
    void addDependentIncrementsDependentsAndTotalMembers() {
        Household hh = new Household(UUID.randomUUID(), 2, 0);

        hh.addDependent();

        assertEquals(1, hh.getDependents());
        assertEquals(2, hh.getWorkers());
        assertEquals(3, hh.getTotalMembers());
    }

    @Test
    @DisplayName("promoteDependentToWorker moves one dependent into workers when available")
    void promoteDependentToWorkerMovesDependentToWorkerWhenAvailable() {
        Household hh = new Household(UUID.randomUUID(), 1, 2);

        boolean promoted = hh.promoteDependentToWorker();

        assertEquals(true, promoted);
        assertEquals(2, hh.getWorkers());
        assertEquals(1, hh.getDependents());
        assertEquals(3, hh.getTotalMembers());
    }

    @Test
    @DisplayName("promoteDependentToWorker returns false without altering counts when dependents == 0")
    void promoteDependentToWorkerReturnsFalseWhenNoDependentsExist() {
        Household hh = new Household(UUID.randomUUID(), 2, 0);

        boolean promoted = hh.promoteDependentToWorker();

        assertEquals(false, promoted);
        assertEquals(2, hh.getWorkers());
        assertEquals(0, hh.getDependents());
    }
}
