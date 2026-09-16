package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.*;
import org.joml.Vector3f;
import java.util.*;

/** Server-authoritative geometry, costs, attack cycles and bounded delayed effects. */
public final class Skills {
    static final UUID REACH=UUID.fromString("a13882a0-07ab-4b25-86b5-b2c4be61be74"),RESIST=UUID.fromString("a13882a0-07ab-4b25-86b5-b2c4be61be75");
    static final List<Flight> FLIGHTS=new ArrayList<>();
    static final List<Pulse> PULSES=new ArrayList<>();
    static final Map<UUID,Long> GUARD=new HashMap<>(),COUNTER=new HashMap<>(),COMBO_TIME=new HashMap<>(),FROST=new HashMap<>();
    static final Map<UUID,Integer> COMBO=new HashMap<>();
    static final Map<UUID,Swing> SWINGS=new HashMap<>();
    static boolean applyingSkill,counterDamage;
    static final DustParticleOptions SWORD=new DustParticleOptions(new Vector3f(.57f,.96f,.92f),1.05f);
    static class Swing {long until;Flight wave;Swing(long until,Flight wave){this.until=until;this.wave=wave;}}
    static class Flight {
        UUID owner;ServerLevel level;Vec3 pos,velocity;double left;float damage;int style,remaining;long start;
        boolean normal;Set<Integer> hit=new HashSet<>();
        Flight(ServerPlayer p,double range,double speed,float damage,int style,int pierces){owner=p.getUUID();level=p.serverLevel();pos=p.getEyePosition();velocity=p.getLookAngle().scale(speed);left=range;this.damage=damage;this.style=style;remaining=pierces;start=level.getGameTime()+1;}
    }
    static class Pulse {
        UUID owner;ServerLevel level;Vec3 at;float damage;double radius;int kind,left,interval;long due;
        Pulse(ServerPlayer p,Vec3 at,double radius,float damage,int kind,int count,int interval,int delay){owner=p.getUUID();level=p.serverLevel();this.at=at;this.radius=radius;this.damage=damage;this.kind=kind;left=count;this.interval=interval;due=level.getGameTime()+delay;}
    }
    public static int manaMax(Profile r){return (int)Math.round(Rules.manaMax(r.stage)*(r.school==2?CombatRules.current.manaMultiplier:1)+Boosts.permanent(r,2)+AscensionRules.bonus(r,AscensionRules.current.mana)+r.enchantedMana);}
    public static int regen(Profile r,int second){double rate=3*(r.school==2?CombatRules.current.regenMultiplier:1)*(1+Boosts.temporary(r,3))*(1+r.enchantedRegen);return (int)(Math.floor((second+1)*rate)-Math.floor(second*rate));}
    static boolean heldValid(ServerPlayer p){ItemStack s=p.getMainHandItem();Profile r=Store.of(p);if(s.getItem() instanceof Content.Artifact a)return r.school==a.school&&r.stage>=a.tier*3&&Content.owned(s,p);return r.school==3&&s.isEmpty()||s.getItem() instanceof SwordItem&&r.school==1;}
    static float power(ServerPlayer p){var c=CombatRules.current;Profile r=Store.of(p);int tier=heldValid(p)&&p.getMainHandItem().getItem() instanceof Content.Artifact a?a.tier:0;return (float)(c.base+Math.min(17,r.stage)*c.stage+tier*c.artifact);}
    static int cost(Profile r,int slot){return (int)Math.ceil(CombatRules.current.costs[r.school-1][slot]*(r.school==2?1-CombatRules.current.mageDiscount:1));}
    public static void attributes(ServerPlayer p){Profile r=Store.of(p);var c=CombatRules.current;boolean sword=r.school==1&&heldValid(p);boolean inPvp=r.combatUntil>System.currentTimeMillis();int oldManaMax=manaMax(r);r.enchantedMana=MysticEnchants.effect(p,3,inPvp);r.enchantedRegen=MysticEnchants.effect(p,4,inPvp);float oldMax=p.getMaxHealth(),ratio=oldMax>0?p.getHealth()/oldMax:1;
        Gameplay.attribute(p,ForgeMod.ENTITY_REACH.get(),REACH,sword?c.reach-3:0);
        Gameplay.attribute(p,Attributes.MAX_HEALTH,Gameplay.HEALTH,Math.min(17,r.stage)*1.2+(r.school==3?c.bodyHealth:0)+Boosts.health(r)+AscensionRules.bonus(r,AscensionRules.current.health));
        Gameplay.attribute(p,Attributes.ATTACK_DAMAGE,Gameplay.ATTACK,Math.min(17,r.stage)*.45);
        Gameplay.multiplier(p,Attributes.MOVEMENT_SPEED,Gameplay.SPEED,(r.school==3?c.bodySpeed:0)+Boosts.speed(r)+AscensionRules.bonus(r,AscensionRules.current.speed));
        Gameplay.attribute(p,Attributes.KNOCKBACK_RESISTANCE,RESIST,Math.min(.9,(r.school==3?c.bodyResistance:0)+AscensionRules.bonus(r,AscensionRules.current.resistance)));
        if(Math.abs(oldMax-p.getMaxHealth())>.0001)p.setHealth(Math.min(p.getMaxHealth(),ratio*p.getMaxHealth()));
        r.mana=Math.min(r.mana,manaMax(r));
    }
    public static boolean target(ServerPlayer p,LivingEntity t){if(t==p||!t.isAlive())return false;if(t.getTags().contains("duskrain_dummy"))return p.level().dimension().equals(Gameplay.CITY);if(Protection.city(p.level())&&!Protection.pvpAllowed(p))return false;return t instanceof ServerPlayer sp?Protection.canFight(p,sp):t instanceof Monster;}
    public static boolean lineClear(ServerLevel l,Vec3 a,Vec3 b,Entity e){return l.clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,e)).getType()==HitResult.Type.MISS;}
    public static double distanceToBox(Vec3 a,AABB b){return a.distanceTo(new Vec3(Math.max(b.minX,Math.min(b.maxX,a.x)),Math.max(b.minY,Math.min(b.maxY,a.y)),Math.max(b.minZ,Math.min(b.maxZ,a.z))));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void attack(AttackEntityEvent e){if(e.isCanceled()||!(e.getEntity() instanceof ServerPlayer p))return;attributes(p);double reach=p.getAttributeValue(ForgeMod.ENTITY_REACH.get())+(p.isCreative()?3:0);
        if(distanceToBox(p.getEyePosition(),e.getTarget().getBoundingBox())>reach+.05||!p.hasLineOfSight(e.getTarget())){e.setCanceled(true);return;}
        if(Store.of(p).school==1&&heldValid(p)){normalSwing(p,e.getTarget().getId());Swing prior=SWINGS.get(p.getUUID());if(prior!=null&&prior.wave!=null&&prior.until>p.serverLevel().getGameTime())prior.wave.hit.add(e.getTarget().getId());}
    }
    public static boolean normalSwing(ServerPlayer p,int meleeTarget){if(SwordFlight.flying(p))return false;Profile r=Store.of(p);var c=CombatRules.current;long now=p.serverLevel().getGameTime();
        if(r.school!=1||!p.isAlive()||p.isSpectator()||!heldValid(p)||p.level().dimension().equals(Gameplay.DEMO)||p.getAttackStrengthScale(.0f)<.999f||SWINGS.containsKey(p.getUUID())&&SWINGS.get(p.getUUID()).until>now)return false;
        Flight f=null;if(r.mana>=c.normalQiCost){r.mana-=c.normalQiCost;f=new Flight(p,c.normalQiRange,1.15,(float)(power(p)*c.normalQiDamage),SpellFx.SWORD,1);f.normal=true;if(meleeTarget>=0)f.hit.add(meleeTarget);FLIGHTS.add(f);Network.sync(p);}
        SWINGS.put(p.getUUID(),new Swing(now+(long)Math.ceil(p.getCurrentItemAttackStrengthDelay()),f));if(meleeTarget<0)p.resetAttackStrengthTicker();return f!=null;
    }
    @SubscribeEvent public void jump(LivingEvent.LivingJumpEvent e){if(e.getEntity() instanceof ServerPlayer p&&(Store.of(p).school==3||CityMobility.city(p))){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,Math.max(v.y,CombatRules.current.jump),v.z);}}
    @SubscribeEvent public void falling(LivingFallEvent e){if(e.getEntity() instanceof ServerPlayer p&&(Store.of(p).school==3||CityMobility.city(p)))e.setDistance(Math.max(0,e.getDistance()-2));}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void prepareCombat(LivingHurtEvent e){if(e.getEntity() instanceof ServerPlayer victim&&e.getSource().getEntity() instanceof ServerPlayer attacker)combat(attacker,victim);}
    @SubscribeEvent(priority=EventPriority.LOW) public void hurt(LivingHurtEvent e){
        if(e.getEntity() instanceof ServerPlayer defender){boolean pvp=e.getSource().getEntity() instanceof ServerPlayer;double guard=GUARD.getOrDefault(defender.getUUID(),0L)>defender.serverLevel().getGameTime()?(pvp?CombatRules.current.guardPvp:CombatRules.current.guardPve):0;double reduction=1-(1-guard)*(1-Math.min(.9,MysticEnchants.effect(defender,5,pvp)));e.setAmount((float)(e.getAmount()*(1-Math.min(pvp?.4:.7,reduction))));}

        if(!applyingSkill&&e.getSource().getEntity() instanceof ServerPlayer p){double gain=Boosts.damage(Store.of(p));if(Store.of(p).school==1&&heldValid(p)&&e.getSource().getMsgId().equals("player"))gain+=CombatRules.current.swordMelee;e.setAmount((float)(e.getAmount()*(1+gain)*AscensionRules.damage(Store.of(p),e.getEntity() instanceof ServerPlayer)));}
        if(e.getEntity() instanceof ServerPlayer p&&GUARD.getOrDefault(p.getUUID(),0L)>p.serverLevel().getGameTime()){
            boolean pvp=e.getSource().getEntity() instanceof ServerPlayer;
            if(!counterDamage&&e.getSource().getEntity() instanceof LivingEntity enemy&&target(p,enemy)&&COUNTER.getOrDefault(p.getUUID(),0L)<=p.serverLevel().getGameTime()){
                COUNTER.put(p.getUUID(),p.serverLevel().getGameTime()+20);PULSES.add(new Pulse(p,enemy.position(),.8,(float)(power(p)*CombatRules.current.damage[2][2]),8,1,1,1));
            }
        }
    }
    @SubscribeEvent public void hit(LivingDamageEvent e){if(e.getAmount()>0&&!applyingSkill&&e.getSource().getEntity() instanceof ServerPlayer p&&Store.of(p).school==1&&heldValid(p)&&target(p,e.getEntity()))addIntent(p);}
    static void addIntent(ServerPlayer p){long now=p.serverLevel().getGameTime();COMBO_TIME.put(p.getUUID(),now+160);COMBO.put(p.getUUID(),Math.min(5,COMBO.getOrDefault(p.getUUID(),0)+1));}
    static void combat(ServerPlayer a,ServerPlayer b){long end=System.currentTimeMillis()+Rules.current.combatSeconds*1000L;Store.of(a).combatUntil=end;Store.of(b).combatUntil=end;attributes(a);attributes(b);}
    static void damage(ServerPlayer p,LivingEntity t,float amount){damage(p,t,amount,false);}
    static void damage(ServerPlayer p,LivingEntity t,float amount,boolean normal){if(!target(p,t))return;
        if(t instanceof ServerPlayer other)combat(p,other);Profile profile=Store.of(p);boolean pvp=t instanceof ServerPlayer;int enchant=normal?0:profile.school==2?1:profile.school==3?2:-1;amount*=(float)((1+Boosts.damage(profile))*AscensionRules.damage(profile,pvp)*(1+(enchant<0?0:MysticEnchants.effect(p,enchant,pvp))));
        if(t.getTags().contains("duskrain_dummy")){Gameplay.say(p,"练功命中："+Math.round(amount)+" · 距离 "+String.format(Locale.ROOT,"%.1f",distanceToBox(p.getEyePosition(),t.getBoundingBox()))+" 格");Gameplay.progress(p,"practice","dummy");return;}
        boolean before=applyingSkill;applyingSkill=true;int recovery=t.invulnerableTime;t.invulnerableTime=0;try{t.hurt(p.damageSources().playerAttack(p),amount*(t instanceof ServerPlayer?(float)CombatRules.current.pvp:1));}finally{t.invulnerableTime=Math.max(recovery,t.invulnerableTime);applyingSkill=before;}
    }
    public static void cast(ServerPlayer p,int slot){if(SwordFlight.flying(p)){Gameplay.say(p,"御剑时不能施法，请先收剑落地。");return;}if(slot<0||slot>2)return;Profile r=Store.of(p);var c=CombatRules.current;
        if(r.school==0){Gameplay.say(p,"请先在问道书院选择流派。");return;}if(r.stage<slot){Gameplay.say(p,"需要"+Rules.realm(slot)+"解锁。");return;}
        if(!p.isAlive()||p.isSpectator()||p.level().dimension().equals(Gameplay.DEMO)){Gameplay.say(p,"此时不能施法。");return;}
        if(!heldValid(p)){Gameplay.say(p,"请持有符合境界、流派和绑定条件的法器；体修也可空手施法。");return;}
        if(charging(p)){Gameplay.say(p,"正在蓄法，请完成当前雷诀。");return;}
        long now=System.currentTimeMillis();long[] cd=Gameplay.COOLDOWNS.computeIfAbsent(p.getUUID(),k->new long[3]);if(cd[slot]>now){Gameplay.say(p,"技能尚需 "+((cd[slot]-now+999)/1000)+" 秒冷却。");return;}
        int cost=cost(r,slot);if(r.mana<cost){Gameplay.say(p,"灵力不足。");return;}r.mana-=cost;r.meditating=false;cd[slot]=now+c.cooldowns[r.school-1][slot]*1000L;
        float a=power(p);if(r.school==1&&COMBO.getOrDefault(p.getUUID(),0)>=5){a*=1+c.intent;COMBO.put(p.getUUID(),0);}float hit=a*(float)c.damage[r.school-1][slot];double range=c.ranges[r.school-1][slot];
        if(r.school==1)switch(slot){
            case 0->FLIGHTS.add(new Flight(p,range,1.25,hit,SpellFx.SWORD,3));
            case 1->PULSES.add(new Pulse(p,p.position(),range,hit,0,3,4,0));
            case 2->{dash(p,range);area(p,p.position(),3,hit,0);fx(p,SpellFx.SWORD,p.position().add(0,1,0),3,10);}
        }else if(r.school==3)switch(slot){
            case 0->{Vec3 from=p.position();dash(p,range);swept(p,from,p.position(),hit);fx(p,SpellFx.BODY,p.position().add(0,1,0),2,8);}
            case 1->{area(p,p.position(),range,hit,1);fx(p,SpellFx.BODY,p.position().add(0,.1,0),range,14);}
            case 2->{GUARD.put(p.getUUID(),p.serverLevel().getGameTime()+c.guardTicks);fx(p,SpellFx.GUARD,p.position().add(0,1,0),1,20);}
        }else switch(slot){
            case 0->FLIGHTS.add(new Flight(p,range,.9,hit,SpellFx.FIRE,1));
            case 1->{Vec3 at=aim(p,12);PULSES.add(new Pulse(p,at,range,hit,2,c.frostTicks/20,20,0));fx(p,SpellFx.FROST,at,range,c.frostTicks);}
            case 2->{Vec3 at=aim(p,range);PULSES.add(new Pulse(p,at,3,hit,3,1,1,c.chargeTicks));fx(p,SpellFx.FROST,at,3,c.chargeTicks);Gameplay.say(p,"九霄雷诀 · 蓄法 1.2 秒");}
        }
        if(r.stage>=18)SpellFx.send(p.serverLevel(),8+r.school-1,p.position().add(0,.15,0),p.position().add(0,.15,0),4+AscensionRules.rank(r),60);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);Store.get(p.server).setDirty();Network.sync(p);
    }
    static boolean charging(ServerPlayer p){return PULSES.stream().anyMatch(t->t.owner.equals(p.getUUID())&&t.kind==3);}
    static void fx(ServerPlayer p,int type,Vec3 at,double radius,int ticks){SpellFx.send(p.serverLevel(),type,at,at,radius,ticks);}
    static Vec3 aim(ServerPlayer p,double range){Vec3 a=p.getEyePosition(),end=a.add(p.getLookAngle().scale(range));BlockHitResult hit=p.serverLevel().clip(new ClipContext(a,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return hit.getType()==HitResult.Type.MISS?end:hit.getLocation().subtract(p.getLookAngle().scale(.15));}
    public static double dash(ServerPlayer p,double range){Vec3 start=p.position(),direction=new Vec3(p.getLookAngle().x,0,p.getLookAngle().z).normalize(),last=start;double travelled=0;
        for(double d=.2;d<=range+.001;d+=.2){Vec3 next=start.add(direction.scale(d));AABB box=p.getBoundingBox().move(next.subtract(start));if(!p.serverLevel().noCollision(p,box)||!p.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(next))||p.level().dimension().equals(Guilds.DIM)&&!Guilds.allowed(p,next))break;last=next;travelled=d;}
        p.connection.teleport(last.x,last.y,last.z,p.getYRot(),p.getXRot());p.fallDistance=0;p.connection.send(new ClientboundSetEntityMotionPacket(p));SpellFx.send(p.serverLevel(),Store.of(p).school==3?SpellFx.BODY:SpellFx.SWORD,start.add(0,1,0),last.add(0,1,0),0,8);return travelled;
    }
    static void swept(ServerPlayer p,Vec3 from,Vec3 to,float hit){Vec3 a=from.add(0,1,0),b=to.add(0,1,0);for(var t:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(a,b).inflate(1),e->target(p,e))){AABB box=t.getBoundingBox().inflate(.8);if((box.contains(a)||box.clip(a,b).isPresent())&&lineClear(p.serverLevel(),a,t.getBoundingBox().getCenter(),p)){damage(p,t,hit);t.knockback(.8,p.getX()-t.getX(),p.getZ()-t.getZ());}}}
    static void area(ServerPlayer p,Vec3 center,double radius,float power,int kind){for(LivingEntity t:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius+1),e->target(p,e))){Vec3 at=t.getBoundingBox().getCenter();if(distanceToBox(center.add(0,.5,0),t.getBoundingBox())>radius||!lineClear(p.serverLevel(),center.add(0,.5,0),at,p))continue;float hit=power;
        if(kind==3&&FROST.getOrDefault(t.getUUID(),0L)>p.serverLevel().getGameTime()){hit*=1+CombatRules.current.frostCombo;FROST.remove(t.getUUID());}
        damage(p,t,hit);if(t.getTags().contains("duskrain_dummy"))continue;
        if(kind==1){Vec3 v=t.getDeltaMovement();t.setDeltaMovement(v.x,Math.max(v.y,t instanceof ServerPlayer?.20:.36),v.z);t.hurtMarked=true;t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,CombatRules.current.controlTicks,1));if(t instanceof ServerPlayer sp)sp.connection.send(new ClientboundSetEntityMotionPacket(sp));}
        if(kind==2){FROST.put(t.getUUID(),p.serverLevel().getGameTime()+80);t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,25,1));}
    }}
    static void arc(ServerPlayer p,double radius,DustParticleOptions ignored){fx(p,SpellFx.SWORD,p.position().add(0,1,0),radius,5);}
    public static void tick(MinecraftServer server){
        // Swap due pulses out before damage callbacks can enqueue counter attacks.
        List<Pulse> due=new ArrayList<>();Iterator<Pulse> pending=PULSES.iterator();while(pending.hasNext()){Pulse t=pending.next();var p=server.getPlayerList().getPlayer(t.owner);if(p==null||!p.isAlive()||p.level()!=t.level){pending.remove();continue;}if(t.level.getGameTime()>=t.due){due.add(t);pending.remove();}}
        for(Pulse t:due){var p=server.getPlayerList().getPlayer(t.owner);if(p==null)continue;boolean previous=counterDamage;counterDamage=t.kind==8;try{area(p,t.kind==0?p.position():t.at,t.radius,t.damage,t.kind);}finally{counterDamage=previous;}if(t.kind==0)fx(p,SpellFx.SWORD,p.position().add(0,1,0),t.radius,5);if(t.kind==3){fx(p,SpellFx.LIGHTNING,t.at,t.radius,12);t.level.playSound(null,t.at.x,t.at.y,t.at.z,net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,net.minecraft.sounds.SoundSource.PLAYERS,.4f,1.4f);}if(--t.left>0){t.due+=t.interval;PULSES.add(t);}}
        Iterator<Flight> it=FLIGHTS.iterator();while(it.hasNext()){Flight f=it.next();ServerPlayer p=server.getPlayerList().getPlayer(f.owner);if(p==null||!p.isAlive()||p.level()!=f.level){it.remove();continue;}if(f.level.getGameTime()<f.start)continue;
            Vec3 from=f.pos,to=from.add(f.velocity.scale(Math.min(1,f.left/f.velocity.length())));BlockHitResult wall=f.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));Vec3 end=wall.getType()==HitResult.Type.MISS?to:wall.getLocation();
            record Hit(LivingEntity target,double distance){}List<Hit> hits=new ArrayList<>();
            for(var t:f.level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.4),e->target(p,e)&&!f.hit.contains(e.getId()))){AABB box=t.getBoundingBox().inflate(.12);Optional<Vec3> contact=box.contains(from)?Optional.of(from):box.clip(from,end);if(contact.isPresent())hits.add(new Hit(t,from.distanceToSqr(contact.get())));}
            hits.sort(Comparator.comparingDouble(Hit::distance));Vec3 limit=end;
            for(Hit h:hits){f.hit.add(h.target.getId());damage(p,h.target,f.damage,f.normal);if(--f.remaining==0){limit=from.add(f.velocity.normalize().scale(Math.sqrt(h.distance)));break;}}
            SpellFx.send(f.level,f.style,from,limit,0,5);f.left-=from.distanceTo(limit);f.pos=limit;if(f.remaining==0||wall.getType()!=HitResult.Type.MISS||f.left<=.01)it.remove();
        }
        if(server.getTickCount()%10==0)for(var p:server.getPlayerList().getPlayers()){long now=p.serverLevel().getGameTime();if(GUARD.getOrDefault(p.getUUID(),0L)>now)fx(p,SpellFx.GUARD,p.position().add(0,1,0),1,10);if(COMBO_TIME.getOrDefault(p.getUUID(),0L)<now)COMBO.remove(p.getUUID());}
        if(server.getTickCount()%200==0){FROST.entrySet().removeIf(e->e.getValue()<server.overworld().getGameTime());}
    }
    public static void clear(UUID id){GUARD.remove(id);COUNTER.remove(id);COMBO.remove(id);COMBO_TIME.remove(id);SWINGS.remove(id);Gameplay.COOLDOWNS.remove(id);FLIGHTS.removeIf(f->f.owner.equals(id));PULSES.removeIf(t->t.owner.equals(id));}
}
