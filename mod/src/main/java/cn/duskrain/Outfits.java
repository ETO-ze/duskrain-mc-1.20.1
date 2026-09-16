package cn.duskrain;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** Eighteen wearable four-piece robes. Equipment permission is enforced server-side. */
public final class Outfits {
    public static final List<RegistryObject<Item>> ALL=new ArrayList<>();
    public static final String[] NAMES={"听雨布衣","青竹道袍","白玉云衣","玄霜法衣","流霞仙服","问劫天衣","太初神衣"};
    static {for(int stage=0;stage<21;stage++)for(ArmorItem.Type type:ArmorItem.Type.values()){
        final int s=stage;ALL.add(Content.ITEMS.register(id(s,type),()->new Robe(s,type)));
    }}
    public static void init(){}
    public static String id(int stage,ArmorItem.Type type){return "robe_"+stage+"_"+type.getName();}
    public static Item item(int stage,ArmorItem.Type type){return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(DuskRain.ID,id(stage,type)));}
    public static String label(int stage,ArmorItem.Type type){return NAMES[stage/3]+"·"+new String[]{"初境","中境","后境"}[stage%3]+"·"+switch(type){case HELMET->"发冠";case CHESTPLATE->"上衣";case LEGGINGS->"下裳";case BOOTS->"云履";};}
    public record Cloth(int stage) implements ArmorMaterial {
        public int getDurabilityForType(ArmorItem.Type t){return (new int[]{11,16,15,13}[t.ordinal()])*(8+stage*3);}
        public int getDefenseForType(ArmorItem.Type t){int tier=stage/3;return switch(t){case HELMET,BOOTS->1+tier/2;case CHESTPLATE->3+tier;case LEGGINGS->2+(tier*4)/5;};}
        public int getEnchantmentValue(){return 12+stage/3;}
        public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_LEATHER;}
        public Ingredient getRepairIngredient(){return Ingredient.of(Items.STRING);}
        public String getName(){return "duskrain:robe_"+stage;}
        public float getToughness(){return stage/3*.35f;}
        public float getKnockbackResistance(){return stage/3*.008f;}
    }
    public static final class Robe extends ArmorItem {
        public final int stage;
        Robe(int s,Type t){super(new Cloth(s),t,new Item.Properties().rarity(s>=12?Rarity.EPIC:s>=6?Rarity.RARE:Rarity.UNCOMMON));stage=s;}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){return RobeModels.model(entity,slot,stage,original);}});}
        @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String type){return "duskrain:textures/models/armor/robe_"+stage+"_layer_"+(slot==EquipmentSlot.LEGS?2:1)+".png";}
        @Override public boolean canEquip(ItemStack stack,EquipmentSlot slot,Entity entity){return super.canEquip(stack,slot,entity)&&(!(entity instanceof ServerPlayer p)||Store.of(p).stage>=stage);}
        @Override public void appendHoverText(ItemStack s,Level l,List<Component> out,TooltipFlag f){out.add(Component.literal("§b"+Rules.realm(stage)+"可穿戴 · 中式交领常服"));out.add(Component.literal("§7装备中的护甲死亡保留；丝线可修补"));}
    }
    @SubscribeEvent public void equipment(LivingEquipmentChangeEvent e){if(e.getEntity() instanceof ServerPlayer p&&e.getSlot().isArmor()&&e.getTo().getItem() instanceof Robe robe&&Store.of(p).stage<robe.stage){ItemStack s=e.getTo().copy();p.setItemSlot(e.getSlot(),ItemStack.EMPTY);Gameplay.giveOrRetain(p,s);Gameplay.say(p,"境界不足：需要"+Rules.realm(robe.stage)+"，法衣已退回背包。");}}
    public static void enforce(ServerPlayer p){for(EquipmentSlot slot:EquipmentSlot.values())if(slot.isArmor()&&p.getItemBySlot(slot).getItem() instanceof Robe robe&&Store.of(p).stage<robe.stage){ItemStack s=p.getItemBySlot(slot).copy();p.setItemSlot(slot,ItemStack.EMPTY);Gameplay.giveOrRetain(p,s);}}
}
