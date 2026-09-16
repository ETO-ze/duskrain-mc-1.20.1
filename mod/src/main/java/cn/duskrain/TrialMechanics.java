package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class TrialMechanics {
    static final int[][] NODES={{-23,10},{0,-24},{23,10}};
    static BlockPos node(Trials.Session s,int n){return new BlockPos(s.slot*256+NODES[n][0],66,NODES[n][1]);}
    static void say(MinecraftServer server,Trials.Session s,String text){for(UUID id:s.alive){ServerPlayer p=server.getPlayerList().getPlayer(id);if(p!=null)Gameplay.say(p,text);}}
    public static void build(ServerLevel l,Trials.Session s){
        int ox=s.slot*256;
        for(int x=-39;x<=39;x++)for(int z=-39;z<=39;z++){
            if(Math.abs(x)==39||Math.abs(z)==39){l.setBlock(new BlockPos(ox+x,65,z),Blocks.STONE_BRICK_WALL.defaultBlockState(),2);continue;}
            if(Math.abs(x)<3||Math.abs(z)<3||Math.abs(Math.hypot(x,z)-23)<1)l.setBlock(new BlockPos(ox+x,64,z),s.tier==1?Blocks.MOSS_BLOCK.defaultBlockState():s.tier==2?Blocks.CUT_COPPER.defaultBlockState():Blocks.DARK_PRISMARINE.defaultBlockState(),2);
        }
        for(int i=0;i<3;i++){BlockPos n=node(s,i);l.setBlock(n.below(),Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),2);l.setBlock(n,s.tier==1?Blocks.BELL.defaultBlockState():s.tier==2?Blocks.LODESTONE.defaultBlockState():Blocks.LIGHTNING_ROD.defaultBlockState(),2);
            l.setBlock(n.north(),Blocks.SEA_LANTERN.defaultBlockState(),2);
        }
        for(int i=0;i<12;i++){double a=i*Math.PI/6;int x=ox+(int)(Math.cos(a)*34),z=(int)(Math.sin(a)*34);
            if(s.tier==1)l.setBlock(new BlockPos(x,64,z),Blocks.MOSS_BLOCK.defaultBlockState(),3);
            for(int y=65;y<=70;y++)l.setBlock(new BlockPos(x,y,z),s.tier==1?Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.STAGE,1):s.tier==2?Blocks.POLISHED_BASALT.defaultBlockState():Blocks.QUARTZ_PILLAR.defaultBlockState(),2);
            l.setBlock(new BlockPos(x,71,z),s.tier==1?Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true):Blocks.SEA_LANTERN.defaultBlockState(),2);
        }
        hint(l.getServer(),s);
    }
    static void hint(MinecraftServer server,Trials.Session s){String[] clues={"循声问阵：依次敲响发光的风铃，三声齐鸣才能打开封印。","玄岩机枢：右键旋转三座导能石；当前方向与目标方向会显示。","引雷问心：依次触碰发光避雷柱，避开地面的雷圈。"};say(server,s,s.tier>=4?"登神三关：问心阵、神躯试与天门神将；循光解开三处阵眼。":clues[s.tier-1]);}
    static int wanted(Trials.Session s){return Math.floorMod(s.wave+s.puzzleStep,3);}
    public static boolean interact(ServerPlayer p,BlockPos pos){Trials.Session s=Arrays.stream(Trials.SLOTS).filter(t->t!=null&&t.alive.contains(p.getUUID())).findFirst().orElse(null);if(s==null)return false;
        int index=-1;for(int i=0;i<3;i++)if(node(s,i).equals(pos)){index=i;break;}if(index<0)return false;
        if(p.distanceToSqr(Vec3.atCenterOf(pos))>36)return true;
        if(s.puzzleSolved||!s.mobs.isEmpty()||s.wave>=3){Gameplay.say(p,"先应对当前战斗，再继续破解阵法。");return true;}
        if(s.tier==2){s.orientation[index]=(s.orientation[index]+1)%4;int need=Math.floorMod(s.wave+index+1,4);Gameplay.say(p,"导能石"+(index+1)+"："+s.orientation[index]+" / 目标 "+need);boolean solved=true;for(int i=0;i<3;i++)if(s.orientation[i]!=Math.floorMod(s.wave+i+1,4))solved=false;if(!solved)return true;}
        else{if(index!=wanted(s)){s.puzzleStep=0;Vec3 at=Vec3.atCenterOf(pos);SpellFx.send(p.serverLevel(),SpellFx.ERROR,at,at,3,30);say(p.server,s,"敲击错误，顺序已重置。请寻找标有「第1鸣」的光柱。");return true;}p.serverLevel().playSound(null,pos,SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.BLOCKS,1,1+s.puzzleStep*.25f);s.puzzleStep++;if(s.puzzleStep<3){SpellFx.send(p.serverLevel(),SpellFx.SWORD,Vec3.atCenterOf(pos).add(0,1,0),Vec3.atCenterOf(node(s,wanted(s))).add(0,1,0),0,20);say(p.server,s,"第"+s.puzzleStep+"鸣已点亮，循光前往第"+(s.puzzleStep+1)+"鸣。");return true;}}
        s.puzzleSolved=true;s.puzzleStep=0;Arrays.fill(s.orientation,0);s.waitTicks=40;for(UUID id:s.alive){ServerPlayer member=p.server.getPlayerList().getPlayer(id);if(member!=null)Gameplay.progress(member,"puzzle",String.valueOf(s.tier));}say(p.server,s,"第"+(s.wave+1)+"道封印解除，准备迎战！");return true;
    }
    public static void tick(MinecraftServer server,Trials.Session s){ServerLevel l=server.getLevel(Gameplay.TRIAL);if(l==null)return;
        if(server.getTickCount()%20==0&&s.tier!=2&&s.wave<3){
            for(int i=0;i<3;i++){boolean done=s.puzzleSolved;for(int step=0;step<s.puzzleStep;step++)if(i==Math.floorMod(s.wave+step,3))done=true;Vec3 at=Vec3.atCenterOf(node(s,i)).add(0,.8,0);
                if(done)SpellFx.send(l,new SpellFx.Event(SpellFx.GUARD,at,at,1.1f,22,4));
                else if(s.mobs.isEmpty()&&!s.puzzleSolved&&i==wanted(s))SpellFx.send(l,new SpellFx.Event(SpellFx.BELL,at,at,1.8f,22,s.puzzleStep+1));
            }
        }
        if(s.mobs.isEmpty()&&!s.puzzleSolved&&s.wave<3&&server.getTickCount()%20==0){if(s.tier!=2){BlockPos p=node(s,wanted(s));l.sendParticles(ParticleTypes.END_ROD,p.getX()+.5,p.getY()+1.3,p.getZ()+.5,5,.2,.4,.2,.01);}else for(int i=0;i<3;i++)if(s.orientation[i]==Math.floorMod(s.wave+i+1,4)){BlockPos p=node(s,i);l.sendParticles(ParticleTypes.HAPPY_VILLAGER,p.getX()+.5,68,p.getZ()+.5,3,.2,.2,.2,.01);}}
        if(s.wave==4)for(UUID id:s.mobs){Entity e=l.getEntity(id);if(e instanceof TrialBoss boss&&server.getTickCount()%20==0){float fraction=boss.getHealth()/boss.getMaxHealth();s.bar.setProgress(Math.max(0,fraction));s.bar.setName(Component.literal(boss.getName().getString()+" · "+(fraction<.5?"第二阶段":"第一阶段")));}}
    }
}
