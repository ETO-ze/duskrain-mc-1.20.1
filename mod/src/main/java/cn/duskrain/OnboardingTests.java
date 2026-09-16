package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder(DuskRain.ID) @PrefixGameTestTemplate(false)
public final class OnboardingTests {
    @GameTest(template="empty") public static void existing_guides_refresh_in_place_including_offhand(GameTestHelper h){
        var p=IntegrationTests.player(h,"DRGuideUpdate");var book=Onboarding.book();book.getTag().putInt("DuskRainGuideVersion",13);
        p.getInventory().setItem(0,book.copy());p.setItemSlot(EquipmentSlot.OFFHAND,book.copy());p.getEnderChestInventory().setItem(0,book.copy());
        var personal=new ItemStack(Items.WRITTEN_BOOK);personal.getOrCreateTag().putString("title","Personal journal");p.getInventory().setItem(1,personal);
        Onboarding.refreshGuide(p);Onboarding.refreshGuide(p);
        h.assertTrue(p.getInventory().getItem(0).getTag().getInt("DuskRainGuideVersion")==15&&p.getOffhandItem().getTag().getInt("DuskRainGuideVersion")==15&&p.getEnderChestInventory().getItem(0).getTag().getInt("DuskRainGuideVersion")==15,"Carried, offhand and ender-chest guides upgrade in place");
        h.assertTrue(p.getInventory().getItem(0).getCount()==1&&p.getOffhandItem().getCount()==1&&p.getInventory().getItem(1)==personal,"Upgrade does not duplicate books or change personal books");h.succeed();
    }
    @GameTest(template="empty") public static void newcomer_grants_once_and_starts_with_city_respawn(GameTestHelper h){
        var server=h.getLevel().getServer();Onboarding.prepare(server);var level=server.getLevel(Gameplay.INTRO);
        try(var peer=new NativeAcceptanceTests.Peer(level,"DRNewcomer",new Vec3(.5,81,.5))){var p=peer.player;var r=Store.of(p);r.initialized=true;
            Onboarding.grant(p);h.assertTrue(r.starterGranted,"Starter grant recorded");for(var slot:EquipmentSlot.values())if(slot.isArmor())h.assertTrue(p.getItemBySlot(slot).getItem() instanceof Outfits.Robe,"All four robe pieces equipped");
            h.assertTrue(p.getOffhandItem().is(Decor.EXPLORER.get().asItem()),"Exploration torch placed in offhand");int items=0;for(var s:p.getInventory().items)items+=s.getCount();Onboarding.grant(p);int again=0;for(var s:p.getInventory().items)again+=s.getCount();h.assertTrue(items==again,"Repeated starter request does not duplicate inventory");
            Onboarding.start(p);h.assertTrue(r.onboardingComplete&&p.level().dimension().equals(Gameplay.CITY),"Start button enters city");h.assertTrue(p.getRespawnDimension().equals(Gameplay.CITY),"Default respawn set to city");
            Profile loaded=Profile.load(r.save());h.assertTrue(loaded.onboardingComplete&&loaded.starterGranted,"Grant flags persist through save/load");
            DuskRain.LOG.info("ONBOARDING_ACCEPTANCE starter=once robes=4 offhand=15 start=city flags=persisted");
        }h.succeed();
    }
    @GameTest(template="empty") public static void legacy_players_preserved_and_full_inventory_retains_gifts(GameTestHelper h){
        CompoundTag old=new CompoundTag();old.putBoolean("initialized",true);old.putInt("xp",345);old.putLong("money",678);old.putInt("main",4);Profile loaded=Profile.load(old);h.assertTrue(loaded.onboardingComplete&&loaded.starterGranted&&loaded.xp==345&&loaded.money==678&&loaded.main==4,"Existing players retain progress and skip the new tutorial");
        var p=IntegrationTests.player(h,"DRFullNew");for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));Onboarding.grant(p);h.assertTrue(p.getOffhandItem().is(Items.SHIELD),"Existing offhand preserved");h.assertTrue(Store.of(p).retained.size()==3,"Full inventory retains book, torch, compass for later delivery");h.succeed();
    }
    @GameTest(template="empty") public static void all_eighteen_robe_sets_and_guide_are_complete(GameTestHelper h){
        int count=0;for(int stage=0;stage<21;stage++)for(ArmorItem.Type type:ArmorItem.Type.values()){Item item=Outfits.item(stage,type);h.assertTrue(item instanceof Outfits.Robe&&((Outfits.Robe)item).stage==stage,"Robe registration matches stage");String id=Outfits.id(stage,type);h.assertTrue(Rules.current.shops.stream().anyMatch(o->o.id().equals(id)&&o.stage()==((Outfits.Robe)item).stage),"Each robe has a stage-gated store offer");count++;}
        var pages=Onboarding.book().getTag().getList("pages",Tag.TAG_STRING);h.assertTrue(count==84&&pages.size()==58,"84 items and 58-page guide");h.assertTrue(pages.getString(pages.size()-1).contains("/dr start"),"Last page offers a real start command");
        var p=IntegrationTests.player(h,"DRRobeGate");p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Outfits.item(17,ArmorItem.Type.CHESTPLATE)));Outfits.enforce(p);h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"Server removes armor above player's stage");
        h.assertTrue(Decor.EXPLORER.get().defaultBlockState().getLightEmission()==15&&Blocks.TORCH.defaultBlockState().getLightEmission()==14,"Exploration light is stronger than the vanilla torch");h.succeed();
    }
    @GameTest(template="empty") public static void compass_targets_owned_home_and_reverts_after_claim_removal(GameTestHelper h){
        var p=IntegrationTests.player(h,"DRCompass");var r=Store.of(p);var store=Store.get(p.server);BlockPos home=p.blockPosition();long key=p.chunkPosition().toLong();var before=store.claims.get(key);try{
            store.claims.put(key,new Store.Claim(p.getUUID()));r.hasHome=true;r.homeDim="minecraft:overworld";r.homeX=home.getX();r.homeY=home.getY();r.homeZ=home.getZ();ItemStack compass=new ItemStack(Content.WAYFINDER.get());compass.getOrCreateTag().putBoolean("DuskRainHome",true);Wayfinder.update(compass,p);h.assertTrue(compass.getTag().getString("LodestoneDimension").equals("minecraft:overworld")&&NbtUtils.readBlockPos(compass.getTag().getCompound("LodestonePos")).equals(home),"Compass targets owned home");store.claims.remove(key);Wayfinder.update(compass,p);h.assertTrue(compass.getTag().getString("LodestoneDimension").equals("duskrain:city"),"Removed claim safely reverts to city");
        }finally{if(before==null)store.claims.remove(key);else store.claims.put(key,before);}h.succeed();
    }
}
