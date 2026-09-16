package cn.duskrain;

import java.util.*;

/** Shared, level junctions replace independent overlapping ramps. */
public final class RoadGrades {
    private static class Cache {static final Map<CityPlan.Route,int[]> GRADES=build();}
    static int y(CityPlan.Route r,int index){int[] a=Cache.GRADES.get(r);return a[Math.max(0,Math.min(a.length-1,index))];}
    static Map<CityPlan.Route,int[]> build(){
        Map<CityPlan.Route,int[]> out=new LinkedHashMap<>();Map<CityPlan.Route,Map<Integer,Integer>> joints=new LinkedHashMap<>();
        for(var r:CityPlan.ROUTES){int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az()));int[] h=new int[n+1];for(int i=0;i<=n;i++)h[i]=CityPlan.designPathY(r,i);out.put(r,h);joints.put(r,new TreeMap<>());}
        var routes=CityPlan.ROUTES;
        for(int a=0;a<routes.size();a++)for(int b=a+1;b<routes.size();b++){
            var r=routes.get(a);var t=routes.get(b);if(r.bridge()||t.bridge())continue;
            boolean rv=r.ax()==r.bx(),tv=t.ax()==t.bx();if(rv==tv)continue;
            var v=rv?r:t;var h=rv?t:r;int x=v.ax(),z=h.az();
            if(x<Math.min(h.ax(),h.bx())||x>Math.max(h.ax(),h.bx())||z<Math.min(v.az(),v.bz())||z>Math.max(v.az(),v.bz()))continue;
            int vi=Math.abs(z-v.az()),hi=Math.abs(x-h.ax()),vy=CityPlan.designPathY(v,vi),hy=CityPlan.designPathY(h,hi);if(Math.abs(vy-hy)>6)continue;
            // Main vertical streets own the junction datum; both ramps meet the same landing.
            joints.get(v).put(vi,vy);joints.get(h).put(hi,vy);
        }
        for(var r:routes){if(r.bridge())continue;var h=out.get(r);int[] original=h.clone();
            for(int i=0;i<h.length;i++){int nearest=1000,y=original[i];for(var j:joints.get(r).entrySet()){int d=Math.abs(i-j.getKey());if(d<nearest){nearest=d;y=j.getValue();}}
                if(nearest<=3)h[i]=y;else if(nearest<12)h[i]=(int)Math.round(y+(original[i]-y)*(nearest-3)/9.0);
            }
            // One block per horizontal block maximum, composed of native half-height stair steps.
            for(int i=1;i<h.length;i++)h[i]=Math.max(h[i-1]-1,Math.min(h[i-1]+1,h[i]));
            for(int i=h.length-2;i>=0;i--)h[i]=Math.max(h[i+1]-1,Math.min(h[i+1]+1,h[i]));
        }
        return out;
    }
}
