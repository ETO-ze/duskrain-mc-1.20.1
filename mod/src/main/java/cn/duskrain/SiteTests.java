package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(DuskRain.ID)
@PrefixGameTestTemplate(false)
public final class SiteTests {
    @GameTest(template="empty",timeoutTicks=120) public static void bamboo_survives_neighbor_updates(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(Gameplay.TRIAL);var s=new Trials.Session(3,new Trials.Request(UUID.randomUUID(),List.of(),1,false));TrialMechanics.build(l,s);
        List<BlockPos> stalks=new ArrayList<>();
        for(int i=0;i<12;i++){double a=i*Math.PI/6;int x=768+(int)(Math.cos(a)*34),z=(int)(Math.sin(a)*34);
            for(int y=65;y<=70;y++){BlockPos p=new BlockPos(x,y,z);stalks.add(p);h.assertTrue(l.getBlockState(p).canSurvive(l,p),"Every bamboo segment must have a valid substrate");l.neighborChanged(p,Blocks.STONE,p.below());}
        }
        h.runAfterDelay(40,()->{h.assertTrue(stalks.stream().allMatch(p->l.getBlockState(p).is(Blocks.BAMBOO)),"Native scheduled updates retain all 72 bamboo blocks");h.assertTrue(l.getEntitiesOfClass(ItemEntity.class,new AABB(728,64,-40,808,75,40),e->e.getItem().is(net.minecraft.world.item.Items.BAMBOO)).isEmpty(),"Decoration must not create bamboo item drops");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=200) public static void service_distance_and_villager_interaction(GameTestHelper h){
        var p=IntegrationTests.servicePlayer(h,"DRLocalService","materials");var r=Store.of(p);r.money=100;
        h.assertTrue(Services.nearby(p,"materials"),"Material counter must be reachable with clear sight");
        var s=Services.find("materials");var v=new Villager(EntityType.VILLAGER,p.serverLevel());v.setUUID(s.uuid());v.addTag("duskrain_service");v.setPos(s.position().x,s.position().y,s.position().z);
        var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND,v);
        new Protection().interactEntity(event);h.assertTrue(event.isCanceled()&&event.getCancellationResult().consumesAction(),"Registered villager opens its custom service and consumes vanilla trading");
        IntegrationTests.command(p,"dr shop buy bread");h.assertTrue(r.money==92,"Local merchant permits validated purchase");
        p.setPos(p.getX()+40,p.getY(),p.getZ());IntegrationTests.command(p,"dr shop buy bread");h.assertTrue(r.money==92,"Walking away invalidates stale menu purchase");
        h.assertTrue(!Services.nearby(p,"pills"),"Another shop cannot be opened remotely");h.succeed();
    }
}
