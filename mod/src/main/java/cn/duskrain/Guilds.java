package cn.duskrain;

import java.util.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import static net.minecraft.commands.Commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class Guilds {
    public static final net.minecraft.resources.ResourceKey<Level> DIM=Gameplay.key("duskrain:guild");
    record Invite(int guild,UUID from,long until){}
    static final Map<UUID,Invite> INVITES=new HashMap<>();
    static final Map<UUID,Long> CONFIRM=new HashMap<>();
    static final Set<UUID> ENTER_WHEN_READY=new HashSet<>();
    static final Set<Integer> VERIFIED=new HashSet<>();
    static final Map<UUID,Integer> BORDERS=new HashMap<>();
    static final int MAX_MEMBERS=16;
    public static LiteralArgumentBuilder<CommandSourceStack> commands(){var root=literal("guild").executes(c->DRCommands.doPlayer(c,Guilds::menu));
        for(String verb:List.of("accept","enter","return","leave","members","disband","confirm")){String action=verb;root.then(literal(verb).executes(c->DRCommands.doPlayer(c,p->act(p,action,""))));}
        for(String verb:List.of("invite","kick","officer","transfer")){String action=verb;root.then(literal(verb).then(argument("target",StringArgumentType.word()).executes(c->DRCommands.doPlayer(c,p->act(p,action,StringArgumentType.getString(c,"target"))))));}
        for(String verb:List.of("create","rename")){String action=verb;root.then(literal(verb).then(argument("name",StringArgumentType.greedyString()).executes(c->DRCommands.doPlayer(c,p->act(p,action,StringArgumentType.getString(c,"name"))))));}
        return root;
    }
    static boolean validName(String name){return name.matches("[\\p{L}\\p{N}_]{2,16}")&&name.codePointCount(0,name.length())<=16;}
    static boolean receive(ServerPlayer p,Network.Action a){
        if(a.target().length()>80)return true;
        String[] args=a.target().split(" ",2);if(!Set.of("create","rename","invite","kick","officer","transfer","accept","enter","return","leave","members","disband","confirm").contains(args[0]))return true;
        if(a.sequence()<=Actions.SEEN.getOrDefault(p.getUUID(),-1L))return true;
        Actions.SEEN.put(p.getUUID(),a.sequence());long now=System.nanoTime();if(now-Network.LAST_ACTION.getOrDefault(p.getUUID(),0L)<80_000_000L)return true;Network.LAST_ACTION.put(p.getUUID(),now);
        if(!Store.of(p).onboardingComplete&&Store.of(p).initialized){Onboarding.menu(p);return true;}
        act(p,args[0],args.length>1?args[1]:"");return true;
    }
    public static boolean allowed(ServerPlayer p,Vec3 to){var g=GuildData.get(p.server).of(p.getUUID());return g!=null&&g.ready&&g.palace!=1&&g.contains(to.x,to.z)&&to.y>=1&&to.y<255;}
    public static boolean edit(net.minecraft.world.entity.player.Player p,LevelAccessor level,BlockPos pos){if(!(level instanceof ServerLevel s))return false;var g=GuildData.get(s.getServer()).of(p.getUUID());return g!=null&&g.ready&&g.palace!=1&&g.contains(pos.getX(),pos.getZ())&&pos.getY()>61&&pos.getY()<254&&!portal(g,pos);}
    static boolean portal(GuildData.Guild g,BlockPos p){return Math.abs(p.getX()-g.x())<=3&&Math.abs(p.getZ()-(g.z()+112))<=3&&p.getY()<=69;}
    static Vec3 entrance(GuildData.Guild g){return new Vec3(g.x()+.5,65,g.z()+108.5);}
    static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
    static UUID resolve(ServerPlayer p,String name){try{return UUID.fromString(name);}catch(IllegalArgumentException ignored){}var online=p.server.getPlayerList().getPlayerByName(name);if(online!=null)return online.getUUID();for(var e:Store.get(p.server).players.entrySet())if(e.getValue().knownName.equalsIgnoreCase(name))return e.getKey();throw new IllegalArgumentException("请输入已入服玩家名或 UUID。");}
    static void eject(MinecraftServer s,UUID id){var p=s.getPlayerList().getPlayer(id);if(p!=null){Gameplay.RECALL.remove(id);if(p.level().dimension().equals(DIM))Gameplay.spawn(p);if(p.getRespawnDimension().equals(DIM))p.setRespawnPosition(Gameplay.CITY,new BlockPos(0,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z),180,true,false);}ENTER_WHEN_READY.remove(id);}
    public static void act(ServerPlayer p,String verb,String target){try{
        GuildData data=GuildData.get(p.server);var g=data.of(p.getUUID());
        target=target.strip();
        final String requestedName=target;
        switch(verb){
            case "rename"->{require(g!=null&&g.leader.equals(p.getUUID()),"只有宗主可以改名。");require(validName(target),"宗门名须为2至16个汉字、字母、数字或下划线。");final int currentId=g.id;require(data.guilds.values().stream().noneMatch(q->!q.archived&&q.id!=currentId&&q.name.equalsIgnoreCase(requestedName)),"已有同名宗门。");g.name=target;data.setDirty();Gameplay.say(p,"宗门现名："+target);}

            case "create"->{require(g==null,"每人只能加入一个帮派。");require(Store.of(p).stage>=17,"创建帮派需要渡劫后期。");require(Services.require(p,"guild"),"请到宗务堂办理。");require(target.matches("[\\p{L}\\p{N}_]{2,16}"),"帮派名须为2至16个汉字、字母或数字。");require(data.guilds.values().stream().noneMatch(q->!q.archived&&q.name.equalsIgnoreCase(requestedName)),"已有同名帮派。");require(data.nextId<10000,"洞天名额已满，请联系管理员扩容。");g=new GuildData.Guild(data.nextId++,target,p.getUUID());data.guilds.put(g.id,g);ENTER_WHEN_READY.add(p.getUUID());data.setDirty();Gameplay.say(p,"宗门已立，洞天正在分批建设；准备好后开始传送引导。");}
            case "invite"->{require(g!=null&&(g.leader.equals(p.getUUID())||g.officers.contains(p.getUUID())),"只有帮主或副帮主可以邀请。");UUID id=resolve(p,target);require(data.of(id)==null,"该玩家已有帮派。");require(g.members.size()<MAX_MEMBERS,"成员已满16人。");INVITES.put(id,new Invite(g.id,p.getUUID(),System.currentTimeMillis()+120000));var other=p.server.getPlayerList().getPlayer(id);if(other!=null)Gameplay.say(other,"收到「"+g.name+"」邀请；两分钟内 /dr guild accept 接受。");Gameplay.say(p,"邀请已发出。");}
            case "accept"->{Invite invite=INVITES.remove(p.getUUID());require(invite!=null&&invite.until>System.currentTimeMillis(),"邀请不存在或已过期。");require(g==null,"你已经加入帮派。");g=data.guilds.get(invite.guild);require(g!=null&&!g.archived&&g.member(invite.from)&&(g.leader.equals(invite.from)||g.officers.contains(invite.from)),"邀请方已无管理权限。");require(g.members.size()<MAX_MEMBERS,"成员已满16人。");g.members.add(p.getUUID());data.setDirty();Gameplay.say(p,"已加入「"+g.name+"」。");}
            case "enter"->{require(g!=null,"请先加入帮派。");require(g.ready&&g.palace!=1,"洞天仍在施工；请稍后进入。");Gameplay.recall(p,DIM,entrance(g));}
            case "return"->Gameplay.recall(p,Gameplay.CITY,new Vec3(.5,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z+.5));
            case "leave"->{require(g!=null,"你尚未加入帮派。");require(!g.leader.equals(p.getUUID()),"帮主请先转让帮主或确认解散。");g.members.remove(p.getUUID());g.officers.remove(p.getUUID());data.setDirty();eject(p.server,p.getUUID());Gameplay.say(p,"已退出帮派。");}
            case "kick","officer","transfer"->{require(g!=null&&g.leader.equals(p.getUUID()),"只有帮主可以执行此项管理。");UUID id=resolve(p,target);require(g.member(id)&&!id.equals(p.getUUID()),"请选择其他帮派成员。");if(verb.equals("kick")){g.members.remove(id);g.officers.remove(id);eject(p.server,id);}else if(verb.equals("officer")){if(!g.officers.remove(id))g.officers.add(id);}else {Profile r=Store.get(p.server).players.get(id);require(r!=null&&r.stage>=17,"接任帮主同样需要渡劫后期。");g.leader=id;g.officers.remove(id);}data.setDirty();Gameplay.say(p,"成员管理已更新。");}
            case "disband"->{require(g!=null&&g.leader.equals(p.getUUID()),"只有帮主可以解散。");CONFIRM.put(p.getUUID(),System.currentTimeMillis()+30000);Network.menu(p,"确认解散 · "+g.name,"解散会移除全体成员的访问权限。洞天存档归档保留，区域编号不复用。30秒内确认。",List.of(DRCommands.entry("确认解散并归档","guild confirm"),DRCommands.entry("返回帮派","guild")));return;}
            case "confirm"->{require(g!=null&&g.leader.equals(p.getUUID())&&CONFIRM.getOrDefault(p.getUUID(),0L)>System.currentTimeMillis(),"请先发起解散并在30秒内二次确认。");CONFIRM.remove(p.getUUID());g.archived=true;data.setDirty();for(UUID id:g.members)eject(p.server,id);Gameplay.say(p,"帮派已解散，洞天存档已归档保留。");}
            case "members"->{members(p);return;}
            default->throw new IllegalArgumentException("未知帮派操作。");
        }menu(p);
    }catch(IllegalArgumentException e){Gameplay.say(p,e.getMessage());}}
    public static void menu(ServerPlayer p){var g=GuildData.get(p.server).of(p.getUUID());List<Network.Entry> es=new ArrayList<>();if(g==null){if(Services.nearby(p,"guild"))es.add(new Network.Entry("创立宗门","local guild-create","渡劫后期 · 免费 · 256×256洞天","minecraft:bell",Store.of(p).stage>=17));es.add(DRCommands.card("接受帮派邀请","guild accept","加入不限制境界；每人一个帮派","minecraft:paper",INVITES.containsKey(p.getUUID())));es.add(DRCommands.card("前往宗务堂","warp guildhall","渡劫后期可创建；/dr guild create 帮派名","minecraft:bell",true));}
        else{if(g.leader.equals(p.getUUID()))es.add(new Network.Entry("更改宗门名","local guild-rename"));if(g.leader.equals(p.getUUID())||g.officers.contains(p.getUUID()))es.add(new Network.Entry("邀请道友","local guild-invite"));es.add(DRCommands.card("进入专属洞天","guild enter",g.ready?"256×256可建设空地；5秒引导":"正在分批生成 "+g.cursor+"/256","minecraft:ender_pearl",g.ready));es.add(DRCommands.entry("帮派成员","guild members"));es.add(DRCommands.entry("返回主城","guild return"));es.add(DRCommands.entry(g.leader.equals(p.getUUID())?"解散与归档":"退出帮派",g.leader.equals(p.getUUID())?"guild disband":"guild leave"));}
        Network.menu(p,g==null?"帮派洞天":"帮派洞天 · "+g.name,g==null?"渡劫后期在宗务堂免费创建；帮派最多16人。洞天关闭PVP，仅成员可建造和开箱。":"成员 "+g.members.size()+"/16 · 帮主 "+LandClaims.name(p,g.leader)+"；邀请 /dr guild invite 玩家名。转让要求渡劫后期。",es);
    }
    static void members(ServerPlayer p){var g=GuildData.get(p.server).of(p.getUUID());if(g==null){menu(p);return;}List<Network.Entry> es=new ArrayList<>();for(UUID id:g.members){String name=LandClaims.name(p,id);if(g.leader.equals(p.getUUID())&&!id.equals(p.getUUID())){es.add(DRCommands.entry("移除 · "+name,"guild kick "+id));es.add(DRCommands.entry((g.officers.contains(id)?"撤销副帮主 · ":"设置副帮主 · ")+name,"guild officer "+id));es.add(DRCommands.entry("转让帮主 · "+name,"guild transfer "+id));}else es.add(DRCommands.card(name,"guild",g.leader.equals(id)?"帮主":g.officers.contains(id)?"副帮主":"成员","minecraft:player_head",false));}es.add(DRCommands.entry("返回帮派","guild"));Network.menu(p,"帮派成员 · "+g.name,"邀请：/dr guild invite 玩家名。退出或被移除后即失去该洞天权限。",es);}
    public static void recover(MinecraftServer server){VERIFIED.clear();BORDERS.clear();ENTER_WHEN_READY.clear();INVITES.clear();CONFIRM.clear();var data=GuildData.get(server);for(var g:data.guilds.values())if(!g.archived){g.cursor=0;g.ready=false;}data.setDirty();}
    public static void tick(MinecraftServer server){ServerLevel level=server.getLevel(DIM);if(level==null)return;var data=GuildData.get(server);
        var g=data.guilds.values().stream().filter(q->!q.archived&&!q.ready).findFirst().orElse(null);
        if(g!=null){try{generateChunk(level,g,g.cursor);g.cursor++;data.setDirty();if(g.cursor==256){g.ready=true;for(UUID id:g.members){var member=server.getPlayerList().getPlayer(id);if(member!=null)Gameplay.say(member,"帮派洞天已准备就绪。");}}}catch(Exception e){DuskRain.LOG.error("Guild {} generation paused at chunk {}",g.id,g.cursor,e);}}
        for(var p:server.getPlayerList().getPlayers()){
            var mine=data.of(p.getUUID());if(p.getRespawnDimension().equals(DIM)&&(mine==null||p.getRespawnPosition()!=null&&!mine.contains(p.getRespawnPosition().getX(),p.getRespawnPosition().getZ())))p.setRespawnPosition(Gameplay.CITY,new BlockPos(0,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z),180,true,false);if(ENTER_WHEN_READY.contains(p.getUUID())&&mine!=null&&mine.ready){ENTER_WHEN_READY.remove(p.getUUID());if(Store.of(p).combatUntil<=System.currentTimeMillis())Gameplay.recall(p,DIM,entrance(mine));else Gameplay.say(p,"洞天已准备好；战斗结束后 /dr guild enter 进入。");}
            if(p.level().dimension().equals(DIM)){if(mine==null||mine.palace==1){eject(server,p.getUUID());continue;}if(!Objects.equals(BORDERS.get(p.getUUID()),mine.id)){var border=new net.minecraft.world.level.border.WorldBorder();border.setCenter(mine.x(),mine.z());border.setSize(256);border.setWarningBlocks(3);p.connection.send(new net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket(border));BORDERS.put(p.getUUID(),mine.id);}if((!mine.contains(p.getX(),p.getZ())||p.getY()<61||p.getY()>=255)){TeleportEffects.travel(p,level,entrance(mine),180);Gameplay.say(p,"已返回本帮派洞天边界内。");}}else BORDERS.remove(p.getUUID());
        }
    }
    static void generateChunk(ServerLevel l,GuildData.Guild g,int cursor){int x0=g.x()-128+(cursor%16)*16,z0=g.z()-128+(cursor/16)*16;var chunk=l.getChunk(x0>>4,z0>>4);BlockPos marker=new BlockPos(x0+8,60,z0+8);var saved=chunk.getBlockEntity(marker);if(saved!=null&&saved.getPersistentData().getInt("DuskRainGuildFloor")==g.id)return;
        for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){l.setBlock(new BlockPos(x,61,z),Blocks.BEDROCK.defaultBlockState(),2);l.setBlock(new BlockPos(x,62,z),Blocks.STONE.defaultBlockState(),2);l.setBlock(new BlockPos(x,63,z),Blocks.DIRT.defaultBlockState(),2);boolean edge=x==g.x()-128||x==g.x()+127||z==g.z()-128||z==g.z()+127;boolean light=Math.floorMod(x,16)==8&&Math.floorMod(z,16)==8;l.setBlock(new BlockPos(x,64,z),(light?Blocks.SEA_LANTERN:edge?Blocks.SMOOTH_QUARTZ:Blocks.GRASS_BLOCK).defaultBlockState(),2);if(edge){l.setBlock(new BlockPos(x,65,z),Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(),2);}}
        BlockPos portal=new BlockPos(g.x(),64,g.z()+112);if(portal.getX()>>4==x0>>4&&portal.getZ()>>4==z0>>4){for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)l.setBlock(portal.offset(x,0,z),(Math.max(Math.abs(x),Math.abs(z))==3?Blocks.SEA_LANTERN:Blocks.QUARTZ_BLOCK).defaultBlockState(),2);l.setBlock(portal.above(),Blocks.LODESTONE.defaultBlockState(),2);}
        l.setBlock(marker,Blocks.BARREL.defaultBlockState(),2);var stamp=l.getBlockEntity(marker);if(stamp==null)throw new IllegalStateException("Generation marker missing");stamp.getPersistentData().putInt("DuskRainGuildFloor",g.id);stamp.setChanged();chunk.setUnsaved(true);
    }
    public static boolean returnPortal(ServerPlayer p,BlockPos pos){var g=GuildData.get(p.server).of(p.getUUID());if(g!=null&&pos.equals(new BlockPos(g.x(),65,g.z()+112))){act(p,"return","");return true;}return false;}
    @SubscribeEvent public void teleport(EntityTeleportEvent e){if(e.getEntity() instanceof ServerPlayer p&&p.level().dimension().equals(DIM)&&!allowed(p,new Vec3(e.getTargetX(),e.getTargetY(),e.getTargetZ())))e.setCanceled(true);}
    @SubscribeEvent public void dimension(EntityTravelToDimensionEvent e){if(e.getDimension().equals(DIM)){if(!(e.getEntity() instanceof ServerPlayer p)||GuildData.get(p.server).of(p.getUUID())==null)e.setCanceled(true);}}
}
