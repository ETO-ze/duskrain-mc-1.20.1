package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.nio.file.Files;

/** Explicit administrator repair. Never runs on login or resets player/guild/market data. */
public final class CityRepair {
    public static void run(MinecraftServer server){
        var level=server.getLevel(Gameplay.CITY);Map<BlockPos,BlockState> plan=new LinkedHashMap<>();Map<BlockPos,CompoundTag> tags=new LinkedHashMap<>();
        CityPlan.Sink sink=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState s){plan.put(new BlockPos(x,y,z),s);}public void entity(CompoundTag n){tags.put(new BlockPos(n.getInt("x"),n.getInt("y"),n.getInt("z")),n);}};
        // Rebuild only public guild buildings lifted above the bay and the cut-through home.
        for(var b:CityPlan.BUILDINGS)if(b.id().startsWith("guild")||b.id().equals("home_14")){
            int old=Terrain.ground(b.x(),b.z())+2;
            if(old<b.y())for(int x=b.x()-b.w()-7;x<=b.x()+b.w()+7;x++)for(int z=b.z()-b.d()-7;z<=b.z()+b.d()+7;z++)for(int y=old;y<=b.y()+b.floors()*8+16;y++)sink.block(x,y,z,Blocks.AIR.defaultBlockState());
            for(int phase=0;phase<6;phase++)CityPlan.building(sink,b,phase);
        }
        for(var shop:PlayerMarket.SITES){int old=Terrain.ground(0,shop.z()+7)+1;if(old<shop.y())for(int x=shop.x()-7;x<=shop.x()+7;x++)for(int z=shop.z()-6;z<=shop.z()+6;z++)for(int y=old;y<=shop.y()+17;y++)sink.block(x,y,z,Blocks.AIR.defaultBlockState());}
        Set<net.minecraft.world.level.ChunkPos> chunks=new HashSet<>();for(var shop:PlayerMarket.SITES)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)chunks.add(new net.minecraft.world.level.ChunkPos((shop.x()>>4)+dx,(shop.z()>>4)+dz));
        for(int phase=0;phase<6;phase++)for(var c:chunks)PlayerMarket.paint(sink,c.x,c.z,phase);
        for(var r:CityPlan.ROUTES){int x0=Math.min(r.ax(),r.bx())-4,x1=Math.max(r.ax(),r.bx())+4,z0=Math.min(r.az(),r.bz())-4,z1=Math.max(r.az(),r.bz())+4;for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++)CityPlan.path(sink,cx,cz,r);}
        for(int cx=-22;cx<=22;cx++)for(int cz=-22;cz<=22;cz++)SkyBridges.junctions(sink,cx,cz);
        for(var b:CityPlan.BUILDINGS)CityPlan.building(sink,b,4);
        for(var b:Landscape.LANDMARKS)Landscape.landmark(sink,b,4);
        for(var lamp:Landscape.lights()){
            Landscape.lamp(sink,lamp);
            for(int y=lamp.y()-1;y>=Math.max(1,lamp.y()-32);y--){var pos=new BlockPos(lamp.x(),y,lamp.z());var existing=plan.getOrDefault(pos,level.getBlockState(pos));if(existing.isCollisionShapeFullBlock(level,pos)&&!existing.is(net.minecraft.tags.BlockTags.LEAVES)&&!existing.is(net.minecraft.tags.BlockTags.LOGS))break;sink.block(lamp.x(),y,lamp.z(),Blocks.STONE_BRICKS.defaultBlockState());}
        }
        for(var r:CityPlan.ROUTES)if(!r.bridge()){
            int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az())),dx=Integer.signum(r.bx()-r.ax()),dz=Integer.signum(r.bz()-r.az());
            for(int i=0;i<=n;i++)for(int lane=-2;lane<=2;lane++){
                int x=r.ax()+dx*i+dz*lane,z=r.az()+dz*i+dx*lane;
                for(int y=CityPlan.pathY(r,i)-2;y>=Math.max(1,CityPlan.pathY(r,i)-32);y--){var pos=new BlockPos(x,y,z);var state=plan.getOrDefault(pos,level.getBlockState(pos));if(state.isCollisionShapeFullBlock(level,pos)&&!state.is(net.minecraft.tags.BlockTags.LEAVES)&&!state.is(net.minecraft.tags.BlockTags.LOGS)&&!state.is(Blocks.STONE_BRICKS)&&!state.is(Blocks.CHISELED_STONE_BRICKS))break;if(state.isAir()||!state.getFluidState().isEmpty()||state.canBeReplaced())plan.put(pos,Blocks.STONE_BRICKS.defaultBlockState());}
            }
        }
        // Fill existing exposed perimeter gaps using saved terrain, not a guessed ground height.
        CityPlan.Sink supports=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState state){}public void room(String id,int x,int y,int z,int w,int d,int floors){for(int dx=-w;dx<=w;dx++)for(int dz=-d;dz<=d;dz++)if(Math.abs(dx)==w||Math.abs(dz)==d){for(int yy=y-1;yy>=Math.max(1,y-24);yy--){BlockPos p=new BlockPos(x+dx,yy,z+dz);BlockState existing=plan.getOrDefault(p,level.getBlockState(p));if(existing.isCollisionShapeFullBlock(level,p)&&existing.getFluidState().isEmpty())break;sink.block(p.getX(),yy,p.getZ(),Blocks.STONE_BRICKS.defaultBlockState());}}}};
        for(var b:CityPlan.BUILDINGS)CityPlan.building(supports,b,0);
        for(var shop:PlayerMarket.SITES)supports.room("shop",shop.x(),shop.y(),shop.z()+1,5,7,1);
        // Containers with actual items are an explicit conflict; do not discard or move them blindly.
        var conflicts=new ArrayList<String>();for(var e:plan.entrySet()){var be=level.getBlockEntity(e.getKey());if(be instanceof net.minecraft.world.Container c&&!c.isEmpty()&&!level.getBlockState(e.getKey()).equals(e.getValue()))conflicts.add(e.getKey().toShortString());}
        if(!conflicts.isEmpty())throw new IllegalStateException("修补中止：实体容器有物品，需要先迁移："+conflicts);
        var changes=new ArrayList<Map<String,Object>>();for(var e:plan.entrySet())if(!level.getBlockState(e.getKey()).equals(e.getValue()))changes.add(Map.of("pos",List.of(e.getKey().getX(),e.getKey().getY(),e.getKey().getZ()),"before",net.minecraft.nbt.NbtUtils.writeBlockState(level.getBlockState(e.getKey())).toString(),"after",net.minecraft.nbt.NbtUtils.writeBlockState(e.getValue()).toString()));
        try{Files.writeString(server.getServerDirectory().toPath().resolve("duskrain-city-repair-"+System.currentTimeMillis()+".json"),Rules.JSON.toJson(changes));}catch(Exception ex){throw new RuntimeException("Cannot write repair journal; no blocks changed",ex);}
        for(var e:plan.entrySet())if(!level.getBlockState(e.getKey()).equals(e.getValue()))level.setBlock(e.getKey(),e.getValue(),2);
        for(var e:tags.entrySet()){var be=level.getBlockEntity(e.getKey());if(be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign)SignRepair.restore(sign,e.getValue());}
        level.getChunkSource().getLightEngine().tryScheduleUpdate();server.saveEverything(false,true,true);
        DuskRain.LOG.info("CITY_REPAIR changed={} (UUID, claims, guilds and escrow untouched)",changes.size());
    }
}
