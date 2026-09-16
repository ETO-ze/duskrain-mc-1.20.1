package cn.duskrain;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.*;
import java.util.*;

public final class Content {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,DuskRain.ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,DuskRain.ID);
    public static final RegistryObject<Item> WAYFINDER=ITEMS.register("wayfinder",Wayfinder::new);
    public static final List<RegistryObject<Item>> ARTIFACTS=new ArrayList<>(),PILLS=new ArrayList<>();
    static {
        for(int s=1;s<=3;s++)for(int t=0;t<6;t++){final int school=s,tier=t;ARTIFACTS.add(ITEMS.register("artifact_"+s+"_"+t,()->new Artifact(school,tier)));}
        for(int s=1;s<=3;s++){final int school=s;ARTIFACTS.add(ITEMS.register("artifact_"+s+"_6",()->new Artifact(school,6)));}
        for(int i=0;i<6;i++){final int n=i;PILLS.add(ITEMS.register("pill_"+i,()->new Pill(n)));}
        TABS.register("cultivation",()->CreativeModeTab.builder().title(Component.literal("DuskRain · 烟雨仙途")).icon(()->new ItemStack(ARTIFACTS.get(0).get())).displayItems((p,o)->{ARTIFACTS.forEach(a->o.accept(a.get()));PILLS.forEach(a->o.accept(a.get()));Boosts.ITEMS.forEach(a->o.accept(a.get()));Outfits.ALL.forEach(a->o.accept(a.get()));o.accept(MysticEnchants.SAND.get());o.accept(Ascension.CORE.get());o.accept(Ascension.CRYSTAL.get());o.accept(WAYFINDER.get());o.accept(Decor.EXPLORER.get());}).build());
    }
    public static boolean owned(ItemStack s,Player p){return !(s.getItem() instanceof Artifact)||!s.hasTag()||!s.getTag().hasUUID("DuskRainOwner")||s.getTag().getUUID("DuskRainOwner").equals(p.getUUID());}
    static final Map<String,String> NAMES=Map.ofEntries(Map.entry("bread","面包"),Map.entry("oak_log","橡木原木"),Map.entry("cobblestone","圆石"),Map.entry("coal","煤炭"),Map.entry("iron_ingot","铁锭"),Map.entry("gold_ingot","金锭"),Map.entry("redstone","红石粉"),Map.entry("lapis_lazuli","青金石"),Map.entry("diamond","钻石"),Map.entry("ender_pearl","末影珍珠"),Map.entry("blaze_rod","烈焰棒"),Map.entry("amethyst_shard","紫水晶碎片"),Map.entry("wheat","小麦"),Map.entry("nether_star","下界之星"),Map.entry("ender_eye","末影之眼"));
    public static String label(Item item){if(item==MysticEnchants.SAND.get())return "灵纹砂";if(item==Ascension.CORE.get())return "劫雷灵核";if(item==Ascension.CRYSTAL.get())return "神纹晶";if(item instanceof Boosts.Elixir e)return Boosts.ELIXIRS[e.index];if(item instanceof Outfits.Robe r)return Outfits.label(r.stage,r.getType());if(item instanceof Wayfinder)return "归云罗盘";if(item instanceof Artifact a)return Rules.current.realms[a.tier]+"·"+new String[]{"","听雨剑","凝霜杖","镇岳印"}[a.school];if(item instanceof Pill p)return new String[]{"回灵丹","九转回灵丹","生肌丹","玉露生肌丹","培元丹","紫府培元丹"}[p.index];var id=ForgeRegistries.ITEMS.getKey(item);return id==null?"未知物品":NAMES.getOrDefault(id.getPath(),item.getDescription().getString());}
    public static void bind(ItemStack s,Player p){if(s.getItem() instanceof Artifact&&!s.getOrCreateTag().hasUUID("DuskRainOwner")){s.getOrCreateTag().putUUID("DuskRainOwner",p.getUUID());s.getOrCreateTag().putString("DuskRainOwnerName",p.getGameProfile().getName());}}
    public static final class Artifact extends SwordItem {
        public final int school,tier;
        Artifact(int school,int tier){super(Tiers.DIAMOND,2+tier*2,school==3?-2.4f:-2.2f,new Item.Properties().durability(1561+tier*300).rarity(tier>=4?Rarity.EPIC:tier>=2?Rarity.RARE:Rarity.UNCOMMON));this.school=school;this.tier=tier;}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(player instanceof ServerPlayer p)Gameplay.cast(p,0);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
        @Override public void inventoryTick(ItemStack s,Level level,net.minecraft.world.entity.Entity e,int slot,boolean selected){if(!level.isClientSide&&e instanceof Player p)bind(s,p);}
        @Override public void appendHoverText(ItemStack s,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.literal("§b"+Rules.current.schools[school]+" · "+Rules.current.realms[tier]+"法器"));lines.add(Component.literal("§7右键施展第一主动技能；Z / X / C 切换施法"));if(s.hasTag()&&s.getTag().contains("DuskRainOwnerName"))lines.add(Component.literal("§6本命绑定："+s.getTag().getString("DuskRainOwnerName")));}
    }
    public static final class Pill extends Item {
        public final int index;
        Pill(int i){super(new Item.Properties().stacksTo(16).rarity(i%2==1?Rarity.RARE:Rarity.UNCOMMON));index=i;}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            ItemStack stack=player.getItemInHand(hand);
            if(player instanceof ServerPlayer p){Profile r=Store.of(p);int tier=index%2;if(r.stage<tier*6){Gameplay.say(p,"境界不足：需要金丹境。");return InteractionResultHolder.fail(stack);}if(p.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
                if(index/2==0)r.mana=Math.min(Skills.manaMax(r),r.mana+(tier+1)*50);
                else if(index/2==1)p.heal((tier+1)*10);
                else {p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,600*(tier+1),tier));p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,600*(tier+1),tier));}
                if(!p.isCreative())stack.shrink(1);p.getCooldowns().addCooldown(this,200);Network.sync(p);
            }return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        @Override public void appendHoverText(ItemStack s,Level l,List<Component> out,TooltipFlag f){out.add(Component.literal(new String[]{"恢复50灵力","恢复100灵力 · 金丹可用","恢复10生命","恢复20生命 · 金丹可用","30秒调息与护体","60秒调息与护体 · 金丹可用"}[index]));out.add(Component.literal("§7丹药冷却：10秒"));}
    }
}
