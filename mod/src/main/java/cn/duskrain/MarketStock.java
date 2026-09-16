package cn.duskrain;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Unit-price stock and original lot-price consignments coexist without repricing old goods. */
public final class MarketStock {
    public static final class Stock {
        UUID id;ItemStack sample;int quantity;long price,version;
        Stock(UUID id,ItemStack sample,int qty,long price,long version){this.id=id;this.sample=sample.copyWithCount(1);this.quantity=qty;this.price=price;this.version=version;}
    }
    public record Row(UUID id,ItemStack sample,int quantity,long price,long version,boolean legacy){}
    public record View(int site,String name,String owner,boolean mine,boolean vacant,long money,long revision,List<Row> rows){}
    public record Request(String verb,int site,UUID id,int slot,int quantity,long price,long revision,long sequence){}
    static final Map<UUID,Long> SEEN=new HashMap<>();
    static final UUID NONE=new UUID(0,0);
    public static void open(ServerPlayer p,PlayerMarket.Site site){
        var shop=PlayerMarket.shop(Store.get(p.server),site.id());List<Row> rows=new ArrayList<>();
        for(var s:shop.stock.values())rows.add(new Row(s.id,s.sample.copy(),s.quantity,s.price,s.version,false));
        for(var s:shop.listings.values())rows.add(new Row(s.id(),s.stack().copyWithCount(1),s.stack().getCount(),s.price(),0,true));
        V14Network.to(p,new View(site.id(),site.name(),shop.ownerName,p.getUUID().equals(shop.owner),shop.owner==null,Store.of(p).money,shop.revision,rows));
    }
    static int matching(ServerPlayer p,ItemStack sample){int total=0;for(ItemStack s:p.getInventory().items)if(ItemStack.isSameItemSameTags(s,sample))total+=s.getCount();return total;}
    static void remove(ServerPlayer p,ItemStack sample,int count){for(ItemStack s:p.getInventory().items)if(ItemStack.isSameItemSameTags(s,sample)){int n=Math.min(count,s.getCount());s.shrink(n);count-=n;if(count==0)break;}if(count!=0)throw new IllegalStateException("Validated stock vanished");}
    static void give(ServerPlayer p,ItemStack sample,int count){while(count>0){int n=Math.min(count,sample.getMaxStackSize());ItemStack stack=sample.copyWithCount(n);if(!p.getInventory().add(stack)||!stack.isEmpty())throw new IllegalStateException("Validated inventory became full");count-=n;}}
    static void require(boolean condition,String why){if(!condition)throw new IllegalArgumentException(why);}
    public static void request(ServerPlayer p,Request req){
        if(!p.isAlive()||p.isSpectator())return;
        var site=PlayerMarket.nearby(p);if(site==null||site.id()!=req.site){Gameplay.say(p,"请靠近对应商铺货柜。");return;}
        if(req.sequence<=SEEN.getOrDefault(p.getUUID(),-1L))return;SEEN.put(p.getUUID(),req.sequence);
        Store data=Store.get(p.server);var shop=PlayerMarket.shop(data,site.id());boolean mine=p.getUUID().equals(shop.owner);
        try{
            if(req.verb.equals("refresh")){open(p,site);return;}
            require(req.revision==shop.revision,"商品已变化，已刷新，请重新确认。");
            if(req.verb.equals("booth")){
                require(shop.owner==null,"商铺已售出。");require(data.market.values().stream().noneMatch(s->p.getUUID().equals(s.owner)),"每人限购一间商铺。");require(Store.of(p).money>=Rules.current.marketBoothPrice,"灵石不足。");
                Transactions.commit(p,"market:booth",()->{Store.of(p).money-=Rules.current.marketBoothPrice;shop.owner=p.getUUID();shop.ownerName=p.getGameProfile().getName();shop.revision++;});open(p,site);return;
            }
            require(shop.owner!=null,"请先购买商铺。");
            int qty=req.quantity;require(qty>=1&&qty<=AscensionRules.current.stockLimit,"数量须为1至"+AscensionRules.current.stockLimit+"。");
            Stock stock=shop.stock.get(req.id);PlayerMarket.Listing legacy=shop.listings.get(req.id);
            switch(req.verb){
                case "list"->{
                    require(mine,"只有铺主可以上架。");require(shop.stock.size()+shop.listings.size()<27,"27个商品位已满。");require(req.slot>=0&&req.slot<36,"请选择背包商品。");require(req.price>=1&&req.price<=100000000,"单价须为1至100000000。");
                    ItemStack sample=p.getInventory().getItem(req.slot).copyWithCount(1);require(!sample.isEmpty()&&!(sample.getItem() instanceof Content.Artifact),"空物品或绑定法器不能出售。");require(matching(p,sample)>=qty,"背包中同类同数据商品不足。");
                    Transactions.commit(p,"market:list",()->{remove(p,sample,qty);UUID id=UUID.randomUUID();shop.stock.put(id,new Stock(id,sample,qty,req.price,1));shop.revision++;});
                }
                case "restock"->{
                    require(mine&&stock!=null,"请选择自己的单价商品。");require(stock.quantity+qty<=AscensionRules.current.stockLimit,"超过该商品库存上限。");require(matching(p,stock.sample)>=qty,"相同附魔、耐久和名称的商品不足。");
                    Transactions.commit(p,"market:restock",()->{remove(p,stock.sample,qty);stock.quantity+=qty;stock.version++;shop.revision++;});
                }
                case "price"->{
                    require(mine&&stock!=null,"旧批次请取回后按新单价上架。");require(req.price>=1&&req.price<=100000000,"无效单价。");Transactions.commit(p,"market:price",()->{stock.price=req.price;stock.version++;shop.revision++;});
                }
                case "buy","take"->{
                    boolean take=req.verb.equals("take");require(take==mine,take?"只有铺主能取回。":"请用取回功能取回自己的商品。");require(stock!=null||legacy!=null,"商品已售出或撤回。");
                    ItemStack sample=stock!=null?stock.sample:legacy.stack();int available=stock!=null?stock.quantity:legacy.stack().getCount();require(legacy!=null||qty<=available,"库存不足。");int amount=legacy!=null?available:qty;require(Gameplay.fits(p,sample.copyWithCount(amount)),"背包空间不足，未扣款。");
                    long total=take?0:stock!=null?Math.multiplyExact(stock.price,amount):legacy.price();Profile buyer=Store.of(p),seller=data.players.get(shop.owner);
                    require(take||buyer.money>=total,"灵石不足。");require(take||seller!=null&&seller.money<=1_000_000_000_000L-total,"铺主账户暂不能入账。");
                    Transactions.commit(p,"market:"+req.verb,()->{if(!take){buyer.money-=total;seller.money+=total;}give(p,sample,amount);if(stock!=null){stock.quantity-=amount;stock.version++;if(stock.quantity==0)shop.stock.remove(stock.id);}else shop.listings.remove(legacy.id());shop.revision++;});
                    var online=p.server.getPlayerList().getPlayer(shop.owner);if(!take&&online!=null)Network.sync(online);
                }
                default->throw new IllegalArgumentException("未知商铺操作。");
            }
            Network.sync(p);open(p,site);
        }catch(IllegalArgumentException|ArithmeticException e){Gameplay.say(p,e.getMessage());open(p,site);}
    }
    static ListTag save(PlayerMarket.Shop shop){ListTag list=new ListTag();for(Stock s:shop.stock.values()){CompoundTag n=new CompoundTag();n.putUUID("id",s.id);n.put("sample",s.sample.save(new CompoundTag()));n.putInt("quantity",s.quantity);n.putLong("price",s.price);n.putLong("version",s.version);list.add(n);}return list;}
    static void load(PlayerMarket.Shop shop,ListTag list){for(int i=0;i<list.size();i++){var n=list.getCompound(i);ItemStack s=ItemStack.of(n.getCompound("sample"));if(!n.hasUUID("id")||s.isEmpty()||n.getInt("quantity")<1||n.getInt("quantity")>4096||n.getLong("price")<1)throw new IllegalArgumentException("Invalid persisted market stock");UUID id=n.getUUID("id");shop.stock.put(id,new Stock(id,s,n.getInt("quantity"),n.getLong("price"),n.getLong("version")));}}
}
