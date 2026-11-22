package com.settlement.sim.core;

import com.settlement.sim.core.SimulationConstants;
import com.settlement.sim.core.town.Household;
import com.settlement.sim.core.town.MalthusianBalance;
import com.settlement.sim.core.town.MalthusianBalance.BalanceOutcome;
import com.settlement.sim.core.town.SettlementStage;
import com.settlement.sim.core.town.Stockpile;
import com.settlement.sim.core.world.Season;
import com.settlement.sim.core.world.SimClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MalthusianBalanceTest {

    @Test
    @DisplayName("winter deficit stalls population at subsistence without touching ring-fenced seedUnits")
    void winterDeficitStallsWithoutKillingOrTouchingSeed() {
        // Start directly on Day 76 (WINTER) where field harvest is 0
        SimClock clock = new SimClock(75L * SimClock.SHIFTS_PER_DAY);
        assertEquals(Season.WINTER, clock.getCurrentSeason());

        Household hh = new Household(UUID.randomUUID(), 1, 2, 0L, 20L);
        Stockpile stockpile = new Stockpile(500L);
        stockpile.depositSeed(50L);
        stockpile.depositCalories(10L); // Less than 30 needed for 3 members

        BalanceOutcome outcome = MalthusianBalance.tickShift(clock, List.of(hh), stockpile, 42L);

        assertEquals(3, outcome.totalPopulation());
        assertEquals(0, outcome.populationDelta(), "Villages must stall, never collapse below 0 delta");
        assertTrue(outcome.stalledAtSubsistence());
        assertEquals(SettlementStage.HAMLET, outcome.recommendedStage());
        assertEquals(0L, stockpile.getCalorieUnits());
        assertEquals(50L, stockpile.getSeedUnits(), "Communal seedUnits must remain untouched");
        assertEquals(20L, hh.getDomesticSeed(), "Domestic seed must remain untouched");
        assertEquals(75L * SimClock.SHIFTS_PER_DAY + 1L, clock.getTotalShifts());
    }

    @Test
    @DisplayName("consistent summer surplus fills buffers and triggers positive population growth")
    void summerSurplusTriggersPositiveGrowth() {
        // Start on Day 26 (SUMMER, 1.20x yield)
        SimClock clock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
        assertEquals(Season.SUMMER, clock.getCurrentSeason());

        Household hh = new Household(UUID.randomUUID(), 4, 0, 200L, 40L);
        Stockpile stockpile = new Stockpile(2000L);
        stockpile.depositCalories(500L); // Above GROWTH_BUFFER_SHIFTS (4 * 10 * 12 = 480)

        BalanceOutcome outcome = MalthusianBalance.tickShift(clock, List.of(hh), stockpile, 99L);

        assertEquals(4, outcome.totalPopulation());
        assertTrue(outcome.populationDelta() > 0);
        assertFalse(outcome.stalledAtSubsistence());
        assertTrue(stockpile.getCalorieUnits() > 500L, "Surplus share should grow communal stockpile");
    }

    @Test
    @DisplayName("full communal stockpile returns surplus overflow to household instead of destroying calories")
    void fullStockpileReturnsOverflowToHousehold() {
        SimClock clock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
        Household hh = new Household(UUID.randomUUID(), 4, 0, 200L, 40L);
        Stockpile fullStockpile = new Stockpile(100L);
        fullStockpile.depositCalories(100L); // Already 100% full

        long beforeDomestic = hh.getDomesticCalories();
        MalthusianBalance.tickShift(clock, List.of(hh), fullStockpile, 7L);

        assertEquals(100L, fullStockpile.getCalorieUnits());
        // Household produced > 40 needed for consumption and kept 100% of surplus because granary was full
        assertTrue(hh.getDomesticCalories() > beforeDomestic);
    }

    @Test
    @DisplayName("400-shift (1 full year) simulation is bit-identical for identical seeds")
    void fullYearRunIsDeterministicForSameSeed() {
        Stockpile run1 = simulateOneYear(123456789L);
        Stockpile run2 = simulateOneYear(123456789L);

        assertEquals(run1.getCalorieUnits(), run2.getCalorieUnits());
        assertEquals(run1.getSeedUnits(), run2.getSeedUnits());
    }

    @Test
    @DisplayName("null arguments throw IllegalArgumentException")
    void nullArgumentsRejected() {
        SimClock clock = new SimClock();
        List<Household> households = List.of();
        Stockpile stockpile = new Stockpile(100L);

        assertThrows(IllegalArgumentException.class, () -> MalthusianBalance.tickShift(null, households, stockpile, 1L));
        assertThrows(IllegalArgumentException.class, () -> MalthusianBalance.tickShift(clock, null, stockpile, 1L));
        assertThrows(IllegalArgumentException.class, () -> MalthusianBalance.tickShift(clock, households, null, 1L));
        assertThrows(IllegalArgumentException.class, () -> MalthusianBalance.seasonalYieldMultiplier(null));
    }

    private static Stockpile simulateOneYear(long seed) {
        SimClock clock = new SimClock();
        List<Household> households = List.of(
                new Household(UUID.nameUUIDFromBytes("h1".getBytes()), 3, 1, 50L, 20L),
                new Household(UUID.nameUUIDFromBytes("h2".getBytes()), 2, 2, 40L, 20L)
        );
        Stockpile stockpile = new Stockpile(10_000L);
        stockpile.depositSeed(100L);
        stockpile.depositCalories(200L);

        for (int i = 0; i < 400; i++) {
            MalthusianBalance.tickShift(clock, households, stockpile, seed);
        }
        return stockpile;
    }

    @Test
    @DisplayName("seasonalYieldMultiplier maps all 4 seasons to exact SimulationConstants multipliers")
    void seasonalYieldMultiplierCoversAllFourSeasons() {
        assertEquals(SimulationConstants.SPRING_YIELD_MULTIPLIER, MalthusianBalance.seasonalYieldMultiplier(Season.SPRING));
        assertEquals(SimulationConstants.SUMMER_YIELD_MULTIPLIER, MalthusianBalance.seasonalYieldMultiplier(Season.SUMMER));
        assertEquals(SimulationConstants.AUTUMN_YIELD_MULTIPLIER, MalthusianBalance.seasonalYieldMultiplier(Season.AUTUMN));
        assertEquals(SimulationConstants.WINTER_YIELD_MULTIPLIER, MalthusianBalance.seasonalYieldMultiplier(Season.WINTER));
        assertEquals(0.0, MalthusianBalance.seasonalYieldMultiplier(Season.WINTER));
    }

    @Test
    @DisplayName("distinct PRNG seeds produce distinct cumulative harvest yields")
    void distinctSeedsProduceDistinctCumulativeYields() {
        long[] seeds = {0L, 1L, 42L, 123456789L, -987654321L};
        Set<Long> totalYieldCalories = new HashSet<>();

        for (long seed : seeds) {
            SimClock clock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
            Household hh = new Household(UUID.nameUUIDFromBytes("seed-hh".getBytes()), 10, 0, 400L, 100L);
            Stockpile stockpile = new Stockpile(100_000L);

            for (int i = 0; i < 20; i++) {
                MalthusianBalance.tickShift(clock, List.of(hh), stockpile, seed);
            }
            totalYieldCalories.add(hh.getDomesticCalories() + stockpile.getCalorieUnits());
        }

        assertEquals(seeds.length, totalYieldCalories.size(), "Each distinct seed should yield a distinct harvest stream");
    }

    @Test
    @DisplayName("emergency communal rationing covers household deficit and grows only when remaining >= GROWTH_BUFFER_SHIFTS")
    void emergencyCommunalRationingCoversDeficitAndChecksGrowthBuffer() {
        // 1 citizen consumes 10 cal/shift; GROWTH_BUFFER_SHIFTS (12) requires 120 cal remaining after rationing
        SimClock growthClock = new SimClock(75L * SimClock.SHIFTS_PER_DAY);
        Household hhGrowth = new Household(UUID.randomUUID(), 1, 0, 0L, 15L);
        Stockpile stockpileExact = new Stockpile(500L);
        stockpileExact.depositSeed(50L);
        stockpileExact.depositCalories(130L); // 10 emergency ration + 120 exact growth buffer

        BalanceOutcome growthOutcome = MalthusianBalance.tickShift(growthClock, List.of(hhGrowth), stockpileExact, 5L);

        assertEquals(120L, stockpileExact.getCalorieUnits());
        assertEquals(50L, stockpileExact.getSeedUnits());
        assertEquals(15L, hhGrowth.getDomesticSeed());
        assertEquals(0L, hhGrowth.getDomesticCalories());
        assertEquals(1, growthOutcome.populationDelta());
        assertFalse(growthOutcome.stalledAtSubsistence());

        // 1 calorie short of GROWTH_BUFFER_SHIFTS after covering the 10-calorie deficit -> stalls
        SimClock stallClock = new SimClock(75L * SimClock.SHIFTS_PER_DAY);
        Household hhStall = new Household(UUID.randomUUID(), 1, 0, 0L, 15L);
        Stockpile stockpileShort = new Stockpile(500L);
        stockpileShort.depositCalories(129L); // 10 emergency ration + 119 (< 120)

        BalanceOutcome stallOutcome = MalthusianBalance.tickShift(stallClock, List.of(hhStall), stockpileShort, 5L);

        assertEquals(119L, stockpileShort.getCalorieUnits());
        assertEquals(0, stallOutcome.populationDelta());
        assertTrue(stallOutcome.stalledAtSubsistence());
    }

    @Test
    @DisplayName("empty households list stalls at zero population and advances clock by one shift")
    void emptyHouseholdsListStallsAtZeroPopulationAndAdvancesClock() {
        SimClock clock = new SimClock(5L);
        Stockpile stockpile = new Stockpile(1000L);
        stockpile.depositCalories(500L);

        BalanceOutcome outcome = MalthusianBalance.tickShift(clock, List.of(), stockpile, 7L);

        assertEquals(0, outcome.totalPopulation());
        assertEquals(0, outcome.populationDelta());
        assertTrue(outcome.stalledAtSubsistence());
        assertEquals(SettlementStage.HAMLET, outcome.recommendedStage());
        assertEquals(6L, clock.getTotalShifts());
        assertEquals(500L, stockpile.getCalorieUnits());
    }

    @Test
    @DisplayName("zero-worker household produces nothing in summer and draws communal rations without touching seed")
    void zeroWorkerHouseholdSurvivesOnCommunalReservesWithoutTouchingSeed() {
        SimClock clock = new SimClock(25L * SimClock.SHIFTS_PER_DAY);
        Household hh = new Household(UUID.randomUUID(), 0, 2, 0L, 10L);
        Stockpile stockpile = new Stockpile(500L);
        stockpile.depositCalories(25L);

        BalanceOutcome outcome = MalthusianBalance.tickShift(clock, List.of(hh), stockpile, 3L);

        assertEquals(2, outcome.totalPopulation());
        assertEquals(0, outcome.populationDelta());
        assertTrue(outcome.stalledAtSubsistence());
        assertEquals(5L, stockpile.getCalorieUnits());
        assertEquals(10L, hh.getDomesticSeed());
    }

    @Test
    @DisplayName("null household element inside households list throws IllegalArgumentException")
    void nullHouseholdElementThrowsIllegalArgumentException() {
        SimClock clock = new SimClock();
        List<Household> withNull = Arrays.asList((Household) null);
        Stockpile stockpile = new Stockpile(100L);

        assertThrows(
                IllegalArgumentException.class,
                () -> MalthusianBalance.tickShift(clock, withNull, stockpile, 1L)
        );
    }

    @Test
    @DisplayName("aggregate population exceeding Integer.MAX_VALUE across households throws ArithmeticException")
    void aggregatePopulationOverflowAcrossHouseholdsThrowsArithmeticException() {
        SimClock clock = new SimClock(75L * SimClock.SHIFTS_PER_DAY);
        Household h1 = new Household(UUID.randomUUID(), Integer.MAX_VALUE / 2 + 10, 0);
        Household h2 = new Household(UUID.randomUUID(), Integer.MAX_VALUE / 2 + 10, 0);
        Stockpile stockpile = new Stockpile(100L);

        assertThrows(
                ArithmeticException.class,
                () -> MalthusianBalance.tickShift(clock, List.of(h1, h2), stockpile, 1L)
        );
    }
}
