package com.settlement.sim.core;

import com.settlement.sim.core.town.Household;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("zero boundaries for consumption, deposits, and harvest allocation leave reserves intact")
    void zeroBoundariesForConsumptionDepositAndAllocation() {
        Household empty = new Household(UUID.randomUUID(), 0, 0, 0L, 0L);
        assertEquals(0L, empty.consumeShift(0));
        assertEquals(0L, empty.consumeShift(10));
        assertEquals(0L, empty.consumeShift(Integer.MAX_VALUE));

        Household hh = new Household(UUID.randomUUID(), 2, 1, 0L, 0L);
        assertEquals(0L, hh.consumeShift(0));
        hh.depositDomesticCalories(0L);
        assertEquals(0L, hh.getDomesticCalories());
        assertEquals(0L, hh.allocateHarvest(0L, 20L, 40L, 0.20));
        assertEquals(0L, hh.getDomesticSeed());
        assertEquals(0L, hh.getDomesticCalories());
    }

    @Test
    @DisplayName("allocateHarvest rejects NaN, infinities, and double values outside [0.0, 1.0]")
    void allocateHarvestRejectsNaNInfinitiesAndOutOfUnitIntervalShareRates() {
        Household hh = new Household(UUID.randomUUID(), 1, 0, 100L, 100L);

        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(50L, 0L, 0L, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(50L, 0L, 0L, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(50L, 0L, 0L, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(50L, 0L, 0L, -Double.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(50L, 0L, 0L, Math.nextUp(1.0)));

        // IEEE-754 negative zero is inside [0.0, 1.0] and contributes nothing
        assertEquals(0L, hh.allocateHarvest(50L, 0L, 0L, -0.0));
        assertEquals(150L, hh.getDomesticCalories());
    }

    @Test
    @DisplayName("allocateHarvest handles 0.0 and 1.0 endpoints and reserves already exceeding targets")
    void allocateHarvestUnitIntervalEndpointsAndSurplusAlreadyAboveTargets() {
        Household keepAll = new Household(UUID.randomUUID(), 1, 0, 100L, 50L);
        assertEquals(0L, keepAll.allocateHarvest(80L, 10L, 20L, 0.0));
        assertEquals(50L, keepAll.getDomesticSeed());
        assertEquals(180L, keepAll.getDomesticCalories());

        Household shareAll = new Household(UUID.randomUUID(), 1, 0, 100L, 50L);
        assertEquals(80L, shareAll.allocateHarvest(80L, 10L, 20L, 1.0));
        assertEquals(50L, shareAll.getDomesticSeed());
        assertEquals(100L, shareAll.getDomesticCalories());
    }

    @Test
    @DisplayName("allocateHarvest above 2^53 and Long.MAX_VALUE conserves calories without underflow")
    void allocateHarvestAbove2Pow53MaintainsConservationWithoutUnderflow() {
        long aboveExactDouble = (1L << 53) + 1L;
        Household hh1 = new Household(UUID.randomUUID(), 1, 0, 0L, 0L);
        long share1 = hh1.allocateHarvest(aboveExactDouble, 0L, 0L, 1.0);
        assertTrue(share1 >= 0L && share1 <= aboveExactDouble);
        assertTrue(hh1.getDomesticCalories() >= 0L);
        assertEquals(aboveExactDouble, share1 + hh1.getDomesticCalories());

        // (1L << 53) + 3L rounds UP to (1L << 53) + 4 in double; clamp must prevent domesticCalories underflow
        long roundsUpInDouble = (1L << 53) + 3L;
        Household hh2 = new Household(UUID.randomUUID(), 1, 0, 0L, 0L);
        long share2 = hh2.allocateHarvest(roundsUpInDouble, 0L, 0L, 1.0);
        assertEquals(roundsUpInDouble, share2);
        assertEquals(0L, hh2.getDomesticCalories());

        Household hhMaxFullShare = new Household(UUID.randomUUID(), 1, 0, 0L, 0L);
        long shareMax = hhMaxFullShare.allocateHarvest(Long.MAX_VALUE, 0L, 0L, 1.0);
        assertEquals(Long.MAX_VALUE, shareMax);
        assertEquals(0L, hhMaxFullShare.getDomesticCalories());

        Household hhMaxPartial = new Household(UUID.randomUUID(), 1, 0, 0L, 0L);
        long sharePartial = hhMaxPartial.allocateHarvest(Long.MAX_VALUE, 100L, 400L, 0.20);
        assertTrue(sharePartial >= 0L);
        assertEquals(100L, hhMaxPartial.getDomesticSeed());
        assertTrue(hhMaxPartial.getDomesticCalories() >= 400L);
        BigInteger totalConserved = BigInteger.valueOf(hhMaxPartial.getDomesticSeed())
                .add(BigInteger.valueOf(hhMaxPartial.getDomesticCalories()))
                .add(BigInteger.valueOf(sharePartial));
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), totalConserved);
    }

    @Test
    @DisplayName("extreme negative and null bounds throw IllegalArgumentException across all mutators")
    void minValueNegativeGuardsOnConstructorsAndMutators() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new Household(null, 0, 0, 0L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new Household(id, Integer.MIN_VALUE, 0));
        assertThrows(IllegalArgumentException.class, () -> new Household(id, 0, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new Household(id, 0, 0, Long.MIN_VALUE, 0L));
        assertThrows(IllegalArgumentException.class, () -> new Household(id, 0, 0, 0L, Long.MIN_VALUE));

        Household hh = new Household(id, 1, 0, 10L, 10L);
        assertThrows(IllegalArgumentException.class, () -> hh.consumeShift(Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> hh.depositDomesticCalories(-1L));
        assertThrows(IllegalArgumentException.class, () -> hh.depositDomesticCalories(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(Long.MIN_VALUE, 0L, 0L, 0.5));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(10L, Long.MIN_VALUE, 0L, 0.5));
        assertThrows(IllegalArgumentException.class, () -> hh.allocateHarvest(10L, 0L, Long.MIN_VALUE, 0.5));
    }

    @Test
    @DisplayName("member sum and increments must guard against int overflow without wrapping negative")
    void memberSumAndIncrementGuardAgainstIntegerOverflow() {
        try {
            Household overflow = new Household(UUID.randomUUID(), Integer.MAX_VALUE, 1, 100L, 0L);
            long deficit = overflow.consumeShift(1);
            assertTrue(overflow.getTotalMembers() > 0, "Total members must never wrap negative");
            assertTrue(overflow.getDomesticCalories() <= 100L, "consumeShift must never create phantom calories");
            assertTrue(deficit >= 0L, "deficit must never be negative");
        } catch (IllegalArgumentException | ArithmeticException expectedGuard) {
            // Rejecting overflowing member sum is also valid
        }

        Household atMaxDependents = new Household(UUID.randomUUID(), 1, Integer.MAX_VALUE);
        try {
            atMaxDependents.addDependent();
            assertTrue(atMaxDependents.getDependents() > 0, "dependents must not wrap negative on addDependent");
        } catch (IllegalStateException | ArithmeticException expectedGuard) {
            // Guarding overflow via exception is also valid
        }

        Household atMaxWorkers = new Household(UUID.randomUUID(), Integer.MAX_VALUE, 1);
        try {
            atMaxWorkers.promoteDependentToWorker();
            assertTrue(atMaxWorkers.getWorkers() > 0, "workers must not wrap negative on promotion");
        } catch (IllegalStateException | ArithmeticException expectedGuard) {
            // Guarding overflow via exception is also valid
        }
    }

    @Test
    @DisplayName("depositDomesticCalories and allocateHarvest must guard against long reserve overflow")
    void depositDomesticCaloriesAndAllocateHarvestGuardAgainstLongOverflow() {
        Household nearMaxDeposit = new Household(UUID.randomUUID(), 1, 0, Long.MAX_VALUE - 5L, 0L);
        try {
            nearMaxDeposit.depositDomesticCalories(10L);
            assertTrue(
                    nearMaxDeposit.getDomesticCalories() >= Long.MAX_VALUE - 5L,
                    "depositDomesticCalories must not wrap domesticCalories negative"
            );
        } catch (ArithmeticException expectedGuard) {
            // Guarding overflow via ArithmeticException is also valid
        }

        Household nearMaxHarvest = new Household(UUID.randomUUID(), 1, 0, Long.MAX_VALUE - 5L, 0L);
        try {
            nearMaxHarvest.allocateHarvest(100L, 0L, 0L, 0.0);
            assertTrue(
                    nearMaxHarvest.getDomesticCalories() >= Long.MAX_VALUE - 5L,
                    "allocateHarvest must not wrap domesticCalories negative"
            );
        } catch (ArithmeticException expectedGuard) {
            // Guarding overflow via ArithmeticException is also valid
        }
    }
}
