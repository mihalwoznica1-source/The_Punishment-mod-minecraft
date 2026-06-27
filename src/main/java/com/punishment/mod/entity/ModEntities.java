package com.punishment.mod.entity;

import com.punishment.mod.ThePunishmentMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Rejestracja wszystkich typów encji modu.
 * Używamy DeferredRegister dla bezpiecznej, opóźnionej rejestracji.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ThePunishmentMod.MOD_ID);

    /**
     * Typ encji "The Punishment".
     * - Rozmiar: 0.6 x 1.95 (jak zombie/gracz)
     * - Kategoria MONSTER (nie spawnuje się naturalnie — kontrolujemy spawn ręcznie)
     * - fireImmune() — jest bytem nadprzyrodzonym, ogień go nie niszczy
     */
    public static final RegistryObject<EntityType<ThePunishmentEntity>> THE_PUNISHMENT =
            ENTITY_TYPES.register("the_punishment",
                    () -> EntityType.Builder.<ThePunishmentEntity>of(ThePunishmentEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)      // szerokość x wysokość
                            .fireImmune()             // odporność na ogień
                            .clientTrackingRange(128) // zasięg śledzenia po stronie klienta
                            .build("the_punishment"));
}
