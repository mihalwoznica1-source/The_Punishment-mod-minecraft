package com.punishment.mod.client;

import com.punishment.mod.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Konfiguracja po stronie klienta.
 * Rejestruje renderery dla wszystkich encji modu.
 */
@OnlyIn(Dist.CLIENT)
public class ClientSetup {

    /**
     * Rejestracja renderera encji "The Punishment".
     * Musi być wywołana w enqueueWork FMLClientSetupEvent.
     */
    public static void registerRenderers() {
        EntityRenderers.register(
                ModEntities.THE_PUNISHMENT.get(),
                ThePunishmentRenderer::new
        );
    }
}
