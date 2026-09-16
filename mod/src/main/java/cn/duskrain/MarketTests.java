package cn.duskrain;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder(DuskRain.ID)
@PrefixGameTestTemplate(false)
public final class MarketTests {
    @GameTest(template="empty",timeoutTicks=200) public static void player_shop_escrow_sale_and_stale_clicks(GameTestHelper h){
        var owner=IntegrationTests.servicePlayer(h,"DRStallOwner","materials");var buyer=IntegrationTests.servicePlayer(h,"DRStallBuyer","materials");var site=PlayerMarket.SITES.get(0);var level=owner.serverLevel();level.getChunkAt(site.counter());
        for(var p:new net.minecraft.server.level.ServerPlayer[]{owner,buyer})p.setPos(site.counter().getX()-.5,site.y()+1,site.counter().getZ()+2);
        Store data=Store.get(owner.server);var original=data.market.remove(site.id());
        try{
            var seller=Store.of(owner);var customer=Store.of(buyer);seller.money=10000;customer.money=1000;
            PlayerMarket.buy(owner,site.id());var shop=data.market.get(site.id());h.assertTrue(shop!=null&&owner.getUUID().equals(shop.owner)&&seller.money==10000-Rules.current.marketBoothPrice,"Buying a real nearby shop assigns ownership and charges the exact deed price");
            PlayerMarket.buy(buyer,site.id());h.assertTrue(customer.money==1000,"A sold shop cannot be purchased a second time");
            ItemStack iron=new ItemStack(Items.IRON_INGOT,8);iron.setHoverName(net.minecraft.network.chat.Component.literal("试铸玄铁"));owner.setItemInHand(InteractionHand.MAIN_HAND,iron);
            PlayerMarket.sell(owner,120,5);h.assertTrue(iron.getCount()==3&&shop.listings.size()==1,"Listing removes only the escrowed quantity from the seller");
            var listing=shop.listings.values().iterator().next();Store saved=Store.load(data.save(new net.minecraft.nbt.CompoundTag()));var restored=saved.market.get(site.id()).listings.get(listing.id());h.assertTrue(restored.stack().getCount()==5&&restored.stack().getHoverName().getString().equals("试铸玄铁")&&restored.price()==120,"Restart retains exact item NBT, quantity, listing ID and price");
            for(int i=0;i<36;i++)buyer.getInventory().setItem(i,new ItemStack(Items.STONE,64));PlayerMarket.purchase(buyer,listing.id().toString(),false);h.assertTrue(customer.money==1000&&shop.listings.size()==1,"Full inventory does not charge or consume stock");
            buyer.getInventory().clearContent();buyer.setPos(buyer.getX()+40,buyer.getY(),buyer.getZ());PlayerMarket.purchase(buyer,listing.id().toString(),false);h.assertTrue(customer.money==1000&&shop.listings.size()==1,"A stale remote shop page cannot transact");
            buyer.setPos(site.counter().getX()-.5,site.y()+1,site.counter().getZ()+2);long before=seller.money;
            PlayerMarket.purchase(buyer,listing.id().toString(),false);h.assertTrue(customer.money==880&&seller.money==before+120&&shop.listings.isEmpty()&&Gameplay.count(buyer,Items.IRON_INGOT)==5,"Sale delivers real items and credits the offline seller atomically in shared saved data");
            PlayerMarket.purchase(buyer,listing.id().toString(),false);h.assertTrue(customer.money==880&&seller.money==before+120&&Gameplay.count(buyer,Items.IRON_INGOT)==5,"Repeated stale purchase cannot duplicate money or items");
            DuskRain.LOG.info("PLAYER_MARKET_ACCEPTANCE deed=true escrowNBT=true restart=true fullInventory=true remoteDenied=true offlineSellerPaid=true stalePurchaseDenied=true");
        }finally{if(original==null)data.market.remove(site.id());else data.market.put(site.id(),original);}
        h.succeed();
    }
}
