package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;

@GameTestHolder(DuskRain.ID) @PrefixGameTestTemplate(false)
public final class BridgeClearanceTests {
    @GameTest(template="empty") public static void ascending_player_clears_relocated_lantern(GameTestHelper h){
        var l=h.getLevel();BlockPos lamp=h.absolutePos(new BlockPos(3,4,3));
        l.setBlock(lamp,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),3);
        // A 0.6-wide, 1.8-high player raising by a half stair, beneath the old lamp.
        AABB headStep=new AABB(lamp.getX()+.2,lamp.getY()-1.5,lamp.getZ()+.2,lamp.getX()+.8,lamp.getY()+.3,lamp.getZ()+.8);
        h.assertTrue(!l.noCollision(headStep),"Original hanging lamp blocks the ascending player");
        h.assertTrue(SkyBridges.liftLantern(l,lamp)&&l.noCollision(headStep),"Raised lamp leaves headroom through the step");
        h.assertTrue(!SkyBridges.liftLantern(l,lamp),"Migration is idempotent");
        l.setBlock(lamp,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true),3);
        l.setBlock(lamp.above(2),Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(),3);
        h.assertTrue(!SkyBridges.liftLantern(l,lamp)&&l.getBlockState(lamp.above(2)).is(Blocks.CHISELED_QUARTZ_BLOCK),"Do not overwrite a maintenance edit");
        h.succeed();
    }
}
