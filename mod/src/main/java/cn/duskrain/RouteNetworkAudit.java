package cn.duskrain;

import net.minecraft.server.MinecraftServer;
import java.util.*;
import java.nio.file.*;

/** Real collision graph: connectivity between routes, their lanes, and bridge turning squares. */
public final class RouteNetworkAudit {
    record Node(int x,int z,int y2){}
    public static boolean run(MinecraftServer server){
        var l=server.getLevel(Gameplay.CITY);Set<Node> nodes=new HashSet<>(),centers=new HashSet<>();List<String> blocked=new ArrayList<>();
        for(var r:CityPlan.ROUTES){int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az())),dx=Integer.signum(r.bx()-r.ax()),dz=Integer.signum(r.bz()-r.az());
            for(int i=0;i<=n;i++){int x=r.ax()+i*dx,z=r.az()+i*dz;int radius=r.bridge()?2:1;
                for(int lane=-radius;lane<=radius;lane++){int xx=x+lane*dz,zz=z+lane*dx;var grade=CourtGarden.walk.get(CourtGarden.key(xx,zz));double y=WalkAudit.surface(l,xx,zz,grade==null?CityPlan.pathY(r,i)+1:grade.top());
                    if(Double.isNaN(y)){blocked.add(xx+","+zz+": lane "+lane);continue;}
                    Node node=new Node(xx,zz,(int)Math.round(y*2));nodes.add(node);if(lane==0)centers.add(node);
                }
            }
        }
        int turnSamples=0;
        for(var j:SkyBridges.joints())for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            turnSamples++;double y=WalkAudit.surface(l,j.x()+dx,j.z()+dz,j.y()+1);
            if(Double.isNaN(y)||Math.abs(y-j.y()-1)>.01)blocked.add("turn "+(j.x()+dx)+","+(j.z()+dz));else nodes.add(new Node(j.x()+dx,j.z()+dz,(int)Math.round(y*2)));
        }
        Node spawn=nodes.stream().filter(n->n.x==0&&n.z==28).findFirst().orElse(null);Set<Node> reached=new HashSet<>();Deque<Node> queue=new ArrayDeque<>();if(spawn!=null){queue.add(spawn);reached.add(spawn);}
        while(!queue.isEmpty()){Node n=queue.removeFirst();for(int[] dir:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy=-2;dy<=2;dy++){
            Node next=new Node(n.x+dir[0],n.z+dir[1],n.y2+dy);if(nodes.contains(next)&&reached.add(next))queue.addLast(next);
        }}
        var disconnected=centers.stream().filter(n->!reached.contains(n)).sorted(Comparator.comparingInt(Node::x).thenComparingInt(Node::z)).toList();
        var report=new LinkedHashMap<String,Object>();report.put("nodes",nodes.size());report.put("reached",reached.size());report.put("routeCenters",centers.size());report.put("disconnectedCenters",disconnected.size());report.put("disconnectedExamples",disconnected.stream().limit(45).map(n->List.of(n.x,n.z,n.y2/2.0)).toList());report.put("blockedLaneCount",blocked.size());report.put("blockedExamples",blocked.stream().limit(45).toList());report.put("turnSamples",turnSamples);report.put("passed",spawn!=null&&blocked.isEmpty()&&disconnected.isEmpty());
        report.put("method","Adjacent real collision graph from spawn across every route center, 3 ground lanes, 5 bridge lanes and all 7x7 turning landings; max step 1 block.");
        report.put("routes",CityPlan.ROUTES);
        try{Files.writeString(server.getServerDirectory().toPath().resolve("duskrain-route-network.json"),Rules.JSON.toJson(report));}catch(Exception e){throw new RuntimeException(e);}
        DuskRain.LOG.info("ROUTE_NETWORK disconnected={} blocked={} turnSamples={}",disconnected.size(),blocked.size(),turnSamples);return (boolean)report.get("passed");
    }
}
