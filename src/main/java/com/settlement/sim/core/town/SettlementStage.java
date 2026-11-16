package com.settlement.sim.core.town;

/**
 * Christaller / Urban Hierarchy stages for a settlement.
 * <p>
 * Stages reflect agglomeration economies: larger settlements unlock cheaper
 * scarcity-driven trades and faster villager profession progression.
 * Pure-Java domain model with zero Minecraft dependencies.
 */
public enum SettlementStage {
    HAMLET(0, 1.00, 1.00),
    VILLAGE(16, 0.90, 1.15),
    TOWN(35, 0.75, 1.30),
    CITY(80, 0.60, 1.50),
    REGION(150, 0.5, 2.00);

    private final int minPopulation;
    private final double tradeDiscountMultiplier;
    private final double xpMultiplier;

    private SettlementStage(int minPopulation, double tradeDiscountMultiplier, double xpMultiplier) {
        this.minPopulation = minPopulation;
        this.tradeDiscountMultiplier = tradeDiscountMultiplier;
        this.xpMultiplier = xpMultiplier;

    }

    public int getMinPopulation() {
        return minPopulation;
    }

    public double getTradeDiscountMultiplier() {
        return tradeDiscountMultiplier;
    }

    public double getXpMultiplier() {
        return xpMultiplier;
    }

    /**
     * Resolves the highest {@link SettlementStage} supported by the given population.
     *
     * @param population non-negative citizen count
     * @return matching hierarchy stage
     */
    public static SettlementStage forPopulation(int population) {
        if (population < 0) throw new IllegalArgumentException("Population must be >= 0");
        else {
            SettlementStage[] stages = values();
            for (int i = stages.length - 1; i >= 0; i--) {
                if (population >= stages[i].minPopulation) {
                    return stages[i];
                }
            }
        }
        return HAMLET;
    }
}