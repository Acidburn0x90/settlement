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

    /**
     * Creates a new household with validated member counts.
     *
     * @param id         unique non-null identifier for this household
     * @param workers    number of productive workers ({@code >= 0})
     * @param dependents number of non-working dependents ({@code >= 0})
     * @throws IllegalArgumentException if {@code id} is {@code null} or any count is negative
     */
    public Household(UUID id, int workers, int dependents) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (workers < 0 || dependents < 0) {
            throw new IllegalArgumentException("Counts must be >= 0");
        }

        this.id = id;
        this.workers = workers;
        this.dependents = dependents;
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
}
