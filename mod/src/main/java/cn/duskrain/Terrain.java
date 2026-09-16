package cn.duskrain;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Original fixed landscape. No building footprint is consulted by this class. */
public final class Terrain {
    public static final int SEA=62;
    private static double smooth(double t){return t*t*(3-2*t);}
    private static double hash(int x,int z){long h=x*341873128712L+z*132897987541L+205255670L;h=(h^(h>>>13))*1274126177L;return ((h^(h>>>16))&65535)/32767.5-1;}
    static double noise(double x,double z,int step){int ix=(int)Math.floor(x/step),iz=(int)Math.floor(z/step);double a=smooth(x/step-ix),b=smooth(z/step-iz);return (hash(ix,iz)*(1-a)+hash(ix+1,iz)*a)*(1-b)+(hash(ix,iz+1)*(1-a)+hash(ix+1,iz+1)*a)*b;}
    static double peak(int x,int z,int px,int pz,int radius,int height){double dx=x-px,dz=z-pz;return height*Math.exp(-(dx*dx+dz*dz)/(radius*(double)radius));}
    public static int ground(int x,int z){
        double coast=Math.hypot(x/345.0,(z+45)/345.0);
        double h=54+25*Math.exp(-coast*coast*1.9)+noise(x,z,64)*3.5+noise(x,z,19)*1.2;
        h+=peak(x,z,-210,-220,100,94)+peak(x,z,175,-260,110,136)+peak(x,z,-5,-330,78,106);
        h+=peak(x,z,292,30,68,83)+peak(x,z,-304,48,83,75);
        // Eroded western stream, draining into the southern bay.
        double creek=-104+13*Math.sin(z*.018),dist=Math.abs(x-creek);
        if(z>-125&&z<190&&dist<10){double bed=61+Math.max(0,120-z)*.032;double t=smooth(Math.min(1,dist/10));h=h*t+(bed-2)*(1-t);}
        return Math.max(28,Math.min(235,(int)Math.floor(h)));
    }
    public static int water(int x,int z){double creek=-104+13*Math.sin(z*.018);return z>-125&&z<190&&Math.abs(x-creek)<6?Math.max(SEA,61+(int)(Math.max(0,120-z)*.032)):SEA;}
    public static final int[][] ISLANDS={{0,-192,56,160},{168,-118,35,137},{-177,-133,31,127}};
    public static int skyTop(int x,int z){for(int[] a:ISLANDS){double r=Math.hypot((x-a[0])/(double)a[2],(z-a[1])/(a[2]*.79));if(r<1)return a[3]-(int)(3*r*r);}return -1;}
    public static int skyBottom(int x,int z){for(int[] a:ISLANDS){double r=Math.hypot((x-a[0])/(double)a[2],(z-a[1])/(a[2]*.79));if(r<1){double angle=Math.atan2(z-a[1],x-a[0]),lobes=Math.max(0,Math.sin(angle*5+.7))*10*(1-r),weather=noise(x+173,z-91,11)*5;return a[3]-9-2*(int)((30*Math.pow(1-r,.72)+lobes+weather)/2);}}return -1;}
    public static BlockState block(int x,int y,int z){
        int h=ground(x,z),top=skyTop(x,z),bottom=skyBottom(x,z);
        if(y==0)return Blocks.BEDROCK.defaultBlockState();
        if(y>h&&bottom>=0&&y>=bottom&&y<=top){
            double radius=0;for(int[] island:ISLANDS){double r=Math.hypot((x-island[0])/(double)island[2],(z-island[1])/(island[2]*.79));if(r<1){radius=r;break;}}
            if(radius>.88&&(y==top||y==top-1))return (y==top?Blocks.SEA_LANTERN:Blocks.DARK_PRISMARINE).defaultBlockState();
            if(y==top)return (Math.floorMod(x,12)==0||Math.floorMod(z,12)==0?Blocks.CHISELED_STONE_BRICKS:Blocks.SMOOTH_QUARTZ).defaultBlockState();
            if(y>=top-3)return (y==top-2?Blocks.DARK_PRISMARINE:Blocks.QUARTZ_BRICKS).defaultBlockState();
            if(Math.floorMod(y+2*(int)noise(x,z,18),11)==0)return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            return (Math.floorMod(x+z,9)==0?Blocks.SMOOTH_BASALT:Blocks.CALCITE).defaultBlockState();
        }
        if(y<=h||bottom>=0&&y>=bottom&&y<=top){int surface=y<=h?h:top;
            if(y==surface)return surface<=water(x,z)+1&&top<0?Blocks.GRAVEL.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState();
            if(y>=surface-3)return Blocks.DIRT.defaultBlockState();
            return Math.floorMod(x+y+z,19)==0?Blocks.TUFF.defaultBlockState():Blocks.STONE.defaultBlockState();}
        if(y<=water(x,z))return Blocks.WATER.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
}
