package com.settlement.mc.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.settlement.mc.SettlementMod;
import com.settlement.sim.core.town.Household;
import com.settlement.sim.core.town.Settlement;
import com.settlement.sim.core.town.Stockpile;
import com.settlement.sim.core.world.SimClock;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tier 2 persistence bridge storing all {@link Settlement} aggregates server-wide
 * via NeoForge 26.1 {@link SavedDataType} and Mojang DataFixerUpper {@link Codec}s.
 * <p>
 * <b>Engine Safeguard:</b> Always call {@link #setDirty()} immediately after mutating
 * settlement records in place so Minecraft flushes the updated ledger to disk on save.
 */
public class WorldLedgerSavedData extends SavedData {

    /** Current on-disk schema version for forward-compatible migrations. */
    public static final long CURRENT_SCHEMA_VERSION = 1L;

    /**
     * Codec serializing a single {@link Household} (id, workers, dependents, domestic reserves).
     */
    public static final Codec<Household> HOUSEHOLD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Household::getId),
            Codec.INT.fieldOf("workers").forGetter(Household::getWorkers),
            Codec.INT.fieldOf("dependents").forGetter(Household::getDependents),
            Codec.LONG.fieldOf("domestic_calories").forGetter(Household::getDomesticCalories),
            Codec.LONG.fieldOf("domestic_seed").forGetter(Household::getDomesticSeed)
    ).apply(instance, Household::new));

    /**
     * Codec serializing a {@link Stockpile} (capacity, consumable calories, ring-fenced seed).
     */
    public static final Codec<Stockpile> STOCKPILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("capacity").forGetter(Stockpile::getCapacity),
            Codec.LONG.fieldOf("calorie_units").forGetter(Stockpile::getCalorieUnits),
            Codec.LONG.fieldOf("seed_units").forGetter(Stockpile::getSeedUnits)
    ).apply(instance, (capacity, calories, seed) -> {
        Stockpile stockpile = new Stockpile(capacity);
        // Deposit seed first so ring-fenced planting reserves are always restored intact
        stockpile.depositSeed(seed);
        stockpile.depositCalories(calories);
        return stockpile;
    }));

    /**
     * Codec serializing a full {@link Settlement} aggregate (id, elapsed shifts, stockpile, households, seed).
     */
    public static final Codec<Settlement> SETTLEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Settlement::getId),
            Codec.LONG.fieldOf("total_shifts").forGetter(s -> s.getClock().getTotalShifts()),
            STOCKPILE_CODEC.fieldOf("stockpile").forGetter(Settlement::getStockpile),
            HOUSEHOLD_CODEC.listOf().fieldOf("households").forGetter(Settlement::getHouseholds),
            Codec.LONG.fieldOf("seed").forGetter(Settlement::getSeed)
    ).apply(instance, (id, totalShifts, stockpile, households, seed) ->
            new Settlement(id, new SimClock(totalShifts), stockpile, households, seed)
    ));

    /**
     * Root codec for {@link WorldLedgerSavedData} stored under {@code data/settlement/world_ledger.dat}.
     */
    public static final Codec<WorldLedgerSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(sd -> sd.schemaVersion),
            SETTLEMENT_CODEC.listOf().optionalFieldOf("settlements", List.of())
                    .forGetter(sd -> new ArrayList<>(sd.settlements.values()))
    ).apply(instance, WorldLedgerSavedData::new));

    /**
     * World-wide {@link SavedDataType} registered under {@code data/settlement/world_ledger.dat}.
     */
    public static final SavedDataType<WorldLedgerSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SettlementMod.MODID, "world_ledger"),
            WorldLedgerSavedData::new,
            CODEC
    );

    private final long schemaVersion;
    private final Map<UUID, Settlement> settlements = new LinkedHashMap<>();

    /**
     * Default constructor invoked by {@link SavedDataType} when no ledger file exists yet.
     */
    public WorldLedgerSavedData() {
        this(CURRENT_SCHEMA_VERSION, List.of());
    }

    /**
     * Deserialization constructor invoked by {@link #CODEC}.
     *
     * @param schemaVersion persisted ledger schema version
     * @param loaded        deserialized settlements to index by {@link UUID}
     */
    private WorldLedgerSavedData(long schemaVersion, List<Settlement> loaded) {
        this.schemaVersion = schemaVersion;
        for (int i = 0, n = loaded.size(); i < n; i++) {
            Settlement settlement = loaded.get(i);
            this.settlements.put(settlement.getId(), settlement);
        }
    }

    /**
     * Retrieves or creates the server-wide {@link WorldLedgerSavedData} attached to {@link MinecraftServer}.
     *
     * @param server active dedicated or integrated {@link MinecraftServer}
     * @return loaded or newly created {@link WorldLedgerSavedData}
     */
    public static WorldLedgerSavedData getOrCreate(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server must not be null");
        }
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Registers or updates a settlement in the ledger and marks data dirty for disk save.
     *
     * @param settlement non-null settlement aggregate
     * @throws IllegalArgumentException if {@code settlement} is {@code null}
     */
    public void putSettlement(Settlement settlement) {
        if (settlement == null) {
            throw new IllegalArgumentException("settlement must not be null");
        }
        this.settlements.put(settlement.getId(), settlement);
        // Critical Engine Safeguard: tell Minecraft's SavedDataStorage to flush to disk
        this.setDirty();
    }

    /**
     * Marks the ledger dirty after mutating a retrieved {@link Settlement} in place.
     */
    public void markUpdated() {
        this.setDirty();
    }

    /**
     * Returns an unmodifiable view of all persisted settlements keyed by {@link UUID}.
     *
     * @return map of settlement ID to {@link Settlement}
     */
    public Map<UUID, Settlement> getSettlements() {
        return Collections.unmodifiableMap(settlements);
    }

    /**
     * @return persisted schema version of this ledger
     */
    public long getSchemaVersion() {
        return schemaVersion;
    }
}
