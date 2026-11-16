package com.settlement.sim.core;

import com.settlement.sim.core.world.Season;
import com.settlement.sim.core.world.SimClock;
import com.settlement.sim.core.world.SimClock.Shift;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimClockTest {

    @Test
    @DisplayName("new clock starts at shift 0, day 1, MORNING, SPRING")
    void initialStateIsDayOneMorningSpring() {
        SimClock clock = new SimClock();

        assertEquals(0L, clock.getTotalShifts());
        assertEquals(Shift.MORNING, clock.getCurrentShift());
        assertEquals(1, clock.getDayOfYear());
        assertEquals(Season.SPRING, clock.getCurrentSeason());
        assertEquals(100, clock.getDaysPerYear());
    }

    @Test
    @DisplayName("advanceShift cycles through 4 daily shifts and rolls to day 2")
    void advanceShiftCyclesThroughFourShiftsPerDay() {
        SimClock clock = new SimClock();

        assertEquals(Shift.MORNING, clock.getCurrentShift());
        clock.advanceShift();
        assertEquals(Shift.AFTERNOON, clock.getCurrentShift());
        clock.advanceShift();
        assertEquals(Shift.EVENING, clock.getCurrentShift());
        clock.advanceShift();
        assertEquals(Shift.NIGHT, clock.getCurrentShift());
        assertEquals(1, clock.getDayOfYear());

        clock.advanceShift();
        assertEquals(Shift.MORNING, clock.getCurrentShift());
        assertEquals(2, clock.getDayOfYear());
        assertEquals(4L, clock.getTotalShifts());
    }

    @Test
    @DisplayName("25-day season boundaries transition cleanly across SPRING, SUMMER, AUTUMN, WINTER")
    void seasonBoundariesTransitionEvery25Days() {
        SimClock clock = new SimClock();

        // Day 25 (last day of SPRING) -> 24 full days = 96 shifts
        clock.advanceShifts(24L * SimClock.SHIFTS_PER_DAY);
        assertEquals(25, clock.getDayOfYear());
        assertEquals(Season.SPRING, clock.getCurrentSeason());

        // Day 26 (first day of SUMMER)
        clock.advanceShifts(SimClock.SHIFTS_PER_DAY);
        assertEquals(26, clock.getDayOfYear());
        assertEquals(Season.SUMMER, clock.getCurrentSeason());

        // Day 50 (last day of SUMMER) -> +24 days
        clock.advanceShifts(24L * SimClock.SHIFTS_PER_DAY);
        assertEquals(50, clock.getDayOfYear());
        assertEquals(Season.SUMMER, clock.getCurrentSeason());

        // Day 51 (first day of AUTUMN)
        clock.advanceShifts(SimClock.SHIFTS_PER_DAY);
        assertEquals(51, clock.getDayOfYear());
        assertEquals(Season.AUTUMN, clock.getCurrentSeason());

        // Day 76 (first day of WINTER) -> +25 days
        clock.advanceShifts(25L * SimClock.SHIFTS_PER_DAY);
        assertEquals(76, clock.getDayOfYear());
        assertEquals(Season.WINTER, clock.getCurrentSeason());

        // Day 100 (last day of WINTER) -> +24 days
        clock.advanceShifts(24L * SimClock.SHIFTS_PER_DAY);
        assertEquals(100, clock.getDayOfYear());
        assertEquals(Season.WINTER, clock.getCurrentSeason());
    }

    @Test
    @DisplayName("400 shifts wraps dayOfYear back to 1 and Season back to SPRING")
    void yearWrapsAroundAfter400Shifts() {
        SimClock clock = new SimClock();
        clock.advanceShifts(400L);

        assertEquals(400L, clock.getTotalShifts());
        assertEquals(1, clock.getDayOfYear());
        assertEquals(Shift.MORNING, clock.getCurrentShift());
        assertEquals(Season.SPRING, clock.getCurrentSeason());
    }

    @Test
    @DisplayName("negative initial shifts or negative advanceShifts throws IllegalArgumentException")
    void negativeShiftsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimClock(-1L));

        SimClock clock = new SimClock();
        assertThrows(IllegalArgumentException.class, () -> clock.advanceShifts(-1L));
    }
}
