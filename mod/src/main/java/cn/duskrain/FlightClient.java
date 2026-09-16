package cn.duskrain;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT)
public final class FlightClient {
    static final KeyMapping KEY=new KeyMapping("key.duskrain.flight",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.duskrain");
    static SwordFlight.State state=new SwordFlight.State(false,false,0,0,0);
    static final Map<UUID,PlayerTitles.Title> titles=new HashMap<>();
    @SubscribeEvent public static void trails(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.player.tickCount%2!=0)return;int left=ClientSpellFx.density()==2?6:16;for(var entity:mc.level.entitiesForRendering()){if(left<=0)break;if(!(entity instanceof SwordFlight.Sword s)||s.distanceToSqr(mc.player)>2304)continue;var delta=s.position().subtract(s.xo,s.yo,s.zo);if(delta.lengthSqr()<.0001)continue;var tail=s.position().subtract(s.getLookAngle().scale(.6));ClientSpellFx.dot(new SpellFx.Event(SpellFx.SWORD,tail,tail,1,1,0),tail);left--;}}
    public static void titles(PlayerTitles.View view){titles.clear();for(var t:view.titles())titles.put(t.uuid(),t);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null){titles.clear();state=new SwordFlight.State(false,false,0,0,0);return;}while(KEY.consumeClick())Network.action("dr flight toggle");if(mc.player.getVehicle() instanceof SwordFlight.Sword){int bits=0;if(mc.screen==null){if(mc.options.keyUp.isDown())bits|=1;if(mc.options.keyDown.isDown())bits|=2;if(mc.options.keyLeft.isDown())bits|=4;if(mc.options.keyRight.isDown())bits|=8;if(mc.options.keyJump.isDown())bits|=16;if(mc.options.keyShift.isDown())bits|=32;}Network.CHANNEL.sendToServer(new SwordFlight.Input(bits));}}
    @SubscribeEvent public static void names(RenderNameTagEvent e){var mc=Minecraft.getInstance();if(mc.player==null||!(e.getEntity() instanceof net.minecraft.world.entity.player.Player p)||p.isDiscrete()||p.isInvisibleTo(mc.player)||!mc.player.hasLineOfSight(p)||p.distanceToSqr(mc.player)>1024)return;var t=titles.get(p.getUUID());if(t==null)return;
        var pose=e.getPoseStack();pose.pushPose();pose.translate(0,p.getBbHeight()+.20,0);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(-.020f,-.020f,.020f);
        int alpha=(int)(255*Math.min(1,(32-Math.sqrt(p.distanceToSqr(mc.player)))/8));if(alpha>10)mc.font.drawInBatch(t.text(),-mc.font.width(t.text())/2f,0,(alpha<<24)|(t.stage()>=18?0xF3D78B:0xB2DEDA),false,pose.last().pose(),e.getMultiBufferSource(),net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,15728880);pose.popPose();
    }
    @SubscribeEvent public static void health(RenderGuiOverlayEvent.Pre e){var mc=Minecraft.getInstance();if(mc.player==null||mc.gameMode==null||!mc.gameMode.canHurtPlayer()||mc.player.getMaxHealth()<=40||!e.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()))return;e.setCanceled(true);var gui=(ForgeGui)mc.gui;var g=e.getGuiGraphics();int x=mc.getWindow().getGuiScaledWidth()/2-91,y=mc.getWindow().getGuiScaledHeight()-gui.leftHeight;int w=81;g.fill(x,y,x+w,y+9,0xC01B3032);g.fill(x+1,y+1,x+1+(int)((w-2)*Math.min(1,mc.player.getHealth()/mc.player.getMaxHealth())),y+8,0xFF9C4349);String text=Math.round(mc.player.getHealth())+" / "+Math.round(mc.player.getMaxHealth());g.drawCenteredString(mc.font,text,x+w/2,y,0xFFFFFF);gui.leftHeight+=10;}
    @Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(KEY);}
        @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(SwordFlight.TYPE.get(),SwordRenderer::new);}
    }
    public static final class SwordRenderer extends EntityRenderer<SwordFlight.Sword>{
        SwordRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.8f;}
        @Override public ResourceLocation getTextureLocation(SwordFlight.Sword e){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
        @Override public void render(SwordFlight.Sword e,float yaw,float pt,PoseStack pose,MultiBufferSource buffer,int light){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-yaw));pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(2.4f,2.4f,2.4f);
            int tier=e.tier();Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Content.ARTIFACTS.get(tier==6?18:tier).get()),ItemDisplayContext.FIXED,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffer,e.level(),e.getId());pose.popPose();super.render(e,yaw,pt,pose,buffer,light);}
    }
}
