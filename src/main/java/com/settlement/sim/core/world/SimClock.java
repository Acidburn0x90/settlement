package com.settlement.sim.core.world;

/**
 * Pure-Java simulation clock running on 4 sub-daily shifts per Minecraft day
 * across a 100-day seasonal year (25 days / 100 shifts per season).
 */
public class SimClock implements SeasonPort {

    public static final int SHIFTS_PER_DAY = 4;
    public static final int DAYS_PER_SEASON = 25;
    public static final int DAYS_PER_YEAR = 100;

    public enum Shift {
        MORNING,
        AFTERNOON,
        EVENING,
        NIGHT
    }

    private long totalShifts;
    private static final Season[] SEASONS = Season.values();
    private static final Shift[] SHIFTS = Shift.values();

    public SimClock() {
        this(0L);
    }

    public SimClock(long initialShifts) {
        if (initialShifts >= 0) totalShifts = initialShifts;
        else throw new IllegalArgumentException("initialShifts must be >= 0");
    }

    public void advanceShift() {
        totalShifts += 1;
    }

    public void advanceShifts(long count) {
        if (count >= 0) totalShifts += count;
        else throw new IllegalArgumentException("count must be >= 0");
    }

    public long getTotalShifts() {
        return totalShifts;
    }

    public Shift getCurrentShift() {
        return SHIFTS[(int) (totalShifts % SHIFTS_PER_DAY)];
    }

    @Override
    public int getDayOfYear() {
        return (int) ((totalShifts / SHIFTS_PER_DAY) % DAYS_PER_YEAR) + 1;
    }

    @Override
    public Season getCurrentSeason() {
        return SEASONS[(getDayOfYear() - 1) / DAYS_PER_SEASON];
    }
}
