package cn.duskrain;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerTitles {
    public record Title(int entity,UUID uuid,String text,int stage,int school){}
    public record View(List<Title> titles){}
    public static String text(ServerPlayer p){Profile r=Store.of(p);var guild=GuildData.get(p.server).of(p.getUUID());return Rules.realm(r.stage)+" · Lv."+(r.stage+1)+(guild==null?"":" · "+guild.name+(guild.leader.equals(p.getUUID())?"宗主":guild.officers.contains(p.getUUID())?"副宗主":"成员"));}
    public static void sync(ServerPlayer viewer){List<Title> titles=new ArrayList<>();if(Store.of(viewer).titles)for(var p:viewer.serverLevel().players())if(p!=viewer&&p.distanceToSqr(viewer)<=40*40)titles.add(new Title(p.getId(),p.getUUID(),text(p),Store.of(p).stage,Store.of(p).school));V14Network.to(viewer,new View(titles));}
    public static void toggle(ServerPlayer p){Profile r=Store.of(p);r.titles=!r.titles;sync(p);Gameplay.say(p,"附近头衔显示："+(r.titles?"开启":"隐藏"));}
}
