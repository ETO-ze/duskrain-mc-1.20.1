package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.nio.file.*;
import java.util.*;

/** Audits real chunk collision shapes at building entrances and stairs. No source-only pass. */
public final class WalkAudit {
    static boolean clear(ServerLevel l,double x,double y,double z){return l.noCollision(new AABB(x-.29,y+.01,z-.29,x+.29,y+1.81,z+.29));}
    static double surface(ServerLevel l,int x,int z,double hint){double best=Double.NaN;for(int y=(int)Math.floor(hint)-2;y<=(int)Math.ceil(hint)+1;y++){BlockPos p=new BlockPos(x,y,z);BlockState b=l.getBlockState(p);var shape=b.getCollisionShape(l,p,CollisionContext.empty());if(shape.isEmpty())continue;for(AABB box:shape.toAabbs()){double h=y+box.maxY;if(box.minX<=.5&&box.maxX>=.5&&box.minZ<=.5&&box.maxZ>=.5&&h>=hint-1.01&&h<=hint+1.01&&clear(l,x+.5,h,z+.5)&&(Double.isNaN(best)||Math.abs(h-hint)<Math.abs(best-hint)))best=h;}}return best;}
    public static boolean run(MinecraftServer server){ServerLevel l=server.getLevel(Gameplay.CITY);List<Map<String,Object>> buildings=new ArrayList<>();List<String> errors=new ArrayList<>();
        for(var b:CityPlan.BUILDINGS){List<String> issues=new ArrayList<>();
            // Follow the 3-wide entry axis from the public road into each plot.
            for(int z=b.z()+b.d()+3;z>=b.z();z--){double y=surface(l,b.x(),z,b.y()+1);if(Double.isNaN(y))issues.add("入口轴线阻挡 x="+b.x()+" z="+z+" y="+(b.y()+1));}
            if(!b.home()&&b.variant()!=10&&b.variant()!=11){var service=Services.ALL.stream().filter(s->s.building().equals(b.id())).findFirst();if(service.isPresent()){var p=service.get().position();if(!clear(l,p.x,p.y,p.z))issues.add("商人站位被方块占用");}}
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",b.id());row.put("name",b.name());row.put("floor",b.y());row.put("layout",b.variant());row.put("issues",issues);buildings.add(row);if(!issues.isEmpty())errors.add(b.id());
        }
        for(var b:Landscape.LANDMARKS){List<String> issues=new ArrayList<>();for(int z=b.z()+14;z>=b.z();z--)if(Double.isNaN(surface(l,b.x(),z,b.y()+1)))issues.add("塔楼入口阻挡 "+b.x()+","+z);
            buildings.add(Map.of("id",b.id(),"name",b.name(),"floor",b.y(),"issues",issues));if(!issues.isEmpty())errors.add(b.id());}
        Set<List<Integer>> stairs=new LinkedHashSet<>();CityPlan.Sink stairSink=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState state){}public void stairway(int x,int y,int z,int width,int steps){stairs.add(List.of(x,y,z,width,steps));}};
        for(var b:CityPlan.BUILDINGS)CityPlan.building(stairSink,b,0);for(var b:Landscape.LANDMARKS)Landscape.landmark(stairSink,b,0);
        for(var stair:stairs){int x=stair.get(0),y=stair.get(1),z=stair.get(2);for(int step=0;step<stair.get(4);step++)for(int lane=0;lane<stair.get(3);lane++)if(Double.isNaN(surface(l,x+lane,z+step,y+step+1.5)))errors.add("stair:"+(x+lane)+","+(y+step+1)+","+(z+step));}
        List<Map<String,Object>> routes=new ArrayList<>();for(var r:CityPlan.ROUTES){int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az())),bad=0;List<String> samples=new ArrayList<>();
            for(int i=0;i<=n;i++){int x=r.ax()+Integer.signum(r.bx()-r.ax())*i,z=r.az()+Integer.signum(r.bz()-r.az())*i;double hint=CityPlan.pathY(r,i)+1;if(Double.isNaN(surface(l,x,z,hint))){bad++;if(samples.size()<5)samples.add(x+","+z);}}
            if(bad>0)routes.add(Map.of("from",List.of(r.ax(),r.az()),"to",List.of(r.bx(),r.bz()),"blocked",bad,"samples",samples));}
        try{Path p=server.getServerDirectory().toPath().resolve("duskrain-walk-audit.json");Files.writeString(p,Rules.JSON.toJson(Map.of("buildings",buildings,"blockedBuildings",errors,"blockedRoutes",routes,"passed",errors.isEmpty()&&routes.isEmpty(),"method","real collision shapes; 30 building entrances, 3 landmark entrances, all registered stair flights and route centers; human walking and room inspection separate")));DuskRain.LOG.info("Walk audit: {} building failures, {} route failures -> {}",errors.size(),routes.size(),p);}catch(Exception e){DuskRain.LOG.error("Walk audit",e);return false;}
        return errors.isEmpty()&&routes.isEmpty();
    }
}
