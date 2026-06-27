package com.punishment.mod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Główna klasa encji "The Punishment".
 *
 * Zasady zachowania:
 * - NIE spawnuje się naturalnie — spawnowana tylko przez WitchingHourHandler
 * - Po spawnie szuka najbliższego gracza i atakuje
 * - Atak to natychmiastowa śmierć (Float.MAX_VALUE obrażeń)
 * - Encja jest niezniszczalna (setInvulnerable) — nic jej nie zabije poza mechaniką moda
 */
public class ThePunishmentEntity extends Monster {

    // Flaga czy encja już zaatakowała (zapobiega wielokrotnemu killowi w jednym ticku)
    private boolean hasAttacked = false;

    public ThePunishmentEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // Encja jest niezniszczalna — nie można jej zabić normalnie
        this.setInvulnerable(true);
        // Zawsze "widzi" cel niezależnie od zasięgu
        this.setNoAi(false);
    }

    /**
     * Rejestracja atrybutów encji.
     * Musi być wywołana przy rejestracji EntityType.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)          // praktycznie niezniszczalna
                .add(Attributes.MOVEMENT_SPEED, 0.5D)          // szybka
                .add(Attributes.FOLLOW_RANGE, 256.0D)          // widzi graczy z daleka
                .add(Attributes.ATTACK_DAMAGE, Float.MAX_VALUE) // instakill
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);   // brak odrzutu
    }

    @Override
    protected void registerGoals() {
        // Cel nr 1: Atak na gracza z bliska (priorytet 1 = najwyższy)
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));

        // Cel nr 2: Ruch w stronę celu
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        // Cel nr 3: Patrzenie na cel
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 256.0F));

        // Wybór celu: Najbliższy gracz w zasięgu 256 bloków
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Wywoływana każdy tick po stronie serwera.
     * Sprawdzamy czy encja jest blisko gracza i obsługujemy atak.
     */
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && !hasAttacked) {
            // Sprawdź czy jest gracz w zasięgu 2 bloków (zasięg ataku)
            Player target = this.level().getNearestPlayer(this, 2.5D);
            if (target != null) {
                performInstakill(target);
            }
        }
    }

    /**
     * Natychmiastowe zabicie gracza lub przetrwanie w strefie bezpieczeństwa.
     *
     * Strefy bezpieczeństwa:
     * a) Y bliskie dołu świata (bedrock) — Y <= minBuildHeight + 5
     * b) Minimum 20 bloków od górnego limitu — Y >= maxBuildHeight - 20
     *
     * @param player Gracz do zaatakowania
     */
    private void performInstakill(Player player) {
        hasAttacked = true;

        int playerY = (int) player.getY();
        int minY = player.level().getMinBuildHeight();  // dolny limit świata
        int maxY = player.level().getMaxBuildHeight();  // górny limit świata

        boolean nearBedrock    = playerY <= minY + 5;      // strefa a: przy bedrocku
        boolean nearBuildLimit = playerY >= maxY - 20;     // strefa b: przy górnym limicie

        boolean inSafeZone = nearBedrock || nearBuildLimit;

        if (inSafeZone) {
            // Gracz przeżywa — encja znika
            ThePunishmentMod_LOGGER_HELPER("Gracz " + player.getName().getString() +
                    " przeżył Witching Hour w strefie bezpieczeństwa! (Y=" + playerY + ")");
            // Sygnał do handlera że gracz przeżył (przez NBT tag)
            player.getPersistentData().putBoolean("punishment_survived", true);
        } else {
            // Natychmiastowa śmierć
            ThePunishmentMod_LOGGER_HELPER("Gracz " + player.getName().getString() +
                    " zostaje zabity przez The Punishment! (Y=" + playerY + ")");
            player.hurt(this.level().damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            // Jeśli hurt nie zadziałał (god mode itp.), force kill
            if (player.isAlive()) {
                player.kill();
            }
        }

        // Encja usuwa się ze świata po ataku
        this.discard();
    }

    // Helper dla logowania (unikamy statycznego importu w tej klasie)
    private void ThePunishmentMod_LOGGER_HELPER(String msg) {
        com.punishment.mod.ThePunishmentMod.LOGGER.info("[ThePunishment] " + msg);
    }

    /** Encja nie może być zraniona normalnie */
    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return true;
    }

    /** Nie spada z klifów w pościgu za graczem */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true; // Znika na peaceful
    }

    /** Encja nie zostawia dropu */
    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        // Brak dropu — byt nadprzyrodzony
    }

    /** Encja nie wydaje dźwięków (cisza to część horroru) */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(@NotNull DamageSource source) { return null; }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return null; }

    @Override
    protected net.minecraft.sounds.SoundEvent getStepSound() { return null; }
}
