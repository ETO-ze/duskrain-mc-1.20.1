package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import java.util.*;

/** Replays both block state and sign NBT after the client's predicted break is rejected. */
public final class SignRepair {
    record Key(UUID player,BlockPos pos){}
    record Pending(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,CompoundTag text,int start){}
    static final Map<Key,Pending> PENDING=new HashMap<>();
    public static boolean hasText(CompoundTag tag){for(String side:List.of("front_text","back_text")){ListTag lines=tag.getCompound(side).getList("messages",Tag.TAG_STRING);for(int i=0;i<lines.size();i++){try{Component c=Component.Serializer.fromJson(lines.getString(i));if(c!=null&&!c.getString().isBlank())return true;}catch(Exception ignored){}}}return false;}
    public static CompoundTag blueprint(BlockPos pos){CompoundTag[] found={null};CityPlan.Sink sink=new CityPlan.Sink(){public void block(int x,int y,int z,net.minecraft.world.level.block.state.BlockState s){}public void entity(CompoundTag n){if(n.getInt("x")==pos.getX()&&n.getInt("y")==pos.getY()&&n.getInt("z")==pos.getZ())found[0]=n.copy();}};for(int phase:new int[]{3,4})CityPlan.paint(sink,pos.getX()>>4,pos.getZ()>>4,phase);return found[0];}
    public static boolean restore(SignBlockEntity sign,CompoundTag saved){if(hasText(sign.saveWithFullMetadata())||saved==null||!hasText(saved))return false;sign.load(saved);sign.setChanged();return true;}
    public static void loaded(net.minecraft.world.level.chunk.LevelChunk chunk){
        if(!(chunk.getLevel() instanceof ServerLevel level)||!Protection.city(level))return;
        level.getServer().execute(()->{if(!level.hasChunk(chunk.getPos().x,chunk.getPos().z))return;for(var entity:chunk.getBlockEntities().values())if(entity instanceof SignBlockEntity sign&&!hasText(sign.saveWithFullMetadata())&&restore(sign,blueprint(sign.getBlockPos())))level.sendBlockUpdated(sign.getBlockPos(),sign.getBlockState(),sign.getBlockState(),2);});
    }
    public static void rejected(ServerPlayer p,BlockPos pos){if(!(p.serverLevel().getBlockEntity(pos) instanceof SignBlockEntity sign))return;CompoundTag data=sign.saveWithFullMetadata();if(!hasText(data)&&Protection.city(p.level())){CompoundTag expected=blueprint(pos);if(restore(sign,expected))data=sign.saveWithFullMetadata();}
        PENDING.put(new Key(p.getUUID(),pos.immutable()),new Pending(p.level().dimension(),data,p.server.getTickCount()));sync(p,pos,data);
    }
    static void sync(ServerPlayer p,BlockPos pos,CompoundTag saved){if(!p.serverLevel().hasChunkAt(pos)||!(p.serverLevel().getBlockEntity(pos) instanceof SignBlockEntity sign))return;restore(sign,saved);p.connection.send(new ClientboundBlockUpdatePacket(p.serverLevel(),pos));var packet=sign.getUpdatePacket();if(packet!=null)p.connection.send(packet);}
    public static void tick(MinecraftServer server){var it=PENDING.entrySet().iterator();while(it.hasNext()){var e=it.next();ServerPlayer p=server.getPlayerList().getPlayer(e.getKey().player);int age=server.getTickCount()-e.getValue().start;if(p==null||!p.level().dimension().equals(e.getValue().dimension)||age>8){it.remove();continue;}if(age==2||age==8)sync(p,e.getKey().pos,e.getValue().text);}}
}
