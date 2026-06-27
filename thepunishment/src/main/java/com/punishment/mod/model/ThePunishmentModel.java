package com.punishment.mod.model;

import com.punishment.mod.ThePunishmentMod;
import com.punishment.mod.entity.ThePunishmentEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Model 3D encji "The Punishment".
 *
 * Używa standardowego HumanoidModel (jak gracz / zombie).
 * Tekstura: 64x64 (standardowy format Minecraft humanoid).
 *
 * Lokalizacja warstwy modelu — potrzebna do rejestracji w EntityRenderersEvent.
 */
@OnlyIn(Dist.CLIENT)
public class ThePunishmentModel extends HumanoidModel<ThePunishmentEntity> {

    /** Identyfikator warstwy modelu rejestrowany w EntityModelLayers */
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(ThePunishmentMod.MOD_ID, "the_punishment"),
            "main"
    );

    public ThePunishmentModel(ModelPart root) {
        super(root);
    }

    /**
     * Definiuje geometrię modelu.
     * Używa standardowych wymiarów humanoidalnych Minecrafta.
     * Wywoływane podczas rejestracji warstwy modelu.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(
                CubeDeformation.NONE, // brak dodatkowego wypełnienia kuboidów
                0.0F
        );
        return LayerDefinition.create(mesh, 64, 64);
    }
}
