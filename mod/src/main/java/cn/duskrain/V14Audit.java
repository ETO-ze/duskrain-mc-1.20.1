package cn.duskrain;

import java.util.*;
import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in local QA. These entry points are absent from player commands and packets. */
@Mod.EventBusSubscriber(modid=DuskRain.ID)
public final class V14Audit {
 private record QaList(net.minecraft.server.players.PlayerList value){
  Object field(String dev,String runtime){if(!Boolean.getBoolean("duskrain.director"))throw new IllegalStateException("Local QA disabled");for(String name:List.of(dev,runtime))try{var f=net.minecraft.server.players.PlayerList.class.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException ignored){}throw new IllegalStateException("Pinned QA player field unavailable");}
  @SuppressWarnings("unchecked") List<ServerPlayer> duskrainPlayers(){return (List<ServerPlayer>)field("players","f_11196_");}
  @SuppressWarnings("unchecked") Map<UUID,ServerPlayer> duskrainPlayersById(){return (Map<UUID,ServerPlayer>)field("playersByUUID","f_11197_");}
 }
 static final List<NativeAcceptanceTests.Peer> actors=new ArrayList<>();
 static final List<net.minecraft.world.entity.monster.Zombie> targets=new ArrayList<>();
 static final List<Double> costs=new ArrayList<>();static int ticks,casts;static long tickStart,started;static ServerPlayer operator;
 static void report(ServerPlayer p,String name,Object value){try{Files.writeString(p.server.getServerDirectory().toPath().resolve(name+".json"),Rules.JSON.toJson(value));}catch(Exception e){DuskRain.LOG.error("V14 audit report",e);}}
 public static void run(ServerPlayer p,String op){if(!Boolean.getBoolean("duskrain.director")||p==null)return;
  if(op.equals("palace")){palace(p);return;}
  if(op.equals("snapshot")){var r=Store.of(p);report(p,"v14-player-check",Map.of("profile",r.save().toString(),"guild",GuildData.get(p.server).save(new net.minecraft.nbt.CompoundTag()).toString(),"inventory",p.getInventory().save(new net.minecraft.nbt.ListTag()).toString(),"flight",SwordFlight.flying(p)));return;}
  if(op.equals("load")&&actors.isEmpty()){operator=p;p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);var l=p.server.overworld();for(int x=23972;x<=24036;x++)for(int z=23972;z<=24036;z++){l.setBlock(new BlockPos(x,160,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);for(int y=161;y<=177;y++)l.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
   var list=new QaList(p.server.getPlayerList());var viewers=List.copyOf(p.server.getPlayerList().getPlayers());
   for(int i=0;i<20;i++){double angle=(i%10)*Math.PI/5;var peer=new NativeAcceptanceTests.Peer(l,"DRLoad"+i,new Vec3(24003+17*Math.sin(angle),161,24003+17*Math.cos(angle)));var q=peer.player;var r=Store.of(q);r.stage=20;r.school=i%3+1;r.mana=2000;r.initialized=true;r.starterGranted=true;r.onboardingComplete=true;q.getAbilities().invulnerable=true;q.setOnGround(true);q.setYRot((float)(180-angle*180/Math.PI));q.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(Content.ARTIFACTS.get(17+r.school).get()));Skills.attributes(q);q.setHealth(q.getMaxHealth());for(var slot:net.minecraft.world.entity.EquipmentSlot.values())if(slot.isArmor())q.setItemSlot(slot,new net.minecraft.world.item.ItemStack(Outfits.item(20,switch(slot){case HEAD->net.minecraft.world.item.ArmorItem.Type.HELMET;case CHEST->net.minecraft.world.item.ArmorItem.Type.CHESTPLATE;case LEGS->net.minecraft.world.item.ArmorItem.Type.LEGGINGS;default->net.minecraft.world.item.ArmorItem.Type.BOOTS;})));
    list.duskrainPlayers().add(q);list.duskrainPlayersById().put(q.getUUID(),q);for(var v:viewers)v.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(q)));l.addNewPlayer(q);actors.add(peer);if(i<10){SwordFlight.toggle(q);if(q.getVehicle()!=null)q.getVehicle().setPos(q.getX(),170,q.getZ());}
   }
   for(int i=0;i<10;i++){var t=net.minecraft.world.entity.EntityType.ZOMBIE.create(l);t.setPos(23998+i%5*2,161,23999+i/5*6);t.setNoAi(true);t.setSilent(true);t.setPersistenceRequired();t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024);t.setHealth(1024);l.addFreshEntity(t);targets.add(t);}
   p.teleportTo(l,24003,169,24032,180,18);ticks=0;casts=0;costs.clear();started=System.nanoTime();}
 }
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(actors.isEmpty())return;if(e.phase==TickEvent.Phase.START){tickStart=System.nanoTime();for(var t:targets)if(t.isAlive())t.setHealth(1024);for(int i=0;i<actors.size();i++){var peer=actors.get(i);var p=peer.player;Store.of(p).mana=2000;Skills.attributes(p);if(SwordFlight.flying(p)){SwordFlight.input(p,new SwordFlight.Input(ticks%120<60?4:8));}else if(ticks%40==i%20){Gameplay.COOLDOWNS.remove(p.getUUID());int before=Store.of(p).mana;Skills.cast(p,(ticks/40+i)%3);if(Store.of(p).mana<before)casts++;}
    if(ticks%40==0)Network.sync(p);peer.packets.clear();}return;}
  costs.add((System.nanoTime()-tickStart)/1e6);if(++ticks<1200)return;double seconds=(System.nanoTime()-started)/1e9;var sorted=new ArrayList<>(costs);Collections.sort(sorted);var rt=Runtime.getRuntime();var result=new LinkedHashMap<String,Object>();result.put("participants",20);result.put("type","20 simulated ServerPlayers registered in world and player list; one real observing client; cooldowns accelerated; fixed area, not exploration or 20 remote network clients");result.put("simulated_flight_actors",10);result.put("simulated_cast_actors",10);result.put("successful_casts",casts);result.put("seconds",seconds);result.put("ticks",ticks);result.put("tps",ticks/seconds);result.put("p95_ms",sorted.get((int)(sorted.size()*.95)));result.put("max_ms",sorted.get(sorted.size()-1));result.put("heap_mb",(rt.totalMemory()-rt.freeMemory())/1048576);result.put("tick_ms",costs);report(operator,"v14-load-20",result);
  var list=new QaList(operator.server.getPlayerList());var ids=actors.stream().map(a->a.player.getUUID()).toList();for(var peer:actors){var p=peer.player;SwordFlight.stop(p);list.duskrainPlayers().remove(p);list.duskrainPlayersById().remove(p.getUUID());p.serverLevel().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);Gameplay.COOLDOWNS.remove(p.getUUID());peer.close();}for(var v:operator.server.getPlayerList().getPlayers())v.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(ids));for(var t:targets)t.discard();targets.clear();actors.clear();Store.get(operator.server).setDirty();Gameplay.say(operator,"20 个模拟参与者测试记录已保存。");
 }
 static void palace(ServerPlayer p){var l=p.server.getLevel(Guilds.DIM);var g=GuildData.get(p.server).guilds.get(1);if(g==null)return;int ox=g.x(),oz=g.z();Set<Long> reached=new HashSet<>();ArrayDeque<int[]> queue=new ArrayDeque<>();queue.add(new int[]{0,107});reached.add(BlockPos.asLong(0,0,107));
  while(!queue.isEmpty()){var at=queue.removeFirst();for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=at[0]+d[0],z=at[1]+d[1];long key=BlockPos.asLong(x,0,z);if(Math.abs(x)>107||Math.abs(z)>107||reached.contains(key))continue;double y=WalkAudit.surface(l,ox+x,oz+z,65);if(Double.isNaN(y)||y>66||y<65)continue;reached.add(key);queue.add(new int[]{x,z});}}
  var rows=new ArrayList<Object>();boolean passed=true;int[][] halls={{0,87,14,6,1},{0,-2,25,15,2},{0,-61,18,10,1},{-29,64,11,8,1},{-31,-50,10,11,2},{32,-47,10,9,1},{66,-51,11,10,1},{-28,34,10,10,1},{29,34,10,10,1}};
  for(int[] h:halls){var errors=new ArrayList<String>();for(int dx=-1;dx<=1;dx++)if(!reached.contains(BlockPos.asLong(h[0]+dx,0,h[1]+h[3]+1)))errors.add("door lane "+dx+" disconnected");if(!reached.contains(BlockPos.asLong(h[0],0,h[1])))errors.add("interior disconnected");
   if(h[4]>1){int sx=ox+h[0]-h[2]+3,sz=oz+h[1]-h[3]+3;for(int j=0;j<9;j++)for(int lane=0;lane<3;lane++)if(Double.isNaN(WalkAudit.surface(l,sx+lane,sz+j,65.5+j)))errors.add("stair "+j+"/"+lane);for(int lane=0;lane<3;lane++)if(!WalkAudit.clear(l,sx+lane+.5,74,sz+9.5))errors.add("upper landing "+lane);}
   int light=l.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,new BlockPos(ox+h[0],65,oz+h[1]));rows.add(Map.of("center",List.of(h[0],h[1]),"floors",h[4],"light",light,"errors",errors));passed&=errors.isEmpty();}
  var waterErrors=new ArrayList<String>();int bridgeSamples=0;for(int[] route:new int[][]{{-105,-11,-73,-11},{-73,-11,-73,8},{-73,8,-49,8}}){int n=Math.max(Math.abs(route[2]-route[0]),Math.abs(route[3]-route[1]));for(int i=0;i<=n;i++)for(int lane=-1;lane<=1;lane++){int x=route[0]+Integer.signum(route[2]-route[0])*i+(route[1]!=route[3]?lane:0),z=route[1]+Integer.signum(route[3]-route[1])*i+(route[0]!=route[2]?lane:0);double y=WalkAudit.surface(l,ox+x,oz+z,66);if(Double.isNaN(y)||y<65||y>66)waterErrors.add("bridge lane "+x+","+z);bridgeSamples++;}}
  int invalidPlants=0;for(int x=-105;x<=-47;x++)for(int z=-89;z<=40;z++){var pos=new BlockPos(ox+x,65,oz+z);var s=l.getBlockState(pos);if((s.is(net.minecraft.world.level.block.Blocks.GRASS)||s.is(net.minecraft.tags.BlockTags.FLOWERS)||s.is(net.minecraft.world.level.block.Blocks.LILY_PAD))&&!s.canSurvive(l,pos)){invalidPlants++;if(waterErrors.size()<20)waterErrors.add("unsupported plant "+x+","+z);}}
  passed&=waterErrors.isEmpty();report(p,"v14-palace-audit",Map.of("passed",passed,"reachable_ground_cells",reached.size(),"halls",rows,"bridge_lane_samples",bridgeSamples,"invalid_waterside_plants",invalidPlants,"water_errors",waterErrors,"method","real saved chunk collision BFS, 3-wide entrances, stairs, bridge and shore joins; actual plant survival; visual and movement QA separate"));
 }
}
