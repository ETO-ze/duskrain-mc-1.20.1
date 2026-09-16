package cn.duskrain.mixin;
import cn.duskrain.Protection;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=FireBlock.class,remap=false)
public abstract class FireGuard {
    // tryCatchFire is a Forge-added stable method in the pinned 47.4.10 build.
    @Inject(method="tryCatchFire",at=@At("HEAD"),cancellable=true,remap=false)
    private void duskrain$protectFuel(Level level,BlockPos pos,int odds,RandomSource random,int age,Direction face,CallbackInfo ci){if(Protection.protectedAt(level,pos))ci.cancel();}
}
