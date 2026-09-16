package cn.duskrain;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ResidentRenderer extends HumanoidMobRenderer<Resident,ResidentModel> {
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER=new net.minecraft.client.model.geom.ModelLayerLocation(new ResourceLocation(DuskRain.ID,"resident"),"main");
    ResidentRenderer(EntityRendererProvider.Context context){super(context,new ResidentModel(context.bakeLayer(LAYER)),.45f);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(LAYER,ResidentModel::layer);}
    @Override public ResourceLocation getTextureLocation(Resident resident){var service=Services.byEntity(resident);return new ResourceLocation(DuskRain.ID,"textures/entity/resident_"+(service==null?"materials":service.id())+".png");}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Resident.TYPE.get(),ResidentRenderer::new);}
}
