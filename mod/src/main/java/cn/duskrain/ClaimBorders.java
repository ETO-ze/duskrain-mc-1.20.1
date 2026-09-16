package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

public final class ClaimBorders {
    public record Edge(int x,int z,boolean alongX,int relation,String owner,int[] heights) {}
    public record Snapshot(List<Edge> edges) {}
    static void write(Snapshot m,FriendlyByteBuf b){if(m.edges.size()>256)throw new IllegalArgumentException("Border size");b.writeVarInt(m.edges.size());for(Edge e:m.edges){b.writeInt(e.x);b.writeInt(e.z);b.writeBoolean(e.alongX);b.writeByte(e.relation);b.writeUtf(e.owner,64);if(e.heights.length!=17)throw new IllegalArgumentException("Border heights");for(int y:e.heights)b.writeShort(y);}}
    static Snapshot read(FriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>256)throw new IllegalArgumentException("Border size");List<Edge> es=new ArrayList<>();for(int i=0;i<n;i++){int x=b.readInt(),z=b.readInt();boolean axis=b.readBoolean();int relation=b.readUnsignedByte();String name=b.readUtf(64);int[] y=new int[17];for(int j=0;j<17;j++)y[j]=b.readShort();if(relation>2)throw new IllegalArgumentException("Border relation");es.add(new Edge(x,z,axis,relation,name,y));}return new Snapshot(es);}
    static Snapshot nearby(ServerPlayer p){List<Edge> result=new ArrayList<>();if(p.level().dimension()!=Level.OVERWORLD||Store.of(p).borderMode.equals("off"))return new Snapshot(result);Store data=Store.get(p.server);int cx=p.chunkPosition().x,cz=p.chunkPosition().z;
        for(int x=cx-3;x<=cx+3;x++)for(int z=cz-3;z<=cz+3;z++){if(!p.serverLevel().hasChunk(x,z))continue;var c=data.claims.get(ChunkPos.asLong(x,z));if(c==null)continue;int relation=c.owner.equals(p.getUUID())?0:c.members.contains(p.getUUID())?1:2;
            for(int side=0;side<4;side++){int dx=side==2?-1:side==3?1:0,dz=side==0?-1:side==1?1:0;var other=data.claims.get(ChunkPos.asLong(x+dx,z+dz));if(other!=null&&other.owner.equals(c.owner))continue;
                boolean alongX=side<2;int bx=x*16+(side==3?16:0),bz=z*16+(side==1?16:0);double nearX=Math.max(bx,Math.min(bx+(alongX?16:0),p.getX())),nearZ=Math.max(bz,Math.min(bz+(alongX?0:16),p.getZ()));if(Math.pow(nearX-p.getX(),2)+Math.pow(nearZ-p.getZ(),2)>48*48)continue;
                int[] heights=new int[17];for(int j=0;j<=16;j++){int hx=Math.min(x*16+15,bx+(alongX?j:0)),hz=Math.min(z*16+15,bz+(alongX?0:j));heights[j]=p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,hx,hz);}
                String name=LandClaims.name(p,c.owner);result.add(new Edge(bx,bz,alongX,relation,name.length()>64?name.substring(0,64):name,heights));
            }
        }return new Snapshot(result);
    }
    static void sync(ServerPlayer p){if(!(p instanceof net.minecraftforge.common.util.FakePlayer))Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),nearby(p));}
    public static void mode(ServerPlayer p,String mode){if(!Set.of("auto","on","off").contains(mode)){Gameplay.say(p,"用法：/dr claim border auto|on|off");return;}Store.of(p).borderMode=mode;Store.get(p.server).setDirty();sync(p);Gameplay.say(p,"领地边界："+switch(mode){case "off"->"隐藏";case "on"->"显示附近已加载区域";default->"接近48格时自动显示";}+"；自己的青色，成员金色，他人红色。");}
}
