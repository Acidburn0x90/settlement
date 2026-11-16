package com.settlement.sim.core.world;

/**
 * Output port for querying environmental calendar state within a 100-day Minecraft year.
 * <p>
 * Implementations adapt external season mods or vanilla world daylight trackers into the
 * pure simulation core.
 */
public interface SeasonPort {

    /**
     * Gets the current season based on the elapsed year cycle.
     *
     * @return the active {@link Season}
     */
    Season getCurrentSeason();

    /**
     * Gets the current day of the year (1..100).
     *
     * @return current day index within the 100-day annual cycle
     */
    int getDayOfYear();

    /**
     * Gets the annual cycle length in in-game days.
     * Default target is 100 days per year.
     *
     * @return total days per simulated year
     */
    default int getDaysPerYear() {
        return 100;
    }
}
