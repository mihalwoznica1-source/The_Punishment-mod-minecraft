package com.punishment.mod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;

public class ThePunishmentEntity extends Monster {

    private boolean hasAttacked = false;

    public ThePunishmentEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoAi(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 256.0D)
                .add(Attributes.ATTACK_DAMAGE, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 256.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !hasAttacked) {
            Player target = this.level().getNearestPlayer(this, 2.5D);
            if (target != null) {
                performInstakill(target);
            }
        }
    }

    private void performInstakill(Player player) {
        hasAttacked = true;
        int playerY = (int) player.getY();
        int minY = player.level().getMinBuildHeight();
        int maxY = player.level().getMaxBuildHeight();
        boolean nearBedrock = playerY <= minY + 5;
        boolean nearBuildLimit = playerY >= maxY - 20;
        boolean inSafeZone = nearBedrock || nearBuildLimit;
        if (inSafeZone) {
            player.getPersistentData().putBoolean("punishment_survived", true);
        } else {
            player.hurt(this.level().damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            if (player.isAlive()) {
                player.kill();
            }
        }
        this.discard();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {}
}
