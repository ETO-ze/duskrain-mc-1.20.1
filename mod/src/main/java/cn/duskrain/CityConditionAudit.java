package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
import java.nio.file.Files;

/** Reports saved blocks, not theoretical terrain. Never changes a world. */
public final class CityConditionAudit {
    static boolean wet(ServerLevel l,int x,int z,double foot){return !l.getFluidState(BlockPos.containing(x+.5,foot+.05,z+.5)).isEmpty()||!l.getFluidState(BlockPos.containing(x+.5,foot+1,z+.5)).isEmpty();}
    static Map<String,Object> sample(ServerLevel l,int x,int y,int z){var states=new ArrayList<String>();for(int dy=-2;dy<=3;dy++)states.add((y+dy)+":"+l.getBlockState(new BlockPos(x,y+dy,z)));return Map.of("x",x,"y",y,"z",z,"blocks",states);}
    public static void run(MinecraftServer server){
        ServerLevel l=server.getLevel(Gameplay.CITY);var rows=new ArrayList<Map<String,Object>>();var roads=new ArrayList<Map<String,Object>>();Set<BlockPos> visited=new HashSet<>();
        for(var b:CityPlan.BUILDINGS){var bad=new ArrayList<Map<String,Object>>();int checked=0,wet=0,unsupported=0;
            for(int z=b.z()+b.d()-1;z<=b.z()+b.d()+3;z++){
                int run=0,best=0;var rowIssues=new ArrayList<Map<String,Object>>();
                for(int x=b.x()-1;x<=b.x()+1;x++){checked++;double foot=WalkAudit.surface(l,x,z,b.y()+1);if(Double.isNaN(foot)||wet(l,x,z,foot)){if(!Double.isNaN(foot))wet++;run=0;rowIssues.add(sample(l,x,b.y(),z));}else best=Math.max(best,++run);}
                if(best<2)bad.addAll(rowIssues);
            }
            // Sample the outside edge of each actual hall floor, including annexes.
            var bases=new HashSet<BlockPos>();CityPlan.building(new CityPlan.Sink(){public void block(int x,int y,int z,net.minecraft.world.level.block.state.BlockState state){} public void room(String id,int x,int y,int z,int w,int d,int floors){for(int dx=-w;dx<=w;dx++)for(int dz=-d;dz<=d;dz++)if(Math.abs(dx)==w||Math.abs(dz)==d)bases.add(new BlockPos(x+dx,y-1,z+dz));}},b,0);
            for(var pos:bases){var state=l.getBlockState(pos);if(state.isAir()||!state.getFluidState().isEmpty()){unsupported++;bad.add(sample(l,pos.getX(),pos.getY()+1,pos.getZ()));}}
            rows.add(Map.of("id",b.id(),"name",b.name(),"entranceLaneSamples",checked,"wetEntrance",wet,"unsupportedPerimeter",unsupported,"issues",bad,"passed",bad.isEmpty()));
        }
        for(var r:CityPlan.ROUTES){int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az())),dx=Integer.signum(r.bx()-r.ax()),dz=Integer.signum(r.bz()-r.az());for(int i=0;i<=n;i++)for(int lane=-1;lane<=1;lane++){
            int x=r.ax()+dx*i+dz*lane,z=r.az()+dz*i+dx*lane,y=CityPlan.pathY(r,i);if(!visited.add(new BlockPos(x,y,z)))continue;var grade=CourtGarden.walk.get(CourtGarden.key(x,z));double foot=WalkAudit.surface(l,x,z,grade==null?y+1:grade.top());
            if(Double.isNaN(foot)||wet(l,x,z,foot))roads.add(sample(l,x,y,z));
        }}
        try{Files.writeString(server.getServerDirectory().toPath().resolve("duskrain-city-condition.json"),Rules.JSON.toJson(Map.of("buildings",rows,"roads",roads,"roadSamples",visited.size(),"passed",roads.isEmpty()&&rows.stream().allMatch(r->(boolean)r.get("passed")),"method","At least two adjacent clear entrance lanes and hall perimeter support; every dry road lane, actual saved blocks. Does not replace visual inspection.")));}catch(Exception e){throw new RuntimeException(e);}
        DuskRain.LOG.info("CITY_CONDITION buildings={} failing={} badRoads={}",rows.size(),rows.stream().filter(r->!(boolean)r.get("passed")).count(),roads.size());
    }
}
