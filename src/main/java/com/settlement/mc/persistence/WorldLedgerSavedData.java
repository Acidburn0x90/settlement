package com.settlement.mc.persistence;

/**
 * World-level persistence ledger for settlement state.
 * <p>
 * <b>Engine Safeguard:</b> Always invoke {@code setDirty()} immediately after any mutation
 * to ensure changes are written to disk during world save passes.
 * <p>
 * Implemented via Minecraft's {@code SavedData} / {@code SavedDataType} framework.
 */
public class WorldLedgerSavedData {

    // TODO: Define SavedDataType factory and Codec for WorldLedgerSavedData

    // TODO: Map and persist macro settlement records and global simulation counters

    // TODO: Ensure setDirty() is called on all state mutation paths
}
