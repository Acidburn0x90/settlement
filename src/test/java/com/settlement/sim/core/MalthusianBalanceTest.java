package com.settlement.sim.core;

import com.settlement.sim.core.town.Household;
import com.settlement.sim.core.town.MalthusianBalance;
import com.settlement.sim.core.town.MalthusianBalance.BalanceOutcome;
import com.settlement.sim.core.town.SettlementStage;
import com.settlement.sim.core.town.Stockpile;
import com.settlement.sim.core.world.Season;
import com.settlement.sim.core.world.SimClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;
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

        MalthusianBalance balance = new MalthusianBalance();
        BalanceOutcome outcome = balance.tickShift(clock, List.of(hh), stockpile, new SplittableRandom(42L));

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

        MalthusianBalance balance = new MalthusianBalance();
        BalanceOutcome outcome = balance.tickShift(clock, List.of(hh), stockpile, new SplittableRandom(99L));

        assertEquals(4, outcome.totalPopulation());
        assertTrue(outcome.populationDelta() > 0);
        assertFalse(outcome.stalledAtSubsistence());
        assertTrue(stockpile.getCalorieUnits() > 500L, "Surplus tithe should grow communal stockpile");
    }

    @Test
    @DisplayName("400-shift (1 full year) simulation is bit-identical for identical PRNG seeds")
    void fullYearRunIsDeterministicForSameSeed() {
        Stockpile run1 = simulateOneYear(123456789L);
        Stockpile run2 = simulateOneYear(123456789L);

        assertEquals(run1.getCalorieUnits(), run2.getCalorieUnits());
        assertEquals(run1.getSeedUnits(), run2.getSeedUnits());
    }

    @Test
    @DisplayName("null arguments throw IllegalArgumentException")
    void nullArgumentsRejected() {
        MalthusianBalance balance = new MalthusianBalance();
        SimClock clock = new SimClock();
        List<Household> households = List.of();
        Stockpile stockpile = new Stockpile(100L);
        SplittableRandom rng = new SplittableRandom(1L);

        assertThrows(IllegalArgumentException.class, () -> balance.tickShift(null, households, stockpile, rng));
        assertThrows(IllegalArgumentException.class, () -> balance.tickShift(clock, null, stockpile, rng));
        assertThrows(IllegalArgumentException.class, () -> balance.tickShift(clock, households, null, rng));
        assertThrows(IllegalArgumentException.class, () -> balance.tickShift(clock, households, stockpile, null));
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

        MalthusianBalance balance = new MalthusianBalance();
        SplittableRandom rng = new SplittableRandom(seed);
        for (int i = 0; i < 400; i++) {
            balance.tickShift(clock, households, stockpile, rng);
        }
        return stockpile;
    }
}
