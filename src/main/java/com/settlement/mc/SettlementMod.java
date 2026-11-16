package com.settlement.mc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tier 1 NeoForge mod entrypoint for {@code settlement}.
 * <p>
 * Responsible only for registering deferred content (items, POIs, memory modules,
 * data attachments) and binding server lifecycle/tick listeners to the pure-Java
 * simulation core.
 */
@Mod(SettlementMod.MODID)
public final class SettlementMod {

    /** Unique lowercase mod identifier matching {@code neoforge.mods.toml}. */
    public static final String MODID = "settlement";

    /** Shared SLF4J logger scoped to the {@code settlement} mod ID. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /**
     * Constructs the mod entrypoint; invoked automatically by NeoForge FML during mod loading.
     *
     * @param modEventBus  mod-specific event bus for deferred registration and setup lifecycle events
     * @param modContainer NeoForge metadata container providing mod version and config registration
     */
    public SettlementMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing {} ({})", MODID, modContainer.getModInfo().getVersion());
    }
}
