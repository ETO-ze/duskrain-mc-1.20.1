package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Isolated first-login reception. Existing UUIDs migrate without replaying grants. */
public final class Onboarding {
    public static final int GUIDE_VERSION=15;
    static final Set<UUID> SHOWN=new HashSet<>();
    public static void prepare(MinecraftServer server){ServerLevel l=server.getLevel(Gameplay.INTRO);if(l==null)return;
        BlockPos mark=new BlockPos(0,80,0);l.getChunkAt(mark);if(l.getBlockState(mark).is(Blocks.CHISELED_QUARTZ_BLOCK))return;
        for(int x=-17;x<=17;x++)for(int z=-17;z<=17;z++){
            l.getChunk(x>>4,z>>4);boolean rim=Math.max(Math.abs(x),Math.abs(z))==17;
            l.setBlock(new BlockPos(x,79,z),Blocks.QUARTZ_BRICKS.defaultBlockState(),3);
            l.setBlock(new BlockPos(x,80,z),(rim||Math.floorMod(x,6)==0&&Math.floorMod(z,6)==0?Blocks.SEA_LANTERN:Blocks.SMOOTH_QUARTZ).defaultBlockState(),3);
            if(rim)l.setBlock(new BlockPos(x,81,z),Blocks.DARK_OAK_FENCE.defaultBlockState(),3);
        }
        for(int x:new int[]{-12,12})for(int z:new int[]{-12,12}){for(int y=81;y<=87;y++)l.setBlock(new BlockPos(x,y,z),Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),3);l.setBlock(new BlockPos(x,88,z),Decor.EAVE.get().defaultBlockState(),3);l.setBlock(new BlockPos(x,86,z-1),Decor.LANTERN.get().defaultBlockState(),3);}
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)if(Math.abs(x)>=11||Math.abs(z)>=11)l.setBlock(new BlockPos(x,88,z),Blocks.DARK_PRISMARINE.defaultBlockState(),3);
        for(int x:new int[]{-8,8})for(int z:new int[]{-8,8}){l.setBlock(new BlockPos(x,81,z),Blocks.MOSS_BLOCK.defaultBlockState(),3);l.setBlock(new BlockPos(x,82,z),Blocks.FLOWERING_AZALEA.defaultBlockState(),3);}
        l.setBlock(mark,Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(),3);
        l.setDayTime(1000);l.setDefaultSpawnPos(mark.above(),180);
    }
    public static void arrive(ServerPlayer p){prepare(p.server);var l=p.server.getLevel(Gameplay.INTRO);if(l==null){Gameplay.spawn(p);return;}TeleportEffects.travel(p,l,new Vec3(.5,81,.5),180);p.setRespawnPosition(Gameplay.INTRO,new BlockPos(0,81,0),180,true,false);grant(p);SHOWN.remove(p.getUUID());}
    public static void grant(ServerPlayer p){Profile r=Store.of(p);if(r.starterGranted)return;r.starterGranted=true;Store.get(p.server).setDirty();
        for(ArmorItem.Type type:ArmorItem.Type.values()){ItemStack s=new ItemStack(Outfits.item(0,type));EquipmentSlot slot=type.getSlot();if(p.getItemBySlot(slot).isEmpty())p.setItemSlot(slot,s);else Gameplay.giveOrRetain(p,s);}
        Gameplay.giveOrRetain(p,book());ItemStack torch=new ItemStack(Decor.EXPLORER.get());if(p.getOffhandItem().isEmpty())p.setItemSlot(EquipmentSlot.OFFHAND,torch);else Gameplay.giveOrRetain(p,torch);
        ItemStack compass=new ItemStack(Content.WAYFINDER.get());Wayfinder.update(compass,p);Gameplay.giveOrRetain(p,compass);p.getInventory().setChanged();
    }
    public static ItemStack book(){ItemStack book=new ItemStack(Items.WRITTEN_BOOK);CompoundTag n=book.getOrCreateTag();n.putString("title","DuskRain · 入世手册");n.putString("author","DuskRain · 烟雨仙途");n.putBoolean("resolved",true);n.putBoolean("DuskRainGuide",true);n.putInt("DuskRainGuideVersion",GUIDE_VERSION);ListTag pages=new ListTag();
        try(var in=Onboarding.class.getResourceAsStream("/data/duskrain/guide.json")){String[] text=Rules.JSON.fromJson(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8),String[].class);for(String t:text)pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(t))));}catch(Exception e){throw new IllegalStateException("Guide unavailable",e);}
        MutableComponent last=Component.literal("准备启程\n\n阅读后，点击下方按钮进入主城。\n\n").append(Component.literal("【启程 · 进入烟雨主城】").withStyle(s->s.withColor(0x286d65).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/dr start"))));pages.add(StringTag.valueOf(Component.Serializer.toJson(last)));n.put("pages",pages);return book;
    }
    static boolean isGuide(ItemStack stack){return stack.is(Items.WRITTEN_BOOK)&&stack.hasTag()&&stack.getTag().getBoolean("DuskRainGuide");}
    static void refreshBooks(net.minecraft.world.Container container){for(int i=0;i<container.getContainerSize();i++){ItemStack stack=container.getItem(i);if(isGuide(stack)&&stack.getTag().getInt("DuskRainGuideVersion")<GUIDE_VERSION){stack.setTag(book().getTag().copy());container.setChanged();}}}
    public static void refreshGuide(ServerPlayer p){refreshBooks(p.getInventory());refreshBooks(p.getEnderChestInventory());}
    public static void guide(ServerPlayer p){refreshGuide(p);for(int i=0;i<p.getInventory().getContainerSize();i++)if(isGuide(p.getInventory().getItem(i))){Gameplay.say(p,"手册已更新并在背包或副手中；手持右键阅读，最后一页可点击启程。");return;}Gameplay.giveOrRetain(p,book());Gameplay.say(p,"已补发最新版《入世手册》，手持右键阅读。");}
    public static void menu(ServerPlayer p){Network.menu(p,"入世 · 烟雨迎客台","欢迎来到 DuskRain。新手法衣已穿戴，探索火把放在副手；请阅读手册，再点击启程。群号 205255670。",List.of(DRCommands.card("查看入世手册","guide","点击直接阅读；手册也会一直保留在背包中。","minecraft:written_book",true),DRCommands.card("启程 · 进入主城","start","将默认重生点设到烟雨主城；有效床或重生锚仍可覆盖。","duskrain:wayfinder",true)));}
    public static void start(ServerPlayer p){Profile r=Store.of(p);if(r.onboardingComplete){Gameplay.say(p,"你已启程，使用 /dr spawn 引导回城。");return;}if(!p.level().dimension().equals(Gameplay.INTRO)){Gameplay.say(p,"请先返回迎客台完成引导。");return;}grant(p);r.onboardingComplete=true;Store.get(p.server).setDirty();p.setRespawnPosition(Gameplay.CITY,new BlockPos(0,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z),180,true,false);Gameplay.spawn(p);Gameplay.say(p,"已启程！默认重生点设为烟雨主城，按 G 查看菜单。可前往问道书院拜师。");}
    public static void tick(ServerPlayer p){Profile r=Store.of(p);if(!r.initialized||r.onboardingComplete)return;if(!p.level().dimension().equals(Gameplay.INTRO)){arrive(p);return;}p.getFoodData().setFoodLevel(20);if(p.getY()<70)arrive(p);if(p.tickCount>40&&SHOWN.add(p.getUUID()))menu(p);}
}
