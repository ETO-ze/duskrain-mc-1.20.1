package cn.duskrain.mixin;

import cn.duskrain.CityMobility;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Player.class,remap=false)
public abstract class CityAirControl {
    @Inject(method={"getFlyingSpeed","m_274460_"},remap=false,at=@At("RETURN"),cancellable=true)
    private void duskrain$air(CallbackInfoReturnable<Float> ci){
        Player p=(Player)(Object)this;
        if(CityMobility.city(p)&&!p.getAbilities().flying&&!p.isFallFlying()&&!p.isInWaterOrBubble()){
            // Vanilla stone: ground damping .6*.91, air damping .91. Match their steady speeds.
            ci.setReturnValue(p.getSpeed()*((1-.91f)/(1-.546f)));
        }
    }
}
