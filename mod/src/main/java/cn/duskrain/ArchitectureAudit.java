package cn.duskrain;

import java.util.*;
import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;

/** Reads one registered chunk each tick. Never rewrites a discrepancy to make an audit pass. */
public final class ArchitectureAudit {
    record Zone(String name,boolean guild,int x,int z,int w,int d,int y,int top) {}
    static final List<Zone> zones=new ArrayList<>();
    static final Deque<String> chunks=new ArrayDeque<>();
    static final Map<String,Map<String,Object>> results=new LinkedHashMap<>();
    static MinecraftServer active;
    public static void stop(MinecraftServer s){if(active==s){active=null;chunks.clear();}}
    public static void start(MinecraftServer s){
        if(active!=null)throw new IllegalStateException("逐栋检查仍在进行");
        zones.clear();chunks.clear();results.clear();
        for(var b:CityPlan.BUILDINGS)zones.add(new Zone(b.name(),false,b.x(),b.z(),b.w()+7,b.d()+9,b.y()-8,b.y()+b.floors()*8+24));
        for(var b:PlayerMarket.SITES)zones.add(new Zone(b.name(),false,b.x(),b.z(),10,12,b.y()-8,b.y()+18));
        for(var b:Landscape.LANDMARKS)zones.add(new Zone(b.name(),false,b.x(),b.z(),16,20,b.y()-8,b.y()+b.floors()*8+20));
        var g=GuildData.get(s).guilds.get(1);
        if(g!=null&&g.palace==2){int[][] halls={{0,87,14,6,1},{0,-2,25,15,2},{0,-61,18,10,1},{-29,64,11,8,1},{-31,-50,10,11,2},{32,-47,10,9,1},{66,-51,11,10,1},{-28,34,10,10,1},{29,34,10,10,1}};
            String[] names={"合欢山门","合欢正殿","凝月后寝","议事厅","藏经阁","丹房","器房","听雨弟子院","照月弟子院"};
            for(int i=0;i<halls.length;i++){var h=halls[i];zones.add(new Zone(names[i],true,g.x()+h[0],g.z()+h[1],h[2]+6,h[3]+8,62,64+h[4]*9+22));}}
        var unique=new LinkedHashSet<String>();
        for(var z:zones){results.put(z.name(),new LinkedHashMap<>(Map.of("checked",0,"differences",0,"missing",0,"samples",new ArrayList<Map<String,Object>>())));
            for(int x=(z.x()-z.w())>>4;x<=(z.x()+z.w())>>4;x++)for(int v=(z.z()-z.d())>>4;v<=(z.z()+z.d())>>4;v++)unique.add((z.guild()?"guild":"city")+"_"+x+"_"+v);}
        chunks.addAll(unique);active=s;
    }
    @SuppressWarnings("unchecked") public static void tick(MinecraftServer s){if(active!=s)return;
        try{if(!chunks.isEmpty()){var key=chunks.removeFirst().split("_");boolean guild=key[0].equals("guild");int cx=Integer.parseInt(key[1]),cz=Integer.parseInt(key[2]);var level=s.getLevel(guild?Guilds.DIM:Gameplay.CITY);
            Map<BlockPos,BlockState> plan=guild?ArchitectureRepair.guild(s,cx,cz,false):ArchitectureRepair.blueprint(cx,cz,false);
            for(var e:plan.entrySet()){var expected=e.getValue();if(!BuiltInRegistries.BLOCK.getKey(expected.getBlock()).getNamespace().equals("duskrain"))continue;
                var p=e.getKey();var actual=level.getBlockState(p);
                for(var z:zones)if(z.guild()==guild&&Math.abs(p.getX()-z.x())<=z.w()&&Math.abs(p.getZ()-z.z())<=z.d()&&p.getY()>=z.y()&&p.getY()<=z.top()){
                    var r=results.get(z.name());r.put("checked",(int)r.get("checked")+1);
                    boolean brokenConnection=false;
                    if(actual.getBlock() instanceof net.minecraft.world.level.block.FenceBlock){var connected=actual;for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)connected=connected.updateShape(direction,level.getBlockState(p.relative(direction)),level,p,p.relative(direction));brokenConnection=!actual.equals(connected);}
                    if(!ArchitectureRepair.matches(actual,expected)||brokenConnection){r.put("differences",(int)r.get("differences")+1);if(actual.isAir())r.put("missing",(int)r.get("missing")+1);
                        var samples=(List<Map<String,Object>>)r.get("samples");if(samples.size()<80)samples.add(Map.of("pos",p.toShortString(),"expected",expected.toString(),"actual",actual.toString()));}
                }
            }
        }
        if(chunks.isEmpty()){int differences=results.values().stream().mapToInt(r->(int)r.get("differences")).sum();Files.writeString(s.getServerDirectory().toPath().resolve("duskrain-building-audit.json"),Rules.JSON.toJson(Map.of("buildings",results,"passed",differences==0,"differences",differences,"method","Actual saved registered architecture including surrounding supports, lamps and roof closure. Player differences are reported, never overwritten by this audit. Visual inspection is separate.")));DuskRain.LOG.info("ARCHITECTURE_AUDIT buildings={} differences={}",results.size(),differences);active=null;}
        }catch(Exception e){active=null;DuskRain.LOG.error("Architecture audit stopped",e);}
    }
}
