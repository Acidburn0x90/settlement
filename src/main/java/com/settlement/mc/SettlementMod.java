package com.settlement.mc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SettlementMod.MODID)
public final class SettlementMod {
    public static final String MODID = "settlement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public SettlementMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing {} ({})", MODID, modContainer.getModInfo().getVersion());
    }
}
