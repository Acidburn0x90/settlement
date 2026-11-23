package com.settlement.sim.core;

import com.settlement.sim.core.world.Season;
import com.settlement.sim.core.world.SeasonPort;
import com.settlement.sim.core.world.SimClock;
import com.settlement.sim.core.world.SimClock.Shift;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("advanceShifts(0L) leaves clock state completely unchanged")
    void advanceShiftsZeroLeavesClockUnchanged() {
        SimClock clock = new SimClock(17L);
        Shift beforeShift = clock.getCurrentShift();
        int beforeDay = clock.getDayOfYear();
        Season beforeSeason = clock.getCurrentSeason();

        clock.advanceShifts(0L);

        assertEquals(17L, clock.getTotalShifts());
        assertEquals(beforeShift, clock.getCurrentShift());
        assertEquals(beforeDay, clock.getDayOfYear());
        assertEquals(beforeSeason, clock.getCurrentSeason());
    }

    @Test
    @DisplayName("advanceShift and advanceShifts guard Long.MAX_VALUE overflow with ArithmeticException")
    void advanceShiftAndAdvanceShiftsGuardLongMaxValueOverflow() {
        SimClock atMax = new SimClock(Long.MAX_VALUE);

        assertEquals(Shift.NIGHT, atMax.getCurrentShift());
        assertTrue(atMax.getDayOfYear() >= 1 && atMax.getDayOfYear() <= 100);
        assertNotNull(atMax.getCurrentSeason());

        atMax.advanceShifts(0L);
        assertEquals(Long.MAX_VALUE, atMax.getTotalShifts());

        assertThrows(ArithmeticException.class, atMax::advanceShift);
        assertThrows(ArithmeticException.class, () -> atMax.advanceShifts(1L));
        assertThrows(ArithmeticException.class, () -> atMax.advanceShifts(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, atMax.getTotalShifts());

        SimClock nearMax = new SimClock(Long.MAX_VALUE - 3L);
        assertThrows(ArithmeticException.class, () -> nearMax.advanceShifts(4L));
        assertEquals(Long.MAX_VALUE - 3L, nearMax.getTotalShifts());

        nearMax.advanceShifts(3L);
        assertEquals(Long.MAX_VALUE, nearMax.getTotalShifts());
    }

    @Test
    @DisplayName("SeasonPort contract holds across every shift of a 100-day year and default getDaysPerYear")
    void seasonPortContractHoldsAcrossFullYearAndDefaultMethod() {
        SeasonPort customPort = new SeasonPort() {
            @Override
            public Season getCurrentSeason() {
                return Season.SPRING;
            }

            @Override
            public int getDayOfYear() {
                return 1;
            }
        };
        assertEquals(100, customPort.getDaysPerYear());

        SimClock clock = new SimClock();
        SeasonPort port = clock;

        for (int shift = 0; shift < 400; shift++) {
            int expectedDay = (shift / SimClock.SHIFTS_PER_DAY) + 1;
            Season expectedSeason = switch ((expectedDay - 1) / SimClock.DAYS_PER_SEASON) {
                case 0 -> Season.SPRING;
                case 1 -> Season.SUMMER;
                case 2 -> Season.AUTUMN;
                default -> Season.WINTER;
            };
            assertEquals(100, port.getDaysPerYear());
            assertEquals(expectedDay, port.getDayOfYear());
            assertEquals(expectedSeason, port.getCurrentSeason());
            clock.advanceShift();
        }

        assertEquals(1, port.getDayOfYear());
        assertEquals(Season.SPRING, port.getCurrentSeason());
    }

    @Test
    @DisplayName("Long.MIN_VALUE initial shifts or advance count throws IllegalArgumentException")
    void minValueNegativeBoundariesRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimClock(Long.MIN_VALUE));

        SimClock clock = new SimClock(10L);
        assertThrows(IllegalArgumentException.class, () -> clock.advanceShifts(Long.MIN_VALUE));
        assertEquals(10L, clock.getTotalShifts());
    }
}
