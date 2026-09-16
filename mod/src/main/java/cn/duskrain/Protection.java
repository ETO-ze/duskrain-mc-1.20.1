package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.*;
import java.util.*;

public final class Protection {
    public static final Set<UUID> MAINTENANCE=new HashSet<>();
    public static boolean city(LevelAccessor l){return l instanceof Level level&&(level.dimension().equals(Gameplay.CITY)||level.dimension().equals(Gameplay.INTRO)||level.dimension().equals(Gameplay.DEMO));}
    public static boolean admin(Player p){return p.hasPermissions(3)&&MAINTENANCE.contains(p.getUUID());}
    public static boolean protectedAt(LevelAccessor l,BlockPos pos){if(city(l))return true;if(l instanceof Level g&&g.dimension().equals(Guilds.DIM))return true;if(l instanceof ServerLevel s&&s.dimension().equals(Level.OVERWORLD))return Store.get(s.getServer()).claims.containsKey(new ChunkPos(pos).toLong());return l instanceof Level level&&level.dimension().equals(Gameplay.TRIAL);}
    public static boolean canEdit(Player p,LevelAccessor l,BlockPos pos){if(admin(p))return true;if(l instanceof Level g&&g.dimension().equals(Guilds.DIM))return Guilds.edit(p,l,pos);if(city(l))return false;if(l instanceof Level level&&level.dimension().equals(Gameplay.TRIAL))return false;if(l instanceof ServerLevel s&&s.dimension().equals(Level.OVERWORLD)){Store.Claim c=Store.get(s.getServer()).claims.get(new ChunkPos(pos).toLong());return c==null||c.allows(p.getUUID());}return true;}
    public static boolean pvpAllowed(ServerPlayer p){if(p.level().dimension().equals(Guilds.DIM))return false;if(p.level().dimension().equals(Gameplay.INTRO)||p.level().dimension().equals(Gameplay.DEMO)||p.level().dimension().equals(Gameplay.TRIAL))return false;if(!p.level().dimension().equals(Gameplay.CITY))return true;CityPlan.Building b=CityPlan.find("arena");return Store.of(p).arena&&Math.abs(p.getX()-b.x())<18&&Math.abs(p.getZ()-b.z())<16;}
    public static boolean canFight(ServerPlayer a,ServerPlayer b){return pvpAllowed(a)&&pvpAllowed(b)&&a.level()==b.level();}
    public static boolean sameDomain(LevelAccessor l,BlockPos a,BlockPos b){if(l instanceof ServerLevel level&&level.dimension().equals(Guilds.DIM)){var ga=GuildData.get(level.getServer()).at(a.getX(),a.getZ());return ga!=null&&ga.ready&&ga==GuildData.get(level.getServer()).at(b.getX(),b.getZ());}if(city(l))return true;if(l instanceof Level g&&g.dimension().equals(Guilds.DIM))return true;if(l instanceof ServerLevel s&&s.dimension().equals(Level.OVERWORLD)){Store data=Store.get(s.getServer());Store.Claim ca=data.claims.get(new ChunkPos(a).toLong()),cb=data.claims.get(new ChunkPos(b).toLong());return ca!=null&&cb!=null&&ca.owner.equals(cb.owner);}return false;}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void breakBlock(BlockEvent.BreakEvent e){if(!canEdit(e.getPlayer(),e.getLevel(),e.getPos())){e.setCanceled(true);if(e.getPlayer() instanceof ServerPlayer p)SignRepair.rejected(p,e.getPos());}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void signAttack(PlayerInteractEvent.LeftClickBlock e){
        if(e.getEntity() instanceof ServerPlayer p&&!canEdit(p,e.getLevel(),e.getPos())&&e.getLevel().getBlockEntity(e.getPos()) instanceof net.minecraft.world.level.block.entity.SignBlockEntity){e.setCanceled(true);SignRepair.rejected(p,e.getPos());}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void place(BlockEvent.EntityPlaceEvent e){if(e.getEntity() instanceof Player p){if(!canEdit(p,e.getLevel(),e.getPos()))e.setCanceled(true);}else if(protectedAt(e.getLevel(),e.getPos()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void multi(BlockEvent.EntityMultiPlaceEvent e){if(e.getEntity() instanceof Player p)for(var b:e.getReplacedBlockSnapshots())if(!canEdit(p,e.getLevel(),b.getPos())){e.setCanceled(true);break;}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getEntity() instanceof ServerPlayer p)||admin(p))return;
        if(p.level().dimension().equals(Guilds.DIM)&&Guilds.returnPortal(p,e.getPos())){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);return;}
        if(p.level().dimension().equals(Gameplay.CITY)){for(String id:List.of("guildgate_west","guildgate_east")){var b=CityPlan.find(id);if(e.getPos().equals(new BlockPos(b.x(),b.y()+1,b.z()-1))){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)Guilds.act(p,"enter","");return;}}}
        if(p.level().dimension().equals(Gameplay.CITY)&&PlayerMarket.at(e.getPos())!=null){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)PlayerMarket.open(p,PlayerMarket.at(e.getPos()));return;}
        if(p.level().dimension().equals(Gameplay.TRIAL)&&TrialMechanics.interact(p,e.getPos())){e.setCanceled(true);return;}
        if(p.level().dimension().equals(Gameplay.CITY)&&Services.byBlock(e.getPos())!=null){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)Services.open(p,Services.byBlock(e.getPos()));return;}
        if(e.getLevel().getBlockEntity(e.getPos()) instanceof net.minecraft.world.level.block.entity.SignBlockEntity&&!canEdit(p,e.getLevel(),e.getPos()))SignRepair.rejected(p,e.getPos());
        Block block=e.getLevel().getBlockState(e.getPos()).getBlock();
        if(city(e.getLevel())){
            // Bells are scenery and may ring. Shops are only the registered entities/counters above.
            if(block==Blocks.BELL&&p.level().dimension().equals(Gameplay.CITY))return;
            if(block instanceof DoorBlock||block instanceof FenceGateBlock)return;
            if((block==Blocks.CRAFTING_TABLE||block==Blocks.SMITHING_TABLE||block==Blocks.ANVIL||((block==Blocks.ENCHANTING_TABLE||block==Blocks.GRINDSTONE)&&Math.abs(e.getPos().getX()-CityPlan.find("enchanter").x())<=10&&Math.abs(e.getPos().getZ()-CityPlan.find("enchanter").z())<=10))&&p.level().dimension().equals(Gameplay.CITY))return;
            e.setCanceled(true);
        }else if(!canEdit(p,e.getLevel(),e.getPos()))e.setCanceled(true);
    }
    @SubscribeEvent public void bucket(FillBucketEvent e){if(e.getTarget()!=null&&e.getTarget().getType()==net.minecraft.world.phys.HitResult.Type.BLOCK){var hit=(net.minecraft.world.phys.BlockHitResult)e.getTarget();if(!canEdit(e.getEntity(),e.getLevel(),hit.getBlockPos())||!canEdit(e.getEntity(),e.getLevel(),hit.getBlockPos().relative(hit.getDirection())))e.setCanceled(true);}}
    @SubscribeEvent public void explosion(ExplosionEvent.Detonate e){e.getAffectedBlocks().removeIf(p->protectedAt(e.getLevel(),p));e.getAffectedEntities().removeIf(x->city(x.level()));}
    @SubscribeEvent public void fluid(BlockEvent.FluidPlaceBlockEvent e){if(protectedAt(e.getLevel(),e.getPos()))e.setCanceled(true);}
    @SubscribeEvent public void piston(PistonEvent.Pre e){if(e.getLevel() instanceof ServerLevel guild&&guild.dimension().equals(Guilds.DIM)){for(var direction:net.minecraft.core.Direction.values())if(!sameDomain(guild,e.getPos(),e.getPos().relative(direction,13))){e.setCanceled(true);return;}}if(city(e.getLevel())){e.setCanceled(true);return;}if(e.getLevel() instanceof ServerLevel s&&s.dimension().equals(Level.OVERWORLD)){Store data=Store.get(s.getServer());Store.Claim origin=data.claims.get(new ChunkPos(e.getPos()).toLong());int x=e.getPos().getX(),z=e.getPos().getZ();for(int cx=(x-13)>>4;cx<=(x+13)>>4;cx++)for(int cz=(z-13)>>4;cz<=(z+13)>>4;cz++){Store.Claim c=data.claims.get(ChunkPos.asLong(cx,cz));if(c!=null&&(origin==null||!origin.owner.equals(c.owner))){e.setCanceled(true);return;}}}}
    @SubscribeEvent public void mobGrief(EntityMobGriefingEvent e){if(protectedAt(e.getEntity().level(),e.getEntity().blockPosition()))e.setResult(Event.Result.DENY);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void damage(LivingAttackEvent e){
        if(e.getEntity() instanceof ServerPlayer victim){if(e.getSource().getEntity() instanceof ServerPlayer attacker&&!canFight(attacker,victim))e.setCanceled(true);else if(city(victim.level())&&!pvpAllowed(victim))e.setCanceled(true);}
        else if(city(e.getEntity().level())&&e.getSource().getEntity() instanceof Player p&&!admin(p)){if(e.getEntity().getTags().contains("duskrain_dummy")&&p instanceof ServerPlayer sp){Gameplay.progress(sp,"practice","dummy");Skills.arc(sp,2.5,Skills.SWORD);}e.setCanceled(true);}
    }
    @SubscribeEvent public void interactEntity(PlayerInteractEvent.EntityInteract e){if(e.getLevel().isClientSide&&Services.byEntity(e.getTarget())!=null)return;if(e.getEntity() instanceof ServerPlayer p&&p.level().dimension().equals(Gameplay.CITY)&&Services.byEntity(e.getTarget())!=null){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);if(e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)Services.open(p,Services.byEntity(e.getTarget()));return;}if(!canEdit(e.getEntity(),e.getLevel(),e.getTarget().blockPosition()))e.setCanceled(true);}
    @SubscribeEvent public void spawn(MobSpawnEvent.FinalizeSpawn e){if(city(e.getLevel())||e.getLevel() instanceof ServerLevel l&&l.dimension().equals(Guilds.DIM))e.setSpawnCancelled(true);}
    @SubscribeEvent public void load(ChunkEvent.Load e){if(e.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)SignRepair.loaded(chunk);}
}
