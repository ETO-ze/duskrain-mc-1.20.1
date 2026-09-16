package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.*;

public final class Decor {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,DuskRain.ID);
    static RegistryObject<Block> part(String name,int kind){RegistryObject<Block> b=BLOCKS.register(name,()->new Part(kind));Content.ITEMS.register(name,()->new BlockItem(b.get(),new Item.Properties()));return b;}
    public static final RegistryObject<Block> LATTICE=part("lattice_window",0),WADANG=part("roof_end",1),RIDGE=part("ridge_beast",2),LANTERN=part("jade_lantern",3),SCREEN=part("ink_screen",4);
    public static final RegistryObject<Block> EXPLORER=BLOCKS.register("exploration_torch",()->new TorchBlock(BlockBehaviour.Properties.copy(Blocks.TORCH).lightLevel(s->15),net.minecraft.core.particles.ParticleTypes.END_ROD));
    static {Content.ITEMS.register("exploration_torch",()->new BlockItem(EXPLORER.get(),new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));}
    public static final RegistryObject<Block> EAVE=part("eave_tip",5),RIDGE_TILE=part("ridge_tile",6),DOUGONG=part("dougong",7);
    public static class Part extends HorizontalDirectionalBlock {
        final int kind;
        Part(int k){super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion().lightLevel(s->k==3?15:0));kind=k;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(kind){case 5->Block.box(0,0,0,16,16,16);case 6->Shapes.or(Block.box(0,0,0,16,3,16),s.getValue(FACING).getAxis()==Direction.Axis.Z?Block.box(0,3,4,16,16,12):Block.box(4,3,0,12,16,16));case 7->Shapes.or(Block.box(5,0,5,11,8,11),Block.box(0,8,0,16,16,16));case 1->Block.box(0,4,0,16,16,16);case 2->Block.box(3,0,3,13,15,13);case 3->Block.box(3,0,3,13,16,13);case 4->s.getValue(FACING).getAxis()==Direction.Axis.Z?Block.box(0,0,6,16,16,10):Block.box(6,0,0,10,16,16);default->Shapes.block();};}
    }
}
