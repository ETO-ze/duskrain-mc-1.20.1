package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Shared safety rule for wilderness destinations and saved homes. */
public final class SafeLanding {
    public static boolean valid(ServerLevel level,BlockPos feet){
        if(!level.hasChunkAt(feet)||!level.getWorldBorder().isWithinBounds(feet)||feet.getY()<=level.getMinBuildHeight()||feet.getY()+2>=level.getMaxBuildHeight())return false;
        for(BlockPos pos:new BlockPos[]{feet,feet.above()}){
            var state=level.getBlockState(pos);
            if(!level.getFluidState(pos).isEmpty()||!state.getCollisionShape(level,pos).isEmpty()||hazard(state))return false;
        }
        var floor=level.getBlockState(feet.below());
        return floor.isFaceSturdy(level,feet.below(),Direction.UP)&&level.getFluidState(feet.below()).isEmpty()&&!hazard(floor);
    }
    static boolean hazard(net.minecraft.world.level.block.state.BlockState state){return state.is(Blocks.MAGMA_BLOCK)||state.is(Blocks.CACTUS)||state.is(Blocks.CAMPFIRE)||state.is(Blocks.SOUL_CAMPFIRE)||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE)||state.is(Blocks.POWDER_SNOW);}
}
