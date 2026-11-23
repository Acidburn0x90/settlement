package com.settlement.sim.core.town;

import com.settlement.sim.core.SimulationConstants;
import com.settlement.sim.core.town.MalthusianBalance.BalanceOutcome;
import com.settlement.sim.core.world.SimClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Tier 3 aggregate root representing a single autonomous settlement.
 * <p>
 * Owns the settlement's {@link SimClock}, communal {@link Stockpile}, constituent
 * {@link Household} units, deterministic {@code seed}, and {@link SettlementStage}.
 * Executing {@link #advanceShift()} or {@link #catchUpShifts(long)} closes the
 * autonomous Malthusian growth loop without any Minecraft dependencies or
 * unserializable PRNG state.
 */
public class Settlement {

    /** Unique immutable identifier for this settlement. */
    private final UUID id;

    /** Settlement simulation clock (4 shifts/day, 100-day seasonal year). */
    private final SimClock clock;

    /** Authoritative communal granary ledger. */
    private final Stockpile stockpile;

    /** Mutable list of constituent households belonging to this settlement. */
    private final List<Household> households;

    /** Persistence-safe PRNG seed for deterministic weather variance across reloads. */
    private final long seed;

    /** Current Christaller hierarchy stage derived from total population. */
    private SettlementStage stage;

    /**
     * Creates a new autonomous settlement aggregate.
     *
     * @param id         unique non-null settlement identifier
     * @param clock      simulation clock (must not be {@code null})
     * @param stockpile  communal granary ledger (must not be {@code null})
     * @param households initial households (must not be {@code null} or contain {@code null} elements)
     * @param seed       deterministic PRNG seed for weather variance
     * @throws IllegalArgumentException if any required object or household element is {@code null}
     */
    public Settlement(
            UUID id,
            SimClock clock,
            Stockpile stockpile,
            List<Household> households,
            long seed
    ) {
        if (id == null || clock == null || stockpile == null || households == null) {
            throw new IllegalArgumentException("id, clock, stockpile, and households must not be null");
        }
        for (int i = 0, n = households.size(); i < n; i++) {
            if (households.get(i) == null) {
                throw new IllegalArgumentException("households must not contain null elements");
            }
        }
        this.id = id;
        this.clock = clock;
        this.stockpile = stockpile;
        this.households = new ArrayList<>(households);
        this.seed = seed;
        this.stage = SettlementStage.forPopulation(this.getTotalPopulation());
    }

    /**
     * Advances the settlement by one 6,000-tick shift:
     * evaluates {@link MalthusianBalance}, distributes positive population growth
     * evenly across households, and updates {@link #stage}.
     *
     * @return immutable {@link BalanceOutcome} for this shift
     */
    public BalanceOutcome advanceShift() {
        BalanceOutcome outcome = MalthusianBalance.tickShift(this.clock, this.households, this.stockpile, this.seed);

        // Distribute each new dependent to the smallest household so growth stays balanced
        if (outcome.populationDelta() > SimulationConstants.STALL_POPULATION_STEP && !this.households.isEmpty()) {
            for (int i = 0; i < outcome.populationDelta(); i++) {
                this.leastPopulatedHousehold().addDependent();
            }
            // Recompute population and stage only when population actually changed
            int updatedPopulation = this.getTotalPopulation();
            this.stage = SettlementStage.forPopulation(updatedPopulation);
            return new BalanceOutcome(
                    updatedPopulation,
                    outcome.populationDelta(),
                    outcome.stalledAtSubsistence(),
                    this.stage
            );
        }

        this.stage = outcome.recommendedStage();
        return outcome;
    }

    /**
     * Fast-forwards an unloaded or dormant settlement by {@code elapsedShifts}.
     *
     * @param elapsedShifts number of shifts to simulate ({@code 0 <= elapsedShifts <= MAX_CATCHUP_SHIFTS})
     * @return {@link BalanceOutcome} after the final catch-up shift,
     *         or an accurate current-state summary if {@code elapsedShifts == 0}
     * @throws IllegalArgumentException if {@code elapsedShifts < 0} or exceeds {@link SimulationConstants#MAX_CATCHUP_SHIFTS}
     */
    public BalanceOutcome catchUpShifts(long elapsedShifts) {
        if (elapsedShifts < 0L || elapsedShifts > SimulationConstants.MAX_CATCHUP_SHIFTS) {
            throw new IllegalArgumentException(
                    "elapsedShifts must be within [0, " + SimulationConstants.MAX_CATCHUP_SHIFTS + "]"
            );
        }
        if (elapsedShifts == 0L) {
            int pop = this.getTotalPopulation();
            boolean stalled = pop == 0 || !this.stockpile.isAboveSubsistence(
                    pop,
                    SimulationConstants.CALORIES_PER_MEMBER_PER_SHIFT,
                    SimulationConstants.GROWTH_BUFFER_SHIFTS
            );
            return new BalanceOutcome(pop, SimulationConstants.STALL_POPULATION_STEP, stalled, this.stage);
        }

        BalanceOutcome lastOutcome = null;
        for (long i = 0L; i < elapsedShifts; i++) {
            lastOutcome = this.advanceShift();
        }
        return lastOutcome;
    }

    /**
     * Finds the household with the fewest total members (breaking ties by fewest dependents)
     * so newly added dependents distribute evenly across the settlement.
     *
     * @return least-populated {@link Household}
     */
    private Household leastPopulatedHousehold() {
        Household smallest = this.households.get(0);
        for (int i = 1, n = this.households.size(); i < n; i++) {
            Household candidate = this.households.get(i);
            int candidateMembers = candidate.getTotalMembers();
            int smallestMembers = smallest.getTotalMembers();
            if (candidateMembers < smallestMembers
                    || (candidateMembers == smallestMembers && candidate.getDependents() < smallest.getDependents())) {
                smallest = candidate;
            }
        }
        return smallest;
    }

    /**
     * Computes aggregate citizen count across all households.
     *
     * @return total population ({@code >= 0})
     */
    public int getTotalPopulation() {
        int total = 0;
        for (int i = 0, n = this.households.size(); i < n; i++) {
            total = Math.addExact(total, this.households.get(i).getTotalMembers());
        }
        return total;
    }

    /** @return unique settlement identifier */
    public UUID getId() {
        return this.id;
    }

    /** @return persistence-safe PRNG seed */
    public long getSeed() {
        return this.seed;
    }

    /** @return current Christaller hierarchy stage */
    public SettlementStage getStage() {
        return this.stage;
    }

    /** @return settlement simulation clock */
    public SimClock getClock() {
        return this.clock;
    }

    /** @return communal granary stockpile */
    public Stockpile getStockpile() {
        return this.stockpile;
    }

    /** @return unmodifiable view of constituent households */
    public List<Household> getHouseholds() {
        return Collections.unmodifiableList(this.households);
    }
}
