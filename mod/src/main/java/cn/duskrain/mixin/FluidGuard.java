package cn.duskrain.mixin;
import cn.duskrain.Protection;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=FlowingFluid.class,remap=false)
public abstract class FluidGuard {
    // Both development and SRG names are verified against the pinned Forge jars.
    @Inject(method={"spreadTo","m_6364_"},at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void duskrain$border(LevelAccessor level,BlockPos pos,BlockState state,Direction direction,FluidState fluid,CallbackInfo ci){if(Protection.protectedAt(level,pos)&&!Protection.sameDomain(level,pos,pos.relative(direction.getOpposite())))ci.cancel();}
}
