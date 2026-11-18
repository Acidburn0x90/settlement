package com.settlement.sim.core.town;

import java.util.UUID;

/**
 * Represents a domestic economic unit within a settlement.
 * <p>
 * Grounded in A. V. Chayanov's peasant household economy: each household tracks
 * productive {@code workers} versus non-working {@code dependents} so its
 * consumer-to-worker dependency ratio scales satisficing labor pressure.
 * <p>
 * Pure-Java domain model with zero Minecraft dependencies.
 */
public class Household {
    // TODO: Implement domestic grain allocation, consumeShift, and Hybrid Tithe-Pool contribution

    /** Unique immutable identifier for this household. */
    private final UUID id;

    /** Number of able-bodied adult workers contributing labor. */
    private int workers;

    /** Number of non-working dependents (e.g., children or elders) that still consume food. */
    private int dependents;

    /** Consumable domestic food calories held by this household. */
    private long domesticCalories;

    /** Ring-fenced domestic seed-corn units reserved for planting (never eaten). */
    private long domesticSeed;

    /**
     * Creates a new household with validated member counts and zero initial reserves.
     *
     * @param id         unique non-null identifier for this household
     * @param workers    number of productive workers ({@code >= 0})
     * @param dependents number of non-working dependents ({@code >= 0})
     * @throws IllegalArgumentException if {@code id} is {@code null} or any count is negative
     */
    public Household(UUID id, int workers, int dependents) {
        this(id, workers, dependents, 0L, 0L);
    }

    /**
     * Creates a new household with validated member counts and initial domestic reserves.
     *
     * @param id               unique non-null identifier for this household
     * @param workers          number of productive workers ({@code >= 0})
     * @param dependents       number of non-working dependents ({@code >= 0})
     * @param domesticCalories initial consumable domestic calories ({@code >= 0})
     * @param domesticSeed     initial ring-fenced domestic seed units ({@code >= 0})
     * @throws IllegalArgumentException if {@code id} is {@code null} or any numeric argument is negative
     */
    public Household(UUID id, int workers, int dependents, long domesticCalories, long domesticSeed) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (workers < 0 || dependents < 0 || domesticCalories < 0 || domesticSeed < 0) {
            throw new IllegalArgumentException("Counts and reserves must be >= 0");
        }

        this.id = id;
        this.workers = workers;
        this.dependents = dependents;
        this.domesticCalories = domesticCalories;
        this.domesticSeed = domesticSeed;
    }

    /**
     * Returns the immutable household identifier.
     *
     * @return non-null household UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the total number of mouths to feed in this household.
     *
     * @return {@code workers + dependents}
     */
    public int getTotalMembers() {
        return workers + dependents;
    }

    /**
     * Returns the number of active workers in this household.
     *
     * @return worker count ({@code >= 0})
     */
    public int getWorkers() {
        return workers;
    }

    /**
     * Returns the number of non-working dependents in this household.
     *
     * @return dependent count ({@code >= 0})
     */
    public int getDependents() {
        return dependents;
    }

    /**
     * Computes Chayanov's consumer-to-worker dependency ratio.
     * <p>
     * Uses {@code Math.max(1, workers)} to prevent division by zero when a household
     * temporarily has zero active workers.
     *
     * @return {@code (workers + dependents) / max(1, workers)}
     */
    public double dependencyRatio() {
        // Cast to double first so Java performs floating-point division instead of truncating
        return (double) getTotalMembers() / Math.max(1, workers);
    }

    /**
     * Returns the household's consumable domestic calories.
     *
     * @return current {@code domesticCalories} ({@code >= 0})
     */
    public long getDomesticCalories() {
        return domesticCalories;
    }

    /**
     * Returns the household's ring-fenced domestic seed-corn units.
     *
     * @return current {@code domesticSeed} ({@code >= 0})
     */
    public long getDomesticSeed() {
        return domesticSeed;
    }

    /**
     * Consumes one shift of calories for all household members from {@code domesticCalories}.
     * <p>
     * Never touches {@code domesticSeed}. Any unmet shortfall is returned as a positive
     * deficit so {@link MalthusianBalance} can request emergency rations from {@link Stockpile}.
     *
     * @param caloriesPerMember calories required per member for this shift ({@code >= 0})
     * @return unmet calorie deficit ({@code 0} if fully covered)
     * @throws IllegalArgumentException if {@code caloriesPerMember < 0}
     */
    public long consumeShift(int caloriesPerMember) {
        if (caloriesPerMember < 0) throw new IllegalArgumentException("Calories must be >= 0");
        long needed = (long) getTotalMembers() * caloriesPerMember;
        long consumed = Math.min(needed, domesticCalories);
        domesticCalories -= consumed;
        return needed - consumed;
    }

    /**
     * Allocates a harvest across domestic seed reserve, domestic subsistence buffer, and
     * the communal Hybrid Tithe-Pool.
     * <p>
     * Priority order:
     * <ol>
     *   <li>Top up {@code domesticSeed} toward {@code seedTarget}.</li>
     *   <li>Top up {@code domesticCalories} toward {@code subsistenceBuffer}.</li>
     *   <li>On any remaining excess, tithe {@code Math.round(excess * titheRate)} to the communal {@link Stockpile}
     *       and retain the rest in {@code domesticCalories}.</li>
     * </ol>
     *
     * @param harvest           newly harvested calorie/grain units ({@code >= 0})
     * @param seedTarget        minimum domestic seed units to hold before tithing ({@code >= 0})
     * @param subsistenceBuffer minimum domestic calories to hold before tithing ({@code >= 0})
     * @param titheRate         fraction of surplus contributed to communal {@link Stockpile} ({@code 0.0..1.0})
     * @return tithe amount to deposit into the communal {@link Stockpile}
     * @throws IllegalArgumentException if any numeric argument is negative or {@code titheRate} is outside {@code [0.0, 1.0]}
     */
    public long receiveHarvestAndTithe(long harvest, long seedTarget, long subsistenceBuffer, double titheRate) {
        // Guard against negative quantities and non-unit-interval (or NaN) tithe rates
        if (harvest < 0 || seedTarget < 0 || subsistenceBuffer < 0 || !(titheRate >= 0.0 && titheRate <= 1.0)) {
            throw new IllegalArgumentException("Arguments must be >= 0 and titheRate within [0.0, 1.0]");
        }

        long remaining = harvest;

        // 1. Ring-fence seed-corn first so next season's planting is always protected
        long seedNeeded = Math.max(0L, seedTarget - domesticSeed);
        long toSeed = Math.min(remaining, seedNeeded);
        domesticSeed += toSeed;
        remaining -= toSeed;

        // 2. Top up domestic subsistence calories before any surplus is shared
        long bufferNeeded = Math.max(0L, subsistenceBuffer - domesticCalories);
        long toBuffer = Math.min(remaining, bufferNeeded);
        domesticCalories += toBuffer;
        remaining -= toBuffer;

        // 3. Hybrid Tithe-Pool: tithe a fraction of true surplus; keep the rest domestically
        long tithe = Math.round(remaining * titheRate);
        domesticCalories += (remaining - tithe);
        return tithe;
    }
}
