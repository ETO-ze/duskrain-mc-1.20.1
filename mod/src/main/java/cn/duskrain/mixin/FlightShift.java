package cn.duskrain.mixin;
import cn.duskrain.SwordFlight;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Shift is vertical flight input; server mount/unmount packets must remain authoritative. */
@Mixin(value=Player.class,remap=false)
public abstract class FlightShift {
 @Inject(method={"wantsToStopRiding","m_36342_"},remap=false,at=@At("HEAD"),cancellable=true)
 private void duskrainDescend(CallbackInfoReturnable<Boolean> ci){if(((Player)(Object)this).getVehicle() instanceof SwordFlight.Sword)ci.setReturnValue(false);}
}
