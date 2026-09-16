package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class Gameplay {
    public static final ResourceKey<Level> CITY=key("duskrain:city"),INTRO=key("duskrain:arrival"),TRIAL=key("duskrain:trial"),DEMO=key("duskrain:construction");
    public static ResourceKey<Level> key(String id){return ResourceKey.create(Registries.DIMENSION,new ResourceLocation(id));}
    static final UUID HEALTH=UUID.fromString("a13882a0-07ab-4b25-86b5-b2c4be61be71"),ATTACK=UUID.fromString("a13882a0-07ab-4b25-86b5-b2c4be61be72"),SPEED=UUID.fromString("a13882a0-07ab-4b25-86b5-b2c4be61be73");
    static final Map<UUID,long[]> COOLDOWNS=new HashMap<>();
    static final Map<UUID,Recall> RECALL=new HashMap<>();
    record Recall(ResourceKey<Level> origin,Vec3 from,ResourceKey<Level> target,Vec3 to,long due) {}
    public static void say(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("§b[DuskRain] §r"+s));Network.notice(p,s);}
    public static String region(ServerPlayer p){
        if(p.level().dimension().equals(Guilds.DIM))return "帮派洞天";
        if(p.level().dimension().equals(INTRO))return "烟雨迎客台";
        if(p.level().dimension().equals(DEMO))return "建造演示";
        if(p.level().dimension().equals(TRIAL))return "问劫试炼";
        if(p.level().dimension().equals(CITY)){CityPlan.Building b=nearBuilding(p);return b==null?"烟雨仙城":b.name();}
        if(p.level().dimension().equals(Level.OVERWORLD)){Store.Claim c=Store.get(p.server).claims.get(p.chunkPosition().toLong());if(c!=null)return c.owner.equals(p.getUUID())?"自家领地":"住宅领地";}
        return p.level().dimension().equals(Level.NETHER)?"下界荒境":p.level().dimension().equals(Level.END)?"末地荒境":"争夺野外";
    }
    public static CityPlan.Building nearBuilding(ServerPlayer p){return CityPlan.BUILDINGS.stream().filter(b->Math.abs(p.getX()-b.x())<b.w()+10&&Math.abs(p.getZ()-b.z())<b.d()+14).min(java.util.Comparator.comparingDouble(b->Math.pow(p.getX()-b.x(),2)+Math.pow(p.getZ()-b.z(),2))).orElse(null);}
    public static void spawn(ServerPlayer p){ServerLevel l=p.server.getLevel(CITY);if(l==null){say(p,"主城维度未加载，请检查模组与数据包。");return;}TeleportEffects.travel(p,l,new Vec3(.5,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z+.5),180);}
    public static void survival(ServerPlayer p){
        WildernessTravel.request(p);
    }
    public static void recall(ServerPlayer p,ResourceKey<Level> key,Vec3 to){
        if(SwordFlight.flying(p)){say(p,"请先收剑落地，再引导传送。");return;}Profile r=Store.of(p);if(r.combatUntil>System.currentTimeMillis()){say(p,"战斗尚未结束，暂时不能传送。");return;}
        if(Trials.inTrial(p.getUUID())){say(p,"请先通过 /dr trial leave 退出试炼。");return;}
        if(RECALL.containsKey(p.getUUID())){say(p,"正在引导传送。");return;}
        RECALL.put(p.getUUID(),new Recall(p.level().dimension(),p.position(),key,to,System.currentTimeMillis()+Rules.current.recallSeconds*1000L));r.meditating=false;say(p,Rules.current.recallSeconds+"秒后传送；移动或受伤将取消。");
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){Transactions.login(p);Profile r=Store.of(p);if(!r.initialized){r.initialized=true;r.cityVersion=3;Onboarding.arrive(p);say(p,"欢迎来到 DuskRain · 烟雨仙途！群号 205255670。按 G 打开主菜单。");}else if(p.level().dimension().equals(TRIAL))spawn(p);if(r.cityVersion<2){if(p.level().dimension().equals(CITY))spawn(p);r.mainProgress=0;r.cityVersion=2;}if(r.cityVersion<3){if(p.level().dimension().equals(CITY))spawn(p);r.cityVersion=3;}if(!r.onboardingComplete&&!p.level().dimension().equals(INTRO))Onboarding.arrive(p);r.meditating=false;r.arena=false;restore(p);Network.sync(p);}}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){Store.of(p).meditating=false;Store.of(p).arena=false;RECALL.remove(p.getUUID());WildernessTravel.cancel(p.getUUID());CityMobility.clear(p.getUUID());Onboarding.SHOWN.remove(p.getUUID());Trials.leave(p,true);ShopCatalog.OPEN.remove(p.getUUID());Actions.SEEN.remove(p.getUUID());Network.LAST_ACTION.remove(p.getUUID());Skills.clear(p.getUUID());Enchanting.QUOTES.remove(p.getUUID());Protection.MAINTENANCE.remove(p.getUUID());}}
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){restore(p);if(p.level().dimension().equals(Guilds.DIM)&&!Guilds.allowed(p,p.position())){Gameplay.spawn(p);return;}if(!Store.of(p).onboardingComplete){Onboarding.arrive(p);Network.sync(p);return;}if(p.getRespawnPosition()==null||p.level().dimension().equals(TRIAL)||p.level().dimension().equals(CITY))spawn(p);Network.sync(p);}}
    @SubscribeEvent public void changed(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){Profile r=Store.of(p);r.meditating=false;r.arena=false;Network.sync(p);}}
    static void restore(ServerPlayer p){Profile r=Store.of(p);if(r.retained.isEmpty())return;ListTag left=new ListTag();for(int i=0;i<r.retained.size();i++){CompoundTag n=r.retained.getCompound(i);ItemStack item=ItemStack.of(n.getCompound("item"));int slot=n.getInt("slot");if(slot>=0&&slot<p.getInventory().getContainerSize()&&p.getInventory().getItem(slot).isEmpty())p.getInventory().setItem(slot,item);else{p.getInventory().add(item);if(!item.isEmpty()){CompoundTag remaining=n.copy();remaining.put("item",item.save(new CompoundTag()));left.add(remaining);}}}r.retained=left;Store.get(p.server).setDirty();}
    static void giveOrRetain(ServerPlayer p,ItemStack reward){ItemStack item=reward.copy();p.getInventory().add(item);if(!item.isEmpty()){CompoundTag n=new CompoundTag();n.putInt("slot",-1);n.put("item",item.save(new CompoundTag()));Store.of(p).retained.add(n);Store.get(p.server).setDirty();say(p,"背包已满，剩余奖励已保存；腾出空间后会自动补发。");}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void death(LivingDeathEvent e){
        if(e.isCanceled())return;
        if(e.getEntity() instanceof ServerPlayer p){Profile r=Store.of(p);r.meditating=false;r.arena=false;RECALL.remove(p.getUUID());WildernessTravel.cancel(p.getUUID());CityMobility.clear(p.getUUID());Skills.clear(p.getUUID());Boosts.died(p);
            if(!p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))for(int slot=0;slot<p.getInventory().getContainerSize();slot++){ItemStack stack=p.getInventory().getItem(slot);if(!stack.isEmpty()&&((slot>=36&&slot<40)||stack.getItem() instanceof Content.Artifact&&Content.owned(stack,p))){CompoundTag n=new CompoundTag();n.putInt("slot",slot);n.put("item",stack.save(new CompoundTag()));r.retained.add(n);p.getInventory().setItem(slot,ItemStack.EMPTY);}}
            SwordFlight.stop(p);Trials.died(p);
        }else if(e.getEntity() instanceof Monster&&e.getSource().getEntity() instanceof ServerPlayer p){Profile r=Store.of(p);r.kills++;r.xp=Math.min(Integer.MAX_VALUE-1000,r.xp+(int)((8+r.stage)*(r.school==2?1.2:1)));String id=ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType()).toString();progress(p,"kill",id);}
    }
    @SubscribeEvent public void hurt(LivingHurtEvent e){

        if(e.getEntity() instanceof ServerPlayer p){RECALL.remove(p.getUUID());WildernessTravel.cancel(p.getUUID());Profile r=Store.of(p);r.meditating=false;
            if(e.getSource().getEntity() instanceof ServerPlayer attacker){long end=System.currentTimeMillis()+Rules.current.combatSeconds*1000L;r.combatUntil=end;Store.of(attacker).combatUntil=end;}
            Skills.attributes(p);
        }
    }
    @SubscribeEvent public void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p){ItemStack s=p.getMainHandItem();if(s.getItem() instanceof Content.Artifact a){Profile r=Store.of(p);if(r.stage<a.tier*3||r.school!=a.school||!Content.owned(s,p)){e.setCanceled(true);say(p,"法器的境界、流派或绑定条件不符。");}}}}
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        Onboarding.tick(p);if(p.tickCount%40==0)Onboarding.refreshGuide(p);CityMobility.tick(p);Outfits.enforce(p);Boosts.tick(p);
        Recall q=RECALL.get(p.getUUID());if(q!=null){if(p.tickCount%10==0)TeleportEffects.circle(p.serverLevel(),p.position(),p.tickCount*.07,false);if(!q.origin.equals(p.level().dimension())||q.from.distanceToSqr(p.position())>.04){RECALL.remove(p.getUUID());say(p,"移动中，传送已取消。");}else if(System.currentTimeMillis()>=q.due){RECALL.remove(p.getUUID());ServerLevel target=p.server.getLevel(q.target);if(target!=null){target.getChunkAt(BlockPos.containing(q.to));if(q.target.equals(Guilds.DIM)&&!Guilds.allowed(p,q.to)||q.target.equals(Level.OVERWORLD)&&!SafeLanding.valid(target,BlockPos.containing(q.to))){say(p,"落点在引导期间发生变化，已取消传送；请重试。");}else TeleportEffects.travel(p,target,q.to,p.getYRot());}}}
        Skills.attributes(p);
        if(p.tickCount%20!=0)return;
        if(p.isAlive())restore(p);ClaimBorders.sync(p);PlayerTitles.sync(p);
        Profile r=Store.of(p);r.mana=Math.min(Skills.manaMax(r),r.mana+Skills.regen(r,p.tickCount/20));
        if(r.meditating&&p.getDeltaMovement().horizontalDistanceSqr()<.001&&p.onGround()&&p.tickCount%(20*Rules.current.meditateIntervalSeconds)==0){r.xp=Math.min(Integer.MAX_VALUE-1000,r.xp+Rules.current.meditateXp);p.serverLevel().sendParticles(ParticleTypes.ENCHANT,p.getX(),p.getY()+1,p.getZ(),6,.5,.6,.5,.1);}
        if(p.level().dimension().equals(CITY)){CityPlan.Building b=nearBuilding(p);if(b!=null)progress(p,"visit",b.id());if(p.getY()<10)spawn(p);}
        if(p.level().dimension().equals(TRIAL)&&p.getY()<45){Trials.leave(p,false);spawn(p);}
        Skills.attributes(p);
        Network.sync(p);
    }
    static void attribute(ServerPlayer p,Attribute key,UUID id,double amount){AttributeInstance a=p.getAttribute(key);if(a==null)return;AttributeModifier old=a.getModifier(id);if(old!=null&&Math.abs(old.getAmount()-amount)<.0001)return;a.removeModifier(id);if(amount!=0)a.addTransientModifier(new AttributeModifier(id,"DuskRain cultivation",amount,AttributeModifier.Operation.ADDITION));}
    static void multiplier(ServerPlayer p,Attribute key,UUID id,double amount){AttributeInstance a=p.getAttribute(key);if(a==null)return;AttributeModifier old=a.getModifier(id);if(old!=null&&old.getOperation()==AttributeModifier.Operation.MULTIPLY_BASE&&Math.abs(old.getAmount()-amount)<.000001)return;a.removeModifier(id);if(amount!=0)a.addTransientModifier(new AttributeModifier(id,"DuskRain training",amount,AttributeModifier.Operation.MULTIPLY_BASE));}
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent e){if(e.phase==TickEvent.Phase.END){Guilds.tick(e.getServer());GuildPalace.tick(e.getServer());WildernessTravel.tick(e.getServer());SignRepair.tick(e.getServer());Skills.tick(e.getServer());Services.tick(e.getServer());Trials.tick(e.getServer());Construction.tick(e.getServer());}}
    @SubscribeEvent public void craft(PlayerEvent.ItemCraftedEvent e){if(e.getEntity() instanceof ServerPlayer p){var id=ForgeRegistries.ITEMS.getKey(e.getCrafting().getItem());if(id!=null)progress(p,"craft",id.toString());}}
    public static void progress(ServerPlayer p,String type,String target){Profile r=Store.of(p);
        if(r.main<24){var q=Rules.current.main.get(r.main);if(r.stage>=q.stage()&&q.type().equals(type)&&(q.target().equals(target)||q.target().equals("hostile")&&type.equals("kill")))r.mainProgress=Math.min(q.count(),r.mainProgress+1);}
        for(int i:r.dailies(p.getUUID())){var q=Rules.current.daily.get(i);if(q.type().equals(type)&&(q.target().equals(target)||q.target().equals("any")||q.target().equals("hostile")&&type.equals("kill")))r.dailyProgress[i]=Math.min(q.count(),r.dailyProgress[i]+1);}
    }
    public static void cast(ServerPlayer p,int skill){Skills.cast(p,skill);}
    public static int count(ServerPlayer p,Item item){int n=0;for(ItemStack s:p.getInventory().items)if(s.is(item))n+=s.getCount();return n;}
    public static boolean take(ServerPlayer p,Item item,int n){if(n<0||count(p,item)<n)return false;for(ItemStack s:p.getInventory().items){if(s.is(item)){int k=Math.min(n,s.getCount());s.shrink(k);n-=k;if(n==0)break;}}p.getInventory().setChanged();return true;}
    public static boolean fits(ServerPlayer p,ItemStack stack){int n=stack.getCount();for(ItemStack s:p.getInventory().items){if(s.isEmpty())n-=stack.getMaxStackSize();else if(ItemStack.isSameItemSameTags(s,stack))n-=Math.max(0,s.getMaxStackSize()-s.getCount());if(n<=0)return true;}return false;}
    public static void reward(ServerPlayer p,int xp,int money){Profile r=Store.of(p);r.xp=(int)Math.min(Integer.MAX_VALUE-1L,(long)r.xp+xp);r.money=Math.min(1_000_000_000_000L,r.money+money);Network.sync(p);}
}
