package cn.duskrain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Room-specific arrangements with a reserved three-block aisle and the real staircase envelope. */
public final class Interiors {
    static final BlockState WOOD=Blocks.DARK_OAK_PLANKS.defaultBlockState();
    public static void paint(CityPlan.Sink sink,CityPlan.Building owner,int x,int y,int z,int w,int d,int floor,int floors,boolean open){
        if(w<4||d<4)return;
        CityPlan.Sink s=(xx,yy,zz,state)->{int dx=xx-x,dz=zz-z;
            if(Math.abs(dx)>=w||Math.abs(dz)>=d||Math.abs(dx)<=1||Math.abs(xx-owner.x())<=1||yy<=y||yy>y+4)return;
            int originalW=w+floor,originalD=d+floor;
            if(floors>1&&dx>=-originalW+2&&dx<=-originalW+7&&dz>=-originalD+2&&dz<=-originalD+14)return;
            sink.block(xx,yy,zz,state);
        };
        String id=owner.id();boolean residence=owner.home()||id.equals("inn");
        if(open){if(id.equals("market")){cabinet(s,x+w-2,y+1,z-d+2,3,false);desk(s,x-w+2,y+1,z+d-2,2);s.block(x+w-2,y+1,z+2,Blocks.COMPOSTER.defaultBlockState());}else{bench(s,x-w+1,y+1,z,Direction.EAST);bench(s,x+w-1,y+1,z,Direction.WEST);s.block(x+w-2,y+1,z-d+2,Blocks.POTTED_AZALEA.defaultBlockState());}return;}
        if(residence){
            bed(s,x+w-3,y+1,z-d+2,floor%2==0?Blocks.CYAN_BED:Blocks.WHITE_BED);
            s.block(x+w-1,y+1,z-d+3,Blocks.BARREL.defaultBlockState());s.block(x+w-1,y+2,z-d+3,Blocks.POTTED_FERN.defaultBlockState());
            // A lattice partition separates sleeping from tea/working space; the center aisle stays open.
            for(int dx=2;dx<w-1;dx++)for(int h=1;h<=3;h++)s.block(x+dx,y+h,z-d+5,Decor.LATTICE.get().defaultBlockState());
            desk(s,x+3,y+1,z+2,Math.min(3,w-4));s.block(x+w-2,y+1,z+2,Blocks.CRAFTING_TABLE.defaultBlockState());
            cabinet(s,x+2,y+1,z+d-1,Math.max(1,w-4),false);
            if(floors==1){cabinet(s,x-w+1,y+1,z-d+1,Math.min(4,w-3),false);desk(s,x-w+2,y+1,z+1,Math.max(1,w-4));}
            if(id.equals("inn")&&floor>0){bed(s,x-w+2,y+1,z+d-4,Blocks.RED_BED);screen(s,x-w+2,y+1,z+1,Math.max(2,w-4));}
        }else if(id.equals("study")||id.equals("library")||id.equals("quests")||id.equals("guildhall")){
            cabinet(s,x+2,y+1,z-d+1,Math.max(1,w-3),true);
            for(int dz=-d+4;dz<=d-3;dz+=5){desk(s,x+3,y+1,z+dz,Math.max(1,Math.min(3,w-4)));if(floors==1&&w>=9)desk(s,x-w+2,y+1,z+dz,3);}
            if(w>=9)screen(s,x+w-3,y+1,z+d-3,2);
            s.block(x+w-2,y+1,z+d-2,Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING,Direction.NORTH));
        }else if(id.equals("alchemy")){
            cabinet(s,x+2,y+1,z-d+1,Math.max(1,w-3),false);
            for(int dz=-d+3;dz<=d-2;dz+=4){s.block(x+w-2,y+1,z+dz,Blocks.SMOOTH_STONE.defaultBlockState());s.block(x+w-2,y+2,z+dz,Blocks.BREWING_STAND.defaultBlockState());s.block(x+w-3,y+1,z+dz,Blocks.COMPOSTER.defaultBlockState());s.block(x+w-3,y+2,z+dz,Blocks.POTTED_FERN.defaultBlockState());}
            desk(s,x+3,y+1,z+2,3);screen(s,x+2,y+1,z+d-2,Math.min(4,w-3));
        }else if(id.equals("forge")){
            for(int dz=-d+3;dz<d-2;dz+=4){s.block(x+w-2,y+1,z+dz,Blocks.SMITHING_TABLE.defaultBlockState());s.block(x+w-3,y+1,z+dz,Blocks.GRINDSTONE.defaultBlockState());s.block(x+w-2,y+2,z+dz,Blocks.IRON_BARS.defaultBlockState());}
            cabinet(s,x-w+2,y+1,z+d-1,Math.max(1,w-5),false);s.block(x+3,y+1,z+1,Blocks.ANVIL.defaultBlockState());screen(s,x+5,y+1,z+3,4);
        }else if(id.equals("palace")){
            for(int dz=-d+5;dz<d-2;dz+=5)for(int side:new int[]{-1,1}){desk(s,x+side*Math.min(7,w-3),y+1,z+dz,2);s.block(x+side*(w-2),y+1,z+dz,Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());s.block(x+side*(w-2),y+2,z+dz,Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState());}
            cabinet(s,x+2,y+1,z+d-1,Math.min(6,w-3),true);
        }
        // Low, wall-side lamps keep rooms bright without hanging into the walking volume.
        for(int side:new int[]{-1,1}){s.block(x+side*(w-2),y+1,z+d-2,Blocks.CHISELED_SANDSTONE.defaultBlockState());s.block(x+side*(w-2),y+2,z+d-2,Decor.LANTERN.get().defaultBlockState());}
    }
    static void desk(CityPlan.Sink s,int x,int y,int z,int n){if(n<1)return;for(int i=0;i<n;i++){s.block(x+i,y,z,Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP));s.block(x+i,y,z+1,CityPlan.stairs(Blocks.SPRUCE_STAIRS,Direction.SOUTH,false));if(!Architecture.LEGACY.get())s.block(x+i,y+1,z+1,Blocks.AIR.defaultBlockState());}s.block(x,y+1,z,Blocks.FLOWER_POT.defaultBlockState());if(n>1)s.block(x+n-1,y+1,z,Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT,true));}
    static void cabinet(CityPlan.Sink s,int x,int y,int z,int n,boolean books){for(int i=0;i<n;i++){s.block(x+i,y,z,books?Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.SOUTH).setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i%6),true):Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,Direction.SOUTH));s.block(x+i,y+1,z,books?Blocks.BOOKSHELF.defaultBlockState():Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,Direction.SOUTH));s.block(x+i,y+2,z,Blocks.DARK_OAK_SLAB.defaultBlockState());}}
    static void screen(CityPlan.Sink s,int x,int y,int z,int n){for(int i=0;i<n;i++)for(int h=0;h<3;h++)s.block(x+i,y+h,z,h==0?WOOD:Decor.LATTICE.get().defaultBlockState());}
    static void bench(CityPlan.Sink s,int x,int y,int z,Direction facing){for(int dz=-1;dz<=1;dz++)s.block(x,y,z+dz,CityPlan.stairs(Blocks.SPRUCE_STAIRS,facing,false));}
    static void bed(CityPlan.Sink s,int x,int y,int z,Block bed){s.block(x,y,z,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT));s.block(x,y,z-1,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.HEAD));}
}
