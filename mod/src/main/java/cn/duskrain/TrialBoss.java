package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class TrialBoss extends Monster implements GeoEntity {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,DuskRain.ID);
    public static final RegistryObject<EntityType<TrialBoss>> WOOD=register("wood_guardian",1),ROCK=register("rock_guardian",2),DRAGON=register("storm_dragon",3),SENTINEL=register("divine_sentinel",4);
    static RegistryObject<EntityType<TrialBoss>> register(String id,int tier){return ENTITIES.register(id,()->EntityType.Builder.<TrialBoss>of((t,l)->new TrialBoss(t,l,tier),MobCategory.MONSTER).sized(tier==3?1.7f:1.2f,tier==3?2.2f:2.9f).clientTrackingRange(12).build("duskrain:"+id));}
    public final int tier;final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);int attackTimer;Vec3 marked;
    static final EntityDataAccessor<Float> DIVINE_HEALTH=SynchedEntityData.defineId(TrialBoss.class,EntityDataSerializers.FLOAT);
    public float divineMax(){return entityData.get(DIVINE_HEALTH);}
    public void divineMax(float amount){entityData.set(DIVINE_HEALTH,amount);}
    static final EntityDataAccessor<Boolean> CHARGING=SynchedEntityData.defineId(TrialBoss.class,EntityDataSerializers.BOOLEAN);
    TrialBoss(EntityType<? extends Monster> t,Level l,int tier){super(t,l);this.tier=tier;xpReward=0;setPersistenceRequired();}
    public static void attributes(EntityAttributeCreationEvent e){var attrs=Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,90).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.24).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,.8).build();e.put(WOOD.get(),attrs);e.put(ROCK.get(),attrs);e.put(DRAGON.get(),attrs);e.put(SENTINEL.get(),attrs);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CHARGING,false);entityData.define(DIVINE_HEALTH,600f);}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new MeleeAttackGoal(this,1,true));goalSelector.addGoal(2,new LookAtPlayerGoal(this,Player.class,32));targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public void tick(){super.tick();if(level().isClientSide||!(level() instanceof net.minecraft.server.level.ServerLevel l)||!isAlive())return;LivingEntity t=getTarget();if(t==null)return;attackTimer++;int cycle=getHealth()<getMaxHealth()*.5f?100:150;
        if(attackTimer%cycle==cycle-30){marked=t.position();entityData.set(CHARGING,true);getNavigation().stop();l.playSound(null,blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.HOSTILE,1,.6f);}
        if(entityData.get(CHARGING)&&marked!=null){getNavigation().stop();if(attackTimer%5==0)for(int i=0;i<32;i++){double a=i*Math.PI/16;l.sendParticles(ParticleTypes.ELECTRIC_SPARK,marked.x+Math.cos(a)*3.2,marked.y+.2,marked.z+Math.sin(a)*3.2,1,0,0,0,0);}
            if(attackTimer%cycle==0){entityData.set(CHARGING,false);for(ServerPlayer p:l.getEntitiesOfClass(ServerPlayer.class,new AABB(marked,marked).inflate(3.2,3,3.2))){if(p.isCreative()||p.isSpectator()||p.position().distanceToSqr(marked)>3.2*3.2||!Skills.lineClear(l,position().add(0,1,0),p.getEyePosition(),this))continue;p.hurt(damageSources().mobAttack(this),(float)(tier>=4?getAttributeValue(Attributes.ATTACK_DAMAGE):tier*4));if(tier==1)p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,60,2));if(tier==2)p.knockback(1.2,getX()-p.getX(),getZ()-p.getZ());}
                l.sendParticles(tier==1?ParticleTypes.COMPOSTER:tier==2?ParticleTypes.POOF:ParticleTypes.ELECTRIC_SPARK,marked.x,marked.y+1,marked.z,35,2,1,2,.1);
                l.playSound(null,marked.x,marked.y,marked.z,tier==3?SoundEvents.LIGHTNING_BOLT_IMPACT:SoundEvents.IRON_GOLEM_ATTACK,SoundSource.HOSTILE,.9f,.8f);marked=null;}
        }
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){if(tier==2&&!entityData.get(CHARGING)&&attackTimer%150>25)amount*=.55f;return super.hurt(source,amount);}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putFloat("DuskRainGodHealth",divineMax());}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){if(tag.contains("DuskRainGodHealth"))divineMax(Math.max(1,tag.getFloat("DuskRainGodHealth")));super.readAdditionalSaveData(tag);}
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source){/* Session owns rewards. */}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"body",5,state->state.setAndContinue(RawAnimation.begin().thenLoop(entityData.get(CHARGING)?"charge":state.isMoving()?"walk":"idle"))));}
    public String modelName(){return tier==1?"wood_guardian":tier==2?"rock_guardian":tier==3?"storm_dragon":"divine_sentinel";}
}
