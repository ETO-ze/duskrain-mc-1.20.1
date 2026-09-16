package cn.duskrain;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BossRenderer extends GeoEntityRenderer<TrialBoss> {
    BossRenderer(EntityRendererProvider.Context ctx){super(ctx,new GeoModel<TrialBoss>(){
        public ResourceLocation getModelResource(TrialBoss b){return new ResourceLocation(DuskRain.ID,"geo/"+b.modelName()+".geo.json");}
        public ResourceLocation getTextureResource(TrialBoss b){return new ResourceLocation(DuskRain.ID,"textures/entity/"+b.modelName()+".png");}
        public ResourceLocation getAnimationResource(TrialBoss b){return new ResourceLocation(DuskRain.ID,"animations/guardian.animation.json");}
    });shadowRadius=.8f;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(TrialBoss.WOOD.get(),BossRenderer::new);e.registerEntityRenderer(TrialBoss.ROCK.get(),BossRenderer::new);e.registerEntityRenderer(TrialBoss.DRAGON.get(),BossRenderer::new);e.registerEntityRenderer(TrialBoss.SENTINEL.get(),BossRenderer::new);}
}
