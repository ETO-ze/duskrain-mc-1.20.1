package cn.duskrain;

import java.util.*;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit, resumable compare-before-write migration. Journals are durable before edits. */
public final class ArchitectureRepair {
    private static MinecraftServer active;
    private static final Deque<String> queue=new ArrayDeque<>();
    private static String scope="sample";
    private static boolean rollback;
    private static long changed,conflicts;
    private static Path directory(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("duskrain-architecture-v3");}
    private static Path manifest(MinecraftServer s){return directory(s).resolve("progress.json");}
    public static String status(){return "建筑修缮："+(active==null?"停止":"运行中")+"，待处理区块 "+queue.size()+"，变更 "+changed+"，跳过差异 "+conflicts;}
    static boolean compactJournal(CompoundTag log){
        var merged=new LinkedHashMap<Long,CompoundTag>();boolean changed=false;
        for(var t:log.getList("edits",10)){var n=(CompoundTag)t;var first=merged.get(n.getLong("pos"));if(first==null){merged.put(n.getLong("pos"),n.copy());continue;}
            // The first blueprint and its envelope pass may schedule the identical pending edit.
            // It has not been applied yet, so its before-state still equals the original state.
            if(first.getCompound("before").equals(n.getCompound("before"))&&first.getCompound("after").equals(n.getCompound("after"))){changed=true;continue;}
            // Consecutive repairs at one position are one reversible transaction.
            var prior=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),first.getCompound("after"));var before=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),n.getCompound("before"));
            if(!matches(prior,before))throw new IllegalStateException("修缮日志链不连续："+BlockPos.of(n.getLong("pos")));
            first.put("intermediate",n.getCompound("before").copy());first.put("after",n.getCompound("after").copy());changed=true;
        }
        if(changed){var edits=new ListTag();merged.values().forEach(edits::add);log.put("edits",edits);}return changed;
    }
    public static boolean matches(BlockState actual,BlockState expected){
        if(actual.equals(expected))return true;
        if(actual.getBlock()!=expected.getBlock())return false;
        // Neighbour-derived shapes are not player edits; orientation, half and all other properties are.
        for(var p:expected.getProperties()){
            String name=p.getName();boolean derived=expected.getBlock() instanceof net.minecraft.world.level.block.StairBlock&&name.equals("shape")
                ||expected.getBlock() instanceof net.minecraft.world.level.block.WallBlock&&(name.equals("up")||name.equals("north")||name.equals("south")||name.equals("east")||name.equals("west"))
                ||expected.getBlock() instanceof net.minecraft.world.level.block.FenceBlock&&(name.equals("north")||name.equals("south")||name.equals("east")||name.equals("west"))
                ||expected.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock&&name.equals("level");
            if(!derived&&!actual.getValue(p).equals(expected.getValue(p)))return false;
        }return true;
    }
    private static boolean naturalPool(BlockPos p,BlockState actual,BlockState desired){
        if(!WaterCourt.plan().containsKey(p)||p.getY()<50||p.getY()>68)return false;
        var b=actual.getBlock();
        // The approved public watercourt includes foundations from earlier road grading.
        // Only known masonry is reconciled here, outside all occupied building footprints.
        boolean publicMasonry=b==net.minecraft.world.level.block.Blocks.SEA_LANTERN||b==net.minecraft.world.level.block.Blocks.STONE_BRICKS||b==net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS||b==net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS;
        boolean oldLamp=b==net.minecraft.world.level.block.Blocks.DARK_PRISMARINE||b==Decor.RIDGE.get()||b==Decor.WADANG.get()||b==net.minecraft.world.level.block.Blocks.LANTERN||b==net.minecraft.world.level.block.Blocks.STRIPPED_DARK_OAK_LOG||b==net.minecraft.world.level.block.Blocks.DARK_PRISMARINE_SLAB;
        return publicMasonry||oldLamp||actual.isAir()||!actual.getFluidState().isEmpty()&&b instanceof net.minecraft.world.level.block.LiquidBlock
            ||b==net.minecraft.world.level.block.Blocks.DIRT||b==net.minecraft.world.level.block.Blocks.GRASS_BLOCK||b==net.minecraft.world.level.block.Blocks.GRAVEL||b==net.minecraft.world.level.block.Blocks.STONE||b==net.minecraft.world.level.block.Blocks.CLAY;
    }
    private static boolean fixtureSupport(BlockPos p,BlockState actual){
        boolean support=ArchitectureFinishes.bases().stream().anyMatch(b->b.getX()==p.getX()&&b.getZ()==p.getZ()&&p.getY()<b.getY()&&p.getY()>=Math.max(Terrain.ground(p.getX(),p.getZ())+1,b.getY()-24));
        var b=actual.getBlock();return support&&(actual.isAir()||b==net.minecraft.world.level.block.Blocks.WATER||b==net.minecraft.world.level.block.Blocks.DIRT||b==net.minecraft.world.level.block.Blocks.GRASS_BLOCK||b==net.minecraft.world.level.block.Blocks.STONE||b==net.minecraft.world.level.block.Blocks.STONE_BRICKS||b==net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS);
    }
    private static boolean legacyLamp(BlockPos p,BlockState actual,BlockState desired){
        return actual.is(net.minecraft.world.level.block.Blocks.STRIPPED_DARK_OAK_LOG)
            &&(desired.is(Architecture.get("lamp_shaft"))||desired.is(Architecture.get("lamp_crown")))
            &&ArchitectureFinishes.bases().stream().anyMatch(b->b.getX()==p.getX()&&b.getZ()==p.getZ()&&p.getY()>b.getY()&&p.getY()<=b.getY()+3);
    }
    private static boolean repairableMissing(ServerLevel level,BlockPos p,BlockState actual,BlockState expected,BlockState desired,boolean city){
        if(!actual.isAir())return false;
        if(desired.is(Architecture.get("palace_lantern")))return desired.canSurvive(level,p);
        // Only the authored first course of the registered guild facade; no interiors or door openings.
        return !city&&p.getY()==65&&expected.is(net.minecraft.world.level.block.Blocks.SMOOTH_QUARTZ)&&desired.is(Architecture.get("white_jade"));
    }
    static Set<BlockPos> shopEnvelope(int cx,int cz){Set<BlockPos> out=new HashSet<>();for(var b:PlayerMarket.SITES)for(int x=b.x()-9;x<=b.x()+9;x++)for(int z=b.z()-9;z<=b.z()+9;z++)if(x>>4==cx&&z>>4==cz&&(Math.abs(x-b.x())>5||Math.abs(z-b.z())>5))for(int y=b.y()+1;y<=b.y()+5;y++)out.add(new BlockPos(x,y,z));return out;}
    static boolean oldRoof(BlockState state){var b=state.getBlock();return b==net.minecraft.world.level.block.Blocks.DARK_PRISMARINE||b==net.minecraft.world.level.block.Blocks.DARK_PRISMARINE_SLAB||b==net.minecraft.world.level.block.Blocks.DARK_PRISMARINE_STAIRS||b==net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES||b==net.minecraft.world.level.block.Blocks.DEEPSLATE_TILE_STAIRS||b==net.minecraft.world.level.block.Blocks.DEEPSLATE_TILE_SLAB||b==Decor.WADANG.get()||b==Decor.RIDGE.get()||b==Decor.EAVE.get();}
    private static void progress(MinecraftServer s)throws Exception{
        Path p=manifest(s),tmp=p.resolveSibling("progress.new");Files.createDirectories(p.getParent());
        Files.writeString(tmp,Rules.JSON.toJson(Map.of("scope",scope,"rollback",rollback,"remaining",new ArrayList<>(queue),"changed",changed,"conflicts",conflicts,"complete",queue.isEmpty())));
        for(int attempt=0;;attempt++){try{Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);break;}
            catch(AccessDeniedException busy){if(attempt>=5)throw busy;Thread.sleep(5);}}
    }
    public static void start(MinecraftServer s,String mode){
        if(active!=null&&!mode.equals("status"))throw new IllegalStateException("已有建筑任务运行中");
        try{
        if(mode.equals("audit")){WaterCourtAudit.run(s);ExteriorAudit.run(s);return;}
        if(mode.equals("auditbuildings")){ArchitectureAudit.start(s);return;}
        if(mode.equals("status")){DuskRain.LOG.info(status());return;}
        queue.clear();changed=conflicts=0;rollback=mode.equals("rollback");
        if(mode.equals("resume")){
            var obj=com.google.gson.JsonParser.parseString(Files.readString(manifest(s))).getAsJsonObject();scope=obj.get("scope").getAsString();rollback=obj.get("rollback").getAsBoolean();changed=obj.get("changed").getAsLong();conflicts=obj.get("conflicts").getAsLong();for(var v:obj.getAsJsonArray("remaining"))queue.add(v.getAsString());
        }else if(rollback){try(var files=Files.list(directory(s))){files.filter(p->p.toString().endsWith(".nbt")).map(p->p.getFileName().toString().replace(".nbt","")).sorted(Comparator.reverseOrder()).forEach(queue::add);}}
        else {
            if(!mode.equals("sample")&&!mode.equals("all"))throw new IllegalArgumentException("使用 sample / all / resume / rollback / status");scope=mode;
            if(Files.exists(manifest(s))){var old=com.google.gson.JsonParser.parseString(Files.readString(manifest(s))).getAsJsonObject();if(!old.get("complete").getAsBoolean())throw new IllegalStateException("上次任务尚未完成，请 resume 或 rollback");}
            int lo=mode.equals("sample")?-2:-22,hi=mode.equals("sample")?7:22,zlo=mode.equals("sample")?9:-22,zhi=mode.equals("sample")?17:22;
            for(int cx=lo;cx<=hi;cx++)for(int cz=zlo;cz<=zhi;cz++)queue.add("city_"+cx+"_"+cz);
            if(mode.equals("all"))for(var g:GuildData.get(s).guilds.values())if(g.id==1&&g.palace==2&&!g.archived)for(int cx=-7;cx<=6;cx++)for(int cz=-7;cz<=6;cz++)queue.add("guild_"+(g.x()/16+cx)+"_"+(g.z()/16+cz));
        }
        active=s;progress(s);
    }catch(Exception e){active=null;queue.clear();throw new IllegalStateException("建筑修缮未启动："+e.getMessage(),e);}}
    public static Map<BlockPos,BlockState> blueprint(int cx,int cz,boolean old){
        Map<BlockPos,BlockState> out=new LinkedHashMap<>();boolean before=Architecture.LEGACY.get();Architecture.LEGACY.set(old);
        try{CityPlan.Sink sink=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState b){if(x>>4==cx&&z>>4==cz&&y>=0&&y<256)out.put(new BlockPos(x,y,z),b);}};for(int phase=0;phase<6;phase++)CityPlan.paint(sink,cx,cz,phase);}finally{Architecture.LEGACY.set(before);}return out;
    }
    static Map<BlockPos,BlockState> previousBlueprint(int cx,int cz){boolean before=CourtGarden.DISABLED.get();CourtGarden.DISABLED.set(true);try{return blueprint(cx,cz,false);}finally{CourtGarden.DISABLED.set(before);}}
    private static final Map<Boolean,Map<BlockPos,BlockState>> palace=new HashMap<>();
    private static Map<BlockPos,BlockState> palace(boolean old){return palace.computeIfAbsent(old,key->{
        boolean before=Architecture.LEGACY.get();Architecture.LEGACY.set(old);GuildPalace.planned=false;GuildPalace.BLOCKS.clear();GuildPalace.TAGS.clear();GuildPalace.BRIDGE_DECK.clear();
        try{GuildPalace.plan();var out=new LinkedHashMap<BlockPos,BlockState>();GuildPalace.BLOCKS.values().forEach(out::putAll);return out;}finally{Architecture.LEGACY.set(before);GuildPalace.planned=false;GuildPalace.BLOCKS.clear();GuildPalace.TAGS.clear();GuildPalace.BRIDGE_DECK.clear();}
    });}
    static Map<BlockPos,BlockState> guild(MinecraftServer s,int cx,int cz,boolean old){var g=GuildData.get(s).guilds.get(1);var out=new LinkedHashMap<BlockPos,BlockState>();if(g==null)return out;for(var e:palace(old).entrySet()){var p=e.getKey().offset(g.x(),0,g.z());if(p.getX()>>4==cx&&p.getZ()>>4==cz)out.put(p,e.getValue());}return out;}
    public static void tick(MinecraftServer s){ArchitectureAudit.tick(s);if(active!=s)return;if(queue.isEmpty()){active=null;palace.clear();return;}
        String key=queue.peekFirst();try{
            String[] v=key.split("_");boolean city=v[0].equals("city");int cx=Integer.parseInt(v[1]),cz=Integer.parseInt(v[2]);ServerLevel level=s.getLevel(city?Gameplay.CITY:Guilds.DIM);
            if(level==null)throw new IllegalStateException("目标维度未加载");level.getChunk(cx,cz);Path journal=directory(s).resolve(key+".nbt");CompoundTag log;
            if(Files.exists(journal)){
                log=NbtIo.readCompressed(journal.toFile());
                if(!rollback&&city){ListTag remaining=new ListTag();boolean retried=false;for(var t:log.getList("skipped",10)){var n=(CompoundTag)t;BlockPos p=BlockPos.of(n.getLong("pos"));var actual=level.getBlockState(p);var desired=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),n.getCompound("desired"));var recorded=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),n.getCompound("actual"));
                    if(level.getBlockEntity(p)==null&&matches(actual,recorded)&&(naturalPool(p,actual,desired)||CourtGarden.plan().containsKey(p)&&(CourtGarden.natural(actual)||CourtGarden.obsoleteLamp(p,actual)))){CompoundTag edit=new CompoundTag();edit.putLong("pos",p.asLong());edit.put("before",NbtUtils.writeBlockState(actual));edit.put("after",NbtUtils.writeBlockState(desired));log.getList("edits",10).add(edit);retried=true;}else remaining.add(n);
                }if(retried){log.put("skipped",remaining);Path tmp=journal.resolveSibling(journal.getFileName()+".retry");NbtIo.writeCompressed(log,tmp.toFile());Files.move(tmp,journal,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}}
            }else{
                if(rollback)throw new IllegalStateException("回退记录缺失");
                Map<BlockPos,BlockState> old=city?blueprint(cx,cz,true):guild(s,cx,cz,true),next=city?blueprint(cx,cz,false):guild(s,cx,cz,false);
                Map<BlockPos,BlockState> baseline=city?previousBlueprint(cx,cz):old;Map<BlockPos,BlockState> prior=new HashMap<>();for(String version:List.of("v1","v2")){Path previous=s.getWorldPath(LevelResource.ROOT).resolve("duskrain-architecture-"+version).resolve(key+".nbt");
                if(Files.exists(previous))for(var t:NbtIo.readCompressed(previous.toFile()).getList("edits",10)){var n=(CompoundTag)t;prior.put(BlockPos.of(n.getLong("pos")),NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),n.getCompound("after")));}}
                Set<BlockPos> positions=new LinkedHashSet<>(old.keySet());positions.addAll(prior.keySet());positions.addAll(next.keySet());ListTag edits=new ListTag(),skips=new ListTag();
                for(var p:positions){BlockState expected=old.getOrDefault(p,city?Terrain.block(p.getX(),p.getY(),p.getZ()):net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());BlockState desired=next.getOrDefault(p,city?Terrain.block(p.getX(),p.getY(),p.getZ()):net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    BlockState actual=level.getBlockState(p);if(actual.equals(desired))continue;
                    if(expected.equals(desired)&&!(city&&(WaterCourt.plan().containsKey(p)||CourtGarden.plan().containsKey(p)))&&!prior.containsKey(p))continue;
                    // Any block entity is kept intact, including empty player containers and signs.
                    if(level.getBlockEntity(p)!=null||!(matches(actual,expected)||city&&matches(actual,baseline.getOrDefault(p,Terrain.block(p.getX(),p.getY(),p.getZ())))||prior.containsKey(p)&&matches(actual,prior.get(p))||Architecture.material(actual).equals(desired)||city&&(naturalPool(p,actual,desired)||CourtGarden.plan().containsKey(p)&&(CourtGarden.natural(actual)||CourtGarden.obsoleteLamp(p,actual))||fixtureSupport(p,actual)||legacyLamp(p,actual,desired))||repairableMissing(level,p,actual,expected,desired,city))){CompoundTag n=new CompoundTag();n.putLong("pos",p.asLong());n.put("actual",NbtUtils.writeBlockState(actual));n.put("expected",NbtUtils.writeBlockState(expected));n.put("desired",NbtUtils.writeBlockState(desired));skips.add(n);continue;}
                    CompoundTag n=new CompoundTag();n.putLong("pos",p.asLong());n.put("before",NbtUtils.writeBlockState(actual));n.put("after",NbtUtils.writeBlockState(desired));edits.add(n);
                }
                log=new CompoundTag();log.putString("dimension",level.dimension().location().toString());log.put("edits",edits);log.put("skipped",skips);log.putLongArray("rails",next.entrySet().stream().filter(e->e.getValue().is(Architecture.get("jade_railing"))||e.getValue().is(Architecture.get("carved_railing"))).mapToLong(e->e.getKey().asLong()).toArray());
                Path tmp=journal.resolveSibling(journal.getFileName()+".new");NbtIo.writeCompressed(log,tmp.toFile());Files.move(tmp,journal,StandardCopyOption.ATOMIC_MOVE);
            }
            if(city&&!rollback&&cx>=-2&&cx<=1&&cz>=10&&cz<=17){var plan=blueprint(cx,cz,false);boolean extra=false;for(var p:shopEnvelope(cx,cz)){var actual=level.getBlockState(p);var desired=plan.getOrDefault(p,Terrain.block(p.getX(),p.getY(),p.getZ()));if(desired.isAir()&&oldRoof(actual)&&level.getBlockEntity(p)==null){CompoundTag n=new CompoundTag();n.putLong("pos",p.asLong());n.put("before",NbtUtils.writeBlockState(actual));n.put("after",NbtUtils.writeBlockState(desired));log.getList("edits",10).add(n);extra=true;}}
                if(extra){Path tmp=journal.resolveSibling(journal.getFileName()+".envelope");NbtIo.writeCompressed(log,tmp.toFile());Files.move(tmp,journal,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}}
            // Upgrade the top landing without losing the original rollback state.
            if(city&&!rollback){boolean revised=false;WaterCourt.plan();Set<BlockPos> corrections=new LinkedHashSet<>();for(var e:WaterCourt.EXITS){var toward=e.outward().getOpposite();for(int k=0;k<2;k++)corrections.add(new BlockPos(e.x(),e.bankY(),e.z()).relative(toward).relative(toward.getClockWise(),k));}for(int z=174;z<=269;z+=8)corrections.add(new BlockPos(24,WaterCourt.promenadeY(z),z));for(var p:corrections){if(p.getX()>>4!=cx||p.getZ()>>4!=cz||!level.getBlockState(p).is(Architecture.get("white_jade")))continue;
                var desired=WaterCourt.plan().get(p);if(desired==null||!(desired.is(Architecture.get("jade_stairs"))||desired.is(Architecture.get("jade_light"))))continue;CompoundTag edit=null;for(var t:log.getList("edits",10)){var n=(CompoundTag)t;if(n.getLong("pos")==p.asLong())edit=n;}if(edit!=null&&!matches(level.getBlockState(p),NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),edit.getCompound("after"))))continue;
                if(edit==null){edit=new CompoundTag();edit.putLong("pos",p.asLong());edit.put("before",NbtUtils.writeBlockState(level.getBlockState(p)));log.getList("edits",10).add(edit);}edit.put("intermediate",NbtUtils.writeBlockState(level.getBlockState(p)));edit.put("after",NbtUtils.writeBlockState(desired));revised=true;
            }if(revised){Path tmp=journal.resolveSibling(journal.getFileName()+".landing");NbtIo.writeCompressed(log,tmp.toFile());Files.move(tmp,journal,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}}
            if(compactJournal(log)){Path tmp=journal.resolveSibling(journal.getFileName()+".compact");NbtIo.writeCompressed(log,tmp.toFile());Files.move(tmp,journal,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            conflicts+=log.getList("skipped",10).size();
            for(var t:log.getList("edits",10)){var n=(CompoundTag)t;BlockPos p=BlockPos.of(n.getLong("pos"));BlockState before=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),n.getCompound(rollback?"after":"before")),after=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),n.getCompound(rollback?"before":"after"));
                if(matches(level.getBlockState(p),after)&&(!(after.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)||level.getBlockState(p).equals(after)))continue;boolean intermediate=!rollback&&n.contains("intermediate")&&matches(level.getBlockState(p),NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),n.getCompound("intermediate")));if(level.getBlockEntity(p)!=null||!(matches(level.getBlockState(p),before)||intermediate||!rollback&&before.getBlock() instanceof net.minecraft.world.level.block.BushBlock&&level.getBlockState(p).isAir())){conflicts++;continue;}level.setBlock(p,after,18);changed++;
            }
            for(long packed:log.getLongArray("rails")){BlockPos p=BlockPos.of(packed);BlockState state=level.getBlockState(p);
                if(state.is(Architecture.get("jade_railing"))||state.is(Architecture.get("carved_railing"))){BlockState connected=state;for(Direction d:Direction.Plane.HORIZONTAL)connected=connected.updateShape(d,level.getBlockState(p.relative(d)),level,p,p.relative(d));if(!state.equals(connected))level.setBlock(p,connected,3);}}
            // Persist chunk data before acknowledging it, so a crash can safely replay the journal.
            level.getChunkSource().save(true);queue.removeFirst();progress(s);
            if(queue.isEmpty()){s.saveEverything(false,true,true);DuskRain.LOG.info("ARCHITECTURE_REPAIR complete scope={} changed={} conflicts={} rollback={}",scope,changed,conflicts,rollback);active=null;palace.clear();if(!rollback)ArchitectureDeployment.completed(s);}
        }catch(Exception e){active=null;DuskRain.LOG.error("建筑修缮已暂停；修复原因后使用 resume，记录保留在 {}",directory(s),e);}
    }
    public static void stop(MinecraftServer s){if(active==s)active=null;ArchitectureAudit.stop(s);palace.clear();}
}
