package com.settlement.sim.core.town;

import com.settlement.sim.core.world.SimClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Tier 3 aggregate root representing a single autonomous settlement.
 * <p>
 * Owns the settlement's {@link SimClock}, communal {@link Stockpile}, constituent
 * {@link Household} units, deterministic {@link SplittableRandom} stream, and
 * {@link SettlementStage}. Executing {@link #advanceShift()} or {@link #catchUpShifts(long)}
 * closes the autonomous Malthusian growth loop without any Minecraft dependencies.
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

    /** Economic/demographic evaluator applied on each shift tick. */
    private final MalthusianBalance balance;

    /** Seeded PRNG ensuring deterministic weather variance and catch-up parity. */
    private final SplittableRandom rng;

    /** Current Christaller hierarchy stage derived from total population. */
    private SettlementStage stage;

    /**
     * Creates a new autonomous settlement aggregate.
     *
     * @param id         unique non-null settlement identifier
     * @param clock      simulation clock (must not be {@code null})
     * @param stockpile  communal granary ledger (must not be {@code null})
     * @param households initial households (must not be {@code null})
     * @param seed       deterministic PRNG seed for weather variance
     * @throws IllegalArgumentException if any required object argument is {@code null}
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
        this.id = id;
        this.clock = clock;
        this.stockpile = stockpile;
        this.households = new ArrayList<>(households);
        this.balance = new MalthusianBalance();
        this.rng = new SplittableRandom(seed);
        this.stage = SettlementStage.forPopulation(getTotalPopulation());
    }

    /**
     * Advances the settlement by one 6,000-tick shift:
     * evaluates {@link MalthusianBalance}, applies positive population growth to a
     * household when surplus allows, and updates {@link #stage}.
     *
     * @return immutable {@link MalthusianBalance.BalanceOutcome} for this shift
     */
    public MalthusianBalance.BalanceOutcome advanceShift() {
        // TODO: Issue #34 — tick MalthusianBalance, apply populationDelta via addDependent(), update stage
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Fast-forwards an unloaded or dormant settlement by {@code elapsedShifts}.
     *
     * @param elapsedShifts non-negative number of shifts to simulate
     * @return {@link MalthusianBalance.BalanceOutcome} after the final catch-up shift,
     *         or a current-state summary if {@code elapsedShifts == 0}
     * @throws IllegalArgumentException if {@code elapsedShifts < 0}
     */
    public MalthusianBalance.BalanceOutcome catchUpShifts(long elapsedShifts) {
        // TODO: Issue #34 — validate elapsedShifts >= 0, loop advanceShift(), handle 0 shifts cleanly
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Computes aggregate citizen count across all households.
     *
     * @return total population ({@code >= 0})
     */
    public int getTotalPopulation() {
        int total = 0;
        for (int i = 0, n = households.size(); i < n; i++) {
            total += households.get(i).getTotalMembers();
        }
        return total;
    }

    /** @return unique settlement identifier */
    public UUID getId() {
        return id;
    }

    /** @return current Christaller hierarchy stage */
    public SettlementStage getStage() {
        return stage;
    }

    /** @return settlement simulation clock */
    public SimClock getClock() {
        return clock;
    }

    /** @return communal granary stockpile */
    public Stockpile getStockpile() {
        return stockpile;
    }

    /** @return unmodifiable view of constituent households */
    public List<Household> getHouseholds() {
        return Collections.unmodifiableList(households);
    }
}
