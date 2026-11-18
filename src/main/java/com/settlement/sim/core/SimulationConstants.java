package com.settlement.sim.core;

/**
 * Central tuning constants for the pure-Java settlement simulation core.
 * <p>
 * Consolidates calendar cadences, agrarian yields, communal surplus sharing,
 * and resilience boundaries in one place so formulas read as plain domain rules.
 */
public final class SimulationConstants {

    private SimulationConstants() {
        // Utility class; prevent instantiation
    }

    // -------------------------------------------------------------------------
    // Calendar & Shift Cadence
    // -------------------------------------------------------------------------

    /** Number of economic and AI shifts in one Minecraft day (6,000 ticks each). */
    public static final int SHIFTS_PER_DAY = 4;

    /** Number of Minecraft days in each of the four seasons. */
    public static final int DAYS_PER_SEASON = 25;

    /** Total Minecraft days in one full simulated year. */
    public static final int DAYS_PER_YEAR = 100;

    // -------------------------------------------------------------------------
    // Household Consumption & Chayanovian Bounds
    // -------------------------------------------------------------------------

    /** Calories consumed by each citizen per 6,000-tick shift (40 calories/day across 4 shifts). */
    public static final int CALORIES_PER_MEMBER_PER_SHIFT = 10;

    /** Floor applied to active workers when computing dependency ratio to avoid divide-by-zero. */
    public static final int MIN_WORKERS_FOR_RATIO = 1;

    /** Minimum non-negative reserve floor for domestic and communal ledgers. */
    public static final long ZERO_RESERVE = 0L;

    /** Sentinel value indicating zero unmet calorie deficit for a household. */
    public static final long NO_DEFICIT = 0L;

    /** Lower bound for valid communal surplus share fractions (0%). */
    public static final double MIN_SURPLUS_SHARE_RATE = 0.0;

    /** Upper bound for valid communal surplus share fractions (100%). */
    public static final double MAX_SURPLUS_SHARE_RATE = 1.0;

    // -------------------------------------------------------------------------
    // Agrarian Production, Seasonal Yields & Surplus Sharing
    // -------------------------------------------------------------------------

    /** Baseline harvest output per active worker per shift during growing seasons. */
    public static final long BASE_HARVEST_PER_WORKER_PER_SHIFT = 22L;

    /** Default fraction of household surplus contributed to the communal stockpile (JASSS p_s). */
    public static final double COMMUNAL_SURPLUS_SHARE = 0.20;

    /** Number of shifts of domestic food each household retains before sharing surplus (1 full day). */
    public static final int SUBSISTENCE_BUFFER_SHIFTS = 4;

    /** Number of shifts of communal stockpile surplus required to trigger growth (3 full days). */
    public static final int GROWTH_BUFFER_SHIFTS = 12;

    /** Ring-fenced domestic seed-corn units target per active worker before sharing surplus. */
    public static final long SEED_TARGET_PER_WORKER = 10L;

    /** Minimum seasonal weather yield multiplier (-20% variance floor). */
    public static final double WEATHER_VARIANCE_MIN = 0.80;

    /** Width of uniform seasonal weather multiplier band (0.80 to 1.20 = +/-20%). */
    public static final double WEATHER_VARIANCE_SPAN = 0.40;

    /** Spring agricultural yield multiplier (early planting / lower harvest). */
    public static final double SPRING_YIELD_MULTIPLIER = 0.85;

    /** Summer agricultural yield multiplier (peak grain harvest). */
    public static final double SUMMER_YIELD_MULTIPLIER = 1.20;

    /** Autumn agricultural yield multiplier (late harvest & winter sowing). */
    public static final double AUTUMN_YIELD_MULTIPLIER = 1.00;

    /** Winter agricultural yield multiplier (no field harvest; living off granary reserves). */
    public static final double WINTER_YIELD_MULTIPLIER = 0.00;

    // -------------------------------------------------------------------------
    // Malthusian Growth vs. Stall Equilibrium
    // -------------------------------------------------------------------------

    /** Population delta emitted when communal surplus and nutrition allow expansion. */
    public static final int GROWTH_POPULATION_STEP = 1;

    /** Population delta emitted when the settlement stalls at subsistence equilibrium. */
    public static final int STALL_POPULATION_STEP = 0;
}
