package com.settlement.mc.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.settlement.mc.SettlementMod;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Per-chunk NeoForge {@link AttachmentType} tracking soil fertility ({@code 0.0..1.0})
 * and cumulative cultivation experience for dynamic agrarian yields.
 * <p>
 * <b>Engine Safeguard:</b> Always mutate attached chunk data via
 * {@link #mutate(ChunkAccess, Consumer)} (or invoke {@code chunk.markUnsaved()} manually in MC 26.1+)
 * so Minecraft persists updated chunk soil state on unload/save.
 */
public class ChunkSoilAttachment {

    /** Default full fertility of untouched virgin soil. */
    public static final double DEFAULT_FERTILITY = 1.0;

    /** Minimum soil fertility floor when heavily exhausted. */
    public static final double MIN_FERTILITY = 0.0;

    /** Maximum soil fertility ceiling. */
    public static final double MAX_FERTILITY = 1.0;

    /** MapCodec serializing chunk soil state directly into chunk NBT via NeoForge DataAttachments. */
    public static final MapCodec<ChunkSoilAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.doubleRange(MIN_FERTILITY, MAX_FERTILITY)
                    .optionalFieldOf("fertility", DEFAULT_FERTILITY)
                    .forGetter(ChunkSoilAttachment::getFertility),
            Codec.intRange(0, Integer.MAX_VALUE)
                    .optionalFieldOf("cultivation_experience", 0)
                    .forGetter(ChunkSoilAttachment::getCultivationExperience)
    ).apply(instance, ChunkSoilAttachment::new));

    /** DeferredRegister for NeoForge attachment types registered in {@link SettlementMod}. */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SettlementMod.MODID);

    /** Per-chunk soil attachment instance serialized with {@link #MAP_CODEC}. */
    public static final Supplier<AttachmentType<ChunkSoilAttachment>> CHUNK_SOIL =
            ATTACHMENT_TYPES.register(
                    "chunk_soil",
                    () -> AttachmentType.builder(ChunkSoilAttachment::new).serialize(MAP_CODEC).build()
            );

    /** Soil nutrient multiplier in {@code [0.0, 1.0]}. */
    private double fertility;

    /** Cumulative harvests/tending cycles experienced by farmers in this chunk ({@code >= 0}). */
    private int cultivationExperience;

    /**
     * Default constructor creating untouched fertile soil ({@code fertility = 1.0}, {@code experience = 0}).
     */
    public ChunkSoilAttachment() {
        this(DEFAULT_FERTILITY, 0);
    }

    /**
     * Deserialization/explicit state constructor.
     *
     * @param fertility             finite fertility level within {@code [0.0, 1.0]}
     * @param cultivationExperience non-negative cultivation experience counter
     * @throws IllegalArgumentException if {@code fertility} is non-finite/outside {@code [0.0, 1.0]}
     *                                  or {@code cultivationExperience < 0}
     */
    public ChunkSoilAttachment(double fertility, int cultivationExperience) {
        if (!Double.isFinite(fertility) || fertility < MIN_FERTILITY || fertility > MAX_FERTILITY) {
            throw new IllegalArgumentException("fertility must be finite within [0.0, 1.0]");
        }
        if (cultivationExperience < 0) {
            throw new IllegalArgumentException("cultivationExperience must be >= 0");
        }
        this.fertility = fertility;
        this.cultivationExperience = cultivationExperience;
    }

    /**
     * Mutates the {@link #CHUNK_SOIL} attachment on {@code chunk} in place and calls
     * {@code chunk.markUnsaved()} so NeoForge flushes the updated data on chunk save.
     *
     * @param chunk  target chunk whose soil attachment will be updated
     * @param action consumer mutating the retrieved {@link ChunkSoilAttachment}
     * @throws IllegalArgumentException if {@code chunk} or {@code action} is {@code null}
     */
    public static void mutate(ChunkAccess chunk, Consumer<ChunkSoilAttachment> action) {
        if (chunk == null || action == null) {
            throw new IllegalArgumentException("chunk and action must not be null");
        }
        ChunkSoilAttachment data = chunk.getData(CHUNK_SOIL);
        action.accept(data);
        // Critical Engine Safeguard: in-place attachment mutations must mark the chunk unsaved (MC 26.1 markUnsaved)
        chunk.markUnsaved();
    }

    /**
     * Degrades soil fertility after harvesting a crop (clamped to {@code [0.0, 1.0]})
     * and increments {@link #cultivationExperience} by 1 (guarded against {@code int} overflow).
     *
     * @param amount non-negative finite fertility exhaustion delta
     * @throws IllegalArgumentException if {@code amount} is negative or non-finite
     * @throws ArithmeticException      if {@code cultivationExperience} overflows {@code Integer.MAX_VALUE}
     */
    public void applyHarvestExhaustion(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        this.fertility = Math.clamp(this.fertility - amount, MIN_FERTILITY, MAX_FERTILITY);
        this.cultivationExperience = Math.addExact(this.cultivationExperience, 1);
    }

    /**
     * Restores soil fertility during fallow periods or crop rotation (clamped to {@code [0.0, 1.0]}).
     *
     * @param amount non-negative finite fertility recovery delta
     * @throws IllegalArgumentException if {@code amount} is negative or non-finite
     */
    public void applyFallowRecovery(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        this.fertility = Math.clamp(this.fertility + amount, MIN_FERTILITY, MAX_FERTILITY);
    }

    /** @return current soil fertility within {@code [0.0, 1.0]} */
    public double getFertility() {
        return this.fertility;
    }

    /** @return cumulative cultivation experience counter ({@code >= 0}) */
    public int getCultivationExperience() {
        return this.cultivationExperience;
    }
}
