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

import java.util.*;

/**
 * ==========================================================================
 * WITCHING HOUR HANDLER — Serce mechaniki "The Punishment"
 * ==========================================================================
 *
 * Odpowiada za:
 * 1. Sprawdzanie czy aktywować Witching Hour (midnight = tick 18000)
 * 2. Licznik nocy i zasadę "co 5. noc 100% szans"
 * 3. Odliczanie 60 sekund i wysyłanie pakietów do klienta
 * 4. Teleportację/spawn encji po 60 sekundach
 *
 * Dane per-gracz trzymamy w mapach indeksowanych po UUID.
 */
public class WitchingHourHandler {

    // -----------------------------------------------------------------------
    // Stałe czasowe
    // -----------------------------------------------------------------------

    /** Czas gry Minecraft odpowiadający północy (czas od początku dnia, 0 = świt) */
    private static final long MIDNIGHT_TIME = 18000L;

    /** Tolerancja sprawdzania czasu — okno ±2 ticki (100ms) by nie przegapić dokładnej chwili */
    private static final long TIME_WINDOW = 2L;

    /** Łączny czas trwania odliczania w tickach (60 sekund × 20 ticków/s = 1200) */
    private static final int COUNTDOWN_TICKS = 1200; // 60 sekund

    /** Tick, od którego wyświetlamy ostrzeżenie "RUN" (10 sekund przed atakiem = 200 ticków) */
    private static final int RUN_WARNING_TICKS = 200;

    /** Szansa bazowa na aktywację Witching Hour (25% = 0.25) */
    private static final double BASE_ACTIVATION_CHANCE = 0.25;

    /** Co którą noc jest gwarantowana aktywacja */
    private static final int GUARANTEED_ACTIVATION_EVERY_N_NIGHTS = 5;

    // -----------------------------------------------------------------------
    // Stan gry per-gracz
    // -----------------------------------------------------------------------

    /**
     * Licznik nocy per-gracz (UUID → liczba nocy które minęły).
     * Resetuje się razem ze światem — trzymamy w pamięci (nie persystujemy).
     */
    private final Map<UUID, Integer> nightCounters = new HashMap<>();

    /**
     * Mapa aktywnych odliczeń per-gracz.
     * UUID → pozostałe ticki do ataku (null = brak aktywnego odliczania)
     */
    private final Map<UUID, Integer> activeCountdowns = new HashMap<>();

    /**
     * Zbiór UUID graczy dla których Witching Hour już się rozpoczął tej nocy.
     * Zapobiega wielokrotnemu triggerowi w obrębie jednej nocy.
     */
    private final Set<UUID> triggeredThisNight = new HashSet<>();

    /**
     * Zbiór UUID graczy dla których ten sam cykl nocny już "minął" — reset przy świcie.
     */
    private final Set<UUID> nightProcessed = new HashSet<>();

    // -----------------------------------------------------------------------
    // Główny tick handler (Server Side)
    // -----------------------------------------------------------------------

    /**
     * Wywoływany co tick po stronie serwera (Phase.END = po zaktualizowaniu gry).
     * To tutaj cała logika czasowa się dzieje.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Iterujemy po wszystkich wymiarach serwera
        for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
            // Interesuje nas tylko Overworld (wymiar z normalną dobą)
            if (serverLevel.dimension() != Level.OVERWORLD) continue;

            long dayTime = serverLevel.getDayTime() % 24000L; // czas w obrębie jednej doby

            // --- Reset przy świcie (czas 0–1000 = poranek) ---
            if (dayTime < 1000) {
                resetNightState(serverLevel);
            }

            // --- Sprawdzanie północy ---
            boolean isMidnight = (dayTime >= MIDNIGHT_TIME - TIME_WINDOW)
                    && (dayTime <= MIDNIGHT_TIME + TIME_WINDOW);

            for (ServerPlayer player : serverLevel.players()) {
                UUID uuid = player.getUUID();

                if (isMidnight && !triggeredThisNight.contains(uuid)) {
                    tryActivateWitchingHour(player, uuid);
                }

                // Tick aktywnego odliczania dla gracza
                if (activeCountdowns.containsKey(uuid)) {
                    tickCountdown(player, serverLevel, uuid);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Logika aktywacji Witching Hour
    // -----------------------------------------------------------------------

    /**
     * Sprawdza warunki i ewentualnie aktywuje Witching Hour dla gracza.
     *
     * @param player Gracz serwera
     * @param uuid   UUID gracza
     */
    private void tryActivateWitchingHour(ServerPlayer player, UUID uuid) {
        // Oznacz że sprawdziliśmy tę noc dla tego gracza
        triggeredThisNight.add(uuid);

        // Inkrementuj licznik nocy
        int nightCount = nightCounters.getOrDefault(uuid, 0) + 1;
        nightCounters.put(uuid, nightCount);

        ThePunishmentMod.LOGGER.info("[ThePunishment] Gracz {} | Noc #{} | Sprawdzanie Witching Hour...",
                player.getName().getString(), nightCount);

        // Sprawdź czy ta noc jest gwarantowaną aktywacją (co N nocy)
        boolean isGuaranteedNight = (nightCount % GUARANTEED_ACTIVATION_EVERY_N_NIGHTS == 0);

        // Losowanie aktywacji
        boolean activates = isGuaranteedNight || (Math.random() < BASE_ACTIVATION_CHANCE);

        if (!activates) {
            ThePunishmentMod.LOGGER.info("[ThePunishment] Witching Hour NIE aktywowała się tej nocy.");
            return;
        }

        ThePunishmentMod.LOGGER.info("[ThePunishment] *** WITCHING HOUR AKTYWNA dla {}! (gwarantowana={}) ***",
                player.getName().getString(), isGuaranteedNight);

        // Rozpocznij odliczanie dla gracza
        activeCountdowns.put(uuid, COUNTDOWN_TICKS);

        // Wyślij pakiet do klienta: wyświetl "It is 600 blocks away from you"
        WitchingHourPacketHelper.sendStartMessage(player);
    }

