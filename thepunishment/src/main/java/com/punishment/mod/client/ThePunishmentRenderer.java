package com.punishment.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.punishment.mod.ThePunishmentMod;
import com.punishment.mod.entity.ThePunishmentEntity;
import com.punishment.mod.model.ThePunishmentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renderer encji "The Punishment".
 *
 * Kluczowe właściwości:
 * - Tekstura: całkowicie czarna (#000000) — brak jakichkolwiek detali
 * - Brak cienia (setShadowRadius(0)) — dodaje do efektu grozy
 * - Brak efektów świetlnych / glow
 * - Brak warstw (pancerz, ubranie itp.)
 */
@OnlyIn(Dist.CLIENT)
public class ThePunishmentRenderer extends MobRenderer<ThePunishmentEntity, ThePunishmentModel> {

    /**
     * Lokalizacja tekstury encji.
     * Plik: src/main/resources/assets/punishment/textures/entity/the_punishment.png
     * Musi być jednolicie czarny plik PNG 64x64.
     */
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ThePunishmentMod.MOD_ID,
            "textures/entity/the_punishment.png"
    );

    public ThePunishmentRenderer(EntityRendererProvider.Context context) {
        super(context,
                new ThePunishmentModel(context.bakeLayer(ThePunishmentModel.LAYER_LOCATION)),
                0.0F // promień cienia = 0 (brak cienia)
        );
    }

    /**
     * Zwraca teksturę encji.
     * Nasza tekstura to czysty czarny PNG — cały model będzie #000000.
     */
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ThePunishmentEntity entity) {
        return TEXTURE;
    }

    /**
     * Override renderowania — upewniamy się że encja jest zawsze ciemna,
     * niezależnie od oświetlenia świata (pełny blok = ciemność).
     */
    @Override
    public void render(@NotNull ThePunishmentEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        // Używamy minimalnego oświetlenia (0) żeby encja była zawsze czarna
        // nawet w pełnym świetle dnia — wywołujemy super z packedLight=0
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, 0);
    }

    /**
     * Wyłącza efekt "świecenia" (glow outline) który mógłby ujawnić krawędzie modelu.
     */
    @Override
    protected boolean shouldShowName(@NotNull ThePunishmentEntity entity) {
        return false; // Brak nameplate'u nad encją
    }

    /**
     * Encja nie rzuca cienia.
     */
    @Override
    protected float getShadowRadius(@NotNull ThePunishmentEntity entity) {
        return 0.0F;
    }
}
