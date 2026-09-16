package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Item escrow and both balances live in the same saved-data snapshot. */
public final class PlayerMarket {
    public record Site(int id,int x,int z,int y){public BlockPos counter(){return new BlockPos(x+3,y+1,z+1);}public String name(){return "烟雨小集 · "+(id+1)+"号铺";}}
    public static final List<Site> SITES=new ArrayList<>();
    static{for(int row=0;row<6;row++)for(int side:new int[]{-1,1}){int z=178+row*16;SITES.add(new Site(SITES.size(),side*16,z,Terrain.ground(0,z+7)+1));}}
    public static final class Shop {UUID owner;String ownerName="";final Map<UUID,Listing> listings=new LinkedHashMap<>();final Map<UUID,MarketStock.Stock> stock=new LinkedHashMap<>();long revision;}
    public record Listing(UUID id,ItemStack stack,long price){}
    public static Site at(BlockPos pos){return SITES.stream().filter(s->s.counter().equals(pos)).findFirst().orElse(null);}
    static Site nearby(ServerPlayer p){if(SwordFlight.flying(p))return null;if(!p.level().dimension().equals(Gameplay.CITY))return null;return SITES.stream().filter(s->p.distanceToSqr(Vec3.atCenterOf(s.counter()))<=36&&p.serverLevel().getBlockState(s.counter()).is(Blocks.BARREL)&&Skills.lineClear(p.serverLevel(),p.getEyePosition(),Vec3.atCenterOf(s.counter()).add(0,.6,0),p)).findFirst().orElse(null);}
    static Shop shop(Store data,int id){return data.market.computeIfAbsent(id,k->new Shop());}
    public static void open(ServerPlayer p){Site site=nearby(p);if(site==null){Gameplay.say(p,"请前往迎仙山门南侧的烟雨小集，右键铺内货柜。");return;}open(p,site);}
    public static void open(ServerPlayer p,Site site){if(site!=nearby(p)){Gameplay.say(p,"请靠近这间商铺的货柜。");return;}
        MarketStock.open(p,site);
    }
    static void legacyMenu(ServerPlayer p,Site site){
        Shop shop=shop(Store.get(p.server),site.id);boolean owner=p.getUUID().equals(shop.owner);List<Network.Entry> entries=new ArrayList<>();
        if(shop.owner==null)entries.add(DRCommands.card("购买这间商铺","stalls buy "+site.id,"价格 "+Rules.current.marketBoothPrice+" 灵石；每人限一间。购铺后右键货柜上架","minecraft:oak_sign",Store.of(p).money>=Rules.current.marketBoothPrice));
        if(owner)entries.add(new Network.Entry("上架手持商品","local stall-list","先将商品拿在主手，再设置整组价格和上架数量；绑定法器不可转售","minecraft:chest",shop.listings.size()<12));
        for(Listing listing:shop.listings.values()){String item=Content.label(listing.stack.getItem());entries.add(DRCommands.card((owner?"取回 · ":"购买 · ")+item+" × "+listing.stack.getCount(),"stalls "+(owner?"take ":"purchase ")+listing.id,"整组 "+listing.price+" 灵石；"+(owner?"取回未售出的实物":"一次购买整组，购买前检查背包空间"),net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(listing.stack.getItem()).toString(),true));}
        if(entries.isEmpty())entries.add(DRCommands.card("暂未上架商品","stalls","铺主还没有存入商品，稍后再来","minecraft:barrel",false));
        Network.menu(p,site.name(),shop.owner==null?"空置商铺 · 购铺后可寄售实际物品。铺面受主城保护，货柜是交易入口。":"铺主："+shop.ownerName+" · 商品由服务器保管，售出货款直接存入铺主灵石账户，离线仍可收款。",entries);
    }
    public static void buy(ServerPlayer p,int id){Site site=nearby(p);if(site==null||site.id!=id)return;Store data=Store.get(p.server);Shop shop=shop(data,id);Profile buyer=Store.of(p);
        if(shop.owner!=null){Gameplay.say(p,"这间商铺已售出。");return;}if(data.market.values().stream().anyMatch(s->p.getUUID().equals(s.owner))){Gameplay.say(p,"每位玩家限购一间商铺。");return;}if(buyer.money<Rules.current.marketBoothPrice){Gameplay.say(p,"灵石不足，未扣款。");return;}
        if(!Transactions.commit(p,"market:legacy-booth",()->{buyer.money-=Rules.current.marketBoothPrice;shop.owner=p.getUUID();shop.ownerName=p.getGameProfile().getName();shop.revision++;}))return;Network.sync(p);Gameplay.say(p,"已购得"+site.name()+"。");open(p,site);
    }
    public static void sell(ServerPlayer p,int price,int amount){Site site=nearby(p);if(site==null){Gameplay.say(p,"上架时需站在自己的商铺货柜旁。");return;}Store data=Store.get(p.server);Shop shop=shop(data,site.id);ItemStack held=p.getMainHandItem();
        if(!p.getUUID().equals(shop.owner)){Gameplay.say(p,"这不是你的商铺。");return;}
        if(price<1||price>100000000||amount<1||amount>64||held.getCount()<amount||held.isEmpty()||shop.listings.size()+shop.stock.size()>=27){Gameplay.say(p,"请检查价格、数量、主手商品与27个上架位。");return;}
        if(held.getItem() instanceof Content.Artifact){Gameplay.say(p,"绑定法器不能通过商铺转售。");return;}
        ItemStack escrow=held.copy();escrow.setCount(amount);UUID id=UUID.randomUUID();if(!Transactions.commit(p,"market:legacy-list",()->{shop.listings.put(id,new Listing(id,escrow,price));held.shrink(amount);p.getInventory().setChanged();shop.revision++;}))return;Gameplay.say(p,"已上架 "+Content.label(escrow.getItem())+" × "+amount+"，整组价格 "+price+" 灵石。");open(p,site);
    }
    public static void purchase(ServerPlayer p,String listingId,boolean take){
        Site site=nearby(p);if(site==null){Gameplay.say(p,"请在商品所在的商铺货柜旁操作。");return;}UUID id;try{id=UUID.fromString(listingId);}catch(IllegalArgumentException ex){return;}
        Store data=Store.get(p.server);Shop shop=shop(data,site.id);Listing listing=shop.listings.get(id);if(listing==null){Gameplay.say(p,"这组商品已售出或已取回，请刷新商铺。");open(p,site);return;}
        boolean owner=p.getUUID().equals(shop.owner);if(take!=owner){Gameplay.say(p,owner?"铺主请使用取回商品。":"只有铺主可以取回寄售物。");return;}
        if(!Gameplay.fits(p,listing.stack)){Gameplay.say(p,"背包空间不足，未扣款、未取走商品。");return;}
        Profile buyer=Store.of(p),seller=data.players.get(shop.owner);if(!take&&(buyer.money<listing.price||seller==null||seller.money>1000000000000L-listing.price)){Gameplay.say(p,"余额不足或铺主账户暂不能入账，交易未发生。");return;}
        if(!Transactions.commit(p,"market:legacy-purchase",()->{if(!take){buyer.money-=listing.price;seller.money+=listing.price;}shop.listings.remove(id);p.getInventory().add(listing.stack.copy());p.getInventory().setChanged();shop.revision++;}))return;Network.sync(p);
        var online=p.server.getPlayerList().getPlayer(shop.owner);if(online!=null&&online!=p){Network.sync(online);Gameplay.say(online,"商铺售出商品，收入 "+listing.price+" 灵石。");}
        DuskRain.LOG.info("PLAYER_MARKET {} listing={} shop={} buyer={} seller={} amount={} price={}",take?"return":"sale",id,site.id,p.getUUID(),shop.owner,listing.stack.getCount(),take?0:listing.price);
        Gameplay.say(p,take?"商品已取回。":"购买成功，货款已存入铺主账户。");open(p,site);
    }
    static CompoundTag save(Map<Integer,Shop> shops){CompoundTag root=new CompoundTag();shops.forEach((id,shop)->{CompoundTag n=new CompoundTag();if(shop.owner!=null)n.putUUID("owner",shop.owner);n.putString("ownerName",shop.ownerName);n.putLong("revision",shop.revision);n.put("stock",MarketStock.save(shop));ListTag listings=new ListTag();shop.listings.values().forEach(l->{CompoundTag v=new CompoundTag();v.putUUID("id",l.id);v.put("stack",l.stack.save(new CompoundTag()));v.putLong("price",l.price);listings.add(v);});n.put("listings",listings);root.put(id.toString(),n);});return root;}
    static void load(CompoundTag root,Map<Integer,Shop> shops){for(String key:root.getAllKeys()){int id;try{id=Integer.parseInt(key);}catch(NumberFormatException e){continue;}if(id<0||id>=SITES.size())continue;CompoundTag n=root.getCompound(key);Shop shop=new Shop();if(n.hasUUID("owner"))shop.owner=n.getUUID("owner");shop.ownerName=n.getString("ownerName");shop.revision=n.getLong("revision");MarketStock.load(shop,n.getList("stock",Tag.TAG_COMPOUND));ListTag values=n.getList("listings",Tag.TAG_COMPOUND);for(int i=0;i<values.size();i++){CompoundTag v=values.getCompound(i);ItemStack stack=ItemStack.of(v.getCompound("stack"));if(v.hasUUID("id")&&!stack.isEmpty()&&v.getLong("price")>0){UUID uuid=v.getUUID("id");shop.listings.put(uuid,new Listing(uuid,stack,v.getLong("price")));}}shops.put(id,shop);}}
    static void routes(){CityPlan.route(0,160,Terrain.ground(0,160)+1,0,267,Terrain.ground(0,267)+1,false);for(Site s:SITES)CityPlan.route(0,s.z+7,CityPlan.hubHeight(0,s.z+7),s.x,s.z+7,s.y,false);}
    static void paint(CityPlan.Sink sink,int cx,int cz,int phase){for(Site s:SITES){if(Math.abs(s.x-(cx*16+8))>17||Math.abs(s.z-(cz*16+8))>18)continue;
        if(phase==0)sink.room("player_shop_"+s.id,s.x,s.y,s.z,5,5,1);
        if(phase==0)for(int x=s.x-5;x<=s.x+5;x++)for(int z=s.z-5;z<=s.z+6;z++){SiteTerrain.foundation(sink,x,s.y,z);sink.block(x,s.y,z,CityPlan.WHITE);CityPlan.box(sink,x,s.y+1,z,x,s.y+7,z,Blocks.AIR.defaultBlockState());}
        if(phase==1){for(int dx:new int[]{-5,5})for(int dz:new int[]{-5,5})CityPlan.column(sink,s.x+dx,s.y,s.z+dz,6);
            for(int x=-4;x<=4;x++)for(int y=1;y<=5;y++){sink.block(s.x+x,s.y+y,s.z-5,CityPlan.WALL);if(Math.abs(x)>1)sink.block(s.x+x,s.y+y,s.z+5,CityPlan.WALL);}
            for(int z=-4;z<=4;z++)for(int side:new int[]{-1,1})for(int y=1;y<=5;y++)sink.block(s.x+side*5,s.y+y,s.z+z,y>=2&&y<=3&&Math.abs(z)<=2?Decor.LATTICE.get().defaultBlockState():CityPlan.WALL);
        }
        if(phase==2)CityPlan.roof(sink,s.x,s.y+7,s.z,7,7,-1,-1,s.id%2);
        if(phase==3){Interiors.desk(sink,s.x-3,s.y+1,s.z+1,2);Interiors.cabinet(sink,s.x+2,s.y+1,s.z-4,2,s.id%3==0);sink.block(s.x-4,s.y+1,s.z+3,Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());sink.block(s.x-4,s.y+2,s.z+3,s.id%3==0?Blocks.POTTED_AZALEA.defaultBlockState():s.id%3==1?Blocks.BREWING_STAND.defaultBlockState():Blocks.GRINDSTONE.defaultBlockState());}
        if(phase==3){for(int dx:new int[]{-3,0,3})for(int dz:new int[]{-3,0,3})sink.block(s.x+dx,s.y,s.z+dz,Blocks.SEA_LANTERN.defaultBlockState());sink.block(s.counter().getX(),s.counter().getY(),s.counter().getZ(),Blocks.BARREL.defaultBlockState());CityPlan.box(sink,s.x-4,s.y+1,s.z-3,s.x-3,s.y+2,s.z-3,Blocks.BOOKSHELF.defaultBlockState());sink.block(s.x,s.y+5,s.z,Decor.LANTERN.get().defaultBlockState());CityPlan.sign(sink,s.x+3,s.y+1,s.z+6,s.name(),"右键铺内货柜","购铺 · 寄售 · 购买","DuskRain 205255670");}
    }}
}
