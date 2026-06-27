package com.punishment.mod.client;

import com.punishment.mod.ThePunishmentMod;
import com.punishment.mod.model.ThePunishmentModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Subskrybent eventów renderowania po stronie klienta.
 *
 * Rejestruje warstwy modelu (ModelLayer) encji.
 * Musi być na MOD event bus (nie Forge event bus).
 */
@Mod.EventBusSubscriber(modid = ThePunishmentMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEventBusSubscriber {

    /**
     * Rejestracja warstwy modelu The Punishment.
     * LayerDefinition jest pobierane z ThePunishmentModel.createBodyLayer().
     */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                ThePunishmentModel.LAYER_LOCATION,
                ThePunishmentModel::createBodyLayer
        );
    }
}
