package com.settlement.sim.core;

import com.settlement.sim.core.town.Household;
import com.settlement.sim.core.town.MalthusianBalance.BalanceOutcome;
import com.settlement.sim.core.town.Settlement;
import com.settlement.sim.core.town.SettlementStage;
import com.settlement.sim.core.town.Stockpile;
import com.settlement.sim.core.world.SimClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementTest {

    @Test
    @DisplayName("advanceShift applies surplus growth to households and promotes SettlementStage")
    void advanceShiftGrowsPopulationAndPromotesStage() {
        // Start near VILLAGE threshold (15 members) in SUMMER with ample surplus
        SimClock clock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
        Household hh = new Household(UUID.randomUUID(), 15, 0, 1000L, 200L);
        Stockpile stockpile = new Stockpile(5000L);
        stockpile.depositCalories(2000L);

        Settlement settlement = new Settlement(UUID.randomUUID(), clock, stockpile, List.of(hh), 77L);
        assertEquals(SettlementStage.HAMLET, settlement.getStage());

        BalanceOutcome outcome = settlement.advanceShift();

        assertEquals(16, settlement.getTotalPopulation());
        assertEquals(SettlementStage.VILLAGE, settlement.getStage());
        assertEquals(SettlementStage.VILLAGE, outcome.recommendedStage());
        assertEquals(1, outcome.populationDelta());
    }

    @Test
    @DisplayName("catchUpShifts produces identical state to calling advanceShift N times")
    void catchUpShiftsMatchesShiftByShiftExecution() {
        Settlement batched = createHamlet(42L);
        Settlement stepByStep = createHamlet(42L);

        batched.catchUpShifts(100L);
        for (int i = 0; i < 100; i++) {
            stepByStep.advanceShift();
        }

        assertEquals(stepByStep.getTotalPopulation(), batched.getTotalPopulation());
        assertEquals(stepByStep.getStage(), batched.getStage());
        assertEquals(stepByStep.getClock().getTotalShifts(), batched.getClock().getTotalShifts());
        assertEquals(stepByStep.getStockpile().getCalorieUnits(), batched.getStockpile().getCalorieUnits());
    }

    @Test
    @DisplayName("catchUpShifts(0) does not advance clock and negative shifts throw IllegalArgumentException")
    void catchUpZeroAndNegativeRejected() {
        Settlement settlement = createHamlet(1L);

        BalanceOutcome outcome = settlement.catchUpShifts(0L);
        assertEquals(0L, settlement.getClock().getTotalShifts());
        assertEquals(settlement.getTotalPopulation(), outcome.totalPopulation());
        assertTrue(outcome.stalledAtSubsistence() || outcome.populationDelta() == 0);

        assertThrows(IllegalArgumentException.class, () -> settlement.catchUpShifts(-1L));
    }

    private static Settlement createHamlet(long seed) {
        SimClock clock = new SimClock();
        Household hh = new Household(UUID.nameUUIDFromBytes("founder".getBytes()), 4, 0, 100L, 40L);
        Stockpile stockpile = new Stockpile(5000L);
        stockpile.depositSeed(50L);
        stockpile.depositCalories(200L);
        return new Settlement(UUID.nameUUIDFromBytes("town".getBytes()), clock, stockpile, List.of(hh), seed);
    }
}
