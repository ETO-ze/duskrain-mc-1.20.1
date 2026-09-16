package cn.duskrain;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT)
public final class ClientSpellFx {
    static final List<Active> EVENTS=new ArrayList<>();
    static int density=-1;
    static int density(){if(density<0){density=1;try{var f=Minecraft.getInstance().gameDirectory.toPath().resolve("config/duskrain-fx-density.txt");if(java.nio.file.Files.exists(f))density=Math.max(0,Math.min(2,Integer.parseInt(java.nio.file.Files.readString(f).strip())));}catch(Exception ignored){}}return density;}
    static void cycleDensity(){density=(density()+1)%3;try{var f=Minecraft.getInstance().gameDirectory.toPath().resolve("config/duskrain-fx-density.txt");java.nio.file.Files.createDirectories(f.getParent());java.nio.file.Files.writeString(f,String.valueOf(density));}catch(Exception e){DuskRain.LOG.warn("Cannot save FX density",e);}}
    static class Active {SpellFx.Event event;int age;Active(SpellFx.Event e){event=e;}}
    static final float[][] COLORS={{.62f,1,.96f},{1,.78f,.26f},{1,.30f,.10f},{.42f,.75f,1},{.80f,.72f,1},{1,.83f,.34f},{.65f,1,.85f},{1,.16f,.14f},{.65f,1,1},{.75f,.45f,1},{1,.85f,.30f}};
    @SubscribeEvent public static void labels(net.minecraftforge.client.event.RenderLevelStageEvent e){var mc=Minecraft.getInstance();if(e.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||mc.level==null)return;var stack=e.getPoseStack();var camera=e.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();Set<String> seen=new HashSet<>();for(Active active:EVENTS){var fx=active.event;if(fx.number()<1||!seen.add(fx.from().toString()))continue;stack.pushPose();stack.translate(fx.from().x-camera.x,fx.from().y-camera.y+2,fx.from().z-camera.z);stack.mulPose(e.getCamera().rotation());stack.scale(-.06f,-.06f,.06f);String text=fx.number()==4?"◆ 已鸣":"◇ 第"+fx.number()+"鸣 ◇";mc.font.drawInBatch(text,-mc.font.width(text)/2f,0,0xEEFFDC,false,stack.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0xAA173A35,15728880);stack.popPose();}buffers.endBatch();}
    public static void receive(SpellFx.Event e){var mc=Minecraft.getInstance();if(mc.player==null||mc.player.distanceToSqr(e.from())>96*96)return;if(EVENTS.size()>=128)EVENTS.remove(0);EVENTS.add(new Active(e));}
    static int budget;
    static void dot(SpellFx.Event e,Vec3 p){if(budget--<=0)return;var l=Minecraft.getInstance().level;if(l==null)return;float[] c=COLORS[e.style()];l.addParticle(new DustParticleOptions(new Vector3f(c[0],c[1],c[2]),e.style()==SpellFx.BELL?1.8f:1.2f),true,p.x,p.y,p.z,0,0,0);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent tick){if(tick.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){EVENTS.clear();return;}int density=density();budget=new int[]{720,360,120}[density];
        Iterator<Active> it=EVENTS.iterator();while(it.hasNext()){Active a=it.next();var e=a.event;if(a.age++>=e.ticks()||mc.player.distanceToSqr(e.from())>96*96){it.remove();continue;}int stride=density==0?1:density==1?2:4;
            if(e.style()>=8){double spin=a.age*.045,r=e.radius();int n=density==2?16:32;for(int ring=0;ring<3;ring++)for(int j=0;j<n;j++){double angle=spin*(ring%2==0?1:-1)+j*Math.PI*2/n;Vec3 q=e.from().add(Math.cos(angle)*r*(1-ring*.2),ring*.18,Math.sin(angle)*r*(1-ring*.2));dot(e,q);if(j%4==0)for(int k=1;k<7;k++)dot(e,q.add(e.style()==8?0:Math.sin(k)*.25,k*.27,0));}if(e.style()==10)for(int j=0;j<30;j++){double v=j/30d*Math.PI*2;dot(e,e.from().add(Math.cos(v)*1.6,2+Math.sin(v)*2,0));}continue;}
            if(e.from().distanceToSqr(e.to())>.01){if(a.age==1){Vec3 delta=e.to().subtract(e.from());int n=Math.max(1,(int)(delta.length()*8/stride));for(int j=0;j<=n;j++){Vec3 p=e.from().add(delta.scale(j/(double)n));dot(e,p);if(e.style()==SpellFx.FIRE)mc.level.addParticle(ParticleTypes.FLAME,p.x,p.y,p.z,0,0,0);if(e.style()==SpellFx.SWORD){dot(e,p.add(0,.18,0));dot(e,p.add(0,-.18,0));}}}continue;}
            if(a.age%Math.max(1,stride-1)!=0)continue;
            double phase=a.age*.17,r=e.radius();int n=e.style()==SpellFx.BELL?24:48/stride;
            for(int j=0;j<n;j++){double angle=phase+j*Math.PI*2/n;double y=e.style()==SpellFx.GUARD?.2+Math.sin(angle*2+phase)*.7:0;dot(e,e.from().add(Math.cos(angle)*r,y,Math.sin(angle)*r));}
            if(e.style()==SpellFx.FROST||e.style()==SpellFx.BELL){for(int j=0;j<6;j++){double angle=j*Math.PI/3+phase*.2;for(int k=0;k<4;k++)dot(e,e.from().add(Math.cos(angle)*r*k/4,0,Math.sin(angle)*r*k/4));}}
            if(e.style()==SpellFx.BELL||e.style()==SpellFx.LIGHTNING){for(int j=0;j<28;j++){double y=j*.4;dot(e,e.from().add(e.style()==SpellFx.LIGHTNING?Math.sin(j*4+a.age)*.35:0,y,0));}}
        }
    }
}
