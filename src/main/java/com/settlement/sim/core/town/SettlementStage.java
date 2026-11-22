package com.settlement.sim.core.town;

/**
 * Christaller / Urban Hierarchy stages for a settlement.
 * <p>
 * Stages reflect Marshallian agglomeration economies: larger settlements unlock cheaper
 * scarcity-driven trades and faster villager profession progression.
 * Pure-Java domain model with zero Minecraft dependencies.
 */
public enum SettlementStage {
    /** Initial founding settlement (0+ citizens): baseline prices and XP. */
    HAMLET(0, 1.00, 1.00),

    /** Growing agrarian community (16+ citizens): 10% trade discount, 1.15x XP. */
    VILLAGE(16, 0.90, 1.15),

    /** Market center with craft workshops (35+ citizens): 25% trade discount, 1.30x XP. */
    TOWN(35, 0.75, 1.30),

    /** Dense urban hub with high Space Syntax centrality (80+ citizens): 40% trade discount, 1.50x XP. */
    CITY(80, 0.60, 1.50),

    /** Regional capital anchoring inter-town trade (150+ citizens): 50% trade discount, 2.00x XP. */
    REGION(150, 0.50, 2.00);

    /** Cached enum values to avoid array allocation on every stage lookup. */
    private static final SettlementStage[] STAGES = values();

    /** Minimum citizen count required to reach this stage. */
    private final int minPopulation;

    /** Multiplier applied to villager trade prices (lower = cheaper for players). */
    private final double tradeDiscountMultiplier;

    /** Multiplier applied to villager profession XP gain (higher = faster leveling). */
    private final double xpMultiplier;

    /**
     * Associates population threshold and Marshallian perk multipliers with each stage.
     *
     * @param minPopulation           minimum population required for this stage ({@code >= 0})
     * @param tradeDiscountMultiplier price multiplier for scarcity-driven trades
     * @param xpMultiplier            profession XP gain multiplier
     */
    SettlementStage(int minPopulation, double tradeDiscountMultiplier, double xpMultiplier) {
        this.minPopulation = minPopulation;
        this.tradeDiscountMultiplier = tradeDiscountMultiplier;
        this.xpMultiplier = xpMultiplier;
    }

    /**
     * Returns the minimum citizen population required to enter this stage.
     *
     * @return minimum population threshold ({@code >= 0})
     */
    public int getMinPopulation() {
        return minPopulation;
    }

    /**
     * Returns the trade price multiplier granted by this stage's market efficiency.
     *
     * @return price multiplier (e.g., {@code 0.75} means 25% cheaper trades)
     */
    public double getTradeDiscountMultiplier() {
        return tradeDiscountMultiplier;
    }

    /**
     * Returns the villager profession XP multiplier granted by this stage's guild spillovers.
     *
     * @return XP gain multiplier (e.g., {@code 1.50} means 50% faster leveling)
     */
    public double getXpMultiplier() {
        return xpMultiplier;
    }

    /**
     * Resolves the highest {@link SettlementStage} supported by the given population.
     *
     * @param population non-negative citizen count
     * @return matching hierarchy stage
     * @throws IllegalArgumentException if {@code population < 0}
     */
    public static SettlementStage forPopulation(int population) {
        if (population < 0) {
            throw new IllegalArgumentException("Population must be >= 0");
        }

        // Scan from highest stage (REGION) downward to match the highest met threshold first
        for (int i = STAGES.length - 1; i >= 0; i--) {
            if (population >= STAGES[i].minPopulation) {
                return STAGES[i];
            }
        }
        return HAMLET;
    }
}
