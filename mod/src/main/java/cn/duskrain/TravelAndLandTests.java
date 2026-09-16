package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(DuskRain.ID)
@PrefixGameTestTemplate(false)
public final class TravelAndLandTests {
    @GameTest(template="empty",timeoutTicks=1800) public static void wilderness_loads_real_terrain_before_height_query(GameTestHelper h){
        ServerLevel level=h.getLevel();int[] cursor={0};var candidates=WildernessTravel.locations(level.getServer());
        // A random native chunk may be all ocean or slope. Exercise production candidates,
        // rather than assuming a fixed coordinate is flat for every generated seed.
        var futures=new java.util.HashMap<Integer,java.util.concurrent.CompletableFuture<com.mojang.datafixers.util.Either<net.minecraft.world.level.chunk.ChunkAccess,ChunkHolder.ChunkLoadingFailure>>>();
        h.succeedWhen(()->{h.assertTrue(cursor[0]<candidates.size(),"No safe terrain in the production search candidates");ChunkPos chunk=candidates.get(cursor[0]);
            var future=futures.computeIfAbsent(cursor[0],i->level.getChunkSource().getChunkFuture(chunk.x,chunk.z,ChunkStatus.FULL,true));h.assertTrue(future.isDone(),"Waiting for native asynchronous terrain generation");h.assertTrue(future.join().left().isPresent(),"Native generation must return a real chunk");
            BlockPos found=WildernessTravel.findInChunk(level,chunk);if(found==null){if(cursor[0]<3){var feet=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,chunk.getMiddleBlockPosition(0));DuskRain.LOG.info("WILDERNESS_DIAGNOSTIC chunk={} loaded={} y={} floor={} feet={} head={} border={}",chunk,level.hasChunk(chunk.x,chunk.z),feet.getY(),level.getBlockState(feet.below()),level.getBlockState(feet),level.getBlockState(feet.above()),level.getWorldBorder().isWithinBounds(feet));}cursor[0]++;h.assertTrue(false,"Continue past ocean or unsafe slope");}h.assertTrue(SafeLanding.valid(level,found),"Selected landing has a solid floor, empty headroom, no fluids or hazards");
            var before=level.getBlockState(found.below());try{level.setBlock(found.below(),Blocks.MAGMA_BLOCK.defaultBlockState(),3);h.assertTrue(!SafeLanding.valid(level,found),"Changed hazardous floor invalidates the landing");}finally{level.setBlock(found.below(),before,3);}
            DuskRain.LOG.info("WILDERNESS_ACCEPTANCE generatedChunk={},{} safeLanding={}",chunk.x,chunk.z,found);
        });
    }
    @GameTest(template="empty") public static void city_blessings_double_jump_and_dimension_cleanup(GameTestHelper h){
        ServerLevel city=h.getLevel().getServer().getLevel(Gameplay.CITY);BlockPos floor=new BlockPos(5,CityPlan.SPAWN_Y-1,28);city.getChunkAt(floor);var old=city.getBlockState(floor);city.setBlock(floor,Blocks.STONE.defaultBlockState(),3);
        try(NativeAcceptanceTests.Peer peer=new NativeAcceptanceTests.Peer(city,"DRCityJump",Vec3.atBottomCenterOf(floor.above()))){
            var p=peer.player;p.setOnGround(true);p.getFoodData().setFoodLevel(3);double base=p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            CityMobility.tick(p);CityMobility.tick(p);h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-base*1.4)<.0001,"City speed is exactly +40% without stacking");h.assertTrue(p.getFoodData().getFoodLevel()==20&&p.getFoodData().getSaturationLevel()==10,"City restores hunger and saturation");
            p.setOnGround(false);p.setPos(p.getX(),p.getY()+1,p.getZ());h.assertTrue(CityMobility.jump(p),"One extra airborne jump follows an actual supported landing");h.assertTrue(!CityMobility.jump(p),"Repeated packets cannot produce a third jump");
            h.assertTrue(peer.packets.stream().anyMatch(packet->{if(!(packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket custom))return false;var data=new net.minecraft.network.FriendlyByteBuf(custom.getData().copy());try{return data.readVarInt()==8;}finally{data.release();}}),"Server authorizes a vertical-only jump impulse");
            h.assertTrue(peer.packets.stream().noneMatch(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket.class::isInstance),"Jump never overwrites the client horizontal momentum with the server stale velocity");
            CityMobility.READY.add(p.getUUID());p.setServerLevel(h.getLevel());CityMobility.tick(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-base)<.0001&&!CityMobility.jump(p)&&!CityMobility.READY.contains(p.getUUID()),"Leaving the city removes speed and extra jump immediately");
        }finally{city.setBlock(floor,old,3);}
        h.succeed();
    }
    @GameTest(template="empty") public static void claim_expansion_inherits_members_and_home_is_safe(GameTestHelper h){
        var owner=IntegrationTests.player(h,"DRLandOwner");var member=IntegrationTests.player(h,"DRLandMember");Store data=Store.get(owner.server);ChunkPos first=owner.chunkPosition();long key=first.toLong(),second=ChunkPos.asLong(first.x+1,first.z);var beforeFirst=data.claims.get(key);var beforeSecond=data.claims.get(second);
        try{
            data.claims.remove(key);data.claims.remove(second);DRCommands.claimAdd(owner);LandClaims.trust(owner,member.getUUID().toString(),true);
            owner.setPos(first.getMinBlockX()+17,owner.getY(),first.getMinBlockZ()+2);DRCommands.claimAdd(owner);
            h.assertTrue(data.claims.get(second)!=null&&data.claims.get(second).allows(member.getUUID()),"New adjacent claim inherits the owner's existing members");
            LandClaims.trust(owner,member.getUUID().toString(),false);h.assertTrue(!data.claims.get(key).allows(member.getUUID())&&!data.claims.get(second).allows(member.getUUID()),"Offline UUID revocation affects all owned chunks");
            Store copy=Store.load(data.save(new net.minecraft.nbt.CompoundTag()));h.assertTrue(copy.claims.get(second).owner.equals(owner.getUUID())&&!copy.claims.get(second).allows(member.getUUID()),"Claim ownership and revocation survive saved-data reload");
            owner.setPos(owner.getX(),250,owner.getZ());DRCommands.homeSet(owner);h.assertTrue(!Store.of(owner).hasHome,"Home cannot be saved in midair");
        }finally{if(beforeFirst==null)data.claims.remove(key);else data.claims.put(key,beforeFirst);if(beforeSecond==null)data.claims.remove(second);else data.claims.put(second,beforeSecond);}
        h.succeed();
    }
}
