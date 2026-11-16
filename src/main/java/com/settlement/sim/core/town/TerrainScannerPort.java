package com.settlement.sim.core.town;

/**
 * Output port for querying voxel terrain geometry and buildability.
 * <p>
 * Phase 1 terrain evaluation port used by settlement layout algorithms
 * to inspect slope, elevation deltas, and contiguous build footprints without
 * coupling the domain logic directly to Minecraft world structures.
 */
public interface TerrainScannerPort {

    /**
     * Computes the vertical slope gradient for a given column location.
     *
     * @param x the world X coordinate
     * @param z the world Z coordinate
     * @return the local slope gradient value
     */
    double scanColumnSlope(int x, int z);

    /**
     * Checks whether a region of given dimensions is suitable for construction.
     *
     * @param originX lower X bound
     * @param originZ lower Z bound
     * @param width   footprint width along X
     * @param depth   footprint depth along Z
     * @param maxDeltaY maximum permitted elevation delta across the footprint
     * @return true if the area satisfies buildability constraints
     */
    boolean isAreaBuildCandidate(int originX, int originZ, int width, int depth, int maxDeltaY);
}
