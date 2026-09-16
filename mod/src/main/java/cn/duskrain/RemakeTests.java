package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.gametest.*;

@GameTestHolder(DuskRain.ID)
@PrefixGameTestTemplate(false)
public final class RemakeTests {
    @GameTest(template="empty") public static void full_inventory_reward_restores_only_remainder(GameTestHelper h){
        ServerPlayer p=IntegrationTests.player(h,"DRRetainedReward");for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,63));
        Gameplay.giveOrRetain(p,new ItemStack(Items.IRON_INGOT,6));Profile r=Store.of(p);h.assertTrue(Gameplay.count(p,Items.IRON_INGOT)==64&&r.retained.size()==1,"One item fits and the remaining reward is persisted");
        Gameplay.restore(p);h.assertTrue(ItemStack.of(r.retained.getCompound(0).getCompound("item")).getCount()==5,"Full inventory does not discard pending items");
        p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,62));Gameplay.restore(p);h.assertTrue(ItemStack.of(r.retained.getCompound(0).getCompound("item")).getCount()==3,"Partial restoration subtracts delivered items from pending NBT");
        p.getInventory().setItem(1,ItemStack.EMPTY);Gameplay.restore(p);h.assertTrue(r.retained.isEmpty()&&Gameplay.count(p,Items.IRON_INGOT)==67,"Final restoration neither duplicates nor loses items");Gameplay.restore(p);h.assertTrue(Gameplay.count(p,Items.IRON_INGOT)==67,"Repeated restoration cannot duplicate reward");h.succeed();
    }
    @GameTest(template="empty") public static void sign_text_survives_restore_and_save(GameTestHelper h){
        BlockPos pos=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(pos,Blocks.SPRUCE_SIGN.defaultBlockState(),3);
        var sign=(net.minecraft.world.level.block.entity.SignBlockEntity)h.getLevel().getBlockEntity(pos);
        var source=SignRepair.blueprint(new BlockPos(-7,CityPlan.SPAWN_Y,29));h.assertTrue(source!=null,"Welcome sign has a recoverable authored definition");
        source.putInt("x",pos.getX());source.putInt("y",pos.getY());source.putInt("z",pos.getZ());
        h.assertTrue(SignRepair.restore(sign,source),"Empty sign restores from the authored NBT");
        h.assertTrue(sign.getFrontText().getMessage(3,false).getString().contains("205255670"),"Front retains the group number");
        h.assertTrue(sign.getBackText().getMessage(3,false).getString().contains("205255670"),"Back retains the group number");
        var restored=new net.minecraft.world.level.block.entity.SignBlockEntity(pos,sign.getBlockState());restored.load(sign.saveWithFullMetadata());
        h.assertTrue(restored.isWaxed()&&restored.getFrontText().hasGlowingText(),"Wax and glowing text survive a save/reload");
        sign.setText(sign.getFrontText().setMessage(0,net.minecraft.network.chat.Component.literal("管理员自定义告示")),true);
        h.assertTrue(!SignRepair.restore(sign,source)&&sign.getFrontText().getMessage(0,false).getString().equals("管理员自定义告示"),"Nonempty edited text is never overwritten by the blueprint");h.succeed();
    }
    @GameTest(template="empty") public static void rejected_break_resends_sign_block_and_nbt(GameTestHelper h){
        ServerPlayer p=IntegrationTests.player(h,"DRSignBreak");BlockPos pos=h.absolutePos(new BlockPos(3,2,3));long claim=new net.minecraft.world.level.ChunkPos(pos).toLong();Store.get(p.server).claims.put(claim,new Store.Claim(java.util.UUID.randomUUID()));
        h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(pos,Blocks.SPRUCE_SIGN.defaultBlockState(),3);
        var sign=(net.minecraft.world.level.block.entity.SignBlockEntity)h.getLevel().getBlockEntity(pos);var n=SignRepair.blueprint(new BlockPos(-7,CityPlan.SPAWN_Y,29));n.putInt("x",pos.getX());n.putInt("y",pos.getY());n.putInt("z",pos.getZ());sign.load(n);
        java.util.List<net.minecraft.network.protocol.Packet<?>> packets=new java.util.ArrayList<>();var previous=p.connection;
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){packets.add(packet);}};
        try{boolean destroyed=p.gameMode.destroyBlock(pos);
            h.assertTrue(!destroyed&&h.getLevel().getBlockEntity(pos)==sign,"The native server block break path rejects removal and retains the sign entity");
            h.assertTrue(packets.size()>=2&&packets.get(0) instanceof net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket,"Restoration sends the block state first");
            var data=(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket)packets.get(1);h.assertTrue(SignRepair.hasText(data.getTag()),"The following packet carries text, not an empty block entity");
            var clientSign=new net.minecraft.world.level.block.entity.SignBlockEntity(pos,sign.getBlockState());clientSign.load(data.getTag());h.assertTrue(clientSign.getBackText().getMessage(3,false).getString().contains("205255670"),"A newly recreated client sign can load both sides");
            h.assertTrue(SignRepair.PENDING.containsKey(new SignRepair.Key(p.getUUID(),pos)),"A delayed resync remains scheduled after the immediate packet");
        }finally{p.connection=previous;Store.get(p.server).claims.remove(claim);SignRepair.PENDING.remove(new SignRepair.Key(p.getUUID(),pos));}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void landscape_preserves_walkways(GameTestHelper h){
        int[] collisions={0};for(var t:Landscape.trees())Landscape.tree(new CityPlan.Sink(){public void block(int x,int y,int z,net.minecraft.world.level.block.state.BlockState state){if(CityPlan.road(x,z))collisions[0]++;}},t);
        h.assertTrue(collisions[0]==0,"Tree branches, foliage and roots must not occupy any public road");
        h.assertTrue(Landscape.trees().size()>300,"Island has authored groves rather than a few isolated trees");h.assertTrue(Landscape.lights().size()>15,"Road network includes intermediate lamps");
        DuskRain.LOG.info("LANDSCAPE_PLANNING trees={} lamps={} landmarks={} roadCollisions={}",Landscape.trees().size(),Landscape.lights().size(),Landscape.LANDMARKS.size(),collisions[0]);h.succeed();
    }
    @GameTest(template="empty") public static void reach_and_school_attributes(GameTestHelper h){ServerPlayer p=IntegrationTests.player(h,"DRReach");Profile r=Store.of(p);r.school=1;p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));Skills.attributes(p);h.assertTrue(Math.abs(p.getAttributeValue(ForgeMod.ENTITY_REACH.get())-4.5)<.001,"Sword school must have real 4.5 entity reach");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);Skills.attributes(p);h.assertTrue(p.getAttributeValue(ForgeMod.ENTITY_REACH.get())==3,"Unequipping removes sword reach immediately");r.school=3;Skills.attributes(p);double speed=p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed*1.25)<.001,"Body school gains 25% speed");Skills.attributes(p);h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed*1.25)<.001,"Repeated updates cannot stack modifiers");r.school=0;Skills.attributes(p);h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed)<.001,"Reset removes speed");r.school=2;h.assertTrue(Skills.manaMax(r)==150,"Mage gains mana capacity");h.succeed();}
    @GameTest(template="empty") public static void projectile_geometry_and_walls(GameTestHelper h){ServerPlayer p=IntegrationTests.player(h,"DRGeometry");Vec3 a=Vec3.atCenterOf(h.absolutePos(new BlockPos(1,2,1))),b=a.add(4,0,0);h.getLevel().setBlock(BlockPos.containing(a.add(2,0,0)),Blocks.STONE.defaultBlockState(),3);h.assertTrue(!Skills.lineClear(h.getLevel(),a,b,p),"Solid wall blocks skill ray");h.getLevel().setBlock(BlockPos.containing(a.add(2,0,0)),Blocks.AIR.defaultBlockState(),3);h.assertTrue(Skills.lineClear(h.getLevel(),a,b,p),"Clear path accepts ray");h.assertTrue(Math.abs(Skills.distanceToBox(Vec3.ZERO,new AABB(4,0,0,5,2,1))-4)<.001,"Reach uses closest hitbox, not feet or inflated cube");h.succeed();}
    @GameTest(template="empty") public static void packet_authority_and_nonce(GameTestHelper h){ServerPlayer p=IntegrationTests.servicePlayer(h,"DRPacket","materials");Profile r=Store.of(p);r.money=100;Network.Action buy=new Network.Action(Actions.Kind.BUY,"bread",1,99);Actions.SEEN.remove(p.getUUID());Network.LAST_ACTION.remove(p.getUUID());Actions.receive(p,buy);h.assertTrue(r.money==92,"Typed action purchases intended item");Network.LAST_ACTION.remove(p.getUUID());Actions.receive(p,buy);h.assertTrue(r.money==92,"Repeated packet sequence cannot repeat purchase");h.assertTrue(Actions.command(new Network.Action(Actions.Kind.BUY,"bread 1\\ngive @s diamond",1,100),p)==null,"Client cannot inject commands");h.assertTrue(Actions.command(new Network.Action(Actions.Kind.BUY,"bread",-10,100),p)==null,"Negative amount rejected before execution");h.succeed();}
    @GameTest(template="empty") public static void site_and_service_contracts(GameTestHelper h){h.assertTrue(CityPlan.BUILDINGS.stream().filter(b->!b.home()).map(CityPlan.Building::variant).distinct().count()==16,"Core buildings have twelve original layouts, three guild layouts and enchanter");h.assertTrue(Services.ALL.stream().map(Services.Service::uuid).distinct().count()==Services.ALL.size(),"Service UUIDs are unique");for(var s:Services.ALL)h.assertTrue(Services.byBlock(s.counter())==s,"Counter maps to exact service");for(var r:CityPlan.ROUTES){int run=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az()));h.assertTrue(Math.abs(r.by()-r.ay())<=run,"Public route must not require climbing more than one block per horizontal block");}h.succeed();}
    @GameTest(template="empty") public static void all_quest_producers_exist(GameTestHelper h){var allowed=java.util.Set.of("visit","talk","practice","craft","investigate","puzzle","kill","submit","trial");for(var q:Rules.current.main){h.assertTrue(allowed.contains(q.type()),"Every main objective has a server producer");if(q.type().equals("talk"))h.assertTrue(Services.ALL.stream().anyMatch(s->s.id().equals(q.target())),"Dialogue target must exist");if(q.type().equals("visit"))h.assertTrue(CityPlan.BUILDINGS.stream().anyMatch(b->b.id().equals(q.target())),"Visit target must exist");}h.succeed();}
}
