package cn.duskrain;

import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

public final class Store extends SavedData {
    public final Map<UUID,Profile> players=new HashMap<>();
    public final Map<Long,Claim> claims=new HashMap<>();
    public final Map<Integer,PlayerMarket.Shop> market=new HashMap<>();
    public net.minecraft.core.BlockPos wildSpawn; public long transactionRevision;
    public static final class Claim {
        public final UUID owner; public final Set<UUID> members=new HashSet<>();
        public Claim(UUID o){owner=o;}
        public boolean allows(UUID u){return owner.equals(u)||members.contains(u);}
    }
    public static Store get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(Store::load,Store::new,"duskrain_state");}
    public static Profile of(ServerPlayer p){Store s=get(p.server);Profile r=s.players.computeIfAbsent(p.getUUID(),k->new Profile());r.knownName=p.getGameProfile().getName();r.refreshDay();s.setDirty();return r;}
    public static Store load(CompoundTag tag){
        Store s=new Store();s.transactionRevision=tag.getLong("transactionRevision");PlayerMarket.load(tag.getCompound("market"),s.market);if(tag.contains("wildSpawn"))s.wildSpawn=net.minecraft.core.BlockPos.of(tag.getLong("wildSpawn"));CompoundTag ps=tag.getCompound("players");for(String id:ps.getAllKeys())s.players.put(UUID.fromString(id),Profile.load(ps.getCompound(id)));
        ListTag cs=tag.getList("claims",Tag.TAG_COMPOUND);for(int i=0;i<cs.size();i++){CompoundTag c=cs.getCompound(i);Claim v=new Claim(c.getUUID("owner"));ListTag ms=c.getList("members",Tag.TAG_STRING);for(int j=0;j<ms.size();j++)v.members.add(UUID.fromString(ms.getString(j)));s.claims.put(c.getLong("pos"),v);}return s;
    }
    @Override public CompoundTag save(CompoundTag tag){
        tag.putLong("transactionRevision",transactionRevision);tag.put("market",PlayerMarket.save(market));
        if(wildSpawn!=null)tag.putLong("wildSpawn",wildSpawn.asLong());
        CompoundTag ps=new CompoundTag();players.forEach((id,p)->ps.put(id.toString(),p.save()));tag.put("players",ps);ListTag cs=new ListTag();
        claims.forEach((pos,c)->{CompoundTag n=new CompoundTag();n.putLong("pos",pos);n.putUUID("owner",c.owner);ListTag ms=new ListTag();c.members.forEach(u->ms.add(StringTag.valueOf(u.toString())));n.put("members",ms);cs.add(n);});tag.put("claims",cs);tag.putInt("schemaVersion",1);return tag;
    }
    public long claimCount(UUID owner){return claims.values().stream().filter(c->c.owner.equals(owner)).count();}
    public boolean adjacent(UUID owner,ChunkPos pos){if(claimCount(owner)==0)return true;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){Claim c=claims.get(ChunkPos.asLong(pos.x+d[0],pos.z+d[1]));if(c!=null&&c.owner.equals(owner))return true;}return false;}
}
