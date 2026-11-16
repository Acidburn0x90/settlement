package com.settlement.sim.core;

import com.settlement.sim.core.town.Household;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HouseholdTest {

    @Test
    @DisplayName("dependencyRatio divides total members by active workers")
    void dependencyRatioDividesTotalMembersByWorkers() {
        Household hh = new Household(UUID.randomUUID(), 2, 1);

        assertEquals(2, hh.getWorkers());
        assertEquals(1, hh.getDependents());
        assertEquals(3, hh.getTotalMembers());
        assertEquals(1.5, hh.dependencyRatio(), 1e-6);
    }

    @Test
    @DisplayName("dependencyRatio clamps zero workers to 1 to avoid divide-by-zero")
    void dependencyRatioGuardsZeroWorkers() {
        Household hh = new Household(UUID.randomUUID(), 0, 3);

        assertEquals(3, hh.getTotalMembers());
        assertEquals(3.0, hh.dependencyRatio(), 1e-6);
    }

    @Test
    @DisplayName("dependencyRatio returns 0.0 for an empty household")
    void zeroMembersYieldsZeroRatio() {
        Household hh = new Household(UUID.randomUUID(), 0, 0);

        assertEquals(0, hh.getTotalMembers());
        assertEquals(0.0, hh.dependencyRatio(), 1e-6);
    }

    @Test
    @DisplayName("constructor rejects null id and negative member counts")
    void constructorRejectsNullIdAndNegativeCounts() {
        UUID validId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new Household(null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Household(validId, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Household(validId, 1, -1));
    }
}
