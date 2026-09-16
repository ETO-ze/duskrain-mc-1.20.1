package cn.duskrain;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** UUID-bound upgrades and online-only elixirs. All purchases mutate one server-thread transaction. */
public final class Boosts {
    public static final String[] KEYS={"health","attack","mana","speed"},NAMES={"养元玉","破锋珏","纳灵珠","轻身佩"},ELIXIRS={"轻身露","养元散","破锋散","回灵露"};
    public static final List<RegistryObject<Item>> ITEMS=new ArrayList<>();
    public static final class Config {
        public int durationTicks=24000;
        public int[] elixirPrices={80,100,120,100},rankPrices={1500,3000,6000,10000,16000};
        public double[] elixirGains={.1,4,.1,.25},rankGains={2,.02,20,.01};
        public double pvp=.5;
        void validate(){if(durationTicks<20||durationTicks>72000||elixirPrices.length!=4||rankPrices.length!=5||elixirGains.length!=4||rankGains.length!=4||!Double.isFinite(pvp)||pvp<0||pvp>1)throw new IllegalArgumentException("Boost config bounds");for(int p:elixirPrices)if(p<1||p>10000000)throw new IllegalArgumentException("Elixir price");for(int p:rankPrices)if(p<1||p>10000000)throw new IllegalArgumentException("Rank price");for(double[] values:new double[][]{elixirGains,rankGains})for(double n:values)if(!Double.isFinite(n)||n<0||n>1000)throw new IllegalArgumentException("Boost amount");}
    }
    public static Config config=new Config();
    public static void init(){for(int i=0;i<4;i++){final int index=i;ITEMS.add(Content.ITEMS.register("elixir_"+i,()->new Elixir(index)));}}
    public static void load(){var f=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("duskrain/boosts.json");try{java.nio.file.Files.createDirectories(f.getParent());if(!java.nio.file.Files.exists(f))java.nio.file.Files.writeString(f,Rules.JSON.toJson(config));var next=Rules.JSON.fromJson(java.nio.file.Files.readString(f),Config.class);next.validate();config=next;installOffers();}catch(Exception e){DuskRain.LOG.error("Boost configuration rejected",e);}}
    static void installOffers(){Rules.current.shops.removeIf(o->o.shop().equals("elixir")||o.shop().equals("treasure"));for(int i=0;i<4;i++){Rules.current.shops.add(new Rules.Offer("elixir_"+i,"elixir","duskrain:elixir_"+i,1,config.elixirPrices[i],0,0));for(int rank=0;rank<5;rank++)Rules.current.shops.add(new Rules.Offer("treasure_"+KEYS[i]+"_"+rank,"treasure","minecraft:"+new String[]{"heart_of_the_sea","nether_star","amethyst_shard","feather"}[i],1,config.rankPrices[rank],0,0));}}
    static int[] upgrade(String id){String[] a=id.split("_");if(a.length!=3||!a[0].equals("treasure"))return null;int kind=Arrays.asList(KEYS).indexOf(a[1]);try{int rank=Integer.parseInt(a[2]);return kind>=0&&rank>=0&&rank<5?new int[]{kind,rank}:null;}catch(NumberFormatException e){return null;}}
    static String label(Rules.Offer o){var a=upgrade(o.id());return a==null?Content.label(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(o.item()))):NAMES[a[0]]+" · 强化 "+(a[1]+1)+" 级";}
    static String detail(Rules.Offer o,Profile p){var a=upgrade(o.id());return a==null?"":(p.upgrades[a[0]]>=5?"已满级":p.upgrades[a[0]]==a[1]?"可购买":"需按等级顺序购买")+"；永久"+new String[]{"生命 +2","伤害 +2%","灵力 +20","移速 +1%"}[a[0]]+"；PVP 增益减半";}
    static boolean available(Rules.Offer o,Profile p){var a=upgrade(o.id());return a==null||p.upgrades[a[0]]==a[1];}
    static boolean purchase(ServerPlayer p,String id,int amount,boolean sell){var a=upgrade(id);if(a==null)return false;
        if(!Services.require(p,"treasures"))return true;Profile r=Store.of(p);
        if(sell||amount!=1||r.upgrades[a[0]]!=a[1]){Gameplay.say(p,"强化等级已变化或已满级，请刷新商品；未扣款。");return true;}
        int price=config.rankPrices[a[1]];if(r.money<price){Gameplay.say(p,"灵石不足，未扣款。");return true;}
        r.money-=price;r.upgrades[a[0]]++;Store.get(p.server).setDirty();Skills.attributes(p);Network.sync(p);Gameplay.say(p,NAMES[a[0]]+"已永久强化至 "+r.upgrades[a[0]]+" 级。");ShopCatalog.open(p,"treasure");return true;
    }
    static boolean pvp(Profile p){return p.combatUntil>System.currentTimeMillis();}
    static double permanent(Profile p,int i){return p.upgrades[i]*config.rankGains[i]*(pvp(p)?config.pvp:1);}
    static double temporary(Profile p,int i){return p.elixirs[i]>0?config.elixirGains[i]:0;}
    static double health(Profile p){return permanent(p,0)+temporary(p,1);}
    static double damage(Profile p){return permanent(p,1)+temporary(p,2);}
    static double speed(Profile p){return permanent(p,3)+temporary(p,0);}
    static void tick(ServerPlayer p){Profile r=Store.of(p);boolean changed=false;for(int i=0;i<4;i++)if(r.elixirs[i]>0){r.elixirs[i]--;changed=true;}if(changed&&p.tickCount%20==0)Store.get(p.server).setDirty();}
    static void died(ServerPlayer p){Arrays.fill(Store.of(p).elixirs,0);Store.get(p.server).setDirty();}
    static String summary(ServerPlayer p){Profile r=Store.of(p);return String.format(Locale.ROOT,"生命 %.1f · 移速 %.0f%% · 伤害强化 +%.0f%% · 灵力 %d%s",p.getMaxHealth(),p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)/p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue()*100,damage(r)*100,Skills.manaMax(r),pvp(r)?" · 对战中：永久增益按50%生效":"");}
    public static class Elixir extends Item {
        public final int index;Elixir(int i){super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));index=i;}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player user,InteractionHand hand){ItemStack stack=user.getItemInHand(hand);if(user instanceof ServerPlayer p){if(p.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);Store.of(p).elixirs[index]=config.durationTicks;if(!p.isCreative())stack.shrink(1);p.getCooldowns().addCooldown(this,20);Skills.attributes(p);Store.get(p.server).setDirty();Network.sync(p);Gameplay.say(p,ELIXIRS[index]+"已生效；同类刷新，离线暂停，死亡消失。");}return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
        @Override public void appendHoverText(ItemStack s,Level l,List<Component> out,TooltipFlag f){out.add(Component.literal(new String[]{"移速 +10%","生命 +4","伤害 +10%","回灵 +25%"}[index]));out.add(Component.literal("20分钟在线时间 · 同类刷新 · 死亡清除"));}
    }
}
