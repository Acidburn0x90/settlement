package com.settlement.sim.core;

import com.settlement.sim.core.town.Household;
import com.settlement.sim.core.town.MalthusianBalance.BalanceOutcome;
import com.settlement.sim.core.town.Settlement;
import com.settlement.sim.core.town.SettlementStage;
import com.settlement.sim.core.town.Stockpile;
import com.settlement.sim.core.world.SimClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @DisplayName("catchUpShifts(0) reports accurate subsistence state without advancing clock, and out-of-range throws")
    void catchUpZeroAndBoundsChecked() {
        Settlement settlement = createHamlet(1L);
        // createHamlet starts with 4 members and 200 calories (< 480 required for 12-shift buffer -> stalled)
        BalanceOutcome stalledOutcome = settlement.catchUpShifts(0L);
        assertEquals(0L, settlement.getClock().getTotalShifts());
        assertEquals(settlement.getTotalPopulation(), stalledOutcome.totalPopulation());
        assertEquals(true, stalledOutcome.stalledAtSubsistence());

        // Top up stockpile above 480 -> catchUpShifts(0L) must report not stalled
        settlement.getStockpile().depositCalories(400L);
        BalanceOutcome surplusOutcome = settlement.catchUpShifts(0L);
        assertFalse(surplusOutcome.stalledAtSubsistence());

        assertThrows(IllegalArgumentException.class, () -> settlement.catchUpShifts(-1L));
        assertThrows(
                IllegalArgumentException.class,
                () -> settlement.catchUpShifts(SimulationConstants.MAX_CATCHUP_SHIFTS + 1L)
        );
    }

    @Test
    @DisplayName("advanceShift distributes new dependents evenly to the least-populated household")
    void advanceShiftDistributesDependentsEvenly() {
        SimClock clock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
        Household larger = new Household(UUID.randomUUID(), 4, 2, 300L, 40L);  // 6 members
        Household smaller = new Household(UUID.randomUUID(), 2, 0, 300L, 20L); // 2 members
        Stockpile stockpile = new Stockpile(5000L);
        stockpile.depositCalories(2000L);

        Settlement settlement = new Settlement(UUID.randomUUID(), clock, stockpile, List.of(larger, smaller), 11L);
        settlement.advanceShift();

        assertEquals(6, larger.getTotalMembers());
        assertEquals(3, smaller.getTotalMembers(), "New dependent should go to the smaller household");
    }

    @Test
    @DisplayName("constructor rejects null household elements and getHouseholds is unmodifiable")
    void rejectsNullHouseholdElementsAndProtectsList() {
        SimClock clock = new SimClock();
        Stockpile stockpile = new Stockpile(100L);
        List<Household> withNull = Arrays.asList((Household) null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Settlement(UUID.randomUUID(), clock, stockpile, withNull, 1L)
        );

        Settlement settlement = createHamlet(5L);
        assertThrows(UnsupportedOperationException.class, () -> settlement.getHouseholds().clear());
    }

    private static Settlement createHamlet(long seed) {
        SimClock clock = new SimClock();
        Household hh = new Household(UUID.nameUUIDFromBytes("founder".getBytes()), 4, 0, 100L, 40L);
        Stockpile stockpile = new Stockpile(5000L);
        stockpile.depositSeed(50L);
        stockpile.depositCalories(200L);
        return new Settlement(UUID.nameUUIDFromBytes("town".getBytes()), clock, stockpile, List.of(hh), seed);
    }

    @Test
    @DisplayName("constructor rejects null id, clock, stockpile, or households list")
    void constructorRejectsAllNullRequiredArguments() {
        UUID id = UUID.randomUUID();
        SimClock clock = new SimClock();
        Stockpile stockpile = new Stockpile(100L);
        List<Household> households = List.of();

        assertThrows(IllegalArgumentException.class, () -> new Settlement(null, clock, stockpile, households, 1L));
        assertThrows(IllegalArgumentException.class, () -> new Settlement(id, null, stockpile, households, 1L));
        assertThrows(IllegalArgumentException.class, () -> new Settlement(id, clock, null, households, 1L));
        assertThrows(IllegalArgumentException.class, () -> new Settlement(id, clock, stockpile, null, 1L));
    }

    @Test
    @DisplayName("empty households list initializes at zero population HAMLET and advances without error")
    void emptyHouseholdsListInitializesAndAdvancesSafely() {
        Settlement empty = new Settlement(UUID.randomUUID(), new SimClock(), new Stockpile(500L), List.of(), 1L);

        assertEquals(0, empty.getTotalPopulation());
        assertEquals(SettlementStage.HAMLET, empty.getStage());

        BalanceOutcome zeroCatchUp = empty.catchUpShifts(0L);
        assertEquals(0, zeroCatchUp.totalPopulation());
        assertEquals(0, zeroCatchUp.populationDelta());
        assertTrue(zeroCatchUp.stalledAtSubsistence());
        assertEquals(SettlementStage.HAMLET, zeroCatchUp.recommendedStage());
        assertEquals(0L, empty.getClock().getTotalShifts());

        BalanceOutcome singleAdvance = empty.advanceShift();
        assertEquals(0, singleAdvance.totalPopulation());
        assertTrue(singleAdvance.stalledAtSubsistence());
        assertEquals(1L, empty.getClock().getTotalShifts());

        BalanceOutcome catchUp = empty.catchUpShifts(5L);
        assertEquals(0, catchUp.totalPopulation());
        assertTrue(catchUp.stalledAtSubsistence());
        assertEquals(6L, empty.getClock().getTotalShifts());
    }

    @Test
    @DisplayName("leastPopulatedHousehold breaks total-member ties by choosing fewest dependents")
    void leastPopulatedHouseholdBreaksTotalMemberTiesByFewestDependents() {
        SimClock clock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
        Household moreDependents = new Household(UUID.randomUUID(), 2, 2, 500L, 40L); // 4 members, 2 dependents
        Household fewerDependents = new Household(UUID.randomUUID(), 3, 1, 500L, 40L); // 4 members, 1 dependent
        Stockpile stockpile = new Stockpile(5000L);
        stockpile.depositCalories(2000L);

        Settlement settlement = new Settlement(
                UUID.randomUUID(),
                clock,
                stockpile,
                List.of(moreDependents, fewerDependents),
                11L
        );
        settlement.advanceShift();

        assertEquals(4, moreDependents.getTotalMembers());
        assertEquals(2, moreDependents.getDependents());
        assertEquals(5, fewerDependents.getTotalMembers(), "Tie on total members must pick household with fewer dependents");
        assertEquals(2, fewerDependents.getDependents());

        // Full tie on both totalMembers and dependents retains the earlier household
        SimClock tieClock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
        Household first = new Household(UUID.randomUUID(), 3, 1, 500L, 40L);
        Household second = new Household(UUID.randomUUID(), 3, 1, 500L, 40L);
        Stockpile tieStockpile = new Stockpile(5000L);
        tieStockpile.depositCalories(2000L);
        Settlement fullTie = new Settlement(UUID.randomUUID(), tieClock, tieStockpile, List.of(first, second), 11L);
        fullTie.advanceShift();

        assertEquals(5, first.getTotalMembers());
        assertEquals(4, second.getTotalMembers());
    }

    @Test
    @DisplayName("catchUpShifts accepts MAX_CATCHUP_SHIFTS inclusive and rejects Long.MIN_VALUE / Long.MAX_VALUE")
    void catchUpShiftsAcceptsMaxCatchupShiftsAndRejectsExtremes() {
        Settlement settlement = createHamlet(42L);

        assertThrows(IllegalArgumentException.class, () -> settlement.catchUpShifts(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> settlement.catchUpShifts(Long.MAX_VALUE));
        assertEquals(0L, settlement.getClock().getTotalShifts());

        BalanceOutcome outcome = settlement.catchUpShifts(SimulationConstants.MAX_CATCHUP_SHIFTS);
        assertEquals(SimulationConstants.MAX_CATCHUP_SHIFTS, settlement.getClock().getTotalShifts());
        assertTrue(outcome.totalPopulation() >= 4, "Resilience invariant: population never collapses");
    }

    @Test
    @DisplayName("constructor defensively copies caller households list")
    void defensiveCopyProtectsHouseholdsFromCallerListMutation() {
        Household hh = new Household(UUID.randomUUID(), 2, 1);
        List<Household> callerList = new ArrayList<>(List.of(hh));
        Settlement settlement = new Settlement(UUID.randomUUID(), new SimClock(), new Stockpile(100L), callerList, 1L);

        callerList.clear();

        assertEquals(1, settlement.getHouseholds().size());
        assertEquals(3, settlement.getTotalPopulation());
    }

    @Test
    @DisplayName("aggregate population exceeding Integer.MAX_VALUE across households throws ArithmeticException")
    void aggregatePopulationOverflowAcrossHouseholdsGuarded() {
        Household h1 = new Household(UUID.randomUUID(), Integer.MAX_VALUE / 2 + 10, 0);
        Household h2 = new Household(UUID.randomUUID(), Integer.MAX_VALUE / 2 + 10, 0);

        assertThrows(
                ArithmeticException.class,
                () -> new Settlement(UUID.randomUUID(), new SimClock(), new Stockpile(100L), List.of(h1, h2), 1L)
        );
    }
}
