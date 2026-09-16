package cn.duskrain;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** One bounded event describes the shape; particles are generated locally. */
public final class SpellFx {
    public static final int SWORD=0,BODY=1,FIRE=2,FROST=3,LIGHTNING=4,GUARD=5,BELL=6,ERROR=7;
    public record Event(int style,Vec3 from,Vec3 to,float radius,int ticks,int number) {}
    static void write(Event e,FriendlyByteBuf b){b.writeByte(e.style);b.writeDouble(e.from.x);b.writeDouble(e.from.y);b.writeDouble(e.from.z);b.writeDouble(e.to.x);b.writeDouble(e.to.y);b.writeDouble(e.to.z);b.writeFloat(e.radius);b.writeVarInt(e.ticks);b.writeVarInt(e.number);}
    static Event read(FriendlyByteBuf b){var e=new Event(b.readUnsignedByte(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readVarInt(),b.readVarInt());if(e.style>10||e.radius<0||e.radius>32||e.ticks<1||e.ticks>240||!Double.isFinite(e.from.lengthSqr())||!Double.isFinite(e.to.lengthSqr())||e.from.distanceToSqr(e.to)>4096)throw new IllegalArgumentException("FX bounds");return e;}
    static void send(ServerLevel l,int style,Vec3 a,Vec3 b,double radius,int ticks){send(l,new Event(style,a,b,(float)radius,ticks,0));}
    static void send(ServerLevel l,Event e){for(var p:l.players())if(!(p instanceof net.minecraftforge.common.util.FakePlayer)&&p.distanceToSqr(e.from)<96*96)Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),e);}
}
