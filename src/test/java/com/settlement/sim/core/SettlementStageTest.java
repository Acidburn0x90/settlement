package com.settlement.sim.core;

import com.settlement.sim.core.town.SettlementStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementStageTest {

    @Test
    @DisplayName("stages have strictly increasing minimum population thresholds starting at 0 for HAMLET")
    void stagesHaveIncreasingPopulationThresholds() {
        SettlementStage[] stages = SettlementStage.values();
        assertEquals(SettlementStage.HAMLET, stages[0]);
        assertEquals(0, SettlementStage.HAMLET.getMinPopulation());

        for (int i = 1; i < stages.length; i++) {
            assertTrue(
                    stages[i].getMinPopulation() > stages[i - 1].getMinPopulation(),
                    stages[i] + " minPopulation must exceed " + stages[i - 1]
            );
        }
    }

    @Test
    @DisplayName("higher stages improve trade discount and villager XP multipliers")
    void higherStagesScaleMarshallianPerks() {
        SettlementStage[] stages = SettlementStage.values();

        for (int i = 1; i < stages.length; i++) {
            assertTrue(
                    stages[i].getTradeDiscountMultiplier() <= stages[i - 1].getTradeDiscountMultiplier(),
                    "Higher stage should have equal or lower trade price multiplier"
            );
            assertTrue(
                    stages[i].getXpMultiplier() >= stages[i - 1].getXpMultiplier(),
                    "Higher stage should have equal or higher XP multiplier"
            );
        }
    }

    @Test
    @DisplayName("forPopulation resolves each stage boundary cleanly and rejects negative population")
    void forPopulationResolvesBoundaries() {
        assertEquals(SettlementStage.HAMLET, SettlementStage.forPopulation(0));

        for (SettlementStage stage : SettlementStage.values()) {
            assertEquals(stage, SettlementStage.forPopulation(stage.getMinPopulation()));
        }

        assertThrows(IllegalArgumentException.class, () -> SettlementStage.forPopulation(-1));
    }
}
