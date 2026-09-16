package cn.duskrain;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT)
public final class ClientClaimBorders {
    static ClaimBorders.Snapshot current=new ClaimBorders.Snapshot(List.of());
    static long received;
    public static void receive(ClaimBorders.Snapshot s){current=s;received=System.currentTimeMillis();}
    static void line(VertexConsumer b,PoseStack.Pose pose,double x,double y,double z,double xx,double yy,double zz,int color){float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,blue=(color&255)/255f;float dx=(float)(xx-x),dy=(float)(yy-y),dz=(float)(zz-z),len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<.001)return;
        b.vertex(pose.pose(),(float)x,(float)y,(float)z).color(r,g,blue,.90f).normal(pose.normal(),dx/len,dy/len,dz/len).endVertex();b.vertex(pose.pose(),(float)xx,(float)yy,(float)zz).color(r,g,blue,.90f).normal(pose.normal(),dx/len,dy/len,dz/len).endVertex();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){var mc=Minecraft.getInstance();if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||mc.level==null||mc.level.dimension()!=Level.OVERWORLD||mc.player==null||System.currentTimeMillis()-received>3500)return;var camera=e.getCamera().getPosition();PoseStack stack=e.getPoseStack();stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);var buffers=mc.renderBuffers().bufferSource();var b=buffers.getBuffer(RenderType.lines());Map<String,ClaimBorders.Edge> labels=new LinkedHashMap<>();
        for(var edge:current.edges()){if(!mc.level.hasChunk(edge.x()>>4,edge.z()>>4)&&!mc.level.hasChunk((edge.x()-(edge.alongX()?0:1))>>4,(edge.z()-(edge.alongX()?1:0))>>4))continue;int color=new int[]{0x60F3E5,0xFFD26C,0xFF5959}[edge.relation()];int[] h=edge.heights();for(int j=0;j<16;j++){double x=edge.x()+(edge.alongX()?j:0),z=edge.z()+(edge.alongX()?0:j);if(mc.player.distanceToSqr(x,mc.player.getY(),z)>48*48)continue;line(b,stack.last(),x,h[j]+.10,z,x+(edge.alongX()?1:0),h[j+1]+.10,z+(edge.alongX()?0:1),color);}line(b,stack.last(),edge.x(),h[0],edge.z(),edge.x(),h[0]+2.5,edge.z(),color);labels.putIfAbsent(edge.owner(),edge);}
        buffers.endBatch(RenderType.lines());
        for(var edge:labels.values()){stack.pushPose();stack.translate(edge.x(),edge.heights()[0]+2.9,edge.z());stack.mulPose(e.getCamera().rotation());stack.scale(-.025f,-.025f,.025f);String label="宅地 · "+edge.owner();mc.font.drawInBatch(label,-mc.font.width(label)/2f,0,new int[]{0x60F3E5,0xFFD26C,0xFF5959}[edge.relation()],false,stack.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0x600E2222,15728880);stack.popPose();}buffers.endBatch();stack.popPose();
    }
}
