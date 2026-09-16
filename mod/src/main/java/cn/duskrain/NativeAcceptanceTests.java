package cn.duskrain;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

/** Native server packets are captured at the connection boundary; no client rendering is inferred. */
@GameTestHolder(DuskRain.ID)
@PrefixGameTestTemplate(false)
public final class NativeAcceptanceTests {
    static final class Peer implements AutoCloseable {
        final ServerPlayer player;
        final List<Packet<?>> packets=new ArrayList<>();
        Peer(ServerLevel level,String name,Vec3 position){
            player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),name));
            Connection connection=new Connection(PacketFlow.SERVERBOUND){@Override public void send(Packet<?> packet){packets.add(packet);}};
            player.connection=new ServerGamePacketListenerImpl(player.server,connection,player){
                @Override public void send(Packet<?> packet){packets.add(packet);}
            };
            player.setPos(position.x,position.y,position.z);
            Store.get(player.server).players.put(player.getUUID(),new Profile());
        }
        List<Network.Entry> menu(){
            List<Network.Entry> entries=new ArrayList<>();
            for(Packet<?> packet:packets)if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.getIdentifier().toString().equals("duskrain:main")){
                FriendlyByteBuf data=new FriendlyByteBuf(custom.getData().copy());
                try{if(data.readVarInt()!=1)continue;data.readUtf(128);data.readUtf(4096);int count=data.readVarInt();
                    entries.clear();for(int i=0;i<count;i++)entries.add(new Network.Entry(data.readUtf(256),data.readUtf(256),data.readUtf(1024),data.readUtf(128),data.readBoolean()));
                }finally{data.release();}
            }
            return entries;
        }
        boolean noticeContains(String text){
            for(Packet<?> packet:packets)if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.getIdentifier().toString().equals("duskrain:main")){
                FriendlyByteBuf data=new FriendlyByteBuf(custom.getData().copy());
                try{if(data.readVarInt()==4&&data.readUtf(2048).contains(text))return true;}finally{data.release();}
            }
            return false;
        }
        void action(Actions.Kind kind,String target,int value){
            Network.LAST_ACTION.remove(player.getUUID());
            Actions.receive(player,new Network.Action(kind,target,value,Actions.SEEN.getOrDefault(player.getUUID(),0L)+1));
        }
        void atService(String id){Vec3 pos=Services.find(id).position().add(-2,0,2);player.setPos(pos.x,pos.y,pos.z);}
        @Override public void close(){
            player.serverLevel().players().remove(player);
            Store.get(player.server).players.remove(player.getUUID());
            Actions.SEEN.remove(player.getUUID());Network.LAST_ACTION.remove(player.getUUID());
        }
    }

    @GameTest(template="empty") public static void menu_and_actions_enforce_service_location(GameTestHelper h){
        ServerLevel city=h.getLevel().getServer().getLevel(Gameplay.CITY);
        try(Peer peer=new Peer(city,"DRSitePackets",new Vec3(.5,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z+.5))){
            ServerPlayer p=peer.player;Profile r=Store.of(p);r.money=100;r.mainProgress=1;
            DRCommands.menu(p);List<Network.Entry> main=peer.menu();
            h.assertTrue(!main.isEmpty(),"Main menu must actually be sent as a native custom payload");
            h.assertTrue(main.stream().noneMatch(e->e.action().contains("shop")||e.action().contains("trial enter")),"G menu contains no remote storefront or trial start");
            peer.packets.clear();DRCommands.profile(p);
            h.assertTrue(!peer.menu().isEmpty()&&peer.menu().stream().noneMatch(e->e.enabled()&&(e.action().contains("choose")||e.action().contains("breakthrough"))),"Remote profile does not expose local cultivation actions");
            peer.action(Actions.Kind.CHOOSE,"",1);h.assertTrue(r.school==0,"Typed school choice is rejected away from the master");
            r.school=1;r.xp=Rules.current.stageXp[0];int xp=r.xp;
            peer.action(Actions.Kind.BREAKTHROUGH,"",0);h.assertTrue(r.stage==0&&r.xp==xp,"Remote breakthrough cannot spend XP or change stage");
            peer.action(Actions.Kind.MAIN_REWARD,"",0);h.assertTrue(r.main==0&&r.money==100,"Remote completed quest cannot pay out");
            int daily=r.dailies(p.getUUID()).get(0);r.dailyProgress[daily]=Rules.current.daily.get(daily).count();
            peer.action(Actions.Kind.DAILY_REWARD,"",daily);h.assertTrue(!r.dailyClaimed.contains(daily)&&r.money==100,"Remote daily reward remains unclaimed");
            peer.packets.clear();peer.action(Actions.Kind.TRIAL_ENTER,"",1);
            h.assertTrue(peer.noticeContains(Services.find("trial").name()),"Trial packet must be rejected by the location guard before team validation");
            peer.packets.clear();peer.atService("master");r.school=0;peer.action(Actions.Kind.CHOOSE,"",1);
            h.assertTrue(r.school==1,"Identical typed choice succeeds beside the registered master");
            peer.atService("quests");peer.action(Actions.Kind.MAIN_REWARD,"",0);
            h.assertTrue(r.main==1&&r.money>100,"Identical completed quest pays out at its service counter");
            DuskRain.LOG.info("NATIVE_SERVICE_ACCEPTANCE menuPayload=true remoteChoose=false remoteBreakthrough=false remoteQuest=false remoteDaily=false remoteTrial=false localChoose=true localQuest=true");
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void counters_require_dimension_barrel_and_clear_sight(GameTestHelper h){
        ServerLevel city=h.getLevel().getServer().getLevel(Gameplay.CITY);
        try(Peer peer=new Peer(city,"DRCounterPackets",Vec3.ZERO)){
            for(var service:Services.ALL){city.getChunkAt(service.counter());peer.atService(service.id());h.assertTrue(Services.nearby(peer.player,service.id()),"Registered service is reachable: "+service.id());}
            var service=Services.find("materials");peer.atService(service.id());BlockPos pos=service.counter();var original=city.getBlockState(pos);
            try{city.setBlock(pos,Blocks.STONE.defaultBlockState(),3);h.assertTrue(!Services.nearby(peer.player,service.id()),"Same coordinate with a non-counter block must reject service");}
            finally{city.setBlock(pos,original,3);}
            Vec3 eye=peer.player.getEyePosition(),target=service.position().add(0,1.5,0);BlockPos wall=BlockPos.containing(eye.add(target).scale(.5));var before=city.getBlockState(wall);
            try{city.setBlock(wall,Blocks.STONE.defaultBlockState(),3);h.assertTrue(!Services.nearby(peer.player,service.id()),"A solid wall blocks interaction even within six blocks");}
            finally{city.setBlock(wall,before,3);}
            try(Peer other=new Peer(h.getLevel(),"DRWrongDimension",peer.player.position())){h.assertTrue(!Services.nearby(other.player,service.id()),"Matching coordinates in another dimension grant no service");}
            DuskRain.LOG.info("NATIVE_COUNTER_ACCEPTANCE reachableServices={} wrongDimension=false missingBarrel=false blockedSight=false",Services.ALL.size());
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void teleport_particles_reach_nearby_connections(GameTestHelper h){
        ServerLevel level=h.getLevel();Vec3 from=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1,3,1))),to=from.add(8,0,0);
        try(Peer traveler=new Peer(level,"DRParticleTravel",from);Peer near=new Peer(level,"DRParticleNear",from.add(3,0,0));Peer far=new Peer(level,"DRParticleFar",from.add(128,0,0))){
            // Register only with the native level broadcast list for this synchronous test.
            level.players().add(traveler.player);level.players().add(near.player);level.players().add(far.player);
            traveler.player.fallDistance=20;TeleportEffects.travel(traveler.player,level,to,90);
            h.assertTrue(traveler.player.position().distanceToSqr(to)<.001&&traveler.player.fallDistance==0,"Native teleport moves the player and clears stale fall damage");
            var particles=near.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).toList();
            h.assertTrue(particles.stream().filter(p->p.getParticle().getType()==ParticleTypes.DUST).count()>=48,"Nearby observer receives both jade rings");
            h.assertTrue(particles.stream().anyMatch(p->p.getParticle().getType()==ParticleTypes.END_ROD&&Math.abs(p.getX()-from.x)<.01),"Observer receives the departure burst at the old position");
            h.assertTrue(particles.stream().anyMatch(p->p.getParticle().getType()==ParticleTypes.END_ROD&&Math.abs(p.getX()-to.x)<.01),"Observer receives the arrival burst at the actual destination");
            h.assertTrue(traveler.packets.stream().anyMatch(ClientboundPlayerPositionPacket.class::isInstance),"Traveler receives native position synchronization");
            h.assertTrue(traveler.packets.stream().anyMatch(ClientboundLevelParticlesPacket.class::isInstance),"Traveler receives particle packets too");
            h.assertTrue(far.packets.stream().noneMatch(ClientboundLevelParticlesPacket.class::isInstance),"Distant players do not receive local particle spam");
            DuskRain.LOG.info("NATIVE_TELEPORT_ACCEPTANCE observerParticlePackets={} travelerPosition=true travelerParticles=true farParticles=false; rendering and cross-dimension arrival remain separate checks",particles.size());
        }
        h.succeed();
    }
}
