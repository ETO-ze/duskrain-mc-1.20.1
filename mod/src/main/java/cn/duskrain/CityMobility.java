package cn.duskrain;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

/** City-only benefits, with one server-authorized extra jump between actual landings. */
public final class CityMobility {
    static final UUID SPEED=UUID.fromString("58a4839b-65c2-4adf-bc8b-1a060b7d431a");
    static final Set<UUID> READY=new HashSet<>();
    public static boolean city(net.minecraft.world.entity.Entity p){return p.level().dimension().equals(Gameplay.CITY);}
    public static void tick(ServerPlayer p){
        boolean active=city(p)&&p.isAlive();
        Gameplay.multiplier(p,Attributes.MOVEMENT_SPEED,SPEED,active?CombatRules.current.citySpeed:0);
        if(!active||p.isSpectator()||p.getAbilities().flying||p.isPassenger()||p.isFallFlying()){READY.remove(p.getUUID());return;}
        if(p.tickCount%20==0){p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(10);}
        if(p.onGround()&&!p.serverLevel().noCollision(p,p.getBoundingBox().move(0,-.08,0)))READY.add(p.getUUID());
    }
    public static boolean jump(ServerPlayer p){
        if(!city(p)||!p.isAlive()||p.onGround()||p.isSpectator()||p.getAbilities().flying||p.isFallFlying()||p.isPassenger()||p.isInWaterOrBubble()||p.onClimbable()||Gameplay.RECALL.containsKey(p.getUUID())||!READY.remove(p.getUUID()))return false;
        var v=p.getDeltaMovement();p.setDeltaMovement(v.x,.55,v.z);p.fallDistance=0;
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.CityJumpImpulse(.55f));
        p.serverLevel().sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY()+.1,p.getZ(),12,.35,.06,.35,.015);
        return true;
    }
    public static void clear(UUID id){READY.remove(id);}
}
