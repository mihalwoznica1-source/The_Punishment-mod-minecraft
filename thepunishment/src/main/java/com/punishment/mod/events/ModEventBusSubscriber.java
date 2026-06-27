package com.punishment.mod.events;

import com.punishment.mod.ThePunishmentMod;
import com.punishment.mod.entity.ModEntities;
import com.punishment.mod.entity.ThePunishmentEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rejestracja atrybutów encji (zdrowie, szybkość, obrażenia itd.)
 *
 * MUSI być na Bus.MOD (event modloadingu, nie Forge event bus).
 */
@Mod.EventBusSubscriber(modid = ThePunishmentMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusSubscriber {

    /**
     * Rejestracja atrybutów The Punishment.
     * Bez tego encja nie będzie funkcjonować w świecie Minecrafta.
     */
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(
                ModEntities.THE_PUNISHMENT.get(),
                ThePunishmentEntity.createAttributes().build()
        );
    }
}
