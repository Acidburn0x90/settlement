package com.settlement.sim.core.town;

import com.settlement.sim.core.SimulationConstants;

/**
 * Authoritative communal food and seed-corn ledger for a settlement.
 * <p>
 * <b>Resilience Invariant:</b> All quantities are stored as non-negative integer units
 * ({@code long}) to eliminate floating-point drift during offscreen catch-up math.
 * {@code seedUnits} are strictly ring-fenced from {@code calorieUnits}: famine rationing
 * and player withdrawals/raiding can drain {@code calorieUnits} to {@code 0}, but
 * never touch {@code seedUnits}, ensuring next season's planting can always recover.
 * <p>
 * Pure-Java domain model with zero Minecraft dependencies.
 */
public class Stockpile {

    /** Consumable food calories available for rationing, trade, or player withdrawal. */
    private long calorieUnits;

    /** Ring-fenced seed-corn units reserved strictly for planting (never consumed or raided). */
    private long seedUnits;

    /** Immutable maximum combined storage capacity ({@code calorieUnits + seedUnits}). */
    private final long capacity;

    /**
     * Creates a communal stockpile with a fixed maximum storage capacity.
     *
     * @param capacity maximum total units ({@code calorieUnits + seedUnits}) the granary can hold; must be {@code >= 0}
     * @throws IllegalArgumentException if {@code capacity < 0}
     */
    public Stockpile(long capacity) {
        if (capacity < SimulationConstants.ZERO_RESERVE) {
            throw new IllegalArgumentException("Capacity must be >= 0");
        }
        this.calorieUnits = SimulationConstants.ZERO_RESERVE;
        this.seedUnits = SimulationConstants.ZERO_RESERVE;
        this.capacity = capacity;
    }

    /**
     * Computes the total units currently stored in the granary on demand.
     * <p>
     * Derived directly from {@code calorieUnits + seedUnits} so it can never drift out of sync.
     *
     * @return combined {@code calorieUnits + seedUnits}
     */
    public long getTotalUnits() {
        return calorieUnits + seedUnits;
    }

    /**
     * Computes how many additional units can still fit before reaching {@code capacity}.
     *
     * @return remaining free storage capacity ({@code >= 0})
     */
    private long remainingCapacity() {
        return capacity - getTotalUnits();
    }

    /**
     * Deposits consumable food calories into the granary, clamped to remaining storage capacity.
     *
     * @param amount non-negative calorie units to add
     * @return actual calorie units accepted (may be less than {@code amount} if capacity is reached)
     * @throws IllegalArgumentException if {@code amount < 0}
     */
    public long depositCalories(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        // Compute accepted amount BEFORE mutating calorieUnits (avoids overflow and stale remaining checks)
        long accepted = Math.min(amount, remainingCapacity());
        calorieUnits += accepted;
        return accepted;
    }

    /**
     * Deposits ring-fenced seed-corn units into the granary, clamped to remaining storage capacity.
     *
     * @param amount non-negative seed units to add
     * @return actual seed units accepted (may be less than {@code amount} if capacity is reached)
     * @throws IllegalArgumentException if {@code amount < 0}
     */
    public long depositSeed(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        // Clamp against shared capacity without altering calorieUnits
        long accepted = Math.min(amount, remainingCapacity());
        seedUnits += accepted;
        return accepted;
    }

    /**
     * Withdraws consumable calories for rationing, trade, or player removal.
     * <p>
     * Never touches {@code seedUnits}; if {@code requested} exceeds {@code calorieUnits},
     * only the available {@code calorieUnits} are returned.
     *
     * @param requested non-negative calorie units requested
     * @return actual calorie units withdrawn ({@code 0 <= withdrawn <= requested})
     * @throws IllegalArgumentException if {@code requested < 0}
     */
    public long withdrawCalories(long requested) {
        if (requested < 0) {
            throw new IllegalArgumentException("requested must be >= 0");
        }
        // Cap withdrawal to available calorieUnits; never touches ring-fenced seedUnits
        long withdrawn = Math.min(requested, calorieUnits);
        calorieUnits -= withdrawn;
        return withdrawn;
    }

    /**
     * Checks whether consumable {@code calorieUnits} cover the requested survival buffer.
     *
     * @param population       total citizen count ({@code >= 0})
     * @param caloriesPerShift calorie consumption per citizen per shift ({@code >= 0})
     * @param shiftsBuffer     number of future shifts to cover ({@code >= 0})
     * @return {@code true} if {@code calorieUnits} meets or exceeds the required buffer
     * @throws IllegalArgumentException if any argument is negative
     */
    public boolean isAboveSubsistence(int population, int caloriesPerShift, int shiftsBuffer) {
        if (population < 0 || caloriesPerShift < 0 || shiftsBuffer < 0) {
            throw new IllegalArgumentException("Arguments must be >= 0");
        }
        try {
            // Use multiplyExact so huge buffers never wrap negative and falsely return true
            long perShift = Math.multiplyExact((long) caloriesPerShift, population);
            long required = Math.multiplyExact(perShift, shiftsBuffer);
            return calorieUnits >= required;
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    /**
     * @return current consumable calorie units
     */
    public long getCalorieUnits() {
        return calorieUnits;
    }

    /**
     * @return current ring-fenced seed-corn units
     */
    public long getSeedUnits() {
        return seedUnits;
    }

    /**
     * @return maximum combined storage capacity
     */
    public long getCapacity() {
        return capacity;
    }
}
