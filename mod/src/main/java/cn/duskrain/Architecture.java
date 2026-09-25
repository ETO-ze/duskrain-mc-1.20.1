package cn.duskrain;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.RegistryObject;

/** Separate identities: original Minecraft blocks are never globally retextured. */
public final class Architecture {
    public static final ThreadLocal<Boolean> LEGACY=ThreadLocal.withInitial(()->false);
    public static final Map<String,RegistryObject<Block>> ALL=new LinkedHashMap<>();
    private static final Map<Block,RegistryObject<Block>> REPLACE=new IdentityHashMap<>();
    static RegistryObject<Block> add(String id,Supplier<Block> factory){var r=Decor.BLOCKS.register(id,factory);ALL.put(id,r);Content.ITEMS.register(id,()->new BlockItem(r.get(),new Item.Properties()));return r;}
    static BlockBehaviour.Properties stone(){return BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS);}
    static void cube(String id,Block source){REPLACE.put(source,add(id,()->source instanceof RotatedPillarBlock?new RotatedPillarBlock(BlockBehaviour.Properties.copy(source)):new Block(BlockBehaviour.Properties.copy(source))));}
    static void stair(String id,Block source){REPLACE.put(source,add(id,()->new StairBlock(()->get("white_jade").defaultBlockState(),BlockBehaviour.Properties.copy(source))));}
    static void slab(String id,Block source){REPLACE.put(source,add(id,()->new SlabBlock(BlockBehaviour.Properties.copy(source))));}
    static {
        cube("white_jade",Blocks.SMOOTH_QUARTZ);cube("jade_bricks",Blocks.QUARTZ_BRICKS);cube("jade_carved",Blocks.CHISELED_QUARTZ_BLOCK);
        cube("plaster",Blocks.CALCITE);cube("grey_bricks",Blocks.STONE_BRICKS);cube("grey_carved",Blocks.CHISELED_STONE_BRICKS);
        cube("qingwa",Blocks.DARK_PRISMARINE);cube("roof_board",Blocks.DEEPSLATE_TILES);
        cube("cedar_boards",Blocks.SPRUCE_PLANKS);cube("dark_boards",Blocks.DARK_OAK_PLANKS);
        cube("timber",Blocks.STRIPPED_DARK_OAK_LOG);cube("red_timber",Blocks.STRIPPED_MANGROVE_LOG);
        cube("grey_paver",Blocks.POLISHED_DEEPSLATE);cube("stone_paver",Blocks.SMOOTH_STONE);cube("grey_plinth",Blocks.POLISHED_ANDESITE);
        cube("gilt_carved",Blocks.CHISELED_SANDSTONE);cube("jade_light",Blocks.SEA_LANTERN);cube("warm_light",Blocks.SHROOMLIGHT);
        for(Block b:List.of(Blocks.QUARTZ_BLOCK,Blocks.QUARTZ_PILLAR))REPLACE.put(b,ALL.get("white_jade"));
        stair("jade_stairs",Blocks.QUARTZ_STAIRS);slab("jade_slab",Blocks.QUARTZ_SLAB);slab("jade_smooth_slab",Blocks.SMOOTH_QUARTZ_SLAB);
        stair("grey_stairs",Blocks.STONE_BRICK_STAIRS);slab("grey_slab",Blocks.STONE_BRICK_SLAB);
        stair("qingwa_stairs",Blocks.DARK_PRISMARINE_STAIRS);slab("qingwa_slab",Blocks.DARK_PRISMARINE_SLAB);
        stair("roof_stairs",Blocks.DEEPSLATE_TILE_STAIRS);slab("roof_slab",Blocks.DEEPSLATE_TILE_SLAB);
        stair("cedar_stairs",Blocks.SPRUCE_STAIRS);slab("cedar_slab",Blocks.SPRUCE_SLAB);
        stair("dark_stairs",Blocks.DARK_OAK_STAIRS);slab("dark_slab",Blocks.DARK_OAK_SLAB);
        var rail=add("jade_railing",()->new FenceBlock(stone()));REPLACE.put(Blocks.STONE_BRICK_WALL,rail);
        var wood=add("carved_railing",()->new FenceBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_FENCE)));
        REPLACE.put(Blocks.SPRUCE_FENCE,wood);REPLACE.put(Blocks.DARK_OAK_FENCE,wood);
        REPLACE.put(Blocks.SPRUCE_DOOR,add("carved_door",()->new DoorBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_DOOR),BlockSetType.SPRUCE)));
        REPLACE.put(Blocks.DARK_OAK_DOOR,ALL.get("carved_door"));
        REPLACE.put(Blocks.LANTERN,add("palace_lantern",()->new LanternBlock(BlockBehaviour.Properties.copy(Blocks.LANTERN).lightLevel(s->14))));
        add("lamp_base",()->new Detail(0));add("lamp_shaft",()->new Detail(1));add("lamp_crown",()->new Detail(2));
        add("garden_lamp",()->new Detail(3));add("lotus_lamp",()->new Detail(4));add("lotus",()->new Detail(5));
    }
    public static void init(){}
    public static Block get(String id){return ALL.get(id).get();}
    public static BlockState state(String id){return get(id).defaultBlockState();}
    @SuppressWarnings({"rawtypes","unchecked"}) public static BlockState material(BlockState raw){
        if(LEGACY.get())return raw;var ref=REPLACE.get(raw.getBlock());if(ref==null)return raw;BlockState out=ref.get().defaultBlockState();
        for(var e:raw.getValues().entrySet())if(out.hasProperty(e.getKey()))out=out.setValue((Property)e.getKey(),(Comparable)e.getValue());
        if(raw.getBlock() instanceof WallBlock)for(Direction d:Direction.Plane.HORIZONTAL){var p=PipeBlock.PROPERTY_BY_DIRECTION.get(d);out=out.setValue(p,true);}
        return out;
    }
    public static CityPlan.Sink wrap(CityPlan.Sink target){return new CityPlan.Sink(){
        public void block(int x,int y,int z,BlockState s){target.block(x,y,z,material(s));}
        public void entity(CompoundTag n){target.entity(n);}public void room(String id,int x,int y,int z,int w,int d,int f){target.room(id,x,y,z,w,d,f);}
        public void stairway(int x,int y,int z,int w,int n){target.stairway(x,y,z,w,n);}public void fixture(int x,int y,int z){target.fixture(x,y,z);}
    };}
    public static void lamp(CityPlan.Sink s,int x,int y,int z){
        s.block(x,y,z,state("lamp_base"));s.block(x,y+1,z,state("lamp_shaft"));s.block(x,y+2,z,state("lamp_shaft"));s.block(x,y+3,z,state("lamp_crown"));
    }
    public static final class Detail extends Block {
        final int kind;
        Detail(int k){super(stone().noOcclusion().lightLevel(s->k==2?15:k==3?13:k==4?10:0));kind=k;}
        @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(kind){
            case 0->Shapes.or(box(2,0,2,14,4,14),box(4,4,4,12,9,12),box(6,9,6,10,16,10));
            case 1->box(6,0,6,10,16,10);case 2->Shapes.or(box(6,0,6,10,16,10),box(2,3,2,14,15,14));
            case 3->Shapes.or(box(2,0,2,14,3,14),box(4,3,4,12,12,12),box(2,12,2,14,14,14));
            default->box(2,0,2,14,4,14);
        };}
        @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return kind>=4?Shapes.empty():getShape(s,l,p,c);}
    }
}
