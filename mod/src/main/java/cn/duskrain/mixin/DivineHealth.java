package cn.duskrain.mixin;
import cn.duskrain.TrialBoss;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Only the authored god guardian needs health above vanilla's 1024 attribute ceiling. */
@Mixin(value=LivingEntity.class,remap=false)
public abstract class DivineHealth {
 @Inject(method={"getMaxHealth","m_21233_"},remap=false,at=@At("HEAD"),cancellable=true)
 private void duskrainDivineMaximum(CallbackInfoReturnable<Float> ci){if((Object)this instanceof TrialBoss b&&b.tier>=4)ci.setReturnValue(b.divineMax());}
}
