package cn.duskrain.mixin;
import cn.duskrain.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Level.class,remap=false)
public abstract class FirePlacementGuard {
    @Inject(method={"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z","m_6933_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"},at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void duskrain$noWildfire(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> ci){Level level=(Level)(Object)this;if(!level.isClientSide&&state.getBlock() instanceof BaseFireBlock&&Protection.protectedAt(level,pos))ci.setReturnValue(false);}
}
