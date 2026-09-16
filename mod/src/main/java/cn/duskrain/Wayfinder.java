package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class Wayfinder extends CompassItem {
    public Wayfinder(){super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    public static void update(ItemStack s,ServerPlayer p){var n=s.getOrCreateTag();Profile r=Store.of(p);boolean home=n.getBoolean("DuskRainHome");
        boolean owned=r.hasHome&&r.homeDim.equals("minecraft:overworld")&&Store.get(p.server).claims.containsKey(new net.minecraft.world.level.ChunkPos(BlockPos.containing(r.homeX,r.homeY,r.homeZ)).toLong())&&Store.get(p.server).claims.get(new net.minecraft.world.level.ChunkPos(BlockPos.containing(r.homeX,r.homeY,r.homeZ)).toLong()).owner.equals(p.getUUID());
        if(home&&!owned){home=false;n.putBoolean("DuskRainHome",false);}
        BlockPos pos=home?BlockPos.containing(r.homeX,r.homeY,r.homeZ):new BlockPos(0,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z);
        n.put("LodestonePos",NbtUtils.writeBlockPos(pos));n.putString("LodestoneDimension",home?r.homeDim:Gameplay.CITY.location().toString());n.putBoolean("LodestoneTracked",false);
        n.putString("DuskRainDestination",home?"住宅领地":"烟雨主城");
    }
    @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(e instanceof ServerPlayer p&&p.tickCount%20==0)update(s,p);}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player player,InteractionHand hand){ItemStack s=player.getItemInHand(hand);if(player instanceof ServerPlayer p){if(p.isShiftKeyDown())s.getOrCreateTag().putBoolean("DuskRainHome",!s.getOrCreateTag().getBoolean("DuskRainHome"));update(s,p);var n=s.getOrCreateTag();BlockPos pos=NbtUtils.readBlockPos(n.getCompound("LodestonePos"));boolean same=p.level().dimension().location().toString().equals(n.getString("LodestoneDimension"));Gameplay.say(p,n.getString("DuskRainDestination")+"："+pos.getX()+" / "+pos.getY()+" / "+pos.getZ()+(same?"；罗盘指针正在引路。":"；目标在另一维度，请通过 /dr warp 或 /dr home 前往。"));}return InteractionResultHolder.sidedSuccess(s,l.isClientSide);}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> out,TooltipFlag f){out.add(Component.literal("§6潜行+右键：主城 / 自家领地切换"));out.add(Component.literal("§7右键查看坐标；先认领并 /dr home set"));if(s.hasTag())out.add(Component.literal("§b当前目标："+s.getTag().getString("DuskRainDestination")));}
}
