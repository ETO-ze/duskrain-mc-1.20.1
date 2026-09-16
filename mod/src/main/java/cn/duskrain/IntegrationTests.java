package cn.duskrain;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(DuskRain.ID)
@PrefixGameTestTemplate(false)
public final class IntegrationTests {
    static ServerPlayer player(GameTestHelper h,String name){ServerPlayer p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));p.getInventory().clearContent();Store.get(p.server).players.put(p.getUUID(),new Profile());p.setPos(h.absolutePos(BlockPos.ZERO).getX(),h.absolutePos(BlockPos.ZERO).getY()+2,h.absolutePos(BlockPos.ZERO).getZ());return p;}
    static ServerPlayer servicePlayer(GameTestHelper h,String name,String service){var l=h.getLevel().getServer().getLevel(Gameplay.CITY);var s=Services.find(service);l.getChunkAt(s.counter());ServerPlayer p=FakePlayerFactory.get(l,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));p.getInventory().clearContent();Store.get(p.server).players.put(p.getUUID(),new Profile());var pos=s.position().add(-2,0,2);p.setPos(pos.x,pos.y,pos.z);return p;}
    static void command(ServerPlayer p,String s){p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),s);}
    @GameTest(template="empty",timeoutTicks=100) public static void economy_atomic_and_full_inventory(GameTestHelper h){
        ServerPlayer p=servicePlayer(h,"DRShopTest","materials");Profile r=Store.of(p);r.money=100;command(p,"dr shop buy bread 3");h.assertTrue(r.money==76&&Gameplay.count(p,Items.BREAD)==3,"Purchase must exchange exact money and items");
        command(p,"dr shop buy bread 64");h.assertTrue(r.money==76&&Gameplay.count(p,Items.BREAD)==3,"Insufficient money must not modify inventory");
        command(p,"dr shop sell bread 3");h.assertTrue(r.money==82&&Gameplay.count(p,Items.BREAD)==0,"Sell must remove exact inventory");
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));command(p,"dr shop buy bread");h.assertTrue(r.money==82,"Full inventory must never charge money");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void quest_and_sign_claim_once(GameTestHelper h){ServerPlayer p=servicePlayer(h,"DRQuestTest","quests");Profile r=Store.of(p);r.mainProgress=1;command(p,"dr quests claim 0");long money=r.money;int xp=r.xp;command(p,"dr quests claim 0");h.assertTrue(r.main==1&&r.money==money&&r.xp==xp,"Stale quest button must not duplicate rewards");r.lastSignDay=-1;command(p,"dr daily sign");money=r.money;command(p,"dr daily sign");h.assertTrue(money==r.money,"Sign reward must be once per Beijing day");h.assertTrue(new HashSet<>(r.dailies(p.getUUID())).size()==3,"Exactly three distinct daily quests");r.day=Profile.today()-1;r.dailyClaimed.add(1);r.refreshDay();h.assertTrue(r.dailyClaimed.isEmpty(),"Day rollover resets only daily state");h.succeed();}
    @GameTest(template="empty",timeoutTicks=100) public static void claims_and_real_break_events(GameTestHelper h){ServerPlayer a=player(h,"DROwnerTest"),b=player(h,"DRVisitorTest");BlockPos pos=h.absolutePos(new BlockPos(1,1,1));Store s=Store.get(a.server);long key=new ChunkPos(pos).toLong();s.claims.put(key,new Store.Claim(a.getUUID()));h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        BlockEvent.BreakEvent denied=new BlockEvent.BreakEvent(h.getLevel(),pos,Blocks.STONE.defaultBlockState(),b);MinecraftForge.EVENT_BUS.post(denied);h.assertTrue(denied.isCanceled(),"Foreign player cannot break claim");
        BlockEvent.BreakEvent allowed=new BlockEvent.BreakEvent(h.getLevel(),pos,Blocks.STONE.defaultBlockState(),a);MinecraftForge.EVENT_BUS.post(allowed);h.assertTrue(!allowed.isCanceled(),"Owner can break own claim");
        s.claims.get(key).members.add(b.getUUID());h.assertTrue(Protection.canEdit(b,h.getLevel(),pos),"Trusted member can edit");s.claims.remove(key);h.succeed();}
    @GameTest(template="empty",timeoutTicks=100) public static void state_serialization_and_binding(GameTestHelper h){ServerPlayer p=player(h,"DRStateTest");Profile r=Store.of(p);r.stage=13;r.school=2;r.money=987654321L;r.xp=1234;r.main=17;r.dailyClaimed.add(2);ItemStack artifact=new ItemStack(Content.ARTIFACTS.get(6).get());Content.bind(artifact,p);h.assertTrue(Content.owned(artifact,p),"Bound item owner can use");ServerPlayer q=player(h,"DRBindingOther");h.assertTrue(!Content.owned(artifact,q),"Other player cannot use bound artifact");Store data=Store.get(p.server);Store copy=Store.load(data.save(new net.minecraft.nbt.CompoundTag()));Profile rr=copy.players.get(p.getUUID());h.assertTrue(rr.stage==13&&rr.school==2&&rr.money==987654321L&&rr.main==17&&rr.dailyClaimed.contains(2),"Save roundtrip retains cultivation, money and task claims");h.succeed();}
    @GameTest(template="empty",timeoutTicks=100) public static void all_content_and_progression(GameTestHelper h){Rules.current.validate();h.assertTrue(Content.ARTIFACTS.size()==21&&Content.PILLS.size()==6,"Registered item counts");h.assertTrue(Rules.current.main.size()==24&&Rules.current.daily.size()==12,"Quest counts");h.assertTrue(CityPlan.BUILDINGS.size()==34,"Thirty original buildings, three guild buildings and enchanter");ServerPlayer p=servicePlayer(h,"DRStageTest","master");Profile r=Store.of(p);r.school=1;r.xp=300;command(p,"dr cultivate breakthrough");h.assertTrue(r.stage==1&&r.xp==100,"Breakthrough spends exact stage requirement");r.stage=17;r.xp=9999;command(p,"dr cultivate breakthrough");h.assertTrue(r.stage==17,"Final realm stays capped");h.succeed();}
    @GameTest(template="empty",timeoutTicks=100) public static void hud_packet_roundtrip(GameTestHelper h){var r=new Network.Snapshot("DuskRain",11,3,123,456,90,200,123456789L,"烟雨仙宫",false,20,true,1.25f);FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.buffer());Network.writeSnapshot(r,buf);var restored=Network.readSnapshot(buf);h.assertTrue(r.equals(restored),"All HUD fields must roundtrip without drift");buf.release();h.succeed();}
    @GameTest(template="empty",timeoutTicks=100) public static void fire_mixin_protects_claim(GameTestHelper h){ServerPlayer p=player(h,"DRFireTest");BlockPos pos=h.absolutePos(new BlockPos(1,1,1));Store data=Store.get(p.server);long key=new ChunkPos(pos).toLong();data.claims.put(key,new Store.Claim(p.getUUID()));h.getLevel().setBlock(pos,Blocks.FIRE.defaultBlockState(),3);h.assertTrue(!h.getLevel().getBlockState(pos).is(Blocks.FIRE),"Native setBlock must reject fire in claimed territory");data.claims.remove(key);h.succeed();}
}
