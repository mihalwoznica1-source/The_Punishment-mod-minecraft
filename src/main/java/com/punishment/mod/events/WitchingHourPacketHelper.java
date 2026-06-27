package com.punishment.mod.events;

import com.punishment.mod.ThePunishmentMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Pomocnik do wysyłania pakietów sieciowych z serwera do klienta.
 *
 * Pakiety używane przez Witching Hour:
 * - START        : "It is 600 blocks away from you"
 * - COUNTDOWN    : aktualizacja liczby bloków + tick
 * - RUN_WARNING  : czerwony ekran + napis "RUN"
 * - ATTACK       : encja zaatakowała — wyczyść HUD
 * - CANCEL       : anuluj odliczanie (np. gracz przeżył noc)
 */
public class WitchingHourPacketHelper {

    /** Wersja protokołu pakietów — zmień przy każdej modyfikacji API */
    private static final String PROTOCOL_VERSION = "1";

    /** Kanał sieciowy Forge */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation(ThePunishmentMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // ID pakietów
    public static final int PACKET_START     = 0;
    public static final int PACKET_COUNTDOWN = 1;
    public static final int PACKET_RUN       = 2;
    public static final int PACKET_ATTACK    = 3;
    public static final int PACKET_CANCEL    = 4;

    /** Rejestracja wszystkich typów pakietów — wywołaj raz w commonSetup */
    public static void registerPackets() {
        int id = 0;

        // Pakiet START — brak dodatkowych danych
        CHANNEL.registerMessage(id++, WitchingPayload.class,
                WitchingPayload::encode, WitchingPayload::decode,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() ->
                            com.punishment.mod.client.WitchingHourClientHandler.handlePacket(msg.type, msg.blocks, msg.ticks));
                    ctx.get().setPacketHandled(true);
                });
    }

    // -----------------------------------------------------------------------
    // Metody wysyłające
    // -----------------------------------------------------------------------

    public static void sendStartMessage(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new WitchingPayload(PACKET_START, 600, COUNTDOWN_TICKS_CONST));
    }

    public static void sendCountdownUpdate(ServerPlayer player, int blocks, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new WitchingPayload(PACKET_COUNTDOWN, blocks, ticks));
    }

    public static void sendRunWarning(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new WitchingPayload(PACKET_RUN, 100, 200));
    }

    public static void sendAttackEvent(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new WitchingPayload(PACKET_ATTACK, 0, 0));
    }

    public static void sendCancelEvent(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new WitchingPayload(PACKET_CANCEL, 0, 0));
    }

    // Stała pomocnicza
    private static final int COUNTDOWN_TICKS_CONST = 1200;

    // -----------------------------------------------------------------------
    // Klasa payloadu (wspólna dla wszystkich pakietów)
    // -----------------------------------------------------------------------

    public static class WitchingPayload {
        public final int type;   // typ pakietu (PACKET_*)
        public final int blocks; // wyświetlana odległość w blokach
        public final int ticks;  // pozostałe ticki

        public WitchingPayload(int type, int blocks, int ticks) {
            this.type = type;
            this.blocks = blocks;
            this.ticks = ticks;
        }

        public static void encode(WitchingPayload msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.type);
            buf.writeInt(msg.blocks);
            buf.writeInt(msg.ticks);
        }

        public static WitchingPayload decode(FriendlyByteBuf buf) {
            return new WitchingPayload(buf.readInt(), buf.readInt(), buf.readInt());
        }
    }
}
