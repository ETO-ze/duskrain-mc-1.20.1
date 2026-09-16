package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import java.util.*;

public final class Trials {
    public static final String[] NAMES={"青竹迷踪","玄岩机枢","雷台问劫","登神台·凝神","登神台·塑躯","登神台·归一"};
    static final Map<UUID,LinkedHashSet<UUID>> PARTIES=new HashMap<>();
    static final Map<UUID,UUID> INVITES=new HashMap<>();
    static final Session[] SLOTS=new Session[4];
    static final Deque<Request> QUEUE=new ArrayDeque<>();
    record Request(UUID leader,List<UUID> members,int tier,boolean breakthrough){}
    public static final class Session {
        final int slot,tier;final List<UUID> members;final Set<UUID> alive=new HashSet<>(),mobs=new HashSet<>();
        int puzzleStep;boolean puzzleSolved;int[] orientation=new int[3];
        final net.minecraft.server.level.ServerBossEvent bar=new net.minecraft.server.level.ServerBossEvent(Component.literal("问劫试炼"),net.minecraft.world.BossEvent.BossBarColor.BLUE,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
        final boolean breakthrough;int originalStage;int wave,waitTicks=80;final long start;boolean ended;
        Session(int slot,Request r){this.slot=slot;tier=r.tier;members=new ArrayList<>(r.members);alive.addAll(members);breakthrough=r.breakthrough;start=System.currentTimeMillis();}
    }
    public static boolean inTrial(UUID id){for(Session s:SLOTS)if(s!=null&&s.alive.contains(id))return true;return false;}
    public static List<UUID> members(UUID p){for(var e:PARTIES.entrySet())if(e.getValue().contains(p))return new ArrayList<>(e.getValue());return new ArrayList<>(List.of(p));}
    public static UUID leader(UUID p){for(var e:PARTIES.entrySet())if(e.getValue().contains(p))return e.getKey();return p;}
    public static void invite(ServerPlayer p,ServerPlayer target){if(!leader(p.getUUID()).equals(p.getUUID())){Gameplay.say(p,"只有队长可以邀请。");return;}if(inTrial(p.getUUID())||inTrial(target.getUUID())){Gameplay.say(p,"试炼中不能修改队伍。");return;}LinkedHashSet<UUID> m=PARTIES.computeIfAbsent(p.getUUID(),k->new LinkedHashSet<>(List.of(k)));if(m.size()>=4){Gameplay.say(p,"队伍已满4人。");return;}INVITES.put(target.getUUID(),p.getUUID());Gameplay.say(target,p.getGameProfile().getName()+"邀请你组队。使用 /dr party accept 接受。");Gameplay.say(p,"邀请已发送。");}
    public static void accept(ServerPlayer p){UUID l=INVITES.remove(p.getUUID());if(l==null){Gameplay.say(p,"没有待接受的邀请。");return;}if(inTrial(p.getUUID())||inTrial(l)){Gameplay.say(p,"试炼中不能加入队伍。");return;}LinkedHashSet<UUID> m=PARTIES.get(l);if(m==null||m.size()>=4){Gameplay.say(p,"邀请已失效或队伍已满。");return;}partyLeave(p);m.add(p.getUUID());Gameplay.say(p,"已加入试炼队伍。");}
    public static void partyLeave(ServerPlayer p){UUID id=p.getUUID();if(inTrial(id)){Gameplay.say(p,"请先退出试炼。");return;}UUID l=leader(id);LinkedHashSet<UUID> m=PARTIES.get(l);if(m!=null){if(l.equals(id))PARTIES.remove(l);else m.remove(id);}QUEUE.removeIf(r->r.members.contains(id));}
    public static void enter(ServerPlayer p,int tier,boolean breakthrough){if(!Services.require(p,breakthrough?"master":"trial"))return;
        if(tier<1||tier>6)return;
        if(tier>=4){if(breakthrough){if(!Ascension.eligible(p,true)||tier!=Store.of(p).stage-13)return;}else if(Store.of(p).stage<18||tier!=Math.max(5,Math.min(6,Store.of(p).stage-13))){Gameplay.say(p,"神纹挑战要求登神境，并选择对应难度。");return;}}
        if(inTrial(p.getUUID())||QUEUE.stream().anyMatch(q->q.members.contains(p.getUUID()))){Gameplay.say(p,"你已在试炼或队列中。");return;}
        if(!leader(p.getUUID()).equals(p.getUUID())&&!breakthrough){Gameplay.say(p,"请由队长开启试炼。");return;}
        List<UUID> ids=breakthrough||tier>=4?List.of(p.getUUID()):members(p.getUUID());
        for(UUID id:ids){ServerPlayer member=p.server.getPlayerList().getPlayer(id);if(member==null||!member.level().dimension().equals(Gameplay.CITY)||inTrial(id)||Store.of(member).combatUntil>System.currentTimeMillis()){Gameplay.say(p,"全体队员须在线并回到安全主城。");return;}if(tier<4&&!breakthrough&&Store.of(member).stage<(tier-1)*6){Gameplay.say(p,"有队员境界不足。");return;}}
        Request r=new Request(p.getUUID(),ids,tier,breakthrough);for(int i=0;i<4;i++)if(SLOTS[i]==null){start(p.server,r,i);return;}QUEUE.addLast(r);Gameplay.say(p,"试炼场地已满，已加入队列，前方 "+(QUEUE.size()-1)+" 队。");
    }
    static void start(MinecraftServer server,Request r,int slot){
        ServerLevel level=server.getLevel(Gameplay.TRIAL);if(level==null)return;
        for(UUID id:r.members){ServerPlayer p=server.getPlayerList().getPlayer(id);if(p==null||!p.level().dimension().equals(Gameplay.CITY))return;}
        if(r.breakthrough&&r.tier<4){ServerPlayer p=server.getPlayerList().getPlayer(r.leader);Profile d=Store.of(p);if(d.stage>=17||d.xp<Rules.current.stageXp[d.stage]){Gameplay.say(p,"突破条件已变化。");return;}int realm=(d.stage+1)/3;Item[] materials={Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.ENDER_EYE,Items.NETHER_STAR};int[] amounts={8,8,4,4,1};if(realm<1||realm>5||!Gameplay.take(p,materials[realm-1],amounts[realm-1])){Gameplay.say(p,"突破材料不足："+materials[Math.max(0,Math.min(4,realm-1))].getDescription().getString()+" × "+amounts[Math.max(0,Math.min(4,realm-1))]);return;}}
        Session s=new Session(slot,r);s.originalStage=Store.of(server.getPlayerList().getPlayer(r.leader)).stage;SLOTS[slot]=s;clean(level,slot);force(level,slot,true);TrialMechanics.build(level,s);
        int j=0;for(UUID id:s.members){ServerPlayer p=server.getPlayerList().getPlayer(id);TeleportEffects.travel(p,level,new net.minecraft.world.phys.Vec3(slot*256-6+j++*4,65,25),180);s.bar.addPlayer(p);Store.of(p).meditating=false;Gameplay.say(p,(s.breakthrough?"突破挑战：":"试炼开始：")+NAMES[s.tier-1]+"。破解三道封印，迎战守阵首领。");}
    }
    static void force(ServerLevel l,int slot,boolean on){for(int x=slot*16-3;x<=slot*16+2;x++)for(int z=-3;z<=2;z++)l.setChunkForced(x,z,on);}
    static void clean(ServerLevel l,int slot){AABB box=new AABB(slot*256-46,45,-46,slot*256+46,140,46);for(Entity e:l.getEntities((Entity)null,box,e->e.getTags().contains("duskrain_trial")))e.discard();}
    static void wave(MinecraftServer server,Session s){
        ServerLevel l=server.getLevel(Gameplay.TRIAL);s.wave++;s.puzzleSolved=false;
        int number=s.wave==4?1:3+s.tier+s.members.size();
        for(int k=0;k<number;k++){
            EntityType<? extends Monster> type=s.wave==4?(s.tier>=4?TrialBoss.SENTINEL.get():s.tier==3?TrialBoss.DRAGON.get():s.tier==2?TrialBoss.ROCK.get():TrialBoss.WOOD.get()):k%3==0?EntityType.SKELETON:k%3==1?EntityType.ZOMBIE:EntityType.SPIDER;
            Monster m=type.create(l);if(m==null)continue;double angle=Math.PI*2*k/number;m.moveTo(s.slot*256+Math.cos(angle)*20,65,Math.sin(angle)*20,0,0);m.addTag("duskrain_trial");m.setPersistenceRequired();
            double health=s.tier>=4?(s.wave==4?AscensionRules.current.trialHealth[s.tier-4]:Math.max(40,AscensionRules.current.trialHealth[s.tier-4]/15)):(s.wave==4?90:22)*s.tier*(1+.3*(s.members.size()-1));m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);if(m instanceof TrialBoss boss&&boss.tier>=4)boss.divineMax((float)health);m.setHealth((float)health);m.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(s.tier>=4?AscensionRules.current.trialDamage[s.tier-4]:(s.wave==4?6:3)*s.tier);m.setCustomName(Component.literal(s.wave==4?(s.tier>=4?"天门神将 · "+NAMES[s.tier-1]:new String[]{"木灵·青虬","石傀·玄枢","雷蛟·烛霄"}[s.tier-1]):"试炼妖影 · 第"+s.wave+"波"));m.setCustomNameVisible(s.wave==4);
            if(m instanceof net.minecraft.world.entity.monster.AbstractSkeleton)m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
            l.addFreshEntity(m);s.mobs.add(m.getUUID());
        }
        for(UUID id:s.alive){ServerPlayer p=server.getPlayerList().getPlayer(id);if(p!=null)Gameplay.say(p,s.wave==4?"首领现身！":"第 "+s.wave+" / 3 波来袭。");}
    }
    public static void tick(MinecraftServer server){
        if(server.getTickCount()%10!=0)return;
        for(Session s:SLOTS){if(s==null)continue;ServerLevel l=server.getLevel(Gameplay.TRIAL);if(l==null)continue;
            s.alive.removeIf(id->{ServerPlayer p=server.getPlayerList().getPlayer(id);return p==null||!p.isAlive()||!p.level().dimension().equals(Gameplay.TRIAL);});
            if(s.alive.isEmpty()||System.currentTimeMillis()-s.start>Rules.current.trialSeconds*1000L){finish(server,s,false);continue;}
            s.mobs.removeIf(id->{Entity e=l.getEntity(id);return e==null||!e.isAlive();});
            TrialMechanics.tick(server,s);
            if(s.mobs.isEmpty()&&(s.puzzleSolved||s.wave>=3)){s.waitTicks-=10;if(s.waitTicks<=0){if(s.wave>=4)finish(server,s,true);else{wave(server,s);s.waitTicks=60;}}}
        }
        for(int i=0;i<4&&!QUEUE.isEmpty();i++)if(SLOTS[i]==null)start(server,QUEUE.removeFirst(),i);
    }
    static void finish(MinecraftServer server,Session s,boolean win){if(s.ended)return;s.ended=true;s.bar.removeAllPlayers();SLOTS[s.slot]=null;ServerLevel l=server.getLevel(Gameplay.TRIAL);if(l!=null){clean(l,s.slot);force(l,s.slot,false);}
        for(UUID id:s.members){ServerPlayer p=server.getPlayerList().getPlayer(id);if(p==null)continue;
            if(win&&s.alive.contains(id)){Gameplay.giveOrRetain(p,new ItemStack(s.tier==1?Items.IRON_INGOT:s.tier==2?Items.DIAMOND:Items.ENDER_PEARL,Math.max(1,s.tier*2)));Profile r=Store.of(p);r.trialWins++;Gameplay.progress(p,"trial",String.valueOf(s.tier));
                if(s.tier>=4&&s.breakthrough)Ascension.complete(p,s.originalStage);
                if(s.tier==3)Gameplay.giveOrRetain(p,new ItemStack(Ascension.CORE.get()));
                if(s.tier>=5&&!s.breakthrough)Gameplay.giveOrRetain(p,new ItemStack(Ascension.CRYSTAL.get(),s.tier-3));
                if(s.tier<4&&s.breakthrough&&r.stage<17&&r.xp>=Rules.current.stageXp[r.stage]){r.xp-=Rules.current.stageXp[r.stage];r.stage++;Gameplay.say(p,"突破成功！当前境界："+Rules.realm(r.stage));}
                Gameplay.reward(p,180*s.tier,150*s.tier);Gameplay.say(p,"试炼通关，奖励已领取。");
            }else Gameplay.say(p,"本次试炼结束，境界保留。");
            if(p.isAlive()&&p.level().dimension().equals(Gameplay.TRIAL))Gameplay.spawn(p);
        }
    }
    public static void died(ServerPlayer p){for(Session s:SLOTS)if(s!=null)s.alive.remove(p.getUUID());}
    public static void leave(ServerPlayer p,boolean disconnect){QUEUE.removeIf(r->r.members.contains(p.getUUID()));for(Session s:SLOTS)if(s!=null&&s.alive.remove(p.getUUID())){if(!disconnect&&p.isAlive())Gameplay.spawn(p);Gameplay.say(p,"已退出试炼，本次不发放奖励。");}}
    public static void recover(MinecraftServer s){Arrays.fill(SLOTS,null);QUEUE.clear();PARTIES.clear();INVITES.clear();ServerLevel l=s.getLevel(Gameplay.TRIAL);if(l!=null)for(int i=0;i<4;i++){force(l,i,false);clean(l,i);}}
    public static void shutdown(MinecraftServer s){for(Session session:SLOTS)if(session!=null)finish(s,session,false);QUEUE.clear();}
}