    // -----------------------------------------------------------------------
    // Tick odliczania
    // -----------------------------------------------------------------------

    /**
     * Odpowiada za odliczanie i kolejne etapy Witching Hour.
     *
     * @param player      Gracz
     * @param serverLevel Wymiar serwera
     * @param uuid        UUID gracza
     */
    private void tickCountdown(ServerPlayer player, ServerLevel serverLevel, UUID uuid) {
        int remaining = activeCountdowns.get(uuid);
        remaining--;

        if (remaining <= 0) {
            // --- Czas minął: Spawn encji i atak ---
            activeCountdowns.remove(uuid);
            spawnPunishment(player, serverLevel);
            return;
        }

        activeCountdowns.put(uuid, remaining);

        // --- Oblicz "odległość" do wyświetlenia (600 → 0 w 1200 ticków) ---
        // Przelicznik: każdy tick = 0.5 bloku mniej
        int displayedBlocks = (int) (remaining * 0.5);

        // Wyślij aktualizację do klienta co 20 ticków (co sekundę) — optymalizacja
        if (remaining % 20 == 0) {
            WitchingHourPacketHelper.sendCountdownUpdate(player, displayedBlocks, remaining);
        }

        // --- Próg "RUN": zostało ≤ 10 sekund (200 ticków) ---
        if (remaining == RUN_WARNING_TICKS) {
            WitchingHourPacketHelper.sendRunWarning(player);
        }
    }

    // -----------------------------------------------------------------------
    // Spawn encji
    // -----------------------------------------------------------------------

    /**
     * Teleportuje / spawnuje The Punishment bezpośrednio przy graczu.
     *
     * @param player      Cel
     * @param serverLevel Wymiar
     */
    private void spawnPunishment(ServerPlayer player, ServerLevel serverLevel) {
        ThePunishmentMod.LOGGER.info("[ThePunishment] Spawning The Punishment przy graczu {}!",
                player.getName().getString());

        ThePunishmentEntity punishment = ModEntities.THE_PUNISHMENT.get()
                .create(serverLevel);

        if (punishment == null) {
            ThePunishmentMod.LOGGER.error("[ThePunishment] Nie udało się stworzyć encji!");
            return;
        }

        // Teleport dokładnie 1.5 bloku za graczem (żeby był widoczny)
        double spawnX = player.getX() + 1.5;
        double spawnY = player.getY();
        double spawnZ = player.getZ() + 1.5;

        punishment.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0);
        serverLevel.addFreshEntity(punishment);

        // Powiadom klienta żeby ukrył overlay HUD
        WitchingHourPacketHelper.sendAttackEvent(player);
    }

    // -----------------------------------------------------------------------
    // Reset stanu nocnego
    // -----------------------------------------------------------------------

    /**
     * Reset flag nocnych przy nadejściu dnia.
     * Wywoływane gdy dayTime < 1000 (rano).
     *
     * @param serverLevel Wymiar
     */
    private void resetNightState(ServerLevel serverLevel) {
        for (ServerPlayer player : serverLevel.players()) {
            UUID uuid = player.getUUID();
            if (nightProcessed.contains(uuid)) continue;

            nightProcessed.add(uuid);
            triggeredThisNight.remove(uuid);

            // Jeśli gracz ma aktywne odliczanie i przeżył noc (np. wylogował się)
            // — anuluj odliczanie
            if (activeCountdowns.containsKey(uuid)) {
                activeCountdowns.remove(uuid);
                WitchingHourPacketHelper.sendCancelEvent(player);
            }
        }
    }
}

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRegisterCommands(net.minecraftforge.eventforge.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("setwitchinghour")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                try {
                    startWitchingHour(context.getSource().getLevel());
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Rozpoczęto Witching Hour!"), true);
                } catch (Exception e) {
                    context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Blad: " + e.getMessage()));
                }
                return 1;
            })
        );
    }
