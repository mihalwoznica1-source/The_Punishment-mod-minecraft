package com.punishment.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.punishment.mod.ThePunishmentMod;
import com.punishment.mod.entity.ThePunishmentEntity;
import com.punishment.mod.model.ThePunishmentModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThePunishmentRenderer extends MobRenderer<ThePunishmentEntity, ThePunishmentModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ThePunishmentMod.MOD_ID,
            "textures/entity/the_punishment.png"
    );

    public ThePunishmentRenderer(EntityRendererProvider.Context context) {
        super(context,
                new ThePunishmentModel(context.bakeLayer(ThePunishmentModel.LAYER_LOCATION)),
                0.0F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(ThePunishmentEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ThePunishmentEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, 0);
    }

    @Override
    protected boolean shouldShowName(ThePunishmentEntity entity) {
        return false;
    }
}
