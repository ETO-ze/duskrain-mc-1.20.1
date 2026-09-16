package cn.duskrain;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** The client receives only one bounded page. Price and eligibility always come from the server. */
public final class ShopCatalog {
    public static final int PAGE_SIZE=8;
    static final Map<UUID,Query> OPEN=new HashMap<>();
    public record Query(String category,String kind,int realm,String part,int page){
        String key(){return category+":"+kind+":"+realm+":"+part;}
    }
    static Query parse(String key,int page){
        String[] bits=key.split(":");
        if(bits.length!=4||!Set.of("forge","alchemy","material","elixir","treasure").contains(bits[0])||!Set.of("all","artifact","robe").contains(bits[1])||!Set.of("all","helmet","chestplate","leggings","boots").contains(bits[3]))throw new IllegalArgumentException("无效商品筛选");
        int realm=Integer.parseInt(bits[2]);if(realm< -1||realm>6||page<0||page>100000)throw new IllegalArgumentException("无效页码或境界");
        return new Query(bits[0],bits[1],realm,bits[3],page);
    }
    static List<Rules.Offer> offers(Query q){
        return Rules.current.shops.stream().filter(o->o.shop().equals(q.category))
            .filter(o->q.kind.equals("all")||o.id().startsWith(q.kind+"_"))
            .filter(o->q.realm<0||o.stage()/3==q.realm)
            .filter(o->q.part.equals("all")||o.id().endsWith("_"+q.part)).toList();
    }
    public static void open(ServerPlayer p,String category){
        Query q=OPEN.get(p.getUUID());if(q==null||!q.category.equals(category))q=new Query(category,"all",-1,"all",0);
        page(p,q.key(),q.page);
    }
    static Network.Menu menu(Query requested){
        List<Rules.Offer> all=offers(requested);int pages=Math.max(1,(all.size()+PAGE_SIZE-1)/PAGE_SIZE),page=Math.min(requested.page,pages-1);
        List<Network.Entry> entries=new ArrayList<>();
        for(var o:all.subList(Math.min(page*PAGE_SIZE,all.size()),Math.min((page+1)*PAGE_SIZE,all.size()))){
            var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(o.item()));if(item==null)continue;
            entries.add(DRCommands.card(Boosts.label(o),"shop inspect "+o.id(),o.buy()+"灵石 / "+o.count()+"件 · "+Rules.realm(o.stage())+"解锁",o.item(),true));
        }
        String title=switch(requested.category){case "forge"->"玄岩器阁";case "alchemy"->"青炉丹坊";case "elixir"->"灵药铺";case "treasure"->"珍宝阁";default->"材料铺";};
        return new Network.Menu("灵石商店 · "+title,"按品类、境界和部位筛选；滚轮查看本页商品。购买后保留当前筛选。",entries,
            new Network.Catalog(requested.category,requested.kind,requested.realm,requested.part,page,pages,all.size()));
    }
    public static void page(ServerPlayer p,String key,int page){
        Query q=parse(key,page);if(!Services.require(p,Services.shopService(q.category)))return;
        var menu=menu(q);OPEN.put(p.getUUID(),new Query(q.category,q.kind,q.realm,q.part,menu.catalog().page()));Network.sendMenu(p,menu);
    }
}
