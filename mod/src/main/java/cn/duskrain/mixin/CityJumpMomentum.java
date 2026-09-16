package cn.duskrain.mixin;

import cn.duskrain.CityMobility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=LivingEntity.class,remap=false)
public abstract class CityJumpMomentum {
    @Unique private Vec3 duskrain$before;
    @Unique private boolean duskrain$groundTravel;
    @Unique private boolean duskrain$controlled;
    @Unique private float duskrain$friction;
    @Inject(method={"travel","m_7023_"},remap=false,at=@At("HEAD"))
    private void duskrain$travelStart(Vec3 input,CallbackInfo ci){
        LivingEntity e=(LivingEntity)(Object)this;
        duskrain$controlled=e instanceof Player p&&CityMobility.city(p)&&!p.getAbilities().flying&&!p.isFallFlying()&&!p.isInWaterOrBubble()&&!p.onClimbable()&&p.hurtTime==0;
        duskrain$groundTravel=duskrain$controlled&&e.onGround();
        if(duskrain$groundTravel){var pos=net.minecraft.core.BlockPos.containing(e.getX(),e.getY()-.5000001,e.getZ());duskrain$friction=e.level().getBlockState(pos).getFriction(e.level(),pos,e);}
    }
    @Inject(method={"travel","m_7023_"},remap=false,at=@At("RETURN"))
    private void duskrain$travelEnd(Vec3 input,CallbackInfo ci){
        LivingEntity e=(LivingEntity)(Object)this;
        // Travel applies the starting surface's drag even on the takeoff tick.
        // Convert that stored velocity to air drag once; don't erase knockback or collision.
        if(duskrain$groundTravel&&!e.onGround()&&!e.horizontalCollision&&duskrain$friction>0){var v=e.getDeltaMovement();e.setDeltaMovement(v.x/duskrain$friction,v.y,v.z/duskrain$friction);}
        else if(duskrain$controlled&&!duskrain$groundTravel&&e.onGround()&&!e.horizontalCollision){var pos=net.minecraft.core.BlockPos.containing(e.getX(),e.getY()-.5000001,e.getZ());float friction=e.level().getBlockState(pos).getFriction(e.level(),pos,e);var v=e.getDeltaMovement();e.setDeltaMovement(v.x*friction,v.y,v.z*friction);}
        duskrain$groundTravel=false;
    }
    @Inject(method={"jumpFromGround","m_6135_"},remap=false,at=@At("HEAD"))
    private void duskrain$remember(CallbackInfo ci){LivingEntity e=(LivingEntity)(Object)this;duskrain$before=e instanceof Player&&CityMobility.city(e)?e.getDeltaMovement():null;}
    @Inject(method={"jumpFromGround","m_6135_"},remap=false,at=@At("RETURN"))
    private void duskrain$restore(CallbackInfo ci){if(duskrain$before!=null){LivingEntity e=(LivingEntity)(Object)this;e.setDeltaMovement(duskrain$before.x,e.getDeltaMovement().y,duskrain$before.z);duskrain$before=null;}}
}
