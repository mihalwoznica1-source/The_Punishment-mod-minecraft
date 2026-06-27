package com.punishment.mod;

import com.punishment.mod.entity.ModEntities;
import com.punishment.mod.events.WitchingHourHandler;
import com.punishment.mod.events.WitchingHourPacketHelper;
import com.punishment.mod.client.ClientSetup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ThePunishmentMod.MOD_ID)
public class ThePunishmentMod {

    public static final String MOD_ID = "punishment";
    public static final Logger LOGGER = LogManager.getLogger();

    public ThePunishmentMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(new WitchingHourHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(WitchingHourPacketHelper::registerPackets);
        LOGGER.info("[ThePunishment] Common setup complete.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::registerRenderers);
        LOGGER.info("[ThePunishment] Client setup complete.");
    }
}
