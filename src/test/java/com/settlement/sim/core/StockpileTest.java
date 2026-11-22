package com.settlement.sim.core;

import com.settlement.sim.core.town.Stockpile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockpileTest {

    @Test
    @DisplayName("depositCalories and depositSeed clamp cleanly to remaining granary capacity")
    void depositsClampToCapacity() {
        Stockpile stockpile = new Stockpile(100L);

        assertEquals(40L, stockpile.depositSeed(40L));
        assertEquals(60L, stockpile.depositCalories(80L)); // Only 60 capacity left
        assertEquals(40L, stockpile.getSeedUnits());
        assertEquals(60L, stockpile.getCalorieUnits());
        assertEquals(100L, stockpile.getCapacity());
    }

    @Test
    @DisplayName("withdrawCalories never touches ring-fenced seedUnits even on overdraw or theft")
    void withdrawCaloriesProtectsSeedUnits() {
        Stockpile stockpile = new Stockpile(200L);
        stockpile.depositSeed(50L);
        stockpile.depositCalories(30L);

        // Request more calories than available: drains calories to 0, leaves seed intact
        long withdrawn = stockpile.withdrawCalories(100L);

        assertEquals(30L, withdrawn);
        assertEquals(0L, stockpile.getCalorieUnits());
        assertEquals(50L, stockpile.getSeedUnits());
    }

    @Test
    @DisplayName("isAboveSubsistence checks consumable calories against population buffer without counting seed")
    void subsistenceCheckIgnoresSeedUnits() {
        Stockpile stockpile = new Stockpile(500L);
        stockpile.depositSeed(200L);
        stockpile.depositCalories(120L);

        // 4 citizens * 10 cal/shift * 3 shifts = 120 required
        assertTrue(stockpile.isAboveSubsistence(4, 10, 3));
        // 4 citizens * 10 cal/shift * 4 shifts = 160 required (seedUnits must not count!)
        assertFalse(stockpile.isAboveSubsistence(4, 10, 4));
    }

    @Test
    @DisplayName("negative capacity, deposits, and withdrawals throw IllegalArgumentException")
    void negativeArgumentsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Stockpile(-1L));

        Stockpile stockpile = new Stockpile(100L);
        assertThrows(IllegalArgumentException.class, () -> stockpile.depositCalories(-5L));
        assertThrows(IllegalArgumentException.class, () -> stockpile.depositSeed(-5L));
        assertThrows(IllegalArgumentException.class, () -> stockpile.withdrawCalories(-5L));
    }

    @Test
    @DisplayName("zero-capacity Stockpile(0L) rejects deposits and enforces subsistence boundaries")
    void zeroCapacityStockpileRejectsDepositsAndReportsEmpty() {
        Stockpile zero = new Stockpile(0L);

        assertEquals(0L, zero.getCapacity());
        assertEquals(0L, zero.getTotalUnits());
        assertEquals(0L, zero.depositCalories(0L));
        assertEquals(0L, zero.depositCalories(100L));
        assertEquals(0L, zero.depositSeed(0L));
        assertEquals(0L, zero.depositSeed(100L));
        assertEquals(0L, zero.withdrawCalories(0L));
        assertEquals(0L, zero.withdrawCalories(100L));
        assertEquals(0L, zero.getTotalUnits());

        assertTrue(zero.isAboveSubsistence(0, 10, 12));
        assertTrue(zero.isAboveSubsistence(1, 0, 12));
        assertTrue(zero.isAboveSubsistence(1, 10, 0));
        assertFalse(zero.isAboveSubsistence(1, 10, 1));
    }

    @Test
    @DisplayName("zero deposits and withdrawals are exact no-ops")
    void zeroDepositsAndWithdrawalsAreNoOps() {
        Stockpile stockpile = new Stockpile(100L);
        stockpile.depositSeed(20L);
        stockpile.depositCalories(30L);

        assertEquals(0L, stockpile.depositCalories(0L));
        assertEquals(0L, stockpile.depositSeed(0L));
        assertEquals(0L, stockpile.withdrawCalories(0L));
        assertEquals(30L, stockpile.getCalorieUnits());
        assertEquals(20L, stockpile.getSeedUnits());
        assertEquals(50L, stockpile.getTotalUnits());
    }

    @Test
    @DisplayName("Long.MAX_VALUE capacity handles full deposits and withdrawals without overflow")
    void longMaxValueCapacityDepositsAndWithdrawalsAvoidOverflow() {
        Stockpile stockpile = new Stockpile(Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE, stockpile.depositCalories(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, stockpile.getCalorieUnits());
        assertEquals(Long.MAX_VALUE, stockpile.getTotalUnits());
        assertEquals(0L, stockpile.depositSeed(1L));
        assertEquals(0L, stockpile.depositCalories(1L));

        assertEquals(Long.MAX_VALUE, stockpile.withdrawCalories(Long.MAX_VALUE));
        assertEquals(0L, stockpile.getCalorieUnits());
        assertEquals(Long.MAX_VALUE, stockpile.depositSeed(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, stockpile.getSeedUnits());
    }

    @Test
    @DisplayName("isAboveSubsistence guards against integer and long multiplication wrap-around")
    void isAboveSubsistenceGuardsAgainstIntegerAndLongOverflow() {
        Stockpile maxStockpile = new Stockpile(Long.MAX_VALUE);
        maxStockpile.depositCalories(Long.MAX_VALUE);

        assertFalse(maxStockpile.isAboveSubsistence(Integer.MAX_VALUE, Integer.MAX_VALUE, 3));
        assertFalse(maxStockpile.isAboveSubsistence(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertFalse(maxStockpile.isAboveSubsistence(Integer.MAX_VALUE, 10, Integer.MAX_VALUE));
        // (2^31 - 1)^2 * 2 < Long.MAX_VALUE -> exact fit when stockpile holds Long.MAX_VALUE
        assertTrue(maxStockpile.isAboveSubsistence(Integer.MAX_VALUE, Integer.MAX_VALUE, 2));

        Stockpile empty = new Stockpile(0L);
        assertTrue(empty.isAboveSubsistence(0, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertTrue(empty.isAboveSubsistence(Integer.MAX_VALUE, Integer.MAX_VALUE, 0));
        assertTrue(empty.isAboveSubsistence(Integer.MAX_VALUE, 0, Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("isAboveSubsistence and Long.MIN_VALUE arguments reject negatives")
    void isAboveSubsistenceAndMinValueNegativeArgumentsRejected() {
        Stockpile stockpile = new Stockpile(100L);

        assertThrows(IllegalArgumentException.class, () -> stockpile.isAboveSubsistence(-1, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> stockpile.isAboveSubsistence(1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> stockpile.isAboveSubsistence(1, 10, -1));
        assertThrows(IllegalArgumentException.class, () -> stockpile.isAboveSubsistence(Integer.MIN_VALUE, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new Stockpile(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> stockpile.depositCalories(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> stockpile.depositSeed(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> stockpile.withdrawCalories(Long.MIN_VALUE));
    }
}
