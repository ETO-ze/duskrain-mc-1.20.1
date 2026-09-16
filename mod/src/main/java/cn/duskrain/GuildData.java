package cn.duskrain;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Plot IDs are never reused, including archived or incompletely generated plots. */
public final class GuildData extends SavedData {
    public int nextId=1;
    public final Map<Integer,Guild> guilds=new LinkedHashMap<>();
    public static final class Guild {
        public int id,cursor,palace,palaceCursor;public String name;public UUID leader;public boolean ready,archived;
        public final Set<UUID> members=new LinkedHashSet<>(),officers=new HashSet<>();
        Guild(int id,String name,UUID leader){this.id=id;this.name=name;this.leader=leader;members.add(leader);}
        public int x(){return (id%100)*1024;}public int z(){return (id/100)*1024;}
        public boolean contains(double x,double z){return x>=x()-128&&x<x()+128&&z>=z()-128&&z<z()+128;}
        public boolean member(UUID u){return !archived&&members.contains(u);}
    }
    public static GuildData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(GuildData::load,GuildData::new,"duskrain_guilds");}
    public Guild of(UUID u){return guilds.values().stream().filter(g->g.member(u)).findFirst().orElse(null);}
    public Guild at(double x,double z){return guilds.values().stream().filter(g->!g.archived&&g.contains(x,z)).findFirst().orElse(null);}
    public static GuildData load(CompoundTag tag){GuildData d=new GuildData();d.nextId=Math.max(1,tag.getInt("nextId"));ListTag list=tag.getList("guilds",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){CompoundTag n=list.getCompound(i);Guild g=new Guild(n.getInt("id"),n.getString("name"),n.getUUID("leader"));g.cursor=Math.max(0,Math.min(256,n.getInt("cursor")));g.ready=n.getBoolean("ready");g.palace=n.getInt("palace");g.palaceCursor=0;g.archived=n.getBoolean("archived");readIds(n,"members",g.members);readIds(n,"officers",g.officers);d.guilds.put(g.id,g);d.nextId=Math.max(d.nextId,g.id+1);}return d;}
    static void readIds(CompoundTag n,String key,Set<UUID> set){for(Tag t:n.getList(key,Tag.TAG_STRING))try{set.add(UUID.fromString(t.getAsString()));}catch(IllegalArgumentException ignored){}}
    static ListTag ids(Set<UUID> ids){ListTag l=new ListTag();for(UUID u:ids)l.add(StringTag.valueOf(u.toString()));return l;}
    @Override public CompoundTag save(CompoundTag tag){tag.putInt("nextId",nextId);ListTag l=new ListTag();for(Guild g:guilds.values()){CompoundTag n=new CompoundTag();n.putInt("id",g.id);n.putString("name",g.name);n.putUUID("leader",g.leader);n.putInt("cursor",g.cursor);n.putBoolean("ready",g.ready);n.putInt("palace",g.palace);n.putBoolean("archived",g.archived);n.put("members",ids(g.members));n.put("officers",ids(g.officers));l.add(n);}tag.put("guilds",l);return tag;}
}
