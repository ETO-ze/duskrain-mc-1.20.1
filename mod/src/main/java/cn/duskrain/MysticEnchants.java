package cn.duskrain;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Real enchantments: ordinary book/anvil/grindstone data remains interoperable. */
public final class MysticEnchants {
    public static final DeferredRegister<Enchantment> REGISTRY=DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,DuskRain.ID);
    public static final String[] IDS={"sword_echo","arcane","mountain","spirit","renewal","heartguard","cloudstep","windrider"};
    public static final String[] NAMES={"剑鸣","通玄","镇岳","聚灵","归元","护心","踏云","御风"};
    public static final String[] DESCRIPTIONS={"普攻剑气伤害","术修主动伤害","体修主动伤害","灵力上限","回灵速度","额外减伤","摔落减免","御剑耗灵减免"};
    public static final List<RegistryObject<Enchantment>> ALL=new ArrayList<>();
    public static final RegistryObject<Item> SAND=Content.ITEMS.register("rune_sand",()->new Item(new Item.Properties().rarity(Rarity.RARE)));
    static{for(int i=0;i<8;i++){final int index=i;ALL.add(REGISTRY.register(IDS[i],()->new Inscription(index)));}}
    public static void init(){}
    public static boolean fits(int i,ItemStack stack){
        Item item=stack.getItem();
        return switch(i){
            case 0->item instanceof SwordItem&&(!(item instanceof Content.Artifact a)||a.school==1);
            case 1,2->item instanceof Content.Artifact a&&a.school==i+1;
            case 3,5->item instanceof ArmorItem a&&a.getType()==ArmorItem.Type.CHESTPLATE;
            case 4->item instanceof ArmorItem a&&a.getType()==ArmorItem.Type.HELMET;
            case 6,7->item instanceof ArmorItem a&&a.getType()==ArmorItem.Type.BOOTS;
            default->false;
        };
    }
    static final class Inscription extends Enchantment {
        final int index;
        Inscription(int i){super(Rarity.RARE,EnchantmentCategory.BREAKABLE,EquipmentSlot.values());index=i;}
        @Override public int getMaxLevel(){return AscensionRules.current.enchantMax[index];}
        @Override public boolean canEnchant(ItemStack s){return fits(index,s);}
        @Override public boolean canApplyAtEnchantingTable(ItemStack s){return false;}
        @Override public boolean isDiscoverable(){return false;}
        @Override public boolean isTradeable(){return false;}
        @Override public boolean isTreasureOnly(){return true;}
        @Override public boolean isAllowedOnBooks(){return true;}
    }
    public static int count(ItemStack s){int n=0;for(var e:ALL)if(EnchantmentHelper.getItemEnchantmentLevel(e.get(),s)>0)n++;return n;}
    public static int level(ItemStack s,int i){return EnchantmentHelper.getItemEnchantmentLevel(ALL.get(i).get(),s);}
    public static int level(ServerPlayer p,int i){
        EquipmentSlot slot=switch(i){case 0,1,2->EquipmentSlot.MAINHAND;case 3,5->EquipmentSlot.CHEST;case 4->EquipmentSlot.HEAD;default->EquipmentSlot.FEET;};
        ItemStack s=p.getItemBySlot(slot);return fits(i,s)?Math.min(AscensionRules.current.enchantMax[i],level(s,i)):0;
    }
    public static double effect(ServerPlayer p,int i,boolean pvp){return level(p,i)*AscensionRules.current.enchantPower[i]*(pvp&&i!=6&&i!=7?AscensionRules.current.enchantPvp:1);}
    public static boolean legal(ItemStack s){if(count(s)>3)return false;for(int i=0;i<8;i++){int n=level(s,i);if(n>0&&(!fits(i,s)||n>AscensionRules.current.enchantMax[i]))return false;}return true;}
    @SubscribeEvent public void anvil(AnvilUpdateEvent e){
        Map<Enchantment,Integer> merged=new HashMap<>(EnchantmentHelper.getEnchantments(e.getLeft()));var right=EnchantmentHelper.getEnchantments(e.getRight());
        right.forEach((en,n)->{if(en instanceof Inscription){int old=merged.getOrDefault(en,0);merged.put(en,Math.min(en.getMaxLevel(),old==n?n+1:Math.max(old,n)));}});
        boolean book=e.getLeft().is(Items.ENCHANTED_BOOK);if(merged.keySet().stream().filter(x->x instanceof Inscription).count()>3||!book&&merged.keySet().stream().anyMatch(x->x instanceof Inscription ins&&!fits(ins.index,e.getLeft())))e.setCanceled(true);
    }
    @SubscribeEvent public void fall(LivingFallEvent e){if(e.getEntity() instanceof ServerPlayer p)e.setDamageMultiplier((float)(e.getDamageMultiplier()*(1-Math.min(.9,effect(p,6,false)))));}
}
