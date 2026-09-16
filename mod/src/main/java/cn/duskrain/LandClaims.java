package cn.duskrain;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import java.util.*;

public final class LandClaims {
    static Set<UUID> trusted(Store data,UUID owner){Set<UUID> result=new HashSet<>();data.claims.values().stream().filter(c->c.owner.equals(owner)).forEach(c->result.addAll(c.members));return result;}
    static String name(ServerPlayer p,UUID id){var online=p.server.getPlayerList().getPlayer(id);if(online!=null)return online.getGameProfile().getName();Profile saved=Store.get(p.server).players.get(id);if(saved!=null&&!saved.knownName.isBlank())return saved.knownName;var cached=p.server.getProfileCache();return cached==null?id.toString():cached.get(id).map(com.mojang.authlib.GameProfile::getName).orElse(id.toString());}
    public static void trust(ServerPlayer p,String target,boolean grant){
        Store data=Store.get(p.server);if(data.claimCount(p.getUUID())==0){Gameplay.say(p,"请先认领住宅区块。");return;}
        UUID id=null;try{id=UUID.fromString(target);}catch(IllegalArgumentException ignored){var online=p.server.getPlayerList().getPlayerByName(target);if(online!=null)id=online.getUUID();else for(var entry:data.players.entrySet())if(entry.getValue().knownName.equalsIgnoreCase(target)){id=entry.getKey();break;}}
        if(id==null||id.equals(p.getUUID())){Gameplay.say(p,"请输入已入服玩家名或 UUID；不能授权自己。");return;}
        if(grant&&!data.players.containsKey(id)&&p.server.getPlayerList().getPlayer(id)==null){Gameplay.say(p,"该玩家尚未入服。");return;}
        for(Store.Claim claim:data.claims.values())if(claim.owner.equals(p.getUUID())){if(grant)claim.members.add(id);else claim.members.remove(id);}
        data.setDirty();Gameplay.say(p,(grant?"已授权住宅成员：":"已撤销住宅授权：")+name(p,id));members(p);
    }
    public static void members(ServerPlayer p){
        Store data=Store.get(p.server);Set<UUID> trusted=trusted(data,p.getUUID());List<Network.Entry> entries=new ArrayList<>();
        for(UUID id:trusted)entries.add(DRCommands.card("撤销 · "+name(p,id),"claim untrust "+id,"对方离线时也可撤销；影响你的全部住宅区块","minecraft:iron_door",true));
        for(ServerPlayer other:p.server.getPlayerList().getPlayers())if(other!=p&&!trusted.contains(other.getUUID()))entries.add(DRCommands.card("授权 · "+other.getGameProfile().getName(),"claim trust "+other.getUUID(),"允许拆建和使用容器；仍受领地内 PVP 规则约束","minecraft:oak_door",data.claimCount(p.getUUID())>0));
        entries.add(DRCommands.entry("返回住宅管理","claim"));Network.menu(p,"住宅成员 · "+trusted.size(),"仅屋主可变更自己的成员。授权覆盖现有和以后相邻扩建的区块。指令：/dr claim trust 玩家名；撤销支持离线 UUID。",entries);
    }
    public static void list(ServerPlayer p){
        List<Network.Entry> entries=new ArrayList<>();Store data=Store.get(p.server);
        data.claims.entrySet().stream().filter(e->e.getValue().owner.equals(p.getUUID())).sorted(Map.Entry.comparingByKey()).forEach(e->{ChunkPos chunk=new ChunkPos(e.getKey());entries.add(DRCommands.card("区块 "+chunk.x+", "+chunk.z,"claim border","X "+chunk.getMinBlockX()+"～"+chunk.getMaxBlockX()+"；Z "+chunk.getMinBlockZ()+"～"+chunk.getMaxBlockZ()+"；成员 "+e.getValue().members.size(),"minecraft:grass_block",false));});
        entries.add(DRCommands.entry("返回住宅管理","claim"));Network.menu(p,"我的住宅 · "+data.claimCount(p.getUUID())+" / "+Rules.current.claimLimit,"每个区块为16×16。只能沿已有领地边缘扩建，放弃区块不能使剩余区域断开。",entries);
    }
    public static void border(ServerPlayer p){ClaimBorders.mode(p,"auto");}
}
