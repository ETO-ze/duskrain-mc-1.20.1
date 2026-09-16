package cn.duskrain;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;

/** All translation happens on the server; the client supplies bounded direction bits only. */
public final class SwordFlight {
    public static final RegistryObject<EntityType<Sword>> TYPE=TrialBoss.ENTITIES.register("flying_sword",()->EntityType.Builder.<Sword>of(Sword::new,MobCategory.MISC).sized(.8f,.2f).clientTrackingRange(10).updateInterval(1).build("duskrain:flying_sword"));
    public record Input(int bits){}
    public record State(boolean flying,boolean landing,long rental,int school,int stage){}
    public static void init(){}
    public static boolean flying(LivingEntity p){return p.getVehicle() instanceof Sword;}
    public static boolean permanent(Profile r){return r.stage>=18||r.school==1&&r.stage>=6;}
    static boolean allowed(ServerPlayer p,Vec3 at){
        if(p.level().dimension().equals(Guilds.DIM))return Guilds.allowed(p,at)&&Guilds.allowed(p,at.add(.5,0,.5))&&Guilds.allowed(p,at.add(-.5,0,-.5));
        if(p.level().dimension().equals(Gameplay.CITY)){var b=CityPlan.find("arena");return !(Math.abs(at.x-b.x())<b.w()+2&&Math.abs(at.z-b.z())<b.d()+2);}
        return p.level().dimension().equals(Level.OVERWORLD)||p.level().dimension().equals(Level.NETHER)||p.level().dimension().equals(Level.END);
    }
    public static void toggle(ServerPlayer p){
        if(p.getVehicle() instanceof Sword sword){sword.landing=true;Gameplay.say(p,"收剑缓降中。");return;}
        Profile r=Store.of(p);
        if(!p.isAlive()||p.isSpectator()||p.isPassenger()||!p.onGround()||p.isInWaterOrBubble()||!r.onboardingComplete){Gameplay.say(p,"请在地面安全位置起剑。");return;}
        if(!allowed(p,p.position())||r.combatUntil>System.currentTimeMillis()){Gameplay.say(p,"此区域或战斗状态下不能御剑。");return;}
        if(!permanent(r)&&r.flightTicks<=0){Gameplay.say(p,"剑修金丹解锁御剑；其他流派登神解锁，也可向云渡使购买体验。");return;}
        if(r.mana<2){Gameplay.say(p,"灵力不足，不能起剑。");return;}
        Sword sword=new Sword(TYPE.get(),p.level());sword.setPos(p.getX(),p.getY()+.12,p.getZ());sword.setYRot(p.getYRot());p.serverLevel().addFreshEntity(sword);p.startRiding(sword,true);sword.owner=p.getUUID();sword.safe=p.position();sword.setDeltaMovement(0,.12,0);Gameplay.RECALL.remove(p.getUUID());r.meditating=false;state(p);
    }
    public static void state(ServerPlayer p){Profile r=Store.of(p);V14Network.to(p,new State(flying(p),p.getVehicle() instanceof Sword s&&s.landing,r.flightTicks,r.school,r.stage));}
    public static void input(ServerPlayer p,Input m){if(m.bits<0||m.bits>63||!(p.getVehicle() instanceof Sword s))return;if(s.lastInputTick==p.serverLevel().getGameTime())return;s.bits=m.bits;s.lastInputTick=p.serverLevel().getGameTime();}
    public static void menu(ServerPlayer p){Profile r=Store.of(p);var c=AscensionRules.current;Network.menu(p,"御剑行空",permanent(r)?"已获得永久御剑资格 · R起剑/缓降 · 空格上升 · Shift下降":"体验余时 "+r.flightTicks/1200+"分"+r.flightTicks/20%60+"秒；只在御剑时计时。",List.of(DRCommands.entry(flying(p)?"收剑缓降":"召出飞剑","flight toggle"),DRCommands.card("10分钟体验","flight rent 0",c.flightPrices[0]+"灵石 · 云渡使处购买","duskrain:artifact_1_0",Services.nearby(p,"travel")&&!permanent(r)),DRCommands.card("60分钟体验","flight rent 1",c.flightPrices[1]+"灵石 · 离线暂停计时","duskrain:artifact_1_0",Services.nearby(p,"travel")&&!permanent(r))));}
    public static void rent(ServerPlayer p,int n){if(n<0||n>1||!Services.require(p,"travel"))return;Profile r=Store.of(p);var c=AscensionRules.current;if(permanent(r)){Gameplay.say(p,"已有永久资格，无需购买。");return;}if(r.money<c.flightPrices[n]){Gameplay.say(p,"灵石不足。");return;}if(r.flightTicks>20L*3600*24*30){Gameplay.say(p,"体验余时已足够，暂不重复购买。");return;}if(Transactions.commit(p,"flight:rent",()->{r.money-=c.flightPrices[n];r.flightTicks+=(long)c.flightMinutes[n]*1200;})){Gameplay.say(p,"御剑体验已入账。");Network.sync(p);menu(p);}}
    public static void stop(ServerPlayer p){if(p.getVehicle() instanceof Sword s){s.release=true;p.stopRiding();s.discard();p.fallDistance=0;}}
    @SubscribeEvent public void hurt(LivingHurtEvent e){if(e.getEntity() instanceof ServerPlayer p&&p.getVehicle() instanceof Sword s)s.landing=true;}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void changed(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void travel(net.minecraftforge.event.entity.EntityTravelToDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void attack(AttackEntityEvent e){if(flying(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void block(PlayerInteractEvent.RightClickBlock e){if(flying(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void entity(PlayerInteractEvent.EntityInteract e){if(flying(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void use(PlayerInteractEvent.RightClickItem e){if(flying(e.getEntity()))e.setCanceled(true);}
    public static final class Sword extends Entity {
        int bits;long lastInputTick=-100;boolean landing,release;UUID owner;Vec3 safe;int landingTicks;double drain;
        double tx,ty,tz;float tr;int steps;
        public Sword(EntityType<? extends Sword> type,Level level){super(type,level);setNoGravity(true);}
        static final net.minecraft.network.syncher.EntityDataAccessor<Integer> TIER=net.minecraft.network.syncher.SynchedEntityData.defineId(Sword.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
        @Override protected void defineSynchedData(){entityData.define(TIER,0);}
        public int tier(){return entityData.get(TIER);}
        @Override protected void readAdditionalSaveData(CompoundTag n){}
        @Override protected void addAdditionalSaveData(CompoundTag n){}
        @Override public boolean shouldRiderSit(){return false;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
        @Override public double getPassengersRidingOffset(){return .55;}
        @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){tx=x;ty=y;tz=z;tr=yaw;this.steps=3;}
        @Override public void tick(){
            super.tick();
            if(level().isClientSide){if(steps>0){setPos(getX()+(tx-getX())/steps,getY()+(ty-getY())/steps,getZ()+(tz-getZ())/steps);setYRot(getYRot()+net.minecraft.util.Mth.wrapDegrees(tr-getYRot())/steps);steps--;}return;}
            if(!(getFirstPassenger() instanceof ServerPlayer p)||!p.isAlive()){release=true;ejectPassengers();discard();return;}
            Profile r=Store.of(p);var c=AscensionRules.current;entityData.set(TIER,Math.min(6,r.stage/3));
            if(!allowed(p,position())||!permanent(r)&&r.flightTicks<=0||r.mana<=0||r.combatUntil>System.currentTimeMillis())landing=true;
            if(!permanent(r)&&r.flightTicks>0)r.flightTicks--;
            if(!landing){drain+=c.flightMana/20*(1-Math.min(.9,MysticEnchants.effect(p,7,false)));if(drain>=1){int n=(int)drain;r.mana=Math.max(0,r.mana-n);drain-=n;}}
            int keys=level().getGameTime()-lastInputTick<=5?bits:0;double forward=((keys&1)>0?1:0)-((keys&2)>0?1:0),side=((keys&8)>0?1:0)-((keys&4)>0?1:0);
            double yaw=p.getYRot()*Math.PI/180,speed=(r.stage>=18?c.divineFlightSpeed:c.flightSpeed)/20;
            Vec3 horizontal=new Vec3(-Math.sin(yaw)*forward+Math.cos(yaw)*side,0,Math.cos(yaw)*forward+Math.sin(yaw)*side);if(horizontal.lengthSqr()>1)horizontal=horizontal.normalize();
            double dy=landing?-.12:(((keys&16)>0?1:0)-((keys&32)>0?1:0))*c.verticalSpeed/20;
            Vec3 motion=(landing?Vec3.ZERO:horizontal.scale(speed)).add(0,dy,0);
            if(tickCount<5&&!landing)motion=motion.add(0,.12,0);
            int parts=Math.max(1,(int)Math.ceil(motion.length()/.1));Vec3 step=motion.scale(1d/parts);
            for(int i=0;i<parts;i++){Vec3 at=position().add(step);AABB rider=p.getBoundingBox().move(at.x-p.getX(),at.y+.2-p.getY(),at.z-p.getZ());
                if(at.y<level().getMinBuildHeight()+2||at.y>level().getMaxBuildHeight()-4||!allowed(p,at)||!level().getWorldBorder().isWithinBounds(BlockPos.containing(at))||!loaded(p,rider)||!level().noCollision(p,rider)||!level().noCollision(this,getBoundingBox().move(step)))break;
                setPos(at.x,at.y,at.z);
            }
            setYRot(p.getYRot());p.fallDistance=0;setDeltaMovement(Vec3.ZERO);
            BlockPos floor=BlockPos.containing(getX(),getY()-.2,getZ());boolean supported=!level().getBlockState(floor).getCollisionShape(level(),floor).isEmpty();
            if(supported){safe=new Vec3(getX(),floor.getY()+1,getZ());if(landing||tickCount>10&&(keys&32)>0){release=true;p.stopRiding();p.connection.teleport(safe.x,safe.y,safe.z,p.getYRot(),p.getXRot());p.fallDistance=0;discard();state(p);return;}}
            if(landing&&++landingTicks>240){release=true;p.stopRiding();if(safe!=null&&allowed(p,safe)&&p.serverLevel().noCollision(p,p.getBoundingBox().move(safe.subtract(p.position()))))p.connection.teleport(safe.x,safe.y,safe.z,p.getYRot(),p.getXRot());else Gameplay.spawn(p);p.fallDistance=0;discard();state(p);return;}
            if(tickCount%20==0){Store.get(p.server).setDirty();state(p);}
        }
        static boolean loaded(ServerPlayer p,AABB b){for(int x=((int)Math.floor(b.minX))>>4;x<=((int)Math.floor(b.maxX))>>4;x++)for(int z=((int)Math.floor(b.minZ))>>4;z<=((int)Math.floor(b.maxZ))>>4;z++)if(!p.serverLevel().hasChunk(x,z))return false;return true;}
    }
}
