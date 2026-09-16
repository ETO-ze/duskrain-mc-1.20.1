package cn.duskrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraftforge.registries.*;
import java.util.*;
import java.util.concurrent.*;

public final class CityGenerator extends ChunkGenerator {
    public static final Codec<CityGenerator> CODEC=RecordCodecBuilder.create(i->i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(g->g.biomeSource),Codec.STRING.fieldOf("kind").forGetter(g->g.kind)).apply(i,CityGenerator::new));
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,DuskRain.ID);
    static {GENERATORS.register("city",()->CODEC);}
    public final String kind;
    public CityGenerator(BiomeSource source,String kind){super(source);this.kind=kind;}
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager sm,ChunkAccess chunk){
        return CompletableFuture.supplyAsync(()->{
            ChunkPos cp=chunk.getPos();int x0=cp.getMinBlockX(),z0=cp.getMinBlockZ();
            CityPlan.Sink sink=new CityPlan.Sink(){
                @Override public void block(int x,int y,int z,BlockState s){if(x>=x0&&x<x0+16&&z>=z0&&z<z0+16&&y>=0&&y<256)chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x&15,y&15,z&15,s,false);}
                @Override public void entity(CompoundTag n){if((n.getInt("x")>>4)==cp.x&&(n.getInt("z")>>4)==cp.z)chunk.setBlockEntityNbt(n);}
            };
            if(!kind.equals("trial")){
                for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
                    int top=Math.max(Math.max(Terrain.ground(x,z),Terrain.water(x,z)),Terrain.skyTop(x,z));
                    for(int y=0;y<=top;y++)sink.block(x,y,z,Terrain.block(x,y,z));
                }
                if(kind.equals("city"))for(int phase=0;phase<6;phase++)CityPlan.paint(sink,cp.x,cp.z,phase);
            }else{
                for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++)for(int slot=0;slot<4;slot++){
                    int dx=x-slot*256,dz=z;
                    if(Math.abs(dx)<=45&&Math.abs(dz)<=45){sink.block(x,64,z,Blocks.POLISHED_DEEPSLATE.defaultBlockState());if(Math.abs(dx)==45||Math.abs(dz)==45)for(int y=65;y<74;y++)sink.block(x,y,z,y%4==0?Blocks.SEA_LANTERN.defaultBlockState():CityPlan.WHITE);}
                }
            }
            Heightmap.primeHeightmaps(chunk,EnumSet.allOf(Heightmap.Types.class));return chunk;
        },executor);
    }
    @Override public void applyCarvers(WorldGenRegion r,long seed,RandomState rs,BiomeManager bm,StructureManager sm,ChunkAccess c,GenerationStep.Carving step){}
    @Override public void buildSurface(WorldGenRegion r,StructureManager sm,RandomState rs,ChunkAccess c){}
    // The authored city owns every tree, plant and path. Vanilla biome features would
    // otherwise grow a second forest through courtyards and roofs during FEATURES.
    @Override public void applyBiomeDecoration(WorldGenLevel l,ChunkAccess c,StructureManager s){}
    @Override public void spawnOriginalMobs(WorldGenRegion r){}
    @Override public int getGenDepth(){return 256;}
    @Override public int getSeaLevel(){return Terrain.SEA;}
    @Override public int getMinY(){return 0;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor a,RandomState r){return kind.equals("trial")?65:CityPlan.height(x,z)+1;}
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor a,RandomState r){int h=getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,a,r);BlockState[] s=new BlockState[256];Arrays.fill(s,Blocks.AIR.defaultBlockState());for(int y=0;y<h;y++)s[y]=Blocks.STONE.defaultBlockState();return new NoiseColumn(0,s);}
    @Override public void addDebugScreenInfo(List<String> text,RandomState random,BlockPos pos){text.add("DuskRain / "+kind+" / 205255670");}
}
