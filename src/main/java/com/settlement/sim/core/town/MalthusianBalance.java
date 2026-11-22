package com.settlement.sim.core.town;

import com.settlement.sim.core.SimulationConstants;
import com.settlement.sim.core.world.Season;
import com.settlement.sim.core.world.SimClock;

import java.util.List;

/**
 * Stateless domain service evaluating one simulation shift of agrarian production,
 * communal surplus sharing, subsistence consumption, and Malthusian growth vs. stall equilibrium.
 * <p>
 * <b>Resilience Invariant:</b> Settlements operate on satisficing principles.
 * When grain yields or reserves fall below subsistence thresholds, demographic
 * growth stalls ({@code populationDelta == 0}) rather than collapsing to zero;
 * ring-fenced {@code seedUnits} are never consumed.
 * <p>
 * Pure-Java domain model with zero Minecraft dependencies.
 */
public final class MalthusianBalance {

    /** IEEE-754 double unit-interval scale factor (2^-53) for SplitMix64 mixing. */
    private static final double DOUBLE_UNIT = 0x1.0p-53;

    private MalthusianBalance() {
        // Stateless utility/service class
    }

    /**
     * Immutable summary of one simulated shift tick.
     *
     * @param totalPopulation      aggregate citizen count across all households
     * @param populationDelta      recommended population change ({@code >= 0}; never negative)
     * @param stalledAtSubsistence {@code true} if the town is below the surplus growth threshold or had an unmet deficit
     * @param recommendedStage     Christaller hierarchy stage matching {@code totalPopulation}
     */
    public record BalanceOutcome(
            int totalPopulation,
            int populationDelta,
            boolean stalledAtSubsistence,
            SettlementStage recommendedStage
    ) {}

    /**
     * Advances the settlement economy by one shift using deterministic, stateless
     * SplitMix64 weather mixing derived from {@code (seed, clock.getTotalShifts())}.
     *
     * @param clock      simulation clock (advanced by 1 shift at the end of the step)
     * @param households list of households belonging to the settlement
     * @param stockpile  communal granary ledger
     * @param seed       settlement PRNG seed (combined with shift index for persistence-safe determinism)
     * @return {@link BalanceOutcome} describing growth vs. subsistence stall and hierarchy stage
     * @throws IllegalArgumentException if {@code clock}, {@code households}, or {@code stockpile} is {@code null}
     */
    public static BalanceOutcome tickShift(
            SimClock clock,
            List<Household> households,
            Stockpile stockpile,
            long seed
    ) {
        if (clock == null || households == null || stockpile == null) {
            throw new IllegalArgumentException("clock, households, and stockpile must not be null");
        }

        // Deterministic, zero-allocation weather variance derived from (seed, shiftIndex)
        double seasonFactor = seasonalYieldMultiplier(clock.getCurrentSeason());
        double weatherFactor = SimulationConstants.WEATHER_VARIANCE_MIN
                + SimulationConstants.WEATHER_VARIANCE_SPAN * unitDoubleFromShift(seed, clock.getTotalShifts());

        int totalPopulation = 0;
        boolean anyUnmetDeficit = false;

        for (int i = 0, n = households.size(); i < n; i++) {
            Household household = households.get(i);
            int members = household.getTotalMembers();
            totalPopulation += members;

            // 1. Harvest based on active workers, season, and weather (0 during WINTER)
            long harvest = Math.round(
                    household.getWorkers()
                            * SimulationConstants.BASE_HARVEST_PER_WORKER_PER_SHIFT
                            * seasonFactor
                            * weatherFactor
            );

            // 2. Household covers domestic seed & 1-day subsistence buffer first, then shares surplus
            long seedTarget = household.getWorkers() * SimulationConstants.SEED_TARGET_PER_WORKER;
            long subsistenceBuffer = (long) members
                    * SimulationConstants.CALORIES_PER_MEMBER_PER_SHIFT
                    * SimulationConstants.SUBSISTENCE_BUFFER_SHIFTS;
            long communalContribution = household.allocateHarvest(
                    harvest,
                    seedTarget,
                    subsistenceBuffer,
                    SimulationConstants.COMMUNAL_SURPLUS_SHARE
            );
            long accepted = stockpile.depositCalories(communalContribution);
            if (accepted < communalContribution) {
                // Conserve overflow calories domestically when the communal granary is at capacity
                household.depositDomesticCalories(communalContribution - accepted);
            }

            // 3. Consume this shift's calories; draw emergency rations from communal Stockpile on deficit
            long deficit = household.consumeShift(SimulationConstants.CALORIES_PER_MEMBER_PER_SHIFT);
            if (deficit > SimulationConstants.NO_DEFICIT) {
                long covered = stockpile.withdrawCalories(deficit);
                if (covered < deficit) {
                    // Track unmet shortfall so the settlement stalls rather than growing on empty reserves
                    anyUnmetDeficit = true;
                }
            }
        }

        // Resilience invariant: growth requires full nutrition + 3-day communal surplus; deficits stall at 0 delta
        boolean canGrow = !anyUnmetDeficit
                && totalPopulation > 0
                && stockpile.isAboveSubsistence(
                        totalPopulation,
                        SimulationConstants.CALORIES_PER_MEMBER_PER_SHIFT,
                        SimulationConstants.GROWTH_BUFFER_SHIFTS
                );
        int populationDelta = canGrow
                ? SimulationConstants.GROWTH_POPULATION_STEP
                : SimulationConstants.STALL_POPULATION_STEP;
        boolean stalledAtSubsistence = !canGrow;

        // Advance simulation clock by 1 shift (6,000 ticks) after evaluating the step
        clock.advanceShift();

        return new BalanceOutcome(
                totalPopulation,
                populationDelta,
                stalledAtSubsistence,
                SettlementStage.forPopulation(totalPopulation)
        );
    }

    /**
     * Maps each {@link Season} to its baseline agricultural yield multiplier.
     *
     * @param season active season from {@link SimClock#getCurrentSeason()}
     * @return seasonal yield multiplier ({@code SPRING}, {@code SUMMER}, {@code AUTUMN}, or {@code WINTER})
     * @throws IllegalArgumentException if {@code season} is {@code null}
     */
    public static double seasonalYieldMultiplier(Season season) {
        if (season == null) {
            throw new IllegalArgumentException("season must not be null");
        }
        return switch (season) {
            case SPRING -> SimulationConstants.SPRING_YIELD_MULTIPLIER;
            case SUMMER -> SimulationConstants.SUMMER_YIELD_MULTIPLIER;
            case AUTUMN -> SimulationConstants.AUTUMN_YIELD_MULTIPLIER;
            case WINTER -> SimulationConstants.WINTER_YIELD_MULTIPLIER;
        };
    }

    /**
     * Pure SplitMix64 hash producing a deterministic uniform double in {@code [0.0, 1.0)}
     * with zero heap allocations and no mutable PRNG state to serialize.
     */
    private static double unitDoubleFromShift(long seed, long shiftIndex) {
        long z = seed + shiftIndex * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z >>> 11) * DOUBLE_UNIT;
    }
}
