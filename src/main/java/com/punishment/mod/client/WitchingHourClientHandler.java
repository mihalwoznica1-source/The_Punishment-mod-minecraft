package com.punishment.mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.punishment.mod.events.WitchingHourPacketHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handler po stronie klienta — odpowiada za:
 * - Wyświetlanie napisu "It is X blocks away from you"
 * - Czerwony overlay ekranu przy ostrzeżeniu "RUN"
 * - Duży napis "RUN" w centrum ekranu
 * - Czyszczenie HUD po zakończeniu zdarzenia
 *
 * TYLKO po stronie klienta (@OnlyIn(Dist.CLIENT)).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = com.punishment.mod.ThePunishmentMod.MOD_ID, value = Dist.CLIENT)
public class WitchingHourClientHandler {

    // -----------------------------------------------------------------------
    // Stan klienta
    // -----------------------------------------------------------------------

    /** Czy aktualnie trwa Witching Hour */
    private static boolean isActive = false;

    /** Czy wyświetlamy ostrzeżenie "RUN" (ostatnie 10 sekund) */
    private static boolean showRunWarning = false;

    /** Aktualnie wyświetlana "odległość" w blokach */
    private static int displayedBlocks = 600;

    /** Pozostałe ticki (do animacji / kalkulacji czasu wyświetlania) */
    private static int remainingTicks = 0;

    // -----------------------------------------------------------------------
    // Obsługa pakietów z serwera
    // -----------------------------------------------------------------------

    /**
     * Wywoływana przez packet handler gdy przychodzi wiadomość z serwera.
     *
     * @param type   Typ pakietu (WitchingHourPacketHelper.PACKET_*)
     * @param blocks Odległość w blokach
     * @param ticks  Pozostałe ticki
     */
    public static void handlePacket(int type, int blocks, int ticks) {
        switch (type) {
            case WitchingHourPacketHelper.PACKET_START -> {
                isActive = true;
                showRunWarning = false;
                displayedBlocks = 600;
                remainingTicks = 1200;
            }
            case WitchingHourPacketHelper.PACKET_COUNTDOWN -> {
                displayedBlocks = blocks;
                remainingTicks = ticks;
            }
            case WitchingHourPacketHelper.PACKET_RUN -> {
                showRunWarning = true;
                displayedBlocks = 100;
                remainingTicks = 200;
            }
            case WitchingHourPacketHelper.PACKET_ATTACK,
                 WitchingHourPacketHelper.PACKET_CANCEL -> {
                // Reset wszystkiego
                isActive = false;
                showRunWarning = false;
                displayedBlocks = 0;
                remainingTicks = 0;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Rendering GUI Overlay
    // -----------------------------------------------------------------------

    /**
     * Wywoływana przed renderowaniem nakładek GUI (hud, health, itd.).
     * Tutaj rysujemy nasze elementy Witching Hour.
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!isActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // --- 1. Czerwony overlay tła (ostatnie 10 sekund) ---
        if (showRunWarning) {
            drawRedOverlay(guiGraphics, screenW, screenH);
        }

        // --- 2. Napis "It is X blocks away from you" (góra ekranu) ---
        if (!showRunWarning) {
            drawDistanceMessage(guiGraphics, mc, screenW);
        }

        // --- 3. Duży napis "RUN" (centrum ekranu) ---
        if (showRunWarning) {
            drawRunWarning(guiGraphics, mc, screenW, screenH);
        }
    }

    /**
     * Rysuje półprzezroczysty czerwony overlay na całym ekranie.
     * Pulsuje rytmicznie dla efektu paniki.
     */
    private static void drawRedOverlay(GuiGraphics guiGraphics, int w, int h) {
        // Pulsacja na podstawie czasu systemowego (co 500ms zmienia intensywność)
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 300.0) * 0.05 + 0.30); // zakres 0.20 – 0.50

        // ARGB: alpha=pulsujące, R=255, G=0, B=0
        int alpha = (int)(pulse * 255);
        int color = (alpha << 24) | 0x00FF0000;

        guiGraphics.fill(0, 0, w, h, color);
    }

    /**
     * Rysuje wiadomość o odległości pod paskiem hotbara.
     * Przykład: "It is 342 blocks away from you"
     */
    private static void drawDistanceMessage(GuiGraphics guiGraphics, Minecraft mc, int screenW) {
        String message = "It is " + displayedBlocks + " blocks away from you";
        Component comp = Component.literal(message);

        int textW = mc.font.width(comp);
        int x = (screenW - textW) / 2;
        int y = 15; // 15 px od górnej krawędzi

        // Biały tekst z cieniem — dobrze widoczny na każdym tle
        guiGraphics.drawString(mc.font, comp, x, y, 0xFFFFFFFF, true);
    }

    /**
     * Rysuje duży, pulsujący napis "RUN" na środku ekranu.
     * Skalowany do 3× normalnego rozmiaru przez transform matrycy.
     */
    private static void drawRunWarning(GuiGraphics guiGraphics, Minecraft mc, int screenW, int screenH) {
        String runText = "RUN";
        Component runComp = Component.literal(runText);

        // Skala 3x dla dużego napisu
        float scale = 3.0f;

        // Pozycja centralna uwzględniająca skalę
        int textW = mc.font.width(runComp);
        float x = (screenW - textW * scale) / 2.0f;
        float y = (screenH / 2.0f) - (9 * scale / 2.0f) - 30; // trochę powyżej środka

        // Pulsowanie koloru (jasnoczerwony → ciemnoczerwony)
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 200.0) * 0.5 + 0.5); // 0.0 – 1.0
        int red = (int)(150 + pulse * 105); // 150 – 255
        int color = (0xFF << 24) | (red << 16); // opaque red

        // Push matrix dla skalowania
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        guiGraphics.drawString(mc.font, runComp, 0, 0, color, true);

        guiGraphics.pose().popPose();

        // Dodatkowy mniejszy napis z sekundami
        int secondsLeft = (int) Math.ceil(remainingTicks / 20.0);
        String secondsText = secondsLeft + "s";
        int secW = mc.font.width(secondsText);
        int secX = (screenW - secW) / 2;
        int secY = (int)(y + 9 * scale + 8);
        guiGraphics.drawString(mc.font, secondsText, secX, secY, 0xFFFF4444, true);
    }
}
