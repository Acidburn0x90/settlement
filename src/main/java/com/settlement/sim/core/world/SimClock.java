package com.settlement.sim.core.world;

/**
 * Pure-Java simulation clock running on 4 sub-daily shifts per Minecraft day
 * across a 100-day seasonal year (25 days / 100 shifts per season).
 * <p>
 * Tracking total elapsed {@code long} shifts makes Phase 5 offscreen catch-up math
 * deterministic via {@link #advanceShifts(long)}.
 */
public class SimClock implements SeasonPort {

    /** Number of economic and AI shifts in one Minecraft day (6,000 ticks each). */
    public static final int SHIFTS_PER_DAY = 4;

    /** Number of Minecraft days in each of the four seasons. */
    public static final int DAYS_PER_SEASON = 25;

    /** Total Minecraft days in one full simulated year. */
    public static final int DAYS_PER_YEAR = 100;

    /**
     * Sub-daily work and rest phases within a single Minecraft day.
     */
    public enum Shift {
        MORNING,
        AFTERNOON,
        EVENING,
        NIGHT
    }

    /** Monotonic count of elapsed simulation shifts since world/settlement epoch. */
    private long totalShifts;

    /**
     * Creates a clock starting at shift {@code 0} (Day 1, {@link Shift#MORNING}, {@link Season#SPRING}).
     */
    public SimClock() {
        this(0L);
    }

    /**
     * Creates a clock initialized at a specific elapsed shift count (used for persistence/catch-up).
     *
     * @param initialShifts non-negative elapsed shift count
     * @throws IllegalArgumentException if {@code initialShifts < 0}
     */
    public SimClock(long initialShifts) {
        if (initialShifts < 0) {
            throw new IllegalArgumentException("initialShifts must be >= 0");
        }
        this.totalShifts = initialShifts;
    }

    /**
     * Advances the simulation clock by exactly one shift.
     */
    public void advanceShift() {
        this.totalShifts += 1;
    }

    /**
     * Advances the simulation clock by {@code count} shifts (e.g., for offscreen catch-up).
     *
     * @param count non-negative number of shifts to advance
     * @throws IllegalArgumentException if {@code count < 0}
     */
    public void advanceShifts(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        this.totalShifts += count;
    }

    /**
     * Returns the total number of elapsed shifts since epoch.
     *
     * @return non-negative elapsed shift count
     */
    public long getTotalShifts() {
        return totalShifts;
    }

    /**
     * Returns the active sub-daily {@link Shift} for the current tick.
     *
     * @return current {@link Shift} ({@code MORNING}..{@code NIGHT})
     */
    public Shift getCurrentShift() {
        return Shift.values()[(int) (totalShifts % SHIFTS_PER_DAY)];
    }

    /**
     * Computes the 1-based day index within the 100-day annual cycle.
     *
     * @return day of year in {@code 1..100}
     */
    @Override
    public int getDayOfYear() {
        // Integer division converts shifts -> elapsed days; modulo wraps every 100 days; +1 makes it 1-based
        return (int) ((totalShifts / SHIFTS_PER_DAY) % DAYS_PER_YEAR) + 1;
    }

    /**
     * Maps the current 1-based day of year into one of the four 25-day seasons.
     *
     * @return {@link Season#SPRING} (1–25), {@link Season#SUMMER} (26–50),
     *         {@link Season#AUTUMN} (51–75), or {@link Season#WINTER} (76–100)
     */
    @Override
    public Season getCurrentSeason() {
        return Season.values()[(getDayOfYear() - 1) / DAYS_PER_SEASON];
    }
}
