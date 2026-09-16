package cn.duskrain;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=DuskRain.ID)
public final class ServerInfo {
    @SubscribeEvent public static void name(PlayerEvent.TabListNameFormat event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;Profile r=Store.of(p);
        event.setDisplayName(Component.literal(p.getGameProfile().getName()).withStyle(ChatFormatting.WHITE)
            .append(Component.literal(" · "+PlayerTitles.text(p)+" · "+p.latency+"ms").withStyle(ChatFormatting.GRAY)));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%100!=0||p instanceof net.minecraftforge.common.util.FakePlayer)return;
        p.refreshTabListName();Profile r=Store.of(p);var server=p.server;
        Component header=Component.literal("DuskRain  ·  烟雨仙途\n").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("在线 "+server.getPlayerCount()+" / "+server.getMaxPlayers()+"    群号 205255670").withStyle(ChatFormatting.WHITE));
        Component footer=Component.literal(Gameplay.region(p)+" · "+(Protection.pvpAllowed(p)?"PVP 开启":"安全区域")+"\n").withStyle(ChatFormatting.AQUA)
            .append(Component.literal("灵石 "+r.money+"    灵力 "+r.mana+" / "+Skills.manaMax(r)+"\n").withStyle(ChatFormatting.WHITE))
            .append(Component.literal("G 仙途菜单 · F8 信息 HUD · 到店右键商人").withStyle(ChatFormatting.GRAY));
        p.connection.send(new ClientboundTabListPacket(header,footer));
    }
}
