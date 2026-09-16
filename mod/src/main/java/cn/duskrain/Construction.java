package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Live, replayable construction changes only duskrain:construction. */
public final class Construction {
    public static boolean paused;
    static boolean pauseAfterReset;
    public static int budget=600;
    static Job job;
    static int pregenCursor=-1;
    static final Map<UUID,String> CAMERAS=new HashMap<>();
    static final String[] PHASES={"基础与台阶","梁柱与墙体","青瓦飞檐","家具与陈设","灯光与道路","山水与植被"};
    static final ServerBossEvent BAR=new ServerBossEvent(Component.literal("DuskRain 自动建造"),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.PROGRESS);
    static class Job {
        final List<ChunkPos> chunks=new ArrayList<>();final String scope;int phase=-1,index;long placed;ArrayDeque<Placement> pending=new ArrayDeque<>();List<CompoundTag> tags=new ArrayList<>();
        Job(String scope){this.scope=scope;int minX=-20,maxX=19,minZ=-20,maxZ=19;if(!scope.equals("city")){var b=CityPlan.find("palace");minX=(b.x()-b.w()-7)>>4;maxX=(b.x()+b.w()+7)>>4;minZ=(b.z()-b.d()-7)>>4;maxZ=(b.z()+b.d()+7)>>4;}for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)chunks.add(new ChunkPos(x,z));chunks.sort(Comparator.comparingDouble(c->Math.hypot(c.x,c.z-(scope.equals("city")?0:-4))));}
    }
    record Placement(BlockPos pos,BlockState state){}
    public static void menu(ServerPlayer p){List<Network.Entry> e=List.of(DRCommands.entry("演示仙宫自动建造","build start palace"),DRCommands.entry("演示整座主城建造","build start city"),DRCommands.entry("暂停施工","build pause"),DRCommands.entry("继续施工","build resume"),DRCommands.entry("慢速 · 100方块/tick","build speed 100"),DRCommands.entry("标准 · 600方块/tick","build speed 600"),DRCommands.entry("快速 · 4000方块/tick","build speed 4000"),DRCommands.entry("仙宫固定机位","build camera palace"),DRCommands.entry("全景机位","build camera aerial"),DRCommands.entry("环绕摄像机","build camera orbit"),DRCommands.entry("停止自动镜头","build camera off"),DRCommands.entry("开始录制帧序列","build record true"),DRCommands.entry("停止录制帧序列","build record false"));Network.menu(p,"DuskRain · 施工摄影台","仅操作独立演示维度。录像为真实游戏帧，可通过附带脚本合成MP4。",e);}
    public static void start(ServerPlayer p,String scope){if(!scope.equals("city")&&!scope.equals("palace")){Gameplay.say(p,"范围只能选择 city 或 palace。");return;}if(job!=null){Gameplay.say(p,"已有施工正在进行，可暂停或调速。");return;}if(p.server.getLevel(Gameplay.DEMO)==null){Gameplay.say(p,"演示维度尚未加载。");return;}job=new Job(scope);paused=false;BAR.removeAllPlayers();BAR.addPlayer(p);camera(p,scope.equals("city")?"aerial":"palace");Gameplay.say(p,"正在整理演示场地，随后自动开始施工。正式主城不受影响。");}
    public static void camera(ServerPlayer p,String mode){if(mode.equals("off")){CAMERAS.remove(p.getUUID());Gameplay.say(p,"自动镜头已停止；当前为观察模式。");return;}if(!Set.of("palace","aerial","orbit","street").contains(mode))return;ServerLevel l=p.server.getLevel(Gameplay.DEMO);if(l==null)return;p.setGameMode(GameType.SPECTATOR);CAMERAS.put(p.getUUID(),mode);Vec3 target=mode.equals("palace")?new Vec3(0,172,-192):new Vec3(0,112,0);Vec3 from=mode.equals("palace")?new Vec3(95,215,-85):mode.equals("street")?new Vec3(0,108,18):new Vec3(238,250,253);look(p,l,from,target);}
    static void look(ServerPlayer p,ServerLevel l,Vec3 from,Vec3 target){Vec3 d=target.subtract(from);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));p.teleportTo(l,from.x,from.y,from.z,yaw,pitch);}
    public static void pregen(MinecraftServer s){if(pregenCursor>=0)return;pregenCursor=0;DuskRain.LOG.info("DuskRain pre-generation started: 4096 city chunks");}
    public static void tick(MinecraftServer server){
        if(pregenCursor>=0){ServerLevel l=server.getLevel(Gameplay.CITY);if(l!=null){int x=pregenCursor%64-32,z=pregenCursor/64-32;l.getChunk(x,z);pregenCursor++;if(pregenCursor%256==0)DuskRain.LOG.info("City generation {}/4096",pregenCursor);if(pregenCursor>=4096){pregenCursor=-1;l.save(null,true,false);DuskRain.LOG.info("DUSKRAIN_CITY_PREGEN_COMPLETE");}}}
        if(server.getTickCount()%2==0){for(var e:new ArrayList<>(CAMERAS.entrySet())){ServerPlayer p=server.getPlayerList().getPlayer(e.getKey());if(p==null){CAMERAS.remove(e.getKey());continue;}if(e.getValue().equals("orbit")){double a=server.getTickCount()*.0025;boolean palace=job!=null&&job.scope.equals("palace");Vec3 t=palace?new Vec3(0,172,-192):new Vec3(0,110,0);double r=palace?105:300;look(p,server.getLevel(Gameplay.DEMO),new Vec3(t.x+Math.cos(a)*r,t.y+(palace?75:220),t.z+Math.sin(a)*r),t);}}}
        if(job==null||paused)return;ServerLevel level=server.getLevel(Gameplay.DEMO);if(level==null){job=null;BAR.removeAllPlayers();return;}
        int spent=0,loaded=0;
        while(spent<budget&&job!=null){
            if(job.pending.isEmpty()){
                for(CompoundTag n:job.tags){BlockPos p=new BlockPos(n.getInt("x"),n.getInt("y"),n.getInt("z"));var be=level.getBlockEntity(p);if(be!=null){be.load(n);be.setChanged();level.sendBlockUpdated(p,level.getBlockState(p),level.getBlockState(p),2);}}job.tags.clear();
                if(job.index>=job.chunks.size()){job.phase++;job.index=0;if(job.phase==0&&pauseAfterReset){pauseAfterReset=false;paused=true;DuskRain.LOG.info("DUSKRAIN_BUILD_FOUNDATION_READY");return;}if(job.phase>=6){DuskRain.LOG.info("DUSKRAIN_BUILD_COMPLETE blocks={}",job.placed);BAR.setName(Component.literal("DuskRain · 建造完成"));BAR.setProgress(1);for(ServerPlayer p:BAR.getPlayers())Gameplay.say(p,"自动建造完成！可停止录像，或重新开始演示。");job=null;return;}}
                if(loaded++>=1)break;
                ChunkPos cp=job.chunks.get(job.index++);level.getChunk(cp.x,cp.z);
                if(job.phase==-1){
                    // Restore only authored block positions, including air cutouts, to the
                    // exact original terrain. Never erase the floating island underneath.
                    Map<BlockPos,BlockState> original=new LinkedHashMap<>();
                    CityPlan.Sink reset=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState state){if((x>>4)==cp.x&&(z>>4)==cp.z&&y>=0&&y<256)original.put(new BlockPos(x,y,z),Terrain.block(x,y,z));}};
                    for(int phase=0;phase<6;phase++)CityPlan.paint(reset,cp.x,cp.z,phase);
                    for(var entry:original.entrySet())if(!level.getBlockState(entry.getKey()).equals(entry.getValue()))job.pending.add(new Placement(entry.getKey(),entry.getValue()));
                }
                else{
                    LinkedHashMap<BlockPos,BlockState> map=new LinkedHashMap<>();Job current=job;
                    CityPlan.paint(new CityPlan.Sink(){public void block(int x,int y,int z,BlockState b){if((x>>4)==cp.x&&(z>>4)==cp.z&&y>=0&&y<256)map.put(new BlockPos(x,y,z),b);}public void entity(CompoundTag n){if((n.getInt("x")>>4)==cp.x&&(n.getInt("z")>>4)==cp.z)current.tags.add(n);}},cp.x,cp.z,job.phase);
                    map.entrySet().stream().sorted(Comparator.<Map.Entry<BlockPos,BlockState>>comparingInt(e->e.getKey().getY()).thenComparingLong(e->e.getKey().asLong())).forEach(e->current.pending.add(new Placement(e.getKey(),e.getValue())));
                }
                BAR.setName(Component.literal("DuskRain · "+(job.phase<0?"准备演示场地":PHASES[job.phase])+" "+job.index+"/"+job.chunks.size()));BAR.setProgress(Math.max(0,Math.min(1,(job.phase+job.index/(float)job.chunks.size())/6f)));
            }
            while(spent<budget&&!job.pending.isEmpty()){Placement p=job.pending.removeFirst();level.setBlock(p.pos,p.state,2|16);spent++;job.placed++;}
        }
    }
}
