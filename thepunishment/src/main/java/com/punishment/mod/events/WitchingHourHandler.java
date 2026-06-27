package com.punishment.mod.events;

import com.punishment.mod.ThePunishmentMod;
import com.punishment.mod.entity.ModEntities;
import com.punishment.mod.entity.ThePunishmentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "punishment", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WitchingHourHandler {

    private static final long MIDNIGHT_TIME = 18000L;
    private static final long TIME_WINDOW = 2L;
    private static final int COUNTDOWN_TICKS = 1200; // 60 sekund
    private static final int RUN_WARNING_TICKS = 200;
    private static final double BASE_ACTIVATION_CHANCE = 0.25;
    private static final int GUARANTEED_ACTIVATION_EVERY_N_NIGHTS = 5;

    private final Map<UUID, Integer> nightCounters = new HashMap<>();
    private final Map<UUID, Integer> activeCountdowns = new HashMap<>();
    private final Set<UUID> triggeredThisNight = new HashSet<>();
    private final Set<UUID> nightProcessed = new HashSet<>();

    public static WitchingHourHandler INSTANCE;

    public WitchingHourHandler() {
        INSTANCE = this;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
            if (serverLevel.dimension() != Level.OVERWORLD) continue;

            long dayTime = serverLevel.getDayTime() % 24000L;

            if (dayTime < 1000) {
                resetNightState(serverLevel);
            }

            boolean isMidnight = (dayTime >= MIDNIGHT_TIME - TIME_WINDOW)
                    && (dayTime <= MIDNIGHT_TIME + TIME_WINDOW);

            for (ServerPlayer player : serverLevel.players()) {
                UUID uuid = player.getUUID();

                if (isMidnight && !triggeredThisNight.contains(uuid)) {
                    tryActivateWitchingHour(player, uuid);
                }

                if (activeCountdowns.containsKey(uuid)) {
                    tickCountdown(player, serverLevel, uuid);
                }
            }
        }
    }

    public void forceStartWitchingHour(ServerPlayer player) {
        UUID uuid = player.getUUID();
        triggeredThisNight.add(uuid);
        activeCountdowns.put(uuid, COUNTDOWN_TICKS);
        WitchingHourPacketHelper.sendStartMessage(player);
        ThePunishmentMod.LOGGER.info("[ThePunishment] Ręcznie aktywowano Witching Hour dla {}", player.getName().getString());
    }

    private void tryActivateWitchingHour(ServerPlayer player, UUID uuid) {
        triggeredThisNight.add(uuid);
        int nightCount = nightCounters.getOrDefault(uuid, 0) + 1;
        nightCounters.put(uuid, nightCount);

        ThePunishmentMod.LOGGER.info("[ThePunishment] Gracz {} | Noc #{} | Sprawdzanie Witching Hour...",
                player.getName().getString(), nightCount);

        boolean isGuaranteedNight = (nightCount % GUARANTEED_ACTIVATION_EVERY_N_NIGHTS == 0);
        boolean activates = isGuaranteedNight || (Math.random() < BASE_ACTIVATION_CHANCE);

        if (!activates) {
            ThePunishmentMod.LOGGER.info("[ThePunishment] Witching Hour NIE aktywowała się tej nocy.");
            return;
        }

        ThePunishmentMod.LOGGER.info("[ThePunishment] *** WITCHING HOUR AKTYWNA dla {}! (gwarantowana={}) ***",
                player.getName().getString(), isGuaranteedNight);

        activeCountdowns.put(uuid, COUNTDOWN_TICKS);
        WitchingHourPacketHelper.sendStartMessage(player);
    }

    private void tickCountdown(ServerPlayer player, ServerLevel serverLevel, UUID uuid) {
        int remaining = activeCountdowns.get(uuid);
        remaining--;

        if (remaining <= 0) {
            activeCountdowns.remove(uuid);
            spawnPunishment(player, serverLevel);
            return;
        }

        activeCountdowns.put(uuid, remaining);
        int displayedBlocks = (int) (remaining * 0.5);

        if (remaining % 20 == 0) {
            WitchingHourPacketHelper.sendCountdownUpdate(player, displayedBlocks, remaining);
        }

        if (remaining == RUN_WARNING_TICKS) {
            WitchingHourPacketHelper.sendRunWarning(player);
        }
    }

    private void spawnPunishment(ServerPlayer player, ServerLevel serverLevel) {
        ThePunishmentMod.LOGGER.info("[ThePunishment] Spawning The Punishment przy graczu {}!",
                player.getName().getString());

        ThePunishmentEntity punishment = ModEntities.THE_PUNISHMENT.get().create(serverLevel);

        if (punishment == null) {
            ThePunishmentMod.LOGGER.error("[ThePunishment] Nie udało się stworzyć encji!");
            return;
        }

        double spawnX = player.getX() + 1.5;
        double spawnY = player.getY();
        double spawnZ = player.getZ() + 1.5;

        punishment.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0);
        serverLevel.addFreshEntity(punishment);
        WitchingHourPacketHelper.sendAttackEvent(player);
    }

    private void resetNightState(ServerLevel serverLevel) {
        for (ServerPlayer player : serverLevel.players()) {
            UUID uuid = player.getUUID();
            if (nightProcessed.contains(uuid)) continue;

            nightProcessed.add(uuid);
            triggeredThisNight.remove(uuid);

            if (activeCountdowns.containsKey(uuid)) {
                activeCountdowns.remove(uuid);
                WitchingHourPacketHelper.sendCancelEvent(player);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("setwitchinghour")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                try {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (INSTANCE != null) {
                        INSTANCE.forceStartWitchingHour(player);
                        context.getSource().sendSuccess(() -> Component.literal("§aPomyślnie uruchomiono Witching Hour! Uciekaj!"), true);
                    } else {
                        // Jeśli gra dopiero startuje i INSTANCE jest puste, tworzymy je na szybko
                        INSTANCE = new WitchingHourHandler();
                        INSTANCE.forceStartWitchingHour(player);
                        context.getSource().sendSuccess(() -> Component.literal("§aPomyślnie uruchomiono Witching Hour! Uciekaj!"), true);
                    }
                } catch (Exception e) {
                    context.getSource().sendFailure(Component.literal("§cBłąd: Komendę można wykonać tylko jako gracz."));
                }
                return 1;
            })
        );
    }
}
