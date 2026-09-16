package cn.duskrain;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** One shared, asynchronous chunk search; requests never read an unloaded heightmap. */
public final class WildernessTravel {
    record Waiting(Vec3 origin,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension){}
    static final Map<UUID,Waiting> WAITING=new LinkedHashMap<>();
    static CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>> future;
    static List<ChunkPos> candidates=List.of();static int cursor;static long started;
    public static void request(ServerPlayer p){
        if(Store.of(p).combatUntil>System.currentTimeMillis()){Gameplay.say(p,"战斗尚未结束，暂时不能传送。");return;}
        if(Trials.inTrial(p.getUUID())){Gameplay.say(p,"请先退出试炼。");return;}
        if(Gameplay.RECALL.containsKey(p.getUUID())||WAITING.containsKey(p.getUUID())){Gameplay.say(p,"正在准备传送，请稍候。");return;}
        if(WAITING.isEmpty()){
            candidates=locations(p.server);cursor=0;future=null;started=System.currentTimeMillis();
        }
        WAITING.put(p.getUUID(),new Waiting(p.position(),p.level().dimension()));Gameplay.say(p,"正在勘察野外地形；找到安全地面后开始5秒引导，移动或受伤可取消。");
    }
    static List<ChunkPos> locations(MinecraftServer server){
            LinkedHashSet<ChunkPos> positions=new LinkedHashSet<>();Store data=Store.get(server);
            if(data.wildSpawn!=null)positions.add(new ChunkPos(data.wildSpawn));
            ChunkPos spawn=new ChunkPos(server.overworld().getSharedSpawnPos());positions.add(spawn);
            // Expand around the native world spawn, then the former wilderness entry region.
            for(ChunkPos center:List.of(spawn,new ChunkPos(75,75)))for(int ring=1;ring<=8;ring++)for(int dx=-ring;dx<=ring;dx++)for(int dz=-ring;dz<=ring;dz++)if(Math.max(Math.abs(dx),Math.abs(dz))==ring)positions.add(new ChunkPos(center.x+dx*2,center.z+dz*2));
        return List.copyOf(positions);
    }
    public static void cancel(UUID player){WAITING.remove(player);}
    public static void reset(){WAITING.clear();future=null;candidates=List.of();cursor=0;}
    public static void tick(MinecraftServer server){
        if(WAITING.isEmpty()){future=null;return;}
        WAITING.entrySet().removeIf(e->{var p=server.getPlayerList().getPlayer(e.getKey());boolean canceled=p==null||!p.isAlive()||!p.level().dimension().equals(e.getValue().dimension)||p.position().distanceToSqr(e.getValue().origin)>.04||Store.of(p).combatUntil>System.currentTimeMillis();if(canceled&&p!=null)Gameplay.say(p,"野外传送已取消。");return canceled;});
        if(WAITING.isEmpty()){future=null;return;}
        if(cursor>=candidates.size()||System.currentTimeMillis()-started>120000){finish(server,null);return;}
        ServerLevel level=server.overworld();ChunkPos chunk=candidates.get(cursor);
        if(future==null){
            if(!level.getWorldBorder().isWithinBounds(chunk.getMiddleBlockPosition(level.getSeaLevel()))){cursor++;return;}
            future=level.getChunkSource().getChunkFuture(chunk.x,chunk.z,ChunkStatus.FULL,true);return;
        }
        if(!future.isDone())return;
        boolean loaded;try{loaded=future.join().left().isPresent();}catch(RuntimeException ex){loaded=false;DuskRain.LOG.warn("Wilderness chunk search failed at {}",chunk,ex);}
        future=null;cursor++;
        if(loaded){BlockPos found=findInChunk(level,chunk);if(found!=null){Store data=Store.get(server);data.wildSpawn=found;data.setDirty();finish(server,found);}}
    }
    static BlockPos findInChunk(ServerLevel level,ChunkPos chunk){
        if(!level.hasChunk(chunk.x,chunk.z)||Store.get(level.getServer()).claims.containsKey(chunk.toLong()))return null;
        for(int dx:new int[]{8,4,12,2,14})for(int dz:new int[]{8,4,12,2,14}){
            BlockPos feet=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(chunk.getMinBlockX()+dx,0,chunk.getMinBlockZ()+dz));
            boolean safe=true;for(int x=-1;x<=1&&safe;x++)for(int z=-1;z<=1;z++)if(!SafeLanding.valid(level,feet.offset(x,0,z))){safe=false;break;}
            if(safe)return feet;
        }
        return null;
    }
    static void finish(MinecraftServer server,BlockPos feet){
        List<UUID> players=List.copyOf(WAITING.keySet());reset();
        for(UUID id:players){var p=server.getPlayerList().getPlayer(id);if(p==null)continue;if(feet==null)Gameplay.say(p,"暂未找到足够宽的安全地面，未扣除任何物品；请稍后重试或联系管理员检查世界边界。");else Gameplay.recall(p,net.minecraft.world.level.Level.OVERWORLD,Vec3.atBottomCenterOf(feet));}
    }
}
