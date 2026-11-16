package com.settlement.mc.persistence;

/**
 * Per-chunk attachment tracking soil fertility, exhaustion, and local cultivation experience.
 * <p>
 * <b>Engine Safeguard:</b> Always invoke {@code chunk.setUnsaved(true)} when modifying chunk
 * data attachments in place to ensure chunk serialization occurs on unload/save.
 * <p>
 * Attached via NeoForge's {@code AttachmentType} registry.
 */
public class ChunkSoilAttachment {

    // TODO: Define AttachmentType registration and Codec serialization

    // TODO: Track soil fertility levels (0.0 .. 1.0) and cumulative crop exhaustion

    // TODO: Ensure chunk.setUnsaved(true) is invoked upon modifying attachment state
}
