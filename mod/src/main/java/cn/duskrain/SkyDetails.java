package cn.duskrain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Hanging roots, green vines and anchored chain lanterns below the authored islands. */
public final class SkyDetails {
    static void paint(CityPlan.Sink sink,int cx,int cz){
        for(int island=0;island<Terrain.ISLANDS.length;island++){int[] a=Terrain.ISLANDS[island];
            for(int i=0;i<18;i++){double angle=i*Math.PI/9+.31*island,r=.55+.25*(i%3)/2;int x=a[0]+(int)(Math.cos(angle)*a[2]*r),z=a[1]+(int)(Math.sin(angle)*a[2]*.79*r),y=Terrain.skyBottom(x,z);
                if(Math.abs(x-cx*16-8)>12||Math.abs(z-cz*16-8)>12||y<=Terrain.ground(x,z)+6)continue;
                int length=Math.min(7+i%9,y-Terrain.ground(x,z)-4);if(length<3)continue;
                if(i%3==0){sink.block(x,y,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());for(int k=1;k<length;k++)sink.block(x,y-k,z,Blocks.CHAIN.defaultBlockState());sink.block(x,y-length,z,Decor.LANTERN.get().defaultBlockState());}
                else if(i%3==1){for(int k=1;k<=length;k++)sink.block(x,y-k,z,k==length?Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.AGE_25,25).setValue(BlockStateProperties.BERRIES,true):Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(BlockStateProperties.BERRIES,k%4==0));}
                else{for(int k=0;k<length-2;k++){int bend=k>length/2?(int)Math.signum(Math.cos(angle)):0;sink.block(x+bend,y-k,z,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.Y));if(bend!=0)sink.block(x,y-k,z,Blocks.DARK_OAK_LOG.defaultBlockState());}sink.block(x,y-length+2,z,Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT,true));}
            }
        }
    }
}
