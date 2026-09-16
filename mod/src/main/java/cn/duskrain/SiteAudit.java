package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import java.nio.file.*;
import java.util.*;

/** Samples actual saved terrain, collisions and block light, independent of sunshine/shaders. */
public final class SiteAudit {
    public static boolean run(MinecraftServer server){
        var l=server.getLevel(Gameplay.CITY);List<Map<String,Object>> rooms=new ArrayList<>();List<String> issues=new ArrayList<>();
        CityPlan.Sink recorder=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState state){}
            public void room(String id,int x,int y,int z,int w,int d,int floors){
                for(int f=0;f<floors;f++){int samples=0,min=15,dark=0,fy=y+8*f;List<List<Integer>> bad=new ArrayList<>();
                    for(int dx=-w+2+f;dx<=w-2-f;dx+=2)for(int dz=-d+2+f;dz<=d-2-f;dz+=2){
                        double foot=WalkAudit.surface(l,x+dx,z+dz,fy+1);if(Double.isNaN(foot))continue;
                        int light=l.getBrightness(LightLayer.BLOCK,new BlockPos(x+dx,(int)Math.floor(foot+.2),z+dz));samples++;min=Math.min(min,light);if(light<10){dark++;if(bad.size()<12)bad.add(List.of(x+dx,(int)foot,z+dz,light));}
                    }
                    rooms.add(Map.of("building",id,"floor",f+1,"samples",samples,"minBlockLight",min,"belowTen",dark,"darkExamples",bad));if(dark>0)issues.add(id+" floor "+(f+1)+" dim samples="+dark);
                }
            }
        };
        for(var b:CityPlan.BUILDINGS)CityPlan.building(recorder,b,0);for(var b:Landscape.LANDMARKS)Landscape.landmark(recorder,b,0);
        for(var shop:PlayerMarket.SITES)recorder.room("player_shop_"+shop.id(),shop.x(),shop.y(),shop.z(),5,5,1);
        int supportSamples=0,unsupported=0,edgeSamples=0,highEdges=0,crossingEdges=0;List<List<Integer>> gaps=new ArrayList<>(),edges=new ArrayList<>();
        for(var r:CityPlan.ROUTES){int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az())),dx=Integer.signum(r.bx()-r.ax()),dz=Integer.signum(r.bz()-r.az());double last=Double.NaN;
            for(int i=0;i<=n;i++){
                int x=r.ax()+dx*i,z=r.az()+dz*i,y=CityPlan.pathY(r,i);double foot=WalkAudit.surface(l,x,z,y+1);
                if(!Double.isNaN(last)&&!Double.isNaN(foot)&&Math.abs(foot-last)>1.01)issues.add("route step "+x+","+z+" delta="+Math.abs(foot-last));last=foot;
                if(r.bridge())continue;
                for(int lane=-2;lane<=2;lane++){int px=x+dz*lane,pz=z+dx*lane;supportSamples++;if(l.getBlockState(new BlockPos(px,y-2,pz)).isAir()){unsupported++;if(gaps.size()<24)gaps.add(List.of(px,y-2,pz));}}
                if(i%4!=0||Terrain.skyTop(x,z)>y-10)continue;
                for(int side:new int[]{-1,1}){int px=x+dz*side*4,pz=z+dx*side*4;if(CityPlan.nearPlot(px,pz,1))continue;if(CityPlan.road(px,pz)){crossingEdges++;continue;}double edge=WalkAudit.surface(l,px,pz,SiteTerrain.surface(px,pz)+1);if(Double.isNaN(edge)||!l.getFluidState(BlockPos.containing(px,edge+.1,pz)).isEmpty())continue;edgeSamples++;if(!Double.isNaN(foot)&&Math.abs(foot-edge)>1.01){highEdges++;if(edges.size()<40)edges.add(List.of(x,z,(int)foot,px,pz,(int)edge));}}
            }
        }
        if(unsupported>0)issues.add("unsupported road samples="+unsupported);if(highEdges>0)issues.add("road edges above one block="+highEdges);
        var report=new LinkedHashMap<String,Object>();report.put("rooms",rooms);report.put("supportSamples",supportSamples);report.put("unsupported",unsupported);report.put("gapExamples",gaps);report.put("edgeSamples",edgeSamples);report.put("highEdges",highEdges);report.put("edgeExamples",edges);report.put("issues",issues);report.put("passed",issues.isEmpty());
        report.put("method","Actual block light at passable room samples (>=10), continuous road subgrade, transverse road/terrain joins and longitudinal step heights. Shader appearance and player walking need separate inspection.");
        report.put("crossingEdgesCheckedAsRoutes",crossingEdges);
        try{Path file=server.getServerDirectory().toPath().resolve("duskrain-site-audit.json");Files.writeString(file,Rules.JSON.toJson(report));DuskRain.LOG.info("SITE_AUDIT issues={} rooms={} -> {}",issues.size(),rooms.size(),file);}catch(Exception e){DuskRain.LOG.error("Site audit",e);return false;}
        return issues.isEmpty();
    }
}
