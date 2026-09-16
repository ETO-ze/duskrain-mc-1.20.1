package cn.duskrain;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** The chosen item remains in its real inventory slot, including across disconnects. */
public final class Enchanting {
    public record Choice(int id,int next,long price,int lapis,int sand,int levels,int stage,boolean enabled,String reason,double effect){}
    public record View(long quote,int slot,ItemStack item,long money,List<Choice> choices){}
    public record Request(boolean apply,int slot,int enchant,long quote){}
    record Quote(long id,int slot,ItemStack original,long expires){}
    static final Map<UUID,Quote> QUOTES=new HashMap<>();
    static long sequence=System.currentTimeMillis();
    static boolean location(ServerPlayer p){return p.isAlive()&&Services.require(p,"enchanter");}
    public static void open(ServerPlayer p){if(location(p))select(p,-1);}
    static void send(ServerPlayer p,View v){V14Network.to(p,v);}
    static List<Choice> choices(ServerPlayer p,ItemStack s){
        List<Choice> list=new ArrayList<>();var c=AscensionRules.current;Profile r=Store.of(p);
        for(int i=0;i<8;i++){if(!MysticEnchants.fits(i,s))continue;int old=MysticEnchants.level(s,i),k=old+1;long price=(long)c.enchantPrice*k*k;int lapis=c.enchantLapis*k,sand=c.enchantSand*k,levels=c.enchantLevels*k,stage=3*(k-1);
            String reason=old>=c.enchantMax[i]?"已满级":!Content.owned(s,p)?"法器属于其他玩家":MysticEnchants.count(s)>=3&&old==0?"最多三个专属词条":r.stage<stage?"境界不足":r.money<price?"灵石不足":p.experienceLevel<levels?"原版经验等级不足":Gameplay.count(p,Items.LAPIS_LAZULI)<lapis?"青金石不足":Gameplay.count(p,MysticEnchants.SAND.get())<sand?"灵纹砂不足":"";
            list.add(new Choice(i,k,price,lapis,sand,levels,stage,reason.isEmpty(),reason,c.enchantPower[i]*k));
        }return list;
    }
    public static void select(ServerPlayer p,int slot){
        if(!location(p))return;if(slot<0||slot>=36){QUOTES.remove(p.getUUID());send(p,new View(0,-1,ItemStack.EMPTY,Store.of(p).money,List.of()));return;}
        ItemStack s=p.getInventory().getItem(slot);Quote q=new Quote(++sequence,slot,s.copy(),System.currentTimeMillis()+60000);QUOTES.put(p.getUUID(),q);send(p,new View(q.id,slot,s.copy(),Store.of(p).money,choices(p,s)));
    }
    public static void handle(ServerPlayer p,Request request){
        if(!location(p))return;if(!request.apply){select(p,request.slot);return;}
        Quote q=QUOTES.get(p.getUUID());if(q==null||q.id!=request.quote||q.slot!=request.slot||q.expires<System.currentTimeMillis()){Gameplay.say(p,"报价已更新，请重新选择装备。");return;}
        ItemStack s=p.getInventory().getItem(q.slot);
        if(!ItemStack.matches(s,q.original)){Gameplay.say(p,"装备已变化，请重新选择。");select(p,q.slot);return;}
        var selected=choices(p,s).stream().filter(x->x.id==request.enchant).findFirst();if(selected.isEmpty()||!selected.get().enabled){Gameplay.say(p,selected.isEmpty()?"此装备不适用该附魔。":selected.get().reason);select(p,q.slot);return;}
        Choice c=selected.get();QUOTES.remove(p.getUUID());
        if(Transactions.commit(p,"enchant:"+MysticEnchants.IDS[c.id],()->{
            Profile r=Store.of(p);r.money-=c.price;Gameplay.take(p,Items.LAPIS_LAZULI,c.lapis);Gameplay.take(p,MysticEnchants.SAND.get(),c.sand);p.giveExperienceLevels(-c.levels);
            var ench=new HashMap<>(EnchantmentHelper.getEnchantments(s));ench.put(MysticEnchants.ALL.get(c.id).get(),c.next);EnchantmentHelper.setEnchantments(ench,s);p.getInventory().setChanged();
        })){Gameplay.say(p,"铭刻成功："+MysticEnchants.NAMES[c.id]+" "+c.next+"级。");Skills.attributes(p);Network.sync(p);}
        select(p,q.slot);
    }
}
