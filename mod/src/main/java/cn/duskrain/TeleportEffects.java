package cn.duskrain;

import net.minecraft.core.particles.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Visible to nearby players; cosmetics follow authoritative successful teleports only. */
public final class TeleportEffects {
    static final DustParticleOptions JADE=new DustParticleOptions(new Vector3f(.42f,.86f,.83f),1.1f);
    public static void circle(ServerLevel l,Vec3 p,double phase,boolean arrival){
        for(int i=0;i<24;i++){double a=i*Math.PI/12+phase;double r=i%2==0?1.1:.75;
            l.sendParticles(JADE,p.x+Math.cos(a)*r,p.y+.12,p.z+Math.sin(a)*r,1,0,0,0,0);}
        if(arrival)l.sendParticles(ParticleTypes.END_ROD,p.x,p.y+1,p.z,24,.55,.8,.55,.025);
    }
    public static void travel(ServerPlayer p,ServerLevel destination,Vec3 to,float yaw){
        circle(p.serverLevel(),p.position(),0,true);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.6f,.8f);
        p.teleportTo(destination,to.x,to.y,to.z,yaw,0);p.fallDistance=0;
        circle(destination,to,Math.PI/6,true);
        destination.playSound(null,p.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.35f,1.4f);
    }
}
