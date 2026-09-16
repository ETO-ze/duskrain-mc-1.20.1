package cn.duskrain;

import net.minecraft.world.level.block.Blocks;

/** Local height-field earthworks: graded verges, cut slopes and compacted foundations.
 * The untouched landscape remains the reference; only masks beside roads and buildings change. */
public final class SiteTerrain {
    static final int R=352,N=R*2+1;
    private static class Heights {static final int[] HEIGHT=build();}
    public static int surface(int x,int z){return Math.abs(x)<=R&&Math.abs(z)<=R?Heights.HEIGHT[(x+R)*N+z+R]:Terrain.ground(x,z);}
    static int[] build(){
        int[] heights=new int[N*N];float[] strength=new float[N*N];
        for(int x=-R;x<=R;x++)for(int z=-R;z<=R;z++)heights[(x+R)*N+z+R]=Terrain.ground(x,z);
        for(var b:CityPlan.BUILDINGS)if(b.variant()!=0&&b.variant()!=8)plot(heights,strength,b.x(),b.z(),b.w()+1,b.d()+4,b.y()-1,9);
        for(var b:Landscape.LANDMARKS)if(Terrain.skyTop(b.x(),b.z())<0)plot(heights,strength,b.x(),b.z(),10,15,b.y()-1,8);
        for(var s:PlayerMarket.SITES)plot(heights,strength,s.x(),s.z(),6,7,s.y()-1,5);
        plot(heights,strength,0,24,11,10,CityPlan.SPAWN_Y-1,9);
        for(var r:CityPlan.ROUTES)if(!r.bridge()){
            int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az()));
            for(int i=0;i<=n;i++){int x=r.ax()+Integer.signum(r.bx()-r.ax())*i,z=r.az()+Integer.signum(r.bz()-r.az())*i,y=CityPlan.pathY(r,i);
                if(Terrain.skyTop(x,z)>y-10)continue;
                for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++){double d=Math.hypot(dx,dz);if(d>10)continue;float w=(float)(d<=4?2-d*.01:1-Math.pow((d-4)/6,1.1));stamp(heights,strength,x+dx,z+dz,y,w);}
            }
        }
        return heights;
    }
    static void plot(int[] h,float[] weights,int x,int z,int w,int d,int y,int feather){
        for(int dx=-w-feather;dx<=w+feather;dx++)for(int dz=-d-feather;dz<=d+feather;dz++){
            double distance=Math.hypot(Math.max(0,Math.abs(dx)-w),Math.max(0,Math.abs(dz)-d));if(distance>feather)continue;
            if(Terrain.ground(x+dx,z+dz)<Terrain.water(x+dx,z+dz))continue;
            float f=(float)(1-distance/feather);stamp(h,weights,x+dx,z+dz,y,f);
        }
    }
    static void stamp(int[] h,float[] weights,int x,int z,int target,float weight){
        if(Math.abs(x)>R||Math.abs(z)>R)return;int index=(x+R)*N+z+R;if(weight<weights[index])return;
        int natural=Terrain.ground(x,z);h[index]=(int)Math.round(natural+(target-natural)*Math.min(1,weight));weights[index]=weight;
    }
    public static void paint(CityPlan.Sink sink,int cx,int cz){
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            int raw=Terrain.ground(x,z),y=surface(x,z);if(y==raw)continue;
            for(int yy=Math.min(raw,y)-3;yy<=Math.max(raw,y);yy++)sink.block(x,yy,z,yy>y?Blocks.AIR.defaultBlockState():yy==y?Blocks.GRASS_BLOCK.defaultBlockState():yy>=y-3?Blocks.DIRT.defaultBlockState():Blocks.STONE.defaultBlockState());
        }
    }
    static void foundation(CityPlan.Sink sink,int x,int y,int z){
        int sky=Terrain.skyTop(x,z),natural=sky<y?Math.max(surface(x,z),sky):surface(x,z);
        for(int yy=Math.max(1,natural+1);yy<y;yy++)sink.block(x,yy,z,yy>=y-2?Blocks.STONE_BRICKS.defaultBlockState():Blocks.STONE.defaultBlockState());
    }
}
