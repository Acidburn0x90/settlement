package com.settlement.sim.core.town;

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

    // TODO: Track long calorieUnits, long seedUnits, and long capacity

    /**
     * Creates a communal stockpile with a fixed maximum storage capacity.
     *
     * @param capacity maximum total units ({@code calorieUnits + seedUnits}) the granary can hold; must be {@code >= 0}
     */
    public Stockpile(long capacity) {
        // TODO: Validate capacity >= 0 and initialize reserves to 0
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Deposits consumable food calories into the granary, clamped to remaining storage capacity.
     *
     * @param amount non-negative calorie units to add
     * @return actual calorie units accepted (may be less than {@code amount} if capacity is reached)
     * @throws IllegalArgumentException if {@code amount < 0}
     */
    public long depositCalories(long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Deposits ring-fenced seed-corn units into the granary, clamped to remaining storage capacity.
     *
     * @param amount non-negative seed units to add
     * @return actual seed units accepted (may be less than {@code amount} if capacity is reached)
     * @throws IllegalArgumentException if {@code amount < 0}
     */
    public long depositSeed(long amount) {
        throw new UnsupportedOperationException("TODO");
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
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Checks whether consumable {@code calorieUnits} cover the requested survival buffer.
     *
     * @param population       total citizen count ({@code >= 0})
     * @param caloriesPerShift calorie consumption per citizen per shift ({@code >= 0})
     * @param shiftsBuffer     number of future shifts to cover ({@code >= 0})
     * @return {@code true} if {@code calorieUnits} meets or exceeds the required buffer
     */
    public boolean isAboveSubsistence(int population, int caloriesPerShift, int shiftsBuffer) {
        throw new UnsupportedOperationException("TODO");
    }

    /** @return current consumable calorie units */
    public long getCalorieUnits() {
        throw new UnsupportedOperationException("TODO");
    }

    /** @return current ring-fenced seed-corn units */
    public long getSeedUnits() {
        throw new UnsupportedOperationException("TODO");
    }

    /** @return maximum combined storage capacity */
    public long getCapacity() {
        throw new UnsupportedOperationException("TODO");
    }
}
