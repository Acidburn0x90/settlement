package com.settlement.sim.core.town;

import java.util.UUID;

/**
 * Represents a domestic economic unit within a settlement.
 * <p>
 * Households manage family members, consumption requirements, and domestic grain reserves.
 * Pure-Java domain model with zero Minecraft dependencies.
 */
public class Household {
    private final UUID id;
    private int workers;
    private int dependents;

    public Household(UUID id, int workers, int dependents) {
        if (id == null) throw new IllegalArgumentException("ID must not be null");
        if (workers < 0 || dependents < 0) throw new IllegalArgumentException("Counts must be >= 0");

        this.id = id;
        this.workers = workers;
        this.dependents = dependents;
    }

    // Getters
    public int getTotalMembers() {
        return (workers + dependents);
    }

    public int getWorkers() {
        return workers;
    }

    public int getDependents() {
        return dependents;
    }

    public UUID getId() {
        return id;
    }

    public double dependencyRatio() {
        return (double) getTotalMembers() / Math.max(1, workers);
    }


}
